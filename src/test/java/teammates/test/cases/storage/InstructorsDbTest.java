package teammates.test.cases.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import teammates.common.datatransfer.DataBundle;
import teammates.common.datatransfer.InstructorPrivileges;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.exception.EntityAlreadyExistsException;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.Const;
import teammates.common.util.FieldValidator;
import teammates.common.util.StringHelper;
import teammates.logic.core.InstructorsLogic;
import teammates.storage.api.EntitiesDb;
import teammates.storage.api.InstructorsDb;
import teammates.test.cases.BaseComponentTestCase;
import teammates.test.driver.AssertHelper;

/**
 * SUT: {@link InstructorsDb}.
 */
public class InstructorsDbTest extends BaseComponentTestCase {

    private static final InstructorsDb instructorsDb = new InstructorsDb();
    private DataBundle dataBundle = getTypicalDataBundle();

    @BeforeClass
    public void classSetup() throws Exception {
        addInstructorsToDb();
    }

    private void addInstructorsToDb() throws Exception {
        for (InstructorAttributes instructor : dataBundle.instructors.values()) {
            instructorsDb.createEntity(instructor);
        }
    }

    @Test
    public void testCreateInstructor() throws Exception {

        ______TS("Success: create an instructor");

        String googleId = "valid.fresh.id";
        String courseId = "valid.course.Id";
        String name = "valid.name";
        String email = "valid@email.tmt";
        String role = Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_COOWNER;
        String displayedName = InstructorAttributes.DEFAULT_DISPLAY_NAME;
        InstructorPrivileges privileges =
                new InstructorPrivileges(Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_COOWNER);
        InstructorAttributes i = InstructorAttributes.builder(googleId, courseId, name, email)
                .withRole(role)
                .withDisplayedName(displayedName)
                .withPrivileges(privileges)
                .build();

        instructorsDb.deleteEntity(i);
        instructorsDb.createEntity(i);

        verifyPresentInDatastore(i);

        ______TS("Failure: create a duplicate instructor");

        EntityAlreadyExistsException eaee = assertThrows(EntityAlreadyExistsException.class,
                () -> instructorsDb.createEntity(i));
        AssertHelper.assertContains(String.format(InstructorsDb.ERROR_CREATE_ENTITY_ALREADY_EXISTS, "Instructor"),
                eaee.getMessage());

        ______TS("Failure: create an instructor with invalid parameters");

        i.googleId = "invalid id with spaces";
        InvalidParametersException ipe = assertThrows(InvalidParametersException.class,
                () -> instructorsDb.createEntity(i));
        AssertHelper.assertContains(
                getPopulatedErrorMessage(
                        FieldValidator.GOOGLE_ID_ERROR_MESSAGE, i.googleId,
                        FieldValidator.GOOGLE_ID_FIELD_NAME, FieldValidator.REASON_INCORRECT_FORMAT,
                        FieldValidator.GOOGLE_ID_MAX_LENGTH),
                ipe.getMessage());

        i.googleId = "valid.fresh.id";
        i.email = "invalid.email.tmt";
        i.role = "role invalid";
        ipe = assertThrows(InvalidParametersException.class, () -> instructorsDb.createEntity(i));
        AssertHelper.assertContains(
                getPopulatedErrorMessage(
                        FieldValidator.EMAIL_ERROR_MESSAGE, i.email,
                        FieldValidator.EMAIL_FIELD_NAME, FieldValidator.REASON_INCORRECT_FORMAT,
                        FieldValidator.EMAIL_MAX_LENGTH) + System.lineSeparator()
                        + String.format(FieldValidator.ROLE_ERROR_MESSAGE, i.role),
                ipe.getMessage());

        ______TS("Failure: null parameters");

        AssertionError ae = assertThrows(AssertionError.class, () -> instructorsDb.createEntity(null));
        assertEquals(Const.StatusCodes.DBLEVEL_NULL_INPUT, ae.getMessage());

    }

    @Test
    public void testGetInstructorForEmail() {

        InstructorAttributes i = dataBundle.instructors.get("instructor1OfCourse1");

        ______TS("Success: get an instructor");

        InstructorAttributes retrieved = instructorsDb.getInstructorForEmail(i.courseId, i.email);
        assertNotNull(retrieved);

        ______TS("Failure: instructor does not exist");

        retrieved = instructorsDb.getInstructorForEmail("non.existent.course", "non.existent");
        assertNull(retrieved);

        ______TS("Failure: null parameters");

        AssertionError ae = assertThrows(AssertionError.class,
                () -> instructorsDb.getInstructorForEmail(null, null));
        assertEquals(Const.StatusCodes.DBLEVEL_NULL_INPUT, ae.getMessage());

    }

    @Test
    public void testGetInstructorForGoogleId() {

        InstructorAttributes i = dataBundle.instructors.get("instructor1OfCourse1");

        ______TS("Success: get an instructor");

        InstructorAttributes retrieved = instructorsDb.getInstructorForGoogleId(i.courseId, i.googleId);
        assertNotNull(retrieved);

        ______TS("Failure: instructor does not exist");

        retrieved = instructorsDb.getInstructorForGoogleId("non.existent.course", "non.existent");
        assertNull(retrieved);

        ______TS("Failure: null parameters");

        AssertionError ae = assertThrows(AssertionError.class, () -> instructorsDb.getInstructorForGoogleId(null, null));
        assertEquals(Const.StatusCodes.DBLEVEL_NULL_INPUT, ae.getMessage());

    }

    @Test
    public void testGetInstructorForRegistrationKey() {

        InstructorAttributes i = dataBundle.instructors.get("instructorNotYetJoinCourse");

        ______TS("Success: get an instructor");

        String key = i.key;

        InstructorAttributes retrieved = instructorsDb.getInstructorForRegistrationKey(StringHelper.encrypt(key));
        assertEquals(i.courseId, retrieved.courseId);
        assertEquals(i.name, retrieved.name);
        assertEquals(i.email, retrieved.email);

        ______TS("Failure: instructor does not exist");

        key = "non.existent.key";
        retrieved = instructorsDb.getInstructorForRegistrationKey(StringHelper.encrypt(key));
        assertNull(retrieved);

        ______TS("Failure: null parameters");

        AssertionError ae = assertThrows(AssertionError.class,
                () -> instructorsDb.getInstructorForRegistrationKey(null));
        assertEquals(Const.StatusCodes.DBLEVEL_NULL_INPUT, ae.getMessage());

    }

    @Test
    public void testGetInstructorsForGoogleId() throws Exception {

        ______TS("Success: get instructors with specific googleId");

        String googleId = "idOfInstructor3";

        List<InstructorAttributes> retrieved = instructorsDb.getInstructorsForGoogleId(googleId, false);
        assertEquals(2, retrieved.size());

        InstructorAttributes instructor1 = retrieved.get(0);
        InstructorAttributes instructor2 = retrieved.get(1);

        assertEquals("idOfTypicalCourse1", instructor1.courseId);
        assertEquals("idOfTypicalCourse2", instructor2.courseId);

        ______TS("Success: get instructors with specific googleId, with 1 archived course.");

        InstructorsLogic.inst().setArchiveStatusOfInstructor(googleId, instructor1.courseId, true);
        retrieved = instructorsDb.getInstructorsForGoogleId(googleId, true);
        assertEquals(1, retrieved.size());
        InstructorsLogic.inst().setArchiveStatusOfInstructor(googleId, instructor1.courseId, false);

        ______TS("Failure: instructor does not exist");

        retrieved = instructorsDb.getInstructorsForGoogleId("non-exist-id", false);
        assertEquals(0, retrieved.size());

        ______TS("Failure: null parameters");

        AssertionError ae = assertThrows(AssertionError.class,
                () -> instructorsDb.getInstructorsForGoogleId(null, false));
        assertEquals(Const.StatusCodes.DBLEVEL_NULL_INPUT, ae.getMessage());

    }

    @Test
    public void testGetInstructorsForCourse() {

        ______TS("Success: get instructors of a specific course");

        String courseId = "idOfTypicalCourse1";

        List<InstructorAttributes> retrieved = instructorsDb.getInstructorsForCourse(courseId);
        assertEquals(5, retrieved.size());

        List<String> idList = new ArrayList<>();
        idList.add("idOfInstructor1OfCourse1");
        idList.add("idOfInstructor2OfCourse1");
        idList.add("idOfInstructor3");
        idList.add("idOfHelperOfCourse1");
        idList.add(null);
        for (InstructorAttributes instructor : retrieved) {
            if (!idList.contains(instructor.googleId)) {
                fail("");
            }
        }

        ______TS("Failure: no instructors for a course");

        retrieved = instructorsDb.getInstructorsForCourse("non-exist-course");
        assertEquals(0, retrieved.size());

        ______TS("Failure: null parameters");

        AssertionError ae = assertThrows(AssertionError.class, () -> instructorsDb.getInstructorsForCourse(null));
        assertEquals(Const.StatusCodes.DBLEVEL_NULL_INPUT, ae.getMessage());
    }

    @Test
    public void testUpdateInstructorByGoogleId() throws Exception {

        InstructorAttributes instructorToEdit = dataBundle.instructors.get("instructor2OfCourse1");

        ______TS("Success: update an instructor");

        instructorToEdit.name = "New Name";
        instructorToEdit.email = "InstrDbT.new-email@email.tmt";
        instructorToEdit.isArchived = true;
        instructorToEdit.role = Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_OBSERVER;
        instructorToEdit.isDisplayedToStudents = false;
        instructorToEdit.displayedName = "New Displayed Name";
        instructorToEdit.privileges = new InstructorPrivileges(
                Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_OBSERVER);
        instructorsDb.updateInstructorByGoogleId(
                InstructorAttributes.updateOptionsWithGoogleIdBuilder(instructorToEdit.courseId, instructorToEdit.googleId)
                        .withName(instructorToEdit.name)
                        .withEmail(instructorToEdit.email)
                        .withIsAchieved(instructorToEdit.isArchived)
                        .withRole(instructorToEdit.role)
                        .withIsDisplayedToStudents(instructorToEdit.isDisplayedToStudents)
                        .withDisplayedName(instructorToEdit.displayedName)
                        .withPrivilege(instructorToEdit.privileges)
                        .build());

        InstructorAttributes instructorUpdated =
                instructorsDb.getInstructorForGoogleId(instructorToEdit.courseId, instructorToEdit.googleId);
        assertEquals(instructorToEdit.name, instructorUpdated.name);
        assertEquals(instructorToEdit.email, instructorUpdated.email);
        assertTrue(instructorUpdated.isArchived);
        assertEquals(Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_OBSERVER, instructorUpdated.role);
        assertFalse(instructorUpdated.isDisplayedToStudents);
        assertEquals("New Displayed Name", instructorUpdated.displayedName);
        assertTrue(instructorUpdated.hasObserverPrivileges());
        // Verifying less privileged 'Observer' role did not return false positive in case old 'Manager' role is unchanged.
        assertFalse(instructorUpdated.hasManagerPrivileges());

        ______TS("Failure: invalid parameters");

        instructorToEdit.name = "";
        instructorToEdit.email = "aaa";
        instructorToEdit.role = "invalid role";
        InvalidParametersException ipe = assertThrows(InvalidParametersException.class,
                () -> instructorsDb.updateInstructorByGoogleId(
                        InstructorAttributes
                                .updateOptionsWithGoogleIdBuilder(instructorToEdit.courseId, instructorToEdit.googleId)
                                .withName(instructorToEdit.name)
                                .withEmail(instructorToEdit.email)
                                .withRole(instructorToEdit.role)
                                .build()));
        AssertHelper.assertContains(
                getPopulatedEmptyStringErrorMessage(
                        FieldValidator.SIZE_CAPPED_NON_EMPTY_STRING_ERROR_MESSAGE_EMPTY_STRING,
                        FieldValidator.PERSON_NAME_FIELD_NAME, FieldValidator.PERSON_NAME_MAX_LENGTH)
                        + System.lineSeparator()
                        + getPopulatedErrorMessage(
                        FieldValidator.EMAIL_ERROR_MESSAGE, instructorToEdit.email,
                        FieldValidator.EMAIL_FIELD_NAME, FieldValidator.REASON_INCORRECT_FORMAT,
                        FieldValidator.EMAIL_MAX_LENGTH) + System.lineSeparator()
                        + String.format(FieldValidator.ROLE_ERROR_MESSAGE, instructorToEdit.role),
                ipe.getMessage());

        ______TS("Failure: non-existent entity");

        InstructorAttributes.UpdateOptionsWithGoogleId updateOptions =
                InstructorAttributes.updateOptionsWithGoogleIdBuilder(instructorToEdit.courseId, "idOfInstructor4")
                        .withName("John Doe")
                        .build();
        EntityDoesNotExistException ednee = assertThrows(EntityDoesNotExistException.class,
                () -> instructorsDb.updateInstructorByGoogleId(updateOptions));
        assertEquals(EntitiesDb.ERROR_UPDATE_NON_EXISTENT + updateOptions, ednee.getMessage());

        ______TS("Failure: null parameters");

        AssertionError ae = assertThrows(AssertionError.class,
                () -> instructorsDb.updateInstructorByGoogleId(null));
        assertEquals(Const.StatusCodes.DBLEVEL_NULL_INPUT, ae.getMessage());
    }

    @Test
    public void testUpdateInstructorByEmail() throws Exception {

        InstructorAttributes instructorToEdit =
                instructorsDb.getInstructorForEmail("idOfTypicalCourse1", "instructor1@course1.tmt");

        ______TS("Success: update an instructor");

        instructorToEdit.googleId = "new-id";
        instructorToEdit.name = "New Name";
        instructorToEdit.isArchived = true;
        instructorToEdit.role = Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_OBSERVER;
        instructorToEdit.isDisplayedToStudents = false;
        instructorToEdit.displayedName = "New Displayed Name";
        instructorToEdit.privileges = new InstructorPrivileges(
                Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_OBSERVER);
        instructorsDb.updateInstructorByEmail(
                InstructorAttributes.updateOptionsWithEmailBuilder(instructorToEdit.courseId, instructorToEdit.email)
                        .withGoogleId(instructorToEdit.googleId)
                        .withName(instructorToEdit.name)
                        .withIsAchieved(instructorToEdit.isArchived)
                        .withRole(instructorToEdit.role)
                        .withIsDisplayedToStudents(instructorToEdit.isDisplayedToStudents)
                        .withDisplayedName(instructorToEdit.displayedName)
                        .withPrivilege(instructorToEdit.privileges)
                        .build());

        InstructorAttributes instructorUpdated =
                instructorsDb.getInstructorForEmail(instructorToEdit.courseId, instructorToEdit.email);
        assertEquals("new-id", instructorUpdated.googleId);
        assertEquals("New Name", instructorUpdated.name);
        assertTrue(instructorUpdated.isArchived);
        assertEquals(Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_OBSERVER, instructorUpdated.role);
        assertFalse(instructorUpdated.isDisplayedToStudents);
        assertEquals("New Displayed Name", instructorUpdated.displayedName);
        assertTrue(instructorUpdated.hasObserverPrivileges());
        // Verifying less privileged 'Observer' role did not return false positive in case old 'CoOwner' role is unchanged.
        assertFalse(instructorUpdated.hasCoownerPrivileges());

        ______TS("Failure: invalid parameters");

        instructorToEdit.googleId = "invalid id";
        instructorToEdit.name = "";
        instructorToEdit.role = "invalid role";
        InvalidParametersException ipe = assertThrows(InvalidParametersException.class,
                () -> instructorsDb.updateInstructorByEmail(
                        InstructorAttributes.updateOptionsWithEmailBuilder(instructorToEdit.courseId, instructorToEdit.email)
                                .withGoogleId(instructorToEdit.googleId)
                                .withName(instructorToEdit.name)
                                .withRole(instructorToEdit.role)
                                .build()));
        AssertHelper.assertContains(
                getPopulatedErrorMessage(
                        FieldValidator.GOOGLE_ID_ERROR_MESSAGE, instructorToEdit.googleId,
                        FieldValidator.GOOGLE_ID_FIELD_NAME, FieldValidator.REASON_INCORRECT_FORMAT,
                        FieldValidator.GOOGLE_ID_MAX_LENGTH) + System.lineSeparator()
                        + getPopulatedEmptyStringErrorMessage(
                        FieldValidator.SIZE_CAPPED_NON_EMPTY_STRING_ERROR_MESSAGE_EMPTY_STRING,
                        FieldValidator.PERSON_NAME_FIELD_NAME, FieldValidator.PERSON_NAME_MAX_LENGTH)
                        + System.lineSeparator()
                        + String.format(FieldValidator.ROLE_ERROR_MESSAGE, instructorToEdit.role),
                ipe.getMessage());

        ______TS("Failure: non-existent entity");

        instructorToEdit.role = Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_MANAGER;
        InstructorAttributes.UpdateOptionsWithEmail updateOptions =
                InstructorAttributes.updateOptionsWithEmailBuilder(instructorToEdit.courseId, "newEmail@email.tmt")
                        .withGoogleId("idOfInstructor4")
                        .build();
        EntityDoesNotExistException ednee = assertThrows(EntityDoesNotExistException.class,
                () -> instructorsDb.updateInstructorByEmail(updateOptions));
        assertEquals(EntitiesDb.ERROR_UPDATE_NON_EXISTENT + updateOptions, ednee.getMessage());

        ______TS("Failure: null parameters");

        AssertionError ae = assertThrows(AssertionError.class,
                () -> instructorsDb.updateInstructorByEmail(null));
        assertEquals(Const.StatusCodes.DBLEVEL_NULL_INPUT, ae.getMessage());

    }

    @Test
    public void testDeleteInstructor() {
        InstructorAttributes i = dataBundle.instructors.get("instructorWithOnlyOneSampleCourse");

        ______TS("Success: delete an instructor");

        instructorsDb.deleteInstructor(i.courseId, i.email);

        InstructorAttributes deleted = instructorsDb.getInstructorForEmail(i.courseId, i.email);
        assertNull(deleted);

        ______TS("Failure: delete a non-exist instructor, should fail silently");

        instructorsDb.deleteInstructor(i.courseId, i.email);

        ______TS("Failure: null parameters");

        AssertionError ae = assertThrows(AssertionError.class, () -> instructorsDb.deleteInstructor(null, null));
        assertEquals(Const.StatusCodes.DBLEVEL_NULL_INPUT, ae.getMessage());

    }

    @Test
    public void testDeleteInstructorsForGoogleId() {

        ______TS("Success: delete instructors with specific googleId");

        String googleId = "instructorWithOnlyOneSampleCourse";
        instructorsDb.deleteInstructorsForGoogleId(googleId);

        List<InstructorAttributes> retrieved = instructorsDb.getInstructorsForGoogleId(googleId, false);
        assertEquals(0, retrieved.size());

        ______TS("Failure: try to delete where there's no instructors associated with the googleId, should fail silently");

        instructorsDb.deleteInstructorsForGoogleId(googleId);

        ______TS("Failure: null parameters");

        AssertionError ae = assertThrows(AssertionError.class,
                () -> instructorsDb.deleteInstructorsForGoogleId(null));
        assertEquals(Const.StatusCodes.DBLEVEL_NULL_INPUT, ae.getMessage());

    }

    @Test
    public void testDeleteInstructorsForCourse() {

        ______TS("Success: delete instructors of a specific course");

        String courseId = "idOfArchivedCourse";
        instructorsDb.deleteInstructorsForCourse(courseId);

        List<InstructorAttributes> retrieved = instructorsDb.getInstructorsForCourse(courseId);
        assertEquals(0, retrieved.size());

        ______TS("Failure: no instructor exists for the course, should fail silently");

        instructorsDb.deleteInstructorsForCourse(courseId);

        ______TS("Failure: null parameters");

        AssertionError ae = assertThrows(AssertionError.class, () -> instructorsDb.deleteInstructorsForCourse(null));
        assertEquals(Const.StatusCodes.DBLEVEL_NULL_INPUT, ae.getMessage());

    }

    @AfterClass
    public void classTearDown() {
        deleteInstructorsFromDb();
    }

    private void deleteInstructorsFromDb() {
        Set<String> keys = dataBundle.instructors.keySet();
        for (String i : keys) {
            instructorsDb.deleteEntity(dataBundle.instructors.get(i));
        }
    }
}
