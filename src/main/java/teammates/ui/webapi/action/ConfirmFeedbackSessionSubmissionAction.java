package teammates.ui.webapi.action;

import java.time.Instant;

import org.apache.http.HttpStatus;

import teammates.common.datatransfer.attributes.FeedbackSessionAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.exception.InvalidHttpParameterException;
import teammates.common.util.Const;
import teammates.common.util.EmailSendingStatus;
import teammates.common.util.EmailWrapper;
import teammates.logic.api.EmailGenerator;

/**
 * Confirm the submission of a feedback session.
 */
public class ConfirmFeedbackSessionSubmissionAction extends BasicFeedbackSubmissionAction {

    @Override
    protected AuthType getMinAuthLevel() {
        return AuthType.PUBLIC;
    }

    @Override
    public void checkSpecificAccessControl() {
        String courseId = getNonNullRequestParamValue(Const.ParamsNames.COURSE_ID);
        String feedbackSessionName = getNonNullRequestParamValue(Const.ParamsNames.FEEDBACK_SESSION_NAME);
        FeedbackSessionAttributes feedbackSession = logic.getFeedbackSession(feedbackSessionName, courseId);

        verifySessionOpenExceptForModeration(feedbackSession);
        verifyNotPreview();

        Intent intent = Intent.valueOf(getNonNullRequestParamValue(Const.ParamsNames.INTENT));
        switch (intent) {
        case STUDENT_SUBMISSION:
            StudentAttributes studentAttributes = getStudentOfCourseFromRequest(feedbackSession.getCourseId());
            checkAccessControlForStudentFeedbackSubmission(studentAttributes, feedbackSession);
            break;
        case INSTRUCTOR_SUBMISSION:
            InstructorAttributes instructorAttributes = getInstructorOfCourseFromRequest(feedbackSession.getCourseId());
            checkAccessControlForInstructorFeedbackSubmission(instructorAttributes, feedbackSession);
            break;
        default:
            throw new InvalidHttpParameterException("Unknown intent " + intent);
        }
    }

    @Override
    public ActionResult execute() {
        String courseId = getNonNullRequestParamValue(Const.ParamsNames.COURSE_ID);
        String feedbackSessionName = getNonNullRequestParamValue(Const.ParamsNames.FEEDBACK_SESSION_NAME);
        FeedbackSessionAttributes feedbackSession = logic.getFeedbackSession(feedbackSessionName, courseId);
        boolean isSubmissionEmailConfirmationEmailRequested =
                getBooleanRequestParamValue(Const.ParamsNames.SEND_SUBMISSION_EMAIL);
        Intent intent = Intent.valueOf(getNonNullRequestParamValue(Const.ParamsNames.INTENT));

        EmailWrapper email = null;
        switch (intent) {
        case STUDENT_SUBMISSION:
            StudentAttributes studentAttributes = getStudentOfCourseFromRequest(feedbackSession.getCourseId());
            if (isSubmissionEmailConfirmationEmailRequested) {
                email = new EmailGenerator().generateFeedbackSubmissionConfirmationEmailForStudent(
                            feedbackSession, studentAttributes, Instant.now());
            }
            break;
        case INSTRUCTOR_SUBMISSION:
            InstructorAttributes instructorAttributes = getInstructorOfCourseFromRequest(feedbackSession.getCourseId());
            if (isSubmissionEmailConfirmationEmailRequested) {
                email = new EmailGenerator().generateFeedbackSubmissionConfirmationEmailForInstructor(
                        feedbackSession, instructorAttributes, Instant.now());
            }
            break;
        default:
            throw new InvalidHttpParameterException("Unknown intent " + intent);
        }

        if (email != null) {
            EmailSendingStatus status = emailSender.sendEmail(email);
            if (!status.isSuccess()) {
                return new JsonResult("Confirmation email is failed to be sent.", HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }

        return new JsonResult("Submission confirmed");
    }
}
