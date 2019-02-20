package teammates.ui.webapi.action;

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
import teammates.ui.webapi.output.FeedbackResponseData;
import teammates.ui.webapi.request.FeedbackResponseCreateRequest;

/**
 * Create a feedback response.
 */
public class CreateFeedbackResponseAction extends BasicFeedbackSubmissionAction {

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
            gateKeeper.verifyAnswerableForInstructor(feedbackQuestion);
            InstructorAttributes instructorAttributes = getInstructorOfCourseFromRequest(feedbackQuestion.getCourseId());
            recipientsOfTheQuestion =
                    logic.getRecipientsOfQuestionForInstructor(feedbackQuestion, instructorAttributes.getEmail());
            break;
        default:
            throw new InvalidHttpParameterException("Unknown intent " + intent);
        }

        FeedbackResponseCreateRequest createRequest = getAndValidateRequestBody(FeedbackResponseCreateRequest.class);
        if (!recipientsOfTheQuestion.containsKey(createRequest.getRecipientIdentifier())) {
            throw new UnauthorizedAccessException("The recipient is not a valid recipient of the question");
        }
    }

    @Override
    public ActionResult execute() {
        Intent intent = Intent.valueOf(getNonNullRequestParamValue(Const.ParamsNames.INTENT));
        String feedbackQuestionId = getNonNullRequestParamValue(Const.ParamsNames.FEEDBACK_QUESTION_ID);
        FeedbackQuestionAttributes feedbackQuestion = logic.getFeedbackQuestion(feedbackQuestionId);

        FeedbackResponseAttributes.Builder feedbackResponseBuilder = FeedbackResponseAttributes.builder();
        switch (intent) {
        case STUDENT_SUBMISSION:
            StudentAttributes studentAttributes = getStudentOfCourseFromRequest(feedbackQuestion.getCourseId());
            feedbackResponseBuilder
                    .withGiver(feedbackQuestion.getGiverType() == FeedbackParticipantType.TEAMS
                            ? studentAttributes.getTeam() : studentAttributes.getEmail())
                    .withGiverSection(studentAttributes.getSection());
            break;
        case INSTRUCTOR_SUBMISSION:
            InstructorAttributes instructorAttributes = getInstructorOfCourseFromRequest(feedbackQuestion.getCourseId());
            feedbackResponseBuilder
                    .withGiver(instructorAttributes.getEmail())
                    .withGiverSection(Const.DEFAULT_SECTION);
            break;
        default:
            throw new InvalidHttpParameterException("Unknown intent " + intent);
        }

        FeedbackResponseCreateRequest createRequest = getAndValidateRequestBody(FeedbackResponseCreateRequest.class);
        feedbackResponseBuilder
                .withRecipient(createRequest.getRecipientIdentifier())
                .withRecipientSection(getRecipientSection(feedbackQuestion.getCourseId(),
                        feedbackQuestion.getRecipientType(), createRequest.getRecipientIdentifier()))
                .withCourseId(feedbackQuestion.getCourseId())
                .withFeedbackSessionName(feedbackQuestion.getFeedbackSessionName())
                .withFeedbackQuestionId(feedbackQuestion.getId())
                .withResponseDetails(createRequest.getResponseDetails());

        FeedbackResponseAttributes feedbackResponse = feedbackResponseBuilder.build();
        validResponseOfQuestion(feedbackQuestion, feedbackResponse);
        try {
            logic.createFeedbackResponses(Arrays.asList(feedbackResponse));
        } catch (InvalidParametersException e) {
            throw new InvalidHttpRequestBodyException(e.getMessage(), e);
        }

        FeedbackResponseAttributes createdFeedbackResponse = logic.getFeedbackResponse(
                feedbackQuestion.getId() + "%" + feedbackResponse.giver + "%" + feedbackResponse.recipient);
        return new JsonResult(new FeedbackResponseData(createdFeedbackResponse));
    }

}
