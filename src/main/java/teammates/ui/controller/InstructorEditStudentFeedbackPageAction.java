package teammates.ui.controller;

import java.util.List;

import teammates.common.datatransfer.StudentAttributes;
import teammates.common.datatransfer.TeamDetailsBundle;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.util.Assumption;
import teammates.common.util.Const;
import teammates.common.util.StatusMessage;
import teammates.common.util.StatusMessageColor;
import teammates.logic.api.GateKeeper;

public class InstructorEditStudentFeedbackPageAction extends Action {

    @Override
    protected ActionResult execute() throws EntityDoesNotExistException {
        String courseId = getRequestParamValue(Const.ParamsNames.COURSE_ID);
        String moderatedEntityIdentifier = getRequestParamValue(Const.ParamsNames.FEEDBACK_SESSION_MODERATED_PERSON);
        
        StudentAttributes studentUnderModeration = logic.getStudentForEmail(courseId, moderatedEntityIdentifier);
        
        if (studentUnderModeration == null) {
            List<TeamDetailsBundle> teams = logic.getTeamsForCourse(courseId);
            boolean isTeam = false;
            
            for (TeamDetailsBundle team : teams) {
                if (team.name.equals(moderatedEntityIdentifier)) {
                    isTeam = true;
                    studentUnderModeration = team.students.get(0);
                    break;
                }
            }
            
            if (!isTeam) {
                throw new EntityDoesNotExistException("An entity with the identifier "
                        + moderatedEntityIdentifier + " does not exist in " + courseId
                        + ".");
            }
        }
        
        String feedbackSessionName = getRequestParamValue(Const.ParamsNames.FEEDBACK_SESSION_NAME);
        
        new GateKeeper().verifyAccessible(logic.getInstructorForGoogleId(courseId, account.googleId),
                logic.getFeedbackSession(feedbackSessionName, courseId),
                false, studentUnderModeration.section,
                Const.ParamsNames.INSTRUCTOR_PERMISSION_MODIFY_SESSION_COMMENT_IN_SECTIONS);
        
        String moderatedQuestionId = getRequestParamValue(Const.ParamsNames.FEEDBACK_SESSION_MODERATED_QUESTION_ID);


        Assumption.assertNotNull(String.format(Const.StatusMessages.NULL_POST_PARAMETER_MESSAGE,
                                               Const.ParamsNames.COURSE_ID),
                                 courseId);
        Assumption.assertNotNull(String.format(Const.StatusMessages.NULL_POST_PARAMETER_MESSAGE,
                                               Const.ParamsNames.FEEDBACK_SESSION_NAME),
                                 feedbackSessionName);
        Assumption.assertNotNull(String.format(Const.StatusMessages.NULL_POST_PARAMETER_MESSAGE,
                                               Const.ParamsNames.FEEDBACK_SESSION_MODERATED_PERSON),
                                 moderatedEntityIdentifier);
        
        FeedbackSubmissionEditPageData data = new FeedbackSubmissionEditPageData(account, student);
        
        data.bundle = logic.getFeedbackSessionQuestionsBundleForStudent(
                feedbackSessionName, courseId, studentUnderModeration.email);
        
        Assumption.assertNotNull(data.bundle);
        
        data.setSessionOpenForSubmission(true);
        data.setModeration(true);
        data.setHeaderHidden(true);
        data.setStudentToViewPageAs(studentUnderModeration);
        data.setSubmitAction(Const.ActionURIs.INSTRUCTOR_EDIT_STUDENT_FEEDBACK_SAVE);
        
        if (moderatedQuestionId != null) {
            data.setModeratedQuestionId(moderatedQuestionId);
        }

        statusToAdmin = "Moderating feedback session for student (" + studentUnderModeration.email + ")<br>"
                + "Session Name: " + feedbackSessionName + "<br>"
                + "Course ID: " + courseId;
        
        data.bundle.hideUnmoderatableQuestions();
        statusToUser.add(new StatusMessage(Const.FEEDBACK_SESSION_QUESTIONS_HIDDEN,
                StatusMessageColor.WARNING));
        
        data.init(courseId);
        
        return createShowPageResult(Const.ViewURIs.STUDENT_FEEDBACK_SUBMISSION_EDIT, data);
    }
}
