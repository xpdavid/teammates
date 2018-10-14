package teammates.test.cases.storage;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import teammates.common.datatransfer.attributes.CourseAttributes;
import teammates.common.exception.EntityAlreadyExistsException;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.Const;
import teammates.common.util.FieldValidator;
import teammates.storage.api.CoursesDb;
import teammates.storage.api.EntitiesDb;
import teammates.test.cases.BaseComponentTestCase;
import teammates.test.driver.AssertHelper;
import teammates.test.driver.StringHelperExtension;

/**
 * SUT: {@link CoursesDb}.
 */
public class CoursesDbTest extends BaseComponentTestCase {

    private CoursesDb coursesDb = new CoursesDb();

    @Test
    public void testCreateCourse() throws EntityAlreadyExistsException, InvalidParametersException {

        /*Explanation:
         * This is an inherited method from EntitiesDb and should be tested in
         * EntitiesDbTest class. We test it here too because the method in
         * the parent class actually calls an overridden method from the SUT.
         */

        ______TS("Success: typical case");

        CourseAttributes c = CourseAttributes
                .builder("CDbT.tCC.newCourse", "Basic Computing", ZoneId.of("UTC"))
                .build();
        coursesDb.createEntity(c);
        verifyPresentInDatastore(c);

        ______TS("Failure: create duplicate course");

        EntityAlreadyExistsException eaee = assertThrows(EntityAlreadyExistsException.class,
                () -> coursesDb.createEntity(c));
        AssertHelper.assertContains(
                String.format(EntitiesDb.ERROR_CREATE_ENTITY_ALREADY_EXISTS, "Course"),
                eaee.getMessage());

        ______TS("Failure: create a course with invalid parameter");

        CourseAttributes invalidIdCourse = CourseAttributes
                .builder("Invalid id", "Basic Computing", ZoneId.of("UTC"))
                .build();
        InvalidParametersException ipe = assertThrows(InvalidParametersException.class,
                () -> coursesDb.createEntity(invalidIdCourse));
        AssertHelper.assertContains(
                "not acceptable to TEAMMATES as a/an course ID because it is not in the correct format",
                ipe.getMessage());

        String longCourseName = StringHelperExtension.generateStringOfLength(FieldValidator.COURSE_NAME_MAX_LENGTH + 1);
        CourseAttributes invalidNameCourse = CourseAttributes
                .builder("CDbT.tCC.newCourse", longCourseName, ZoneId.of("UTC"))
                .build();
        ipe = assertThrows(InvalidParametersException.class, () -> coursesDb.createEntity(invalidNameCourse));
        AssertHelper.assertContains("not acceptable to TEAMMATES as a/an course name because it is too long",
                ipe.getMessage());

        ______TS("Failure: null parameter");

        AssertionError ae = assertThrows(AssertionError.class, () -> coursesDb.createEntity(null));
        assertEquals(Const.StatusCodes.DBLEVEL_NULL_INPUT, ae.getMessage());

    }

    @Test
    public void testGetCourse() throws InvalidParametersException {
        CourseAttributes c = createNewCourse();

        ______TS("Success: get an existent course");

        CourseAttributes retrieved = coursesDb.getCourse(c.getId());
        assertNotNull(retrieved);

        ______TS("Failure: get a non-existent course");

        retrieved = coursesDb.getCourse("non-existent-course");
        assertNull(retrieved);

        ______TS("Failure: get null parameters");

        AssertionError ae = assertThrows(AssertionError.class, () -> coursesDb.getCourse(null));
        assertEquals(Const.StatusCodes.DBLEVEL_NULL_INPUT, ae.getMessage());

    }

    @Test
    public void testGetCourses() throws InvalidParametersException {
        CourseAttributes c = createNewCourse();
        List<String> courseIds = new ArrayList<>();

        ______TS("Success: get an existent course");

        courseIds.add(c.getId());
        List<CourseAttributes> retrieved = coursesDb.getCourses(courseIds);
        assertEquals(1, retrieved.size());

        ______TS("Failure: get a non-existent course");

        courseIds.remove(c.getId());
        courseIds.add("non-existent-course");
        retrieved = coursesDb.getCourses(courseIds);
        assertEquals(0, retrieved.size());

        ______TS("Failure: get null parameters");

        AssertionError ae = assertThrows(AssertionError.class, () -> coursesDb.getCourse(null));
        assertEquals(Const.StatusCodes.DBLEVEL_NULL_INPUT, ae.getMessage());

    }

    @Test
    public void testUpdateCourse() throws Exception {

        ______TS("Failure: null paramater");

        AssertionError ae = assertThrows(AssertionError.class, () -> coursesDb.updateCourse(null));
        assertEquals(Const.StatusCodes.DBLEVEL_NULL_INPUT, ae.getMessage());

        ______TS("fail: non-exisitng course");

        EntityDoesNotExistException ednee = assertThrows(EntityDoesNotExistException.class,
                () -> coursesDb.updateCourse(
                        CourseAttributes.updateOptionsBuilder("CDbT.non-exist-course")
                                .withName("Non existing course")
                                .build()
                ));
        assertEquals(CoursesDb.ERROR_UPDATE_NON_EXISTENT_COURSE, ednee.getMessage());

        ______TS("success: typical case");

        CourseAttributes c = createNewCourse();
        c.setDeletedAt();

        coursesDb.updateCourse(
                CourseAttributes.updateOptionsBuilder(c.getId())
                        .withName(c.getName() + " updated")
                        .withDeletedAt(c.deletedAt)
                        .build()
        );
        CourseAttributes retrieved = coursesDb.getCourse(c.getId());
        assertEquals(c.getName() + " updated", retrieved.getName());
        assertEquals(c.deletedAt, retrieved.deletedAt);

        ______TS("Failure: update course with invalid parameters");

        InvalidParametersException ipe = assertThrows(InvalidParametersException.class,
                () -> coursesDb.updateCourse(
                        CourseAttributes.updateOptionsBuilder(c.getId())
                            .withName("")
                            .build()
                ));
        AssertHelper.assertContains("The field 'course name' is empty", ipe.getMessage());
    }

    @Test
    public void testDeleteCourse() throws InvalidParametersException {
        CourseAttributes c = createNewCourse();

        ______TS("Success: delete an existing course");

        coursesDb.deleteCourse(c.getId());

        CourseAttributes deleted = coursesDb.getCourse(c.getId());
        assertNull(deleted);

        ______TS("Failure: delete a non-existent courses");

        // Should fail silently
        coursesDb.deleteCourse(c.getId());

        ______TS("Failure: null parameter");

        AssertionError ae = assertThrows(AssertionError.class, () -> coursesDb.deleteCourse(null));
        assertEquals(Const.StatusCodes.DBLEVEL_NULL_INPUT, ae.getMessage());

    }

    private CourseAttributes createNewCourse() throws InvalidParametersException {

        CourseAttributes c = CourseAttributes
                .builder("Computing101", "Basic Computing", ZoneId.of("UTC"))
                .build();

        try {
            coursesDb.createEntity(c);
        } catch (EntityAlreadyExistsException e) {
            //It is ok if it already exists.
            ignorePossibleException();
        }

        return c;
    }
}
