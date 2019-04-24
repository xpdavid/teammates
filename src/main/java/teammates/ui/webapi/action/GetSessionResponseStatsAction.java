package teammates.ui.webapi.action;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import teammates.common.datatransfer.FeedbackParticipantType;
import teammates.common.datatransfer.attributes.FeedbackQuestionAttributes;
import teammates.common.datatransfer.attributes.FeedbackSessionAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.util.Const;
import teammates.ui.webapi.output.FeedbackSessionStatsData;

/**
 * Action: gets the response stats (submitted / total) of a feedback session.
 */
public class GetSessionResponseStatsAction extends Action {

    @Override
    protected AuthType getMinAuthLevel() {
        return AuthType.LOGGED_IN;
    }

    @Override
    public void checkSpecificAccessControl() {
        if (userInfo.isAdmin) {
            return;
        }

        String courseId = getNonNullRequestParamValue(Const.ParamsNames.COURSE_ID);
        String feedbackSessionName = getNonNullRequestParamValue(Const.ParamsNames.FEEDBACK_SESSION_NAME);
        FeedbackSessionAttributes fsa = logic.getFeedbackSession(feedbackSessionName, courseId);
        InstructorAttributes instructor = logic.getInstructorForGoogleId(courseId, userInfo.getId());
        gateKeeper.verifyAccessible(instructor, fsa);
    }

    @Override
    public ActionResult execute() {
        String courseId = getNonNullRequestParamValue(Const.ParamsNames.COURSE_ID);
        String feedbackSessionName = getNonNullRequestParamValue(Const.ParamsNames.FEEDBACK_SESSION_NAME);
        FeedbackSessionAttributes fsa = logic.getFeedbackSession(feedbackSessionName, courseId);

        Set<String> allEmails = new HashSet<>();

        // check whether there are questions for student to answer
        List<FeedbackQuestionAttributes> questionsUnderSession =
                logic.getFeedbackQuestionsForSession(feedbackSessionName, courseId);
        if (questionsUnderSession.stream().anyMatch(
                question -> question.getGiverType().equals(FeedbackParticipantType.STUDENTS))) {
            // student need to answer the feedback session
            logic.getStudentsForCourse(courseId).forEach(s -> allEmails.add(s.getEmail()));
        }

        // the creator need to answer
        if (questionsUnderSession.stream().anyMatch(
                question -> question.getGiverType().equals(FeedbackParticipantType.SELF))) {
            allEmails.add(fsa.getCreatorEmail());
        }

        // all instructors need to answer
        if (questionsUnderSession.stream().anyMatch(
                question -> question.getGiverType().equals(FeedbackParticipantType.INSTRUCTORS))) {
            logic.getInstructorsForCourse(courseId).forEach(i -> allEmails.add(i.getEmail()));
        }

        Set<String> giverIdentifierSet = logic.getGiverSetThatAnswerFeedbackSession(courseId, feedbackSessionName);

        FeedbackSessionStatsData output =
                new FeedbackSessionStatsData(Sets.intersection(allEmails, giverIdentifierSet).size(), allEmails.size());
        return new JsonResult(output);
    }

}
