package teammates.ui.webapi.action;

import org.apache.http.HttpStatus;

import teammates.common.datatransfer.attributes.FeedbackQuestionAttributes;
import teammates.common.datatransfer.attributes.FeedbackResponseAttributes;
import teammates.common.datatransfer.attributes.FeedbackResponseCommentAttributes;
import teammates.common.datatransfer.attributes.FeedbackSessionAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.EntityNotFoundException;
import teammates.common.exception.InvalidHttpParameterException;
import teammates.common.util.Const;
import teammates.ui.webapi.output.FeedbackResponseCommentData;

/**
 * Get all the comments given by the user for a response.
 */
public class GetFeedbackResponseCommentAction extends BasicCommentSubmissionAction {

    @Override
    protected AuthType getMinAuthLevel() {
        return AuthType.PUBLIC;
    }

    @Override
    public void checkSpecificAccessControl() {
        String feedbackResponseId = getNonNullRequestParamValue(Const.ParamsNames.FEEDBACK_RESPONSE_ID);
        FeedbackResponseAttributes feedbackResponseAttributes = logic.getFeedbackResponse(feedbackResponseId);

        if (feedbackResponseAttributes == null) {
            throw new EntityNotFoundException(new EntityDoesNotExistException("The feedback response does not exist."));
        }
        String courseId = feedbackResponseAttributes.courseId;
        FeedbackSessionAttributes feedbackSession =
                logic.getFeedbackSession(feedbackResponseAttributes.getFeedbackSessionName(),
                        feedbackResponseAttributes.getCourseId());
        FeedbackQuestionAttributes feedbackQuestion =
                logic.getFeedbackQuestion(feedbackResponseAttributes.getFeedbackQuestionId());

        verifyInstructorCanSeeQuestionIfInModeration(feedbackQuestion);
        verifyNotPreview();

        Intent intent = Intent.valueOf(getNonNullRequestParamValue(Const.ParamsNames.INTENT));
        switch (intent) {
        case STUDENT_SUBMISSION:
            gateKeeper.verifyAnswerableForStudent(feedbackQuestion);
            StudentAttributes student = getStudentOfCourseFromRequest(courseId);
            checkAccessControlForStudentFeedbackSubmission(student, feedbackSession);
            break;
        case INSTRUCTOR_SUBMISSION:
            gateKeeper.verifyAnswerableForInstructor(feedbackQuestion);
            InstructorAttributes instructor = getInstructorOfCourseFromRequest(courseId);
            checkAccessControlForInstructorFeedbackSubmission(instructor, feedbackSession);
            break;
        default:
            throw new InvalidHttpParameterException("Unknown intent " + intent);
        }
    }

    @Override
    public ActionResult execute() {
        String feedbackResponseId = getNonNullRequestParamValue(Const.ParamsNames.FEEDBACK_RESPONSE_ID);
        Intent intent = Intent.valueOf(getNonNullRequestParamValue(Const.ParamsNames.INTENT));

        switch (intent) {
        case STUDENT_SUBMISSION:
        case INSTRUCTOR_SUBMISSION:
            FeedbackResponseCommentAttributes comment =
                    logic.getFeedbackResponseCommentForResponseFromParticipant(feedbackResponseId);
            if (comment == null) {
                return new JsonResult("Comment not found", HttpStatus.SC_NOT_FOUND);
            }
            return new JsonResult(new FeedbackResponseCommentData(comment));
        default:
            throw new InvalidHttpParameterException("Unknown intent " + intent);
        }
    }

}
