package teammates.ui.newcontroller;

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
import teammates.common.exception.UnauthorizedAccessException;
import teammates.common.util.Const;

/**
 * Save a feedback response.
 */
public class SaveFeedbackResponse extends BasicFeedbackSubmissionAction {

    @Override
    protected AuthType getMinAuthLevel() {
        return AuthType.PUBLIC;
    }

    @Override
    public void checkSpecificAccessControl() {
        String feedbackResponseId = getNonNullRequestParamValue(Const.ParamsNames.FEEDBACK_RESPONSE_ID);
        FeedbackResponseAttributes feedbackResponse = logic.getFeedbackResponse(feedbackResponseId);
        if (feedbackResponse == null) {
            throw new EntityNotFoundException(new EntityDoesNotExistException("The feedback response does not exist."));
        }
        FeedbackQuestionAttributes feedbackQuestion = logic.getFeedbackQuestion(feedbackResponse.feedbackQuestionId);
        FeedbackSessionAttributes feedbackSession =
                logic.getFeedbackSession(feedbackResponse.feedbackSessionName, feedbackResponse.courseId);

        verifyInstructorCanSeeQuestionIfInModeration(feedbackQuestion);
        verifySessionOpenExceptForModeration(feedbackSession);
        verifyNotPreview();

        Map<String, String> recipientsOfTheQuestion;
        Intent intent = Intent.valueOf(getNonNullRequestParamValue(Const.ParamsNames.INTENT));
        switch (intent) {
        case STUDENT_SUBMISSION:
            gateKeeper.verifyAnswerableForStudent(feedbackQuestion);
            StudentAttributes studentAttributes = getStudentOfCourseFromRequest(feedbackQuestion.getCourseId());
            checkAccessControlOfStudentFeedbackSubmission(studentAttributes, feedbackSession);
            recipientsOfTheQuestion =
                    logic.getRecipientsOfQuestionForStudent(
                            feedbackQuestion, studentAttributes.getEmail(), studentAttributes.getTeam());
            break;
        case INSTRUCTOR_SUBMISSION:
            InstructorAttributes instructorAttributes = getInstructorOfCourseFromRequest(feedbackQuestion.getCourseId());
            checkAccessControlForInstructorFeedbackSubmission(instructorAttributes, feedbackSession);
            recipientsOfTheQuestion =
                    logic.getRecipientsOfQuestionForInstructor(feedbackQuestion, instructorAttributes.getEmail());
            break;
        default:
            throw new InvalidHttpParameterException("Unknown intent " + intent);
        }


        FeedbackResponseInfo.FeedbackResponseSaveRequest saveRequest =
                getAndValidateRequestBody(FeedbackResponseInfo.FeedbackResponseSaveRequest.class);
        if (!recipientsOfTheQuestion.containsKey(saveRequest.getRecipientIdentifier())) {
            throw new UnauthorizedAccessException("The recipient is not a valid recipient of the question");
        }
    }

    @Override
    public ActionResult execute() {
        String feedbackResponseId = getNonNullRequestParamValue(Const.ParamsNames.FEEDBACK_RESPONSE_ID);
        FeedbackResponseAttributes feedbackResponse = logic.getFeedbackResponse(feedbackResponseId);
        FeedbackQuestionAttributes feedbackQuestion = logic.getFeedbackQuestion(feedbackResponse.feedbackQuestionId);

        String giverIdentifier;
        String giverSection;
        Intent intent = Intent.valueOf(getNonNullRequestParamValue(Const.ParamsNames.INTENT));
        switch (intent) {
        case STUDENT_SUBMISSION:
            StudentAttributes studentAttributes = getStudentOfCourseFromRequest(feedbackQuestion.getCourseId());
            giverIdentifier =
                    feedbackQuestion.getGiverType() == FeedbackParticipantType.TEAMS
                            ? studentAttributes.getTeam() : studentAttributes.getEmail();
            giverSection = studentAttributes.getSection();
            break;
        case INSTRUCTOR_SUBMISSION:
            InstructorAttributes instructorAttributes = getInstructorOfCourseFromRequest(feedbackQuestion.getCourseId());
            giverIdentifier = instructorAttributes.getEmail();
            giverSection = Const.DEFAULT_SECTION;
            break;
        default:
            throw new InvalidHttpParameterException("Unknown intent " + intent);
        }

        FeedbackResponseInfo.FeedbackResponseSaveRequest saveRequest =
                getAndValidateRequestBody(FeedbackResponseInfo.FeedbackResponseSaveRequest.class);
        feedbackResponse.giver = giverIdentifier;
        feedbackResponse.giverSection = giverSection;
        feedbackResponse.recipient = saveRequest.getRecipientIdentifier();
        feedbackResponse.recipientSection =
                getRecipientSection(feedbackQuestion.getCourseId(),
                        feedbackQuestion.getRecipientType(), saveRequest.getRecipientIdentifier());
        feedbackResponse.setResponseDetails(saveRequest.getResponseDetails());

        validResponseOfQuestion(feedbackQuestion, feedbackResponse);
        try {
            logic.updateFeedbackResponse(feedbackResponse);
        } catch (Exception e) {
            throw new InvalidHttpRequestBodyException(e.getMessage(), e);
        }

        FeedbackResponseAttributes updatedFeedbackResponse =
                logic.getFeedbackResponse(feedbackQuestion.getId(), feedbackResponse.giver, feedbackResponse.recipient);
        return new JsonResult(new FeedbackResponseInfo.FeedbackResponseResponse(updatedFeedbackResponse));
    }

}
