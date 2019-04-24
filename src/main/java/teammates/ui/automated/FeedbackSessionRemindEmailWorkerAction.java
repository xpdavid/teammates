package teammates.ui.automated;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import teammates.common.datatransfer.attributes.FeedbackSessionAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.exception.TeammatesException;
import teammates.common.util.Const.ParamsNames;
import teammates.common.util.EmailWrapper;
import teammates.common.util.Logger;

/**
 * Task queue worker action: sends feedback session reminder email to a course.
 */
public class FeedbackSessionRemindEmailWorkerAction extends AutomatedAction {

    private static final Logger log = Logger.getLogger();

    @Override
    public void execute() {
        String feedbackSessionName = getNonNullRequestParamValue(ParamsNames.SUBMISSION_FEEDBACK);
        String courseId = getNonNullRequestParamValue(ParamsNames.SUBMISSION_COURSE);
        String instructorId = getNonNullRequestParamValue(ParamsNames.USER_ID);

        try {
            FeedbackSessionAttributes session = logic.getFeedbackSession(feedbackSessionName, courseId);
            List<StudentAttributes> studentList = logic.getStudentsForCourse(courseId);
            List<InstructorAttributes> instructorList = logic.getInstructorsForCourse(courseId);
            Set<String> giverSetSubmitted = logic.getGiverSetThatAnswerFeedbackSession(courseId, feedbackSessionName);

            InstructorAttributes instructorToNotify = logic.getInstructorForGoogleId(courseId, instructorId);

            List<StudentAttributes> studentsToRemindList = new ArrayList<>();
            for (StudentAttributes student : studentList) {
                if (!giverSetSubmitted.contains(student.getEmail())) {
                    studentsToRemindList.add(student);
                }
            }

            // Filter out instructors who have submitted the feedback session
            List<InstructorAttributes> instructorsToRemindList = new ArrayList<>();
            for (InstructorAttributes instructor : instructorList) {
                if (!giverSetSubmitted.contains(instructor.getEmail())) {
                    instructorsToRemindList.add(instructor);
                }
            }

            List<EmailWrapper> emails = emailGenerator.generateFeedbackSessionReminderEmails(
                    session, studentsToRemindList, instructorsToRemindList, instructorToNotify);
            taskQueuer.scheduleEmailsForSending(emails);
        } catch (Exception e) {
            log.severe("Unexpected error while sending emails: " + TeammatesException.toStringWithStackTrace(e));
        }
    }

}
