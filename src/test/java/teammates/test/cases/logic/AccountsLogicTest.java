package teammates.test.cases.logic;

import org.testng.annotations.Test;

import teammates.common.datatransfer.attributes.AccountAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.datatransfer.attributes.StudentProfileAttributes;
import teammates.common.exception.EntityAlreadyExistsException;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.FieldValidator;
import teammates.common.util.StringHelper;
import teammates.logic.core.AccountsLogic;
import teammates.logic.core.InstructorsLogic;
import teammates.logic.core.ProfilesLogic;
import teammates.logic.core.StudentsLogic;
import teammates.storage.api.AccountsDb;
import teammates.test.driver.AssertHelper;

/**
 * SUT: {@link AccountsLogic}.
 */
public class AccountsLogicTest extends BaseLogicTest {

    private static final AccountsLogic accountsLogic = AccountsLogic.inst();
    private static final ProfilesLogic profilesLogic = ProfilesLogic.inst();
    private static final InstructorsLogic instructorsLogic = InstructorsLogic.inst();
    private static final StudentsLogic studentsLogic = StudentsLogic.inst();

    @Test
    public void testCreateAccount() throws Exception {

        ______TS("typical success case");

        AccountAttributes accountToCreate = AccountAttributes.builder("id")
                .withName("name")
                .withEmail("test@email.com")
                .withInstitute("dev")
                .withIsInstructor(true)
                .build();

        accountsLogic.createAccount(accountToCreate);
        verifyPresentInDatastore(accountToCreate);

        accountsLogic.deleteAccountCascade("id");

        ______TS("invalid parameters exception case");

        accountToCreate = AccountAttributes.builder("")
                .withName("name")
                .withEmail("test@email.com")
                .withInstitute("dev")
                .withIsInstructor(true)
                .build();
        AccountAttributes[] finalAccount = new AccountAttributes[] { accountToCreate };
        assertThrows(InvalidParametersException.class, () -> accountsLogic.createAccount(finalAccount[0]));

    }

    @Test
    public void testAccountFunctions() throws Exception {

        ______TS("test isAccountPresent");

        assertTrue(accountsLogic.isAccountPresent("idOfInstructor1OfCourse1"));
        assertTrue(accountsLogic.isAccountPresent("student1InCourse1"));

        assertFalse(accountsLogic.isAccountPresent("id-does-not-exist"));

        ______TS("test isAccountAnInstructor");

        assertTrue(accountsLogic.isAccountAnInstructor("idOfInstructor1OfCourse1"));

        assertFalse(accountsLogic.isAccountAnInstructor("student1InCourse1"));
        assertFalse(accountsLogic.isAccountAnInstructor("id-does-not-exist"));

        ______TS("test downgradeInstructorToStudentCascade");

        accountsLogic.downgradeInstructorToStudentCascade("idOfInstructor2OfCourse1");
        assertFalse(accountsLogic.isAccountAnInstructor("idOfInstructor2OfCourse1"));

        accountsLogic.downgradeInstructorToStudentCascade("student1InCourse1");
        assertFalse(accountsLogic.isAccountAnInstructor("student1InCourse1"));

        assertThrows(EntityDoesNotExistException.class, () -> {
            accountsLogic.downgradeInstructorToStudentCascade("id-does-not-exist");
        });

        ______TS("test makeAccountInstructor");

        accountsLogic.makeAccountInstructor("student2InCourse1");
        assertTrue(accountsLogic.isAccountAnInstructor("student2InCourse1"));
        accountsLogic.downgradeInstructorToStudentCascade("student2InCourse1");

        assertThrows(EntityDoesNotExistException.class, () -> {
            accountsLogic.makeAccountInstructor("id-does-not-exist");
        });
    }

    @Test
    public void testJoinCourseForStudent() throws Exception {

        String correctStudentId = "correctStudentId";
        String courseId = "idOfTypicalCourse1";
        String originalEmail = "original@email.com";

        // Create correct student with original@email.com
        StudentAttributes studentData = StudentAttributes
                .builder(courseId, originalEmail)
                .withName("name")
                .withSectionName("sectionName")
                .withTeamName("teamName")
                .withComment("")
                .build();
        studentsLogic.createStudentCascade(studentData);
        studentData = StudentsLogic.inst().getStudentForEmail(courseId,
                originalEmail);
        StudentAttributes finalStudent = studentData;

        verifyPresentInDatastore(studentData);

        ______TS("failure: wrong key");

        String wrongKey = StringHelper.encrypt("wrongkey");
        EntityDoesNotExistException ednee = assertThrows(EntityDoesNotExistException.class,
                () -> accountsLogic.joinCourseForStudent(wrongKey, correctStudentId));
        assertEquals("No student with given registration key: " + wrongKey, ednee.getMessage());

        ______TS("failure: invalid parameters");

        InvalidParametersException ipe = assertThrows(InvalidParametersException.class,
                () -> accountsLogic.joinCourseForStudent(StringHelper.encrypt(finalStudent.key), "wrong student"));
        AssertHelper.assertContains(FieldValidator.REASON_INCORRECT_FORMAT, ipe.getMessage());

        ______TS("failure: googleID belongs to an existing student in the course");

        String existingId = "AccLogicT.existing.studentId";
        StudentAttributes existingStudent = StudentAttributes
                .builder(courseId, "differentEmail@email.com")
                .withName("name")
                .withSectionName("sectionName")
                .withTeamName("teamName")
                .withComment("")
                .withGoogleId(existingId)
                .build();
        studentsLogic.createStudentCascade(existingStudent);

        EntityAlreadyExistsException eaee = assertThrows(EntityAlreadyExistsException.class,
                () -> accountsLogic.joinCourseForStudent(StringHelper.encrypt(finalStudent.key), existingId));
        assertEquals("Student has already joined course", eaee.getMessage());

        ______TS("success: without encryption and account already exists");

        AccountAttributes accountData = AccountAttributes.builder(correctStudentId)
                .withName("nameABC")
                .withEmail("real@gmail.com")
                .withInstitute("TEAMMATES Test Institute 1")
                .withIsInstructor(true)
                .build();

        accountsLogic.createAccount(accountData);
        accountsLogic.joinCourseForStudent(StringHelper.encrypt(studentData.key), correctStudentId);

        studentData.googleId = accountData.googleId;
        verifyPresentInDatastore(studentData);
        assertEquals(
                correctStudentId,
                logic.getStudentForEmail(studentData.course, studentData.email).googleId);

        ______TS("failure: already joined");

        eaee = assertThrows(EntityAlreadyExistsException.class,
                () -> accountsLogic.joinCourseForStudent(StringHelper.encrypt(finalStudent.key), correctStudentId));
        assertEquals("Student has already joined course", eaee.getMessage());

        ______TS("failure: valid key belongs to a different user");

        eaee = assertThrows(EntityAlreadyExistsException.class,
                () -> accountsLogic.joinCourseForStudent(StringHelper.encrypt(finalStudent.key), "wrongstudent"));
        assertEquals("Student has already joined course", eaee.getMessage());

        ______TS("success: with encryption and new account to be created");

        accountsLogic.deleteAccountCascade(correctStudentId);

        originalEmail = "email2@gmail.com";
        studentData = StudentAttributes
                .builder(courseId, originalEmail)
                .withName("name")
                .withSectionName("sectionName")
                .withTeamName("teamName")
                .withComment("")
                .build();
        studentsLogic.createStudentCascade(studentData);
        studentData = StudentsLogic.inst().getStudentForEmail(courseId,
                originalEmail);

        String encryptedKey = StringHelper.encrypt(studentData.key);
        accountsLogic.joinCourseForStudent(encryptedKey, correctStudentId);
        studentData.googleId = correctStudentId;
        verifyPresentInDatastore(studentData);
        assertEquals(correctStudentId,
                logic.getStudentForEmail(studentData.course, studentData.email).googleId);

        // check that we have the corresponding new account created.
        accountData.googleId = correctStudentId;
        accountData.email = originalEmail;
        accountData.name = "name";
        accountData.isInstructor = false;
        verifyPresentInDatastore(accountData);

        ______TS("success: join course as student does not revoke instructor status");

        // promote account to instructor
        accountsLogic.makeAccountInstructor(correctStudentId);

        // make the student 'unregistered' again
        studentData.googleId = "";
        studentsLogic.updateStudentCascade(
                StudentAttributes.updateOptionsBuilder(studentData.course, studentData.email)
                        .withGoogleId(studentData.googleId)
                        .build()
        );
        assertEquals("",
                logic.getStudentForEmail(studentData.course, studentData.email).googleId);

        // rejoin
        logic.joinCourseForStudent(encryptedKey, correctStudentId);
        assertEquals(correctStudentId,
                logic.getStudentForEmail(studentData.course, studentData.email).googleId);

        // check if still instructor
        assertTrue(logic.isInstructor(correctStudentId));

        accountsLogic.deleteAccountCascade(correctStudentId);
        accountsLogic.deleteAccountCascade(existingId);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testJoinCourseForInstructor() throws Exception {

        InstructorAttributes instructor = dataBundle.instructors.get("instructorNotYetJoinCourse");
        String loggedInGoogleId = "AccLogicT.instr.id";
        String[] encryptedKey = new String[] {
                instructorsLogic.getEncryptedKeyForInstructor(instructor.courseId, instructor.email),
        };

        ______TS("failure: googleID belongs to an existing instructor in the course");

        EntityAlreadyExistsException eaee = assertThrows(EntityAlreadyExistsException.class,
                () -> accountsLogic.joinCourseForInstructor(encryptedKey[0], "idOfInstructorWithOnlyOneSampleCourse", null));
        assertEquals("Instructor has already joined course", eaee.getMessage());

        ______TS("success: instructor joined and new account be created");

        accountsLogic.joinCourseForInstructor(encryptedKey[0], loggedInGoogleId, null);

        InstructorAttributes joinedInstructor =
                instructorsLogic.getInstructorForEmail(instructor.courseId, instructor.email);
        assertEquals(loggedInGoogleId, joinedInstructor.googleId);

        AccountAttributes accountCreated = accountsLogic.getAccount(loggedInGoogleId);
        assertNotNull(accountCreated);

        ______TS("success: instructor joined but Account object creation goes wrong");

        //Delete account to simulate Account object creation goes wrong
        AccountsDb accountsDb = new AccountsDb();
        accountsDb.deleteAccount(loggedInGoogleId);

        //Try to join course again, Account object should be recreated
        accountsLogic.joinCourseForInstructor(encryptedKey[0], loggedInGoogleId, null);

        joinedInstructor = instructorsLogic.getInstructorForEmail(instructor.courseId, instructor.email);
        assertEquals(loggedInGoogleId, joinedInstructor.googleId);

        accountCreated = accountsLogic.getAccount(loggedInGoogleId);
        assertNotNull(accountCreated);

        accountsLogic.deleteAccountCascade(loggedInGoogleId);

        ______TS("success: instructor joined but account already exists");

        AccountAttributes nonInstrAccount = dataBundle.accounts.get("student1InCourse1");
        InstructorAttributes newIns = InstructorAttributes
                .builder(instructor.courseId, nonInstrAccount.email)
                .withName(nonInstrAccount.name)
                .build();

        instructorsLogic.createInstructor(newIns);
        encryptedKey[0] = instructorsLogic.getEncryptedKeyForInstructor(instructor.courseId, nonInstrAccount.email);
        assertFalse(accountsLogic.getAccount(nonInstrAccount.googleId).isInstructor);

        accountsLogic.joinCourseForInstructor(encryptedKey[0], nonInstrAccount.googleId, null);

        joinedInstructor = instructorsLogic.getInstructorForEmail(instructor.courseId, nonInstrAccount.email);
        assertEquals(nonInstrAccount.googleId, joinedInstructor.googleId);
        assertTrue(accountsLogic.getAccount(nonInstrAccount.googleId).isInstructor);
        instructorsLogic.verifyInstructorExists(nonInstrAccount.googleId);

        ______TS("success: instructor join and assigned institute when some instructors have not joined course");

        instructor = dataBundle.instructors.get("instructor4");
        newIns = InstructorAttributes
                .builder(instructor.courseId, "anInstructorWithoutGoogleId@gmail.com")
                .withName("anInstructorWithoutGoogleId")
                .build();

        instructorsLogic.createInstructor(newIns);

        nonInstrAccount = dataBundle.accounts.get("student2InCourse1");
        nonInstrAccount.email = "newInstructor@gmail.com";
        nonInstrAccount.name = " newInstructor";
        nonInstrAccount.googleId = "newInstructorGoogleId";
        newIns = InstructorAttributes.builder(instructor.courseId, nonInstrAccount.email)
                .withName(nonInstrAccount.name)
                .build();

        instructorsLogic.createInstructor(newIns);
        encryptedKey[0] = instructorsLogic.getEncryptedKeyForInstructor(instructor.courseId, nonInstrAccount.email);

        accountsLogic.joinCourseForInstructor(encryptedKey[0], nonInstrAccount.googleId, null);

        joinedInstructor = instructorsLogic.getInstructorForEmail(instructor.courseId, nonInstrAccount.email);
        assertEquals(nonInstrAccount.googleId, joinedInstructor.googleId);
        instructorsLogic.verifyInstructorExists(nonInstrAccount.googleId);

        AccountAttributes instructorAccount = accountsLogic.getAccount(nonInstrAccount.googleId);
        assertEquals("TEAMMATES Test Institute 1", instructorAccount.institute);

        accountsLogic.deleteAccountCascade(nonInstrAccount.googleId);

        ______TS("failure: instructor already joined");

        nonInstrAccount = dataBundle.accounts.get("student1InCourse1");
        instructor = dataBundle.instructors.get("instructorNotYetJoinCourse");

        encryptedKey[0] = instructorsLogic.getEncryptedKeyForInstructor(instructor.courseId, nonInstrAccount.email);
        joinedInstructor = instructorsLogic.getInstructorForEmail(instructor.courseId, nonInstrAccount.email);
        InstructorAttributes[] finalInstructor = new InstructorAttributes[] { joinedInstructor };
        eaee = assertThrows(EntityAlreadyExistsException.class,
                () -> accountsLogic.joinCourseForInstructor(encryptedKey[0], finalInstructor[0].googleId, null));
        assertEquals("Instructor has already joined course", eaee.getMessage());

        ______TS("failure: key belongs to a different user");

        eaee = assertThrows(EntityAlreadyExistsException.class,
                () -> accountsLogic.joinCourseForInstructor(encryptedKey[0], "otherUserId", null));
        assertEquals("Instructor has already joined course", eaee.getMessage());

        ______TS("failure: invalid key");
        String invalidKey = StringHelper.encrypt("invalidKey");

        EntityDoesNotExistException ednee = assertThrows(EntityDoesNotExistException.class,
                () -> accountsLogic.joinCourseForInstructor(invalidKey, loggedInGoogleId, null));
        assertEquals("No instructor with given registration key: " + invalidKey,
                ednee.getMessage());
    }

    @Test
    public void testDeleteAccountCascade() throws Exception {

        ______TS("typical success case");

        InstructorAttributes instructor = dataBundle.instructors.get("instructor5");
        AccountAttributes account = dataBundle.accounts.get("instructor5");
        // create a profile for the account
        StudentProfileAttributes studentProfile = StudentProfileAttributes.builder(account.googleId)
                .withShortName("Test")
                .build();
        profilesLogic.updateOrCreateStudentProfile(
                StudentProfileAttributes.updateOptionsBuilder(account.googleId)
                        .withShortName(studentProfile.shortName)
                        .build());

        // Make instructor account id a student too.
        StudentAttributes student = StudentAttributes
                .builder(instructor.courseId, "email@com")
                .withName(instructor.name)
                .withSectionName("section")
                .withTeamName("team")
                .withComment("")
                .withGoogleId(instructor.googleId)
                .build();
        studentsLogic.createStudentCascade(student);
        verifyPresentInDatastore(account);
        verifyPresentInDatastore(studentProfile);
        verifyPresentInDatastore(instructor);
        verifyPresentInDatastore(student);

        accountsLogic.deleteAccountCascade(instructor.googleId);

        verifyAbsentInDatastore(account);
        verifyAbsentInDatastore(studentProfile);
        verifyAbsentInDatastore(instructor);
        verifyAbsentInDatastore(student);
    }

    //TODO: add missing test cases
}
