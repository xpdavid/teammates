package teammates.ui.webapi.action;

import teammates.common.datatransfer.FeedbackParticipantType;
import teammates.common.datatransfer.attributes.FeedbackQuestionAttributes;
import teammates.common.datatransfer.attributes.FeedbackResponseAttributes;
import teammates.common.datatransfer.attributes.FeedbackResponseCommentAttributes;
import teammates.common.datatransfer.attributes.FeedbackSessionAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.exception.InvalidHttpParameterException;
import teammates.common.util.Const;

/**
 * Deletes a feedback response comment.
 */
public class DeleteFeedbackResponseCommentAction extends BasicCommentSubmissionAction {

    @Override
    protected AuthType getMinAuthLevel() {
        return AuthType.PUBLIC;
    }

    @Override
    public void checkSpecificAccessControl() {
        long feedbackResponseCommentId = getLongRequestParamValue(Const.ParamsNames.FEEDBACK_RESPONSE_COMMENT_ID);
        FeedbackResponseCommentAttributes frc = logic.getFeedbackResponseComment(feedbackResponseCommentId);
        if (frc == null) {
            return;
        }
        FeedbackSessionAttributes session = logic.getFeedbackSession(frc.getFeedbackSessionName(), frc.getCourseId());
        FeedbackQuestionAttributes question = logic.getFeedbackQuestion(frc.getFeedbackQuestionId());

        Intent intent = Intent.valueOf(getNonNullRequestParamValue(Const.ParamsNames.INTENT));
        String courseId = frc.courseId;

        switch (intent) {
        case STUDENT_SUBMISSION:
            StudentAttributes student = getStudentOfCourseFromRequest(courseId);

            gateKeeper.verifyAnswerableForStudent(question);
            verifySessionOpenExceptForModeration(session);
            verifyInstructorCanSeeQuestionIfInModeration(question);
            verifyNotPreview();

            checkAccessControlForStudentFeedbackSubmission(student, session);
            gateKeeper.verifyOwnership(frc,
                    question.getGiverType() == FeedbackParticipantType.TEAMS
                            ? student.getTeam() : student.getEmail());
            break;
        case INSTRUCTOR_SUBMISSION:
            InstructorAttributes instructorAsFeedbackParticipant = getInstructorOfCourseFromRequest(courseId);

            gateKeeper.verifyAnswerableForInstructor(question);
            verifySessionOpenExceptForModeration(session);
            verifyInstructorCanSeeQuestionIfInModeration(question);
            verifyNotPreview();

            checkAccessControlForInstructorFeedbackSubmission(instructorAsFeedbackParticipant, session);
            gateKeeper.verifyOwnership(frc, instructorAsFeedbackParticipant.getEmail());
            break;
        case INSTRUCTOR_RESULT:
            gateKeeper.verifyLoggedInUserPrivileges();
            InstructorAttributes instructor = logic.getInstructorForGoogleId(courseId, userInfo.getId());

            if (instructor != null && frc.commentGiver.equals(instructor.email)) { // giver, allowed by default
                return;
            }

            FeedbackResponseAttributes response = logic.getFeedbackResponse(frc.getFeedbackResponseId());
            gateKeeper.verifyAccessible(instructor, session, response.giverSection,
                    Const.ParamsNames.INSTRUCTOR_PERMISSION_MODIFY_SESSION_COMMENT_IN_SECTIONS);
            gateKeeper.verifyAccessible(instructor, session, response.recipientSection,
                    Const.ParamsNames.INSTRUCTOR_PERMISSION_MODIFY_SESSION_COMMENT_IN_SECTIONS);
            break;
        default:
            throw new InvalidHttpParameterException("Unknown intent " + intent);
        }
    }

    @Override
    public ActionResult execute() {
        long feedbackResponseCommentId = getLongRequestParamValue(Const.ParamsNames.FEEDBACK_RESPONSE_COMMENT_ID);

        logic.deleteFeedbackResponseComment(feedbackResponseCommentId);

        return new JsonResult("Successfully deleted feedback response comment.");
    }

}
