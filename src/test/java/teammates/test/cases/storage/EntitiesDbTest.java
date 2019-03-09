package teammates.test.cases.storage;

import static teammates.common.util.FieldValidator.COURSE_ID_ERROR_MESSAGE;
import static teammates.common.util.FieldValidator.REASON_INCORRECT_FORMAT;

import java.time.ZoneId;

import org.testng.annotations.Test;

import teammates.common.datatransfer.attributes.CourseAttributes;
import teammates.common.exception.EntityAlreadyExistsException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.Const;
import teammates.common.util.FieldValidator;
import teammates.storage.api.CoursesDb;
import teammates.test.cases.BaseComponentTestCase;
import teammates.test.driver.AssertHelper;

/**
 * SUT: {@link teammates.storage.api.EntitiesDb}.
 */
public class EntitiesDbTest extends BaseComponentTestCase {

    @Test
    public void testCreateEntity() throws Exception {
        //We are using CoursesDb to test EntititesDb here.
        CoursesDb coursesDb = new CoursesDb();

        /*Explanation:
         * The SUT (i.e. EntitiesDb::createEntity) has 4 paths. Therefore, we
         * have 4 test cases here, one for each path.
         */

        ______TS("success: typical case");
        CourseAttributes c = CourseAttributes
                .builder("Computing101-fresh", "Basic Computing", ZoneId.of("UTC"))
                .build();
        coursesDb.deleteCourse(c.getId());
        verifyAbsentInDatastore(c);
        coursesDb.createEntity(c);
        verifyPresentInDatastore(c);

        ______TS("fails: entity already exists");
        EntityAlreadyExistsException eaee = assertThrows(EntityAlreadyExistsException.class,
                () -> coursesDb.createEntity(c));
        AssertHelper.assertContains(String.format(CoursesDb.ERROR_CREATE_ENTITY_ALREADY_EXISTS,
                c.getEntityTypeAsString())
                        + c.getIdentificationString(),
                eaee.getMessage());
        coursesDb.deleteCourse(c.getId());

        ______TS("fails: invalid parameters");
        CourseAttributes invalidCourse = CourseAttributes
                .builder("invalid id spaces", "Basic Computing", ZoneId.of("UTC"))
                .build();
        InvalidParametersException ipe = assertThrows(InvalidParametersException.class,
                () -> coursesDb.createEntity(invalidCourse));
        AssertHelper.assertContains(
                getPopulatedErrorMessage(
                        COURSE_ID_ERROR_MESSAGE, invalidCourse.getId(),
                        FieldValidator.COURSE_ID_FIELD_NAME, REASON_INCORRECT_FORMAT,
                        FieldValidator.COURSE_ID_MAX_LENGTH),
                ipe.getMessage());

        ______TS("fails: null parameter");
        AssertionError ae = assertThrows(AssertionError.class, () -> coursesDb.createEntity(null));
        assertEquals(Const.StatusCodes.DBLEVEL_NULL_INPUT, ae.getMessage());
    }

}
