package teammates.ui.newcontroller;

import java.util.Arrays;
import java.util.Map;

import teammates.common.datatransfer.FeedbackParticipantType;
import teammates.common.datatransfer.attributes.FeedbackQuestionAttributes;
import teammates.common.datatransfer.attributes.FeedbackResponseAttributes;
import teammates.common.datatransfer.attributes.FeedbackSessionAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.EntityNotFoundException;
import teammates.common.exception.InvalidHttpParameterException;
import teammates.common.exception.InvalidHttpRequestBodyException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.exception.UnauthorizedAccessException;
import teammates.common.util.Const;

/**
 * Create a feedback response.
 */
public class CreateFeedbackResponse extends BasicFeedbackSubmissionAction {

    @Override
    protected AuthType getMinAuthLevel() {
        return AuthType.PUBLIC;
    }

    @Override
    public void checkSpecificAccessControl() {
        String feedbackQuestionId = getNonNullRequestParamValue(Const.ParamsNames.FEEDBACK_QUESTION_ID);
        FeedbackQuestionAttributes feedbackQuestion = logic.getFeedbackQuestion(feedbackQuestionId);
        if (feedbackQuestion == null) {
            throw new EntityNotFoundException(new EntityDoesNotExistException("The feedback question does not exist."));
        }
        FeedbackSessionAttributes feedbackSession =
                logic.getFeedbackSession(feedbackQuestion.getFeedbackSessionName(), feedbackQuestion.getCourseId());

        verifyInstructorCanSeeQuestionIfInModeration(feedbackQuestion);
        verifySessionOpenExceptForModeration(feedbackSession);
        verifyNotPreview();

        Map<String, String> recipientsOfTheQuestion;
        Intent intent = Intent.valueOf(getNonNullRequestParamValue(Const.ParamsNames.INTENT));
        switch (intent) {
        case STUDENT_SUBMISSION:
            gateKeeper.verifyAnswerableForStudent(feedbackQuestion);
            StudentAttributes studentAttributes = getStudentOfCourseFromRequest(feedbackQuestion.getCourseId());
            recipientsOfTheQuestion =
                    logic.getRecipientsOfQuestionForStudent(
                            feedbackQuestion, studentAttributes.getEmail(), studentAttributes.getTeam());
            break;
        case INSTRUCTOR_SUBMISSION:
            InstructorAttributes instructorAttributes = getInstructorOfCourseFromRequest(feedbackQuestion.getCourseId());
            recipientsOfTheQuestion =
                    logic.getRecipientsOfQuestionForInstructor(feedbackQuestion, instructorAttributes.getEmail());
            break;
        default:
            throw new InvalidHttpParameterException("Unknown intent " + intent);
        }

        FeedbackResponseInfo.FeedbackResponseCreateRequest createRequest =
                getAndValidateRequestBody(FeedbackResponseInfo.FeedbackResponseCreateRequest.class);
        if (!recipientsOfTheQuestion.containsKey(createRequest.getRecipientIdentifier())) {
            throw new UnauthorizedAccessException("The recipient is not a valid recipient of the question");
        }
    }

    @Override
    public ActionResult execute() {
        Intent intent = Intent.valueOf(getNonNullRequestParamValue(Const.ParamsNames.INTENT));
        String feedbackQuestionId = getNonNullRequestParamValue(Const.ParamsNames.FEEDBACK_QUESTION_ID);
        FeedbackQuestionAttributes feedbackQuestion = logic.getFeedbackQuestion(feedbackQuestionId);

        // TODO use builder pattern
        FeedbackResponseAttributes feedbackResponse = new FeedbackResponseAttributes();
        switch (intent) {
        case STUDENT_SUBMISSION:
            StudentAttributes studentAttributes = getStudentOfCourseFromRequest(feedbackQuestion.getCourseId());
            feedbackResponse.giver =
                    feedbackQuestion.getGiverType() == FeedbackParticipantType.TEAMS
                            ? studentAttributes.getTeam() : studentAttributes.getEmail();
            feedbackResponse.giverSection = studentAttributes.getSection();
            break;
        case INSTRUCTOR_SUBMISSION:
            InstructorAttributes instructorAttributes = getInstructorOfCourseFromRequest(feedbackQuestion.getCourseId());
            feedbackResponse.giver = instructorAttributes.getEmail();
            feedbackResponse.giverSection = Const.DEFAULT_SECTION;
            break;
        default:
            throw new InvalidHttpParameterException("Unknown intent " + intent);
        }

        FeedbackResponseInfo.FeedbackResponseCreateRequest createRequest =
                getAndValidateRequestBody(FeedbackResponseInfo.FeedbackResponseCreateRequest.class);
        feedbackResponse.recipient = createRequest.getRecipientIdentifier();
        feedbackResponse.recipientSection =
                getRecipientSection(feedbackQuestion.getCourseId(),
                        feedbackQuestion.getRecipientType(), createRequest.getRecipientIdentifier());

        feedbackResponse.courseId = feedbackQuestion.getCourseId();
        feedbackResponse.feedbackSessionName = feedbackQuestion.getFeedbackSessionName();
        feedbackResponse.feedbackQuestionType = feedbackQuestion.getQuestionType();
        feedbackResponse.feedbackQuestionId = feedbackQuestion.getId();

        feedbackResponse.setResponseDetails(createRequest.getResponseDetails());

        validResponseOfQuestion(feedbackQuestion, feedbackResponse);
        try {
            logic.createFeedbackResponses(Arrays.asList(feedbackResponse));
        } catch (InvalidParametersException e) {
            throw new InvalidHttpRequestBodyException(e.getMessage(), e);
        }

        FeedbackResponseAttributes createdFeedbackResponse =
                logic.getFeedbackResponse(feedbackQuestion.getId(), feedbackResponse.giver, feedbackResponse.recipient);
        return new JsonResult(new FeedbackResponseInfo.FeedbackResponseResponse(createdFeedbackResponse));
    }

}
