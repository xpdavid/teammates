package teammates.ui.automated;

import teammates.common.datatransfer.attributes.AccountAttributes;
import teammates.common.datatransfer.attributes.CourseAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.exception.TeammatesException;
import teammates.common.util.Assumption;
import teammates.common.util.Const.ParamsNames;
import teammates.common.util.EmailWrapper;
import teammates.logic.api.EmailGenerator;

/**
 * Task queue worker action: sends registration email for an instructor of a course.
 */
public class InstructorCourseJoinEmailWorkerAction extends AutomatedAction {

    @Override
    protected String getActionMessage() {
        return null;
    }

    @Override
    public void execute() {
        String inviterId = getNonNullRequestParamValue(ParamsNames.INVITER_ID);
        String courseId = getNonNullRequestParamValue(ParamsNames.COURSE_ID);
        String instructorEmail = getNonNullRequestParamValue(ParamsNames.INSTRUCTOR_EMAIL);

        AccountAttributes inviter = logic.getAccount(inviterId);
        Assumption.assertNotNull(inviter);

        CourseAttributes course = logic.getCourse(courseId);
        Assumption.assertNotNull(course);

        // The instructor is queried using the `id`of instructor as it ensures that the
        // instructor is retrieved (and not null) even if the index building for
        // saving the new instructor takes more time in GAE.
        // The instructor `id` can be constructed back using (instructorEmail%courseId)
        // because instructors' email cannot be changed before joining the course.
        InstructorAttributes instructor = logic.getInstructorById(courseId, instructorEmail);
        Assumption.assertNotNull(instructor);

        EmailWrapper email = new EmailGenerator()
                .generateInstructorCourseJoinEmail(inviter, instructor, course);
        try {
            emailSender.sendEmail(email);
        } catch (Exception e) {
            Assumption.fail("Unexpected error while sending email" + TeammatesException.toStringWithStackTrace(e));
        }
    }

}
