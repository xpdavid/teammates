package teammates.ui.webapi.action;

import java.time.ZoneId;

import org.apache.http.HttpStatus;

import teammates.common.datatransfer.attributes.CourseAttributes;
import teammates.common.exception.EntityAlreadyExistsException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.exception.UnauthorizedAccessException;
import teammates.common.util.Const;
import teammates.common.util.FieldValidator;

/**
 * Action: Adds a new course for instructor.
 */
public class AddInstructorCourseAction extends Action {

    @Override
    protected AuthType getMinAuthLevel() {
        return AuthType.LOGGED_IN;
    }

    @Override
    public void checkSpecificAccessControl() {
        if (!userInfo.isInstructor) {
            throw new UnauthorizedAccessException("Instructor privilege is required to access this resource.");
        }
    }

    @Override
    public ActionResult execute() {
        String newCourseTimeZone = getNonNullRequestParamValue(Const.ParamsNames.COURSE_TIME_ZONE);

        FieldValidator validator = new FieldValidator();
        String timeZoneErrorMessage = validator.getInvalidityInfoForTimeZone(newCourseTimeZone);
        if (!timeZoneErrorMessage.isEmpty()) {
            return new JsonResult(timeZoneErrorMessage, HttpStatus.SC_BAD_REQUEST);
        }

        String newCourseId = getNonNullRequestParamValue(Const.ParamsNames.COURSE_ID);
        String newCourseName = getNonNullRequestParamValue(Const.ParamsNames.COURSE_NAME);

        CourseAttributes courseAttributes =
                CourseAttributes.builder(newCourseId)
                        .withName(newCourseName)
                        .withTimezone(ZoneId.of(newCourseTimeZone))
                        .build();

        try {
            logic.createCourseAndInstructor(userInfo.getId(), courseAttributes);
        } catch (EntityAlreadyExistsException e) {
            return new JsonResult(e.getMessage(), HttpStatus.SC_CONFLICT);
        } catch (InvalidParametersException e) {
            return new JsonResult(e.getMessage(), HttpStatus.SC_BAD_REQUEST);
        }

        String statusMessage = "The course has been added. Click <a href=\"/web/instructor/courses/enroll?courseid="
                + newCourseId + "\">here</a> to add students to the course or click "
                + "<a href=\"/web/instructor/courses/edit?courseid=" + newCourseId + "\">here</a> to add other instructors."
                + "<br>If you don't see the course in the list below, please refresh the page after a few moments.";
        return new JsonResult(statusMessage);
    }
}
