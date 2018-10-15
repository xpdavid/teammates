package teammates.test.cases.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import teammates.common.datatransfer.InstructorPrivileges;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.exception.EntityAlreadyExistsException;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.Const;
import teammates.common.util.StringHelper;
import teammates.logic.core.CoursesLogic;
import teammates.logic.core.InstructorsLogic;
import teammates.storage.api.InstructorsDb;
import teammates.test.driver.AssertHelper;

/**
 * SUT: {@link InstructorsLogic}.
 */
public class InstructorsLogicTest extends BaseLogicTest {

    private static InstructorsLogic instructorsLogic = InstructorsLogic.inst();
    private static InstructorsDb instructorsDb = new InstructorsDb();
    private static CoursesLogic coursesLogic = CoursesLogic.inst();

    @BeforeClass
    public void classSetup() {
        instructorsLogic.deleteInstructorCascade("FSQTT.idOfTypicalCourse1", "instructor3@course1.tmt");
    }

    @Test
    public void testAll() throws Exception {
        testGetInstructorForEmail();
        testGetInstructorForGoogleId();
        testGetInstructorsForGoogleId();
        testGetInstructorForRegistrationKey();
        testGetInstructorsForCourse();
        testGetKeyForInstructor();
        testIsGoogleIdOfInstructorOfCourse();
        testIsEmailOfInstructorOfCourse();
        testVerifyInstructorExists();
        testVerifyIsEmailOfInstructorOfCourse();
        testIsNewInstructor();
        testAddInstructor();
        testGetCoOwnersForCourse();
        testUpdateInstructorByGoogleId();
        testUpdateInstructorByEmail();
        testDeleteInstructor();
        testDeleteInstructorsForGoogleId();
        testDeleteInstructorsForCourse();
    }

    private void testAddInstructor() throws Exception {

        ______TS("success: add an instructor");

        String courseId = "test-course";
        String name = "New Instructor";
        String email = "ILT.instr@email.tmt";
        String role = Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_COOWNER;
        String displayedName = InstructorAttributes.DEFAULT_DISPLAY_NAME;
        InstructorPrivileges privileges =
                new InstructorPrivileges(Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_COOWNER);
        InstructorAttributes instr = InstructorAttributes.builder(null, courseId, name, email)
                .withRole(role)
                .withDisplayedName(displayedName)
                .withPrivileges(privileges)
                .build();

        instructorsLogic.createInstructor(instr);

        verifyPresentInDatastore(instr);

        ______TS("failure: instructor already exists");

        EntityAlreadyExistsException ednee = assertThrows(EntityAlreadyExistsException.class,
                () -> instructorsLogic.createInstructor(instr));
        AssertHelper.assertContains("Trying to create a Instructor that exists", ednee.getMessage());

        instructorsLogic.deleteInstructorCascade(instr.courseId, instr.email);

        ______TS("failure: invalid parameter");

        instr.email = "invalidEmail.tmt";
        String expectedError =
                "\"" + instr.email + "\" is not acceptable to TEAMMATES as a/an email "
                + "because it is not in the correct format. An email address contains "
                + "some text followed by one '@' sign followed by some more text. "
                + "It cannot be longer than 254 characters, cannot be empty and "
                + "cannot contain spaces.";
        InvalidParametersException ipe = assertThrows(InvalidParametersException.class,
                () -> instructorsLogic.createInstructor(instr));
        assertEquals(expectedError, ipe.getMessage());

        ______TS("failure: null parameters");

        AssertionError ae = assertThrows(AssertionError.class, () -> instructorsLogic.createInstructor(null));
        AssertHelper.assertContains("Supplied parameter was null", ae.getMessage());
    }

    private void testGetInstructorForEmail() {

        ______TS("failure: instructor doesn't exist");

        assertNull(instructorsLogic.getInstructorForEmail("idOfTypicalCourse1", "non-exist@email.tmt"));

        ______TS("success: get an instructor by using email");

        String courseId = "idOfTypicalCourse1";
        String email = "instructor1@course1.tmt";

        InstructorAttributes instr = instructorsLogic.getInstructorForEmail(courseId, email);

        assertEquals(courseId, instr.courseId);
        assertEquals(email, instr.email);
        assertEquals("idOfInstructor1OfCourse1", instr.googleId);
        assertEquals("Instructor1 Course1", instr.name);

        ______TS("failure: null parameters");

        AssertionError ae = assertThrows(AssertionError.class, () -> instructorsLogic.getInstructorForEmail(null, email));
        AssertHelper.assertContains("Supplied parameter was null", ae.getMessage());

        ae = assertThrows(AssertionError.class, () -> instructorsLogic.getInstructorForEmail(courseId, null));
        AssertHelper.assertContains("Supplied parameter was null", ae.getMessage());

    }

    private void testGetInstructorForGoogleId() {

        ______TS("failure: instructor doesn't exist");

        assertNull(instructorsLogic.getInstructorForGoogleId("idOfTypicalCourse1", "non-exist-id"));

        ______TS("success: typical case");

        String courseId = "idOfTypicalCourse1";
        String googleId = "idOfInstructor1OfCourse1";

        InstructorAttributes instr = instructorsLogic.getInstructorForGoogleId(courseId, googleId);

        assertEquals(courseId, instr.courseId);
        assertEquals(googleId, instr.googleId);
        assertEquals("instructor1@course1.tmt", instr.email);
        assertEquals("Instructor1 Course1", instr.name);

        ______TS("failure: null parameters");

        AssertionError ae = assertThrows(AssertionError.class,
                () -> instructorsLogic.getInstructorForGoogleId(null, googleId));
        AssertHelper.assertContains("Supplied parameter was null", ae.getMessage());

        ae = assertThrows(AssertionError.class, () -> instructorsLogic.getInstructorForGoogleId(courseId, null));
        AssertHelper.assertContains("Supplied parameter was null", ae.getMessage());

    }

    private void testGetInstructorForRegistrationKey() {

        ______TS("failure: instructor doesn't exist");
        String key = "non-existing-key";
        assertNull(instructorsDb.getInstructorForRegistrationKey(StringHelper.encrypt(key)));

        ______TS("success: typical case");

        String courseId = "idOfSampleCourse-demo";
        String email = "instructorNotYetJoined@email.tmt";

        InstructorAttributes instr = instructorsDb.getInstructorForEmail(courseId, email);
        key = instr.key;

        InstructorAttributes retrieved = instructorsLogic.getInstructorForRegistrationKey(StringHelper.encrypt(key));

        assertEquals(instr.courseId, retrieved.courseId);
        assertEquals(instr.name, retrieved.name);
        assertEquals(instr.email, retrieved.email);

        ______TS("failure: null parameter");
        AssertionError ae = assertThrows(AssertionError.class,
                () -> instructorsLogic.getInstructorForRegistrationKey(null));
        AssertHelper.assertContains("Supplied parameter was null", ae.getMessage());
    }

    private void testGetInstructorsForCourse() throws Exception {

        ______TS("success: get all instructors for a course");

        String courseId = "idOfTypicalCourse1";

        List<InstructorAttributes> instructors = instructorsLogic.getInstructorsForCourse(courseId);
        assertEquals(5, instructors.size());

        HashMap<String, Boolean> idMap = new HashMap<>();
        idMap.put("idOfInstructor1OfCourse1", false);
        idMap.put("idOfInstructor2OfCourse1", false);
        idMap.put("idOfInstructor3", false);

        for (InstructorAttributes i : instructors) {
            idMap.computeIfPresent(i.googleId, (key, value) -> true);
        }

        assertTrue(idMap.get("idOfInstructor1OfCourse1").booleanValue());
        assertTrue(idMap.get("idOfInstructor2OfCourse1").booleanValue());
        assertTrue(idMap.get("idOfInstructor3").booleanValue());

        ______TS("failure: no instructors for a given course");

        courseId = "new-course";
        coursesLogic.createCourse(courseId, "New course", "UTC");

        instructors = instructorsLogic.getInstructorsForCourse(courseId);
        assertEquals(0, instructors.size());

        ______TS("failure: null parameters");

        AssertionError ae = assertThrows(AssertionError.class, () -> instructorsLogic.getInstructorsForCourse(null));
        AssertHelper.assertContains("Supplied parameter was null", ae.getMessage());
    }

    private void testGetInstructorsForGoogleId() {

        ______TS("success: get all instructors for a google id");

        String googleId = "idOfInstructor3";

        List<InstructorAttributes> instructors = instructorsLogic.getInstructorsForGoogleId(googleId);
        assertEquals(2, instructors.size());

        InstructorAttributes instructor1 = instructorsDb.getInstructorForGoogleId("idOfTypicalCourse1", googleId);
        InstructorAttributes instructor2 = instructorsDb.getInstructorForGoogleId("idOfTypicalCourse2", googleId);

        verifySameInstructor(instructor1, instructors.get(0));
        verifySameInstructor(instructor2, instructors.get(1));

        ______TS("failure: non-exist google id");

        googleId = "non-exist-id";

        instructors = instructorsLogic.getInstructorsForGoogleId(googleId);
        assertEquals(0, instructors.size());

        ______TS("failure: null parameter");

        AssertionError ae = assertThrows(AssertionError.class, () -> instructorsLogic.getInstructorsForGoogleId(null));
        AssertHelper.assertContains("Supplied parameter was null", ae.getMessage());
    }

    private void testGetKeyForInstructor() throws Exception {

        ______TS("success: get encrypted key for instructor");

        String courseId = "idOfSampleCourse-demo";
        String email = "instructorNotYetJoined@email.tmt";

        InstructorAttributes instructor = instructorsDb.getInstructorForEmail(courseId, email);

        String key = instructorsLogic.getEncryptedKeyForInstructor(instructor.courseId, instructor.email);
        String expected = StringHelper.encrypt(instructor.key);
        assertEquals(expected, key);

        ______TS("failure: non-existent instructor");

        EntityDoesNotExistException ednee = assertThrows(EntityDoesNotExistException.class,
                () -> instructorsLogic.getEncryptedKeyForInstructor(courseId, "non-existent@email.tmt"));
        assertEquals("Instructor non-existent@email.tmt does not belong to course " + courseId,
                ednee.getMessage());

        ______TS("failure: null parameter");

        AssertionError ae = assertThrows(AssertionError.class,
                () -> instructorsLogic.getEncryptedKeyForInstructor(courseId, null));
        AssertHelper.assertContains("Supplied parameter was null", ae.getMessage());

        ae = assertThrows(AssertionError.class, () -> instructorsLogic.getEncryptedKeyForInstructor(null, email));
        AssertHelper.assertContains("Supplied parameter was null", ae.getMessage());

    }

    private void testIsGoogleIdOfInstructorOfCourse() {

        ______TS("success: is an instructor of a given course");

        String instructorId = "idOfInstructor1OfCourse1";

        boolean result = instructorsLogic.isGoogleIdOfInstructorOfCourse(instructorId, "idOfTypicalCourse1");

        assertTrue(result);

        ______TS("failure: not an instructor of a given course");

        String courseId = "idOfTypicalCourse2";

        result = instructorsLogic.isGoogleIdOfInstructorOfCourse(instructorId, courseId);

        assertFalse(result);

        ______TS("failure: null parameter");

        AssertionError ae = assertThrows(AssertionError.class,
                () -> instructorsLogic.isGoogleIdOfInstructorOfCourse(null, courseId));
        AssertHelper.assertContains("Supplied parameter was null", ae.getMessage());

        ae = assertThrows(AssertionError.class, () -> instructorsLogic.isGoogleIdOfInstructorOfCourse(instructorId, null));
        AssertHelper.assertContains("Supplied parameter was null", ae.getMessage());
    }

    private void testIsEmailOfInstructorOfCourse() {

        ______TS("success: is an instructor of a given course");

        String instructorEmail = "instructor1@course1.tmt";

        boolean result = instructorsLogic.isEmailOfInstructorOfCourse(instructorEmail, "idOfTypicalCourse1");

        assertTrue(result);

        ______TS("failure: not an instructor of a given course");

        String courseId = "idOfTypicalCourse2";

        result = instructorsLogic.isEmailOfInstructorOfCourse(instructorEmail, courseId);

        assertFalse(result);

        ______TS("failure: null parameter");

        AssertionError ae = assertThrows(AssertionError.class,
                () -> instructorsLogic.isEmailOfInstructorOfCourse(instructorEmail, null));
        AssertHelper.assertContains("Supplied parameter was null", ae.getMessage());

        ae = assertThrows(AssertionError.class, () -> instructorsLogic.isEmailOfInstructorOfCourse(null, courseId));
        AssertHelper.assertContains("Supplied parameter was null", ae.getMessage());

    }

    private void testVerifyInstructorExists() throws Exception {

        ______TS("success: instructor does exist");

        instructorsLogic.verifyInstructorExists("idOfInstructor1OfCourse1");

        ______TS("failure: instructor doesn't exist");

        EntityDoesNotExistException ednee = assertThrows(EntityDoesNotExistException.class,
                () -> instructorsLogic.verifyInstructorExists("nonExistingInstructor"));
        AssertHelper.assertContains("Instructor does not exist", ednee.getMessage());

        ______TS("failure: null parameter");

        AssertionError ae = assertThrows(AssertionError.class, () -> instructorsLogic.verifyInstructorExists(null));
        AssertHelper.assertContains("Supplied parameter was null", ae.getMessage());
    }

    private void testVerifyIsEmailOfInstructorOfCourse() throws Exception {

        ______TS("success: instructor belongs to course");

        String courseId = "idOfTypicalCourse1";
        instructorsLogic.verifyIsEmailOfInstructorOfCourse("instructor1@course1.tmt", courseId);

        ______TS("failure: instructor doesn't belong to course");
        String instructorEmail = "nonExistingInstructor@email.tmt";

        EntityDoesNotExistException ednee = assertThrows(EntityDoesNotExistException.class,
                () -> instructorsLogic.verifyIsEmailOfInstructorOfCourse(instructorEmail, courseId));
        assertEquals("Instructor " + instructorEmail + " does not belong to course " + courseId,
                ednee.getMessage());
        ______TS("failure: null parameter");

        AssertionError ae = assertThrows(AssertionError.class,
                () -> instructorsLogic.verifyIsEmailOfInstructorOfCourse(null, courseId));
        AssertHelper.assertContains("Supplied parameter was null", ae.getMessage());

        ae = assertThrows(AssertionError.class,
                () -> instructorsLogic.verifyIsEmailOfInstructorOfCourse(instructorEmail, null));
        AssertHelper.assertContains("Supplied parameter was null", ae.getMessage());
    }

    private void testIsNewInstructor() {

        ______TS("success: instructor with only 1 sample course");

        String instructorId = "idOfInstructorWithOnlyOneSampleCourse";
        assertTrue(instructorsLogic.isNewInstructor(instructorId));

        ______TS("success: instructor without any course");

        instructorId = "instructorWithoutCourses";
        assertTrue(instructorsLogic.isNewInstructor(instructorId));

        ______TS("failure: instructor with only 1 course, but not a sample course");

        instructorId = "idOfInstructor4";
        assertFalse(instructorsLogic.isNewInstructor(instructorId));

        ______TS("failure: instructor is not new user");

        instructorId = "idOfInstructor1OfCourse1";
        assertFalse(instructorsLogic.isNewInstructor(instructorId));

        ______TS("failure: null parameter");

        AssertionError ae = assertThrows(AssertionError.class, () -> instructorsLogic.isNewInstructor(null));
        AssertHelper.assertContains("Supplied parameter was null", ae.getMessage());

    }

    private void testUpdateInstructorByGoogleId() throws Exception {

        ______TS("typical case: update an instructor");

        String courseId = "idOfTypicalCourse1";
        String googleId = "idOfInstructor2OfCourse1";

        InstructorAttributes instructorToBeUpdated = instructorsLogic.getInstructorForGoogleId(courseId, googleId);
        instructorToBeUpdated.name = "New Name";
        instructorToBeUpdated.email = "new-email@course1.tmt";

        instructorsLogic.updateInstructorByGoogleId(
                InstructorAttributes
                        .updateOptionsWithGoogleIdBuilder(
                                instructorToBeUpdated.courseId, instructorToBeUpdated.googleId)
                        .withName(instructorToBeUpdated.name)
                        .withEmail(instructorToBeUpdated.email)
                        .build());

        InstructorAttributes instructorUpdated = instructorsLogic.getInstructorForGoogleId(courseId, googleId);
        verifySameInstructor(instructorToBeUpdated, instructorUpdated);

        ______TS("failure: instructor doesn't exist");

        instructorsLogic.deleteInstructorCascade(courseId, instructorUpdated.email);

        InstructorAttributes.UpdateOptionsWithGoogleId updateOptions =
                InstructorAttributes
                        .updateOptionsWithGoogleIdBuilder(
                                instructorToBeUpdated.courseId, instructorToBeUpdated.googleId)
                        .withName("New Name")
                        .build();
        EntityDoesNotExistException ednee = assertThrows(EntityDoesNotExistException.class,
                () -> instructorsLogic.updateInstructorByGoogleId(updateOptions));
        assertEquals("Trying to update non-existent Entity: " + updateOptions, ednee.getMessage());

        ______TS("failure: course doesn't exist");

        courseId = "random-course";
        instructorToBeUpdated.courseId = courseId;

        InstructorAttributes.UpdateOptionsWithGoogleId updateOptions2 =
                InstructorAttributes
                        .updateOptionsWithGoogleIdBuilder(
                                instructorToBeUpdated.courseId, instructorToBeUpdated.googleId)
                        .withName("New Name")
                        .build();
        ednee = assertThrows(EntityDoesNotExistException.class,
                () -> instructorsLogic.updateInstructorByGoogleId(updateOptions2));
        assertEquals("Trying to update non-existent Entity: " + updateOptions2, ednee.getMessage());

        ______TS("failure: null parameter");

        AssertionError ae = assertThrows(AssertionError.class,
                () -> instructorsLogic.updateInstructorByGoogleId(null));
        assertEquals("Supplied parameter was null", ae.getMessage());

    }

    private void testUpdateInstructorByEmail() throws Exception {

        ______TS("typical case: update an instructor");

        String email = "instructor1@course1.tmt";
        String courseId = "idOfTypicalCourse1";

        String newName = "New name for instructor 1";
        String newGoogleId = "newIdForInstructor1";

        InstructorAttributes instructorToBeUpdated = instructorsLogic.getInstructorForEmail(courseId, email);
        instructorToBeUpdated.googleId = newGoogleId;
        instructorToBeUpdated.name = newName;

        instructorsLogic.updateInstructorByEmail(
                InstructorAttributes
                        .updateOptionsWithEmailBuilder(
                                instructorToBeUpdated.courseId, instructorToBeUpdated.email)
                        .withName(instructorToBeUpdated.name)
                        .withGoogleId(instructorToBeUpdated.googleId)
                        .build());

        InstructorAttributes instructorUpdated = instructorsLogic.getInstructorForEmail(courseId, email);
        verifySameInstructor(instructorToBeUpdated, instructorUpdated);

        ______TS("failure: instructor doesn't belong to course");

        instructorsLogic.deleteInstructorCascade(courseId, instructorToBeUpdated.email);

        InstructorAttributes.UpdateOptionsWithEmail updateOptions =
                InstructorAttributes
                        .updateOptionsWithEmailBuilder(
                                instructorToBeUpdated.courseId, instructorToBeUpdated.email)
                        .withName("New Name")
                        .build();
        EntityDoesNotExistException ednee = assertThrows(EntityDoesNotExistException.class,
                () -> instructorsLogic.updateInstructorByEmail(updateOptions));
        assertEquals("Trying to update non-existent Entity: " + updateOptions, ednee.getMessage());

        ______TS("failure: course doesn't exist");

        courseId = "random-course";
        instructorToBeUpdated.courseId = courseId;

        InstructorAttributes.UpdateOptionsWithEmail updateOptions2 =
                InstructorAttributes
                        .updateOptionsWithEmailBuilder(
                                instructorToBeUpdated.courseId, instructorToBeUpdated.email)
                        .withName("New Name")
                        .build();
        ednee = assertThrows(EntityDoesNotExistException.class,
                () -> instructorsLogic.updateInstructorByEmail(updateOptions2));
        assertEquals("Trying to update non-existent Entity: " + updateOptions2, ednee.getMessage());

        ______TS("failure: null parameter");

        AssertionError ae = assertThrows(AssertionError.class,
                () -> instructorsLogic.updateInstructorByEmail(null));
        assertEquals("Supplied parameter was null", ae.getMessage());

    }

    private void testDeleteInstructor() throws Exception {

        ______TS("typical case: delete an instructor for specific course");

        String courseId = "idOfTypicalCourse1";
        String email = "instructor3@course1.tmt";

        InstructorAttributes instructorDeleted = instructorsLogic.getInstructorForEmail(courseId, email);

        instructorsLogic.deleteInstructorCascade(courseId, email);

        verifyAbsentInDatastore(instructorDeleted);

        ______TS("typical case: delete a non-existent instructor");

        instructorsLogic.deleteInstructorCascade(courseId, "non-existent@course1.tmt");

        ______TS("failure: null parameter");

        AssertionError ae = assertThrows(AssertionError.class,
                () -> instructorsLogic.deleteInstructorCascade(courseId, null));
        AssertHelper.assertContains("Supplied parameter was null", ae.getMessage());

        ae = assertThrows(AssertionError.class, () -> instructorsLogic.deleteInstructorCascade(null, email));
        AssertHelper.assertContains("Supplied parameter was null", ae.getMessage());

        // restore deleted instructor
        instructorsLogic.createInstructor(instructorDeleted);
    }

    private void testDeleteInstructorsForGoogleId() throws Exception {
        ______TS("typical case: delete all instructors for a given googleId");

        String googleId = "idOfInstructor1";

        List<InstructorAttributes> instructors = instructorsLogic.getInstructorsForGoogleId(googleId);

        instructorsLogic.deleteInstructorsForGoogleIdAndCascade(googleId);

        List<InstructorAttributes> instructorList = instructorsLogic.getInstructorsForGoogleId(googleId);
        assertTrue(instructorList.isEmpty());

        ______TS("typical case: delete an non-existent googleId");

        instructorsLogic.deleteInstructorsForGoogleIdAndCascade("non-existent");

        ______TS("failure: null parameter");

        AssertionError ae = assertThrows(AssertionError.class,
                () -> instructorsLogic.deleteInstructorsForGoogleIdAndCascade(null));
        AssertHelper.assertContains("Supplied parameter was null", ae.getMessage());

        // restore deleted instructors
        for (InstructorAttributes instructor : instructors) {
            instructorsLogic.createInstructor(instructor);
        }
    }

    private void testDeleteInstructorsForCourse() {

        ______TS("typical case: delete all instructors of a given course");

        String courseId = "idOfTypicalCourse1";

        instructorsLogic.deleteInstructorsForCourse(courseId);

        List<InstructorAttributes> instructorList = instructorsLogic.getInstructorsForCourse(courseId);

        assertTrue(instructorList.isEmpty());

        ______TS("typical case: delete all instructors for a non-existent course");

        instructorsLogic.deleteInstructorsForCourse("non-existent");

        ______TS("failure case: null parameter");

        AssertionError ae = assertThrows(AssertionError.class, () -> instructorsLogic.deleteInstructorsForCourse(null));
        AssertHelper.assertContains("Supplied parameter was null", ae.getMessage());

    }

    private void verifySameInstructor(InstructorAttributes instructor1, InstructorAttributes instructor2) {
        assertEquals(instructor1.googleId, instructor2.googleId);
        assertEquals(instructor1.courseId, instructor2.courseId);
        assertEquals(instructor1.name, instructor2.name);
        assertEquals(instructor1.email, instructor2.email);
    }

    private void testGetCoOwnersForCourse() {
        ______TS("Verify co-owner status of generated co-owners list");
        String courseId = "idOfTypicalCourse1";
        List<InstructorAttributes> generatedCoOwners = instructorsLogic.getCoOwnersForCourse(courseId);
        for (InstructorAttributes generatedCoOwner : generatedCoOwners) {
            assertTrue(generatedCoOwner.hasCoownerPrivileges());
        }

        ______TS("Verify all co-owners present in generated co-owners list");

        // Generate ArrayList<String> of emails of all coOwners in course from data bundle
        List<String> coOwnersEmailsFromDataBundle = new ArrayList<>();
        for (InstructorAttributes instructor : new ArrayList<>(dataBundle.instructors.values())) {
            if (!(instructor.getCourseId().equals(courseId) && instructor.hasCoownerPrivileges())) {
                continue;
            }
            coOwnersEmailsFromDataBundle.add(instructor.email);
        }

        // Generate ArrayList<String> of emails of all coOwners from instructorsLogic.getCoOwnersForCourse
        List<String> generatedCoOwnersEmails = new ArrayList<>();
        for (InstructorAttributes generatedCoOwner : generatedCoOwners) {
            generatedCoOwnersEmails.add(generatedCoOwner.email);
        }

        assertTrue(coOwnersEmailsFromDataBundle.containsAll(generatedCoOwnersEmails)
                && generatedCoOwnersEmails.containsAll(coOwnersEmailsFromDataBundle));
    }

}
