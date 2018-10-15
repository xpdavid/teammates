package teammates.test.cases.storage;

import static teammates.common.util.FieldValidator.COURSE_ID_ERROR_MESSAGE;
import static teammates.common.util.FieldValidator.REASON_INCORRECT_FORMAT;

import org.testng.annotations.Test;

import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.exception.EntityAlreadyExistsException;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.Const;
import teammates.common.util.FieldValidator;
import teammates.common.util.StringHelper;
import teammates.storage.api.StudentsDb;
import teammates.test.cases.BaseComponentTestCase;
import teammates.test.driver.AssertHelper;

/**
 * SUT: {@link StudentsDb}.
 */
public class StudentsDbTest extends BaseComponentTestCase {

    private StudentsDb studentsDb = new StudentsDb();

    @Test
    public void testTimestamp() throws InvalidParametersException, EntityDoesNotExistException {
        ______TS("success : created");

        StudentAttributes s = createNewStudent();

        StudentAttributes student = studentsDb.getStudentForEmail(s.course, s.email);
        assertNotNull(student);

        // Assert dates are now.
        AssertHelper.assertInstantIsNow(student.getCreatedAt());
        AssertHelper.assertInstantIsNow(student.getUpdatedAt());

        ______TS("success : update lastUpdated");

        s.name = "new-name";
        studentsDb.updateStudent(
                StudentAttributes.updateOptionsBuilder(s.course, s.email)
                        .withName(s.name)
                        .build(),
                true, false);
        StudentAttributes updatedStudent = studentsDb.getStudentForGoogleId(s.course, s.googleId);

        // Assert lastUpdate has changed, and is now.
        assertFalse(student.getUpdatedAt().equals(updatedStudent.getUpdatedAt()));
        AssertHelper.assertInstantIsNow(updatedStudent.getUpdatedAt());

        ______TS("success : keep lastUpdated");

        s.name = "new-name-2";
        studentsDb.updateStudent(
                StudentAttributes.updateOptionsBuilder(s.course, s.email)
                        .withName(s.name)
                        .build(),
                true, true);
        StudentAttributes updatedStudent2 = studentsDb.getStudentForGoogleId(s.course, s.googleId);

        // Assert lastUpdate has NOT changed.
        assertEquals(updatedStudent.getUpdatedAt(), updatedStudent2.getUpdatedAt());
    }

    @Test
    public void testCreateStudent() throws Exception {

        StudentAttributes s = StudentAttributes
                .builder("course id", "valid student", "valid-fresh@email.com")
                .withComments("")
                .withTeam("validTeamName")
                .withSection("validSectionName")
                .withGoogleId("validGoogleId")
                .withLastName("student")
                .build();

        ______TS("fail : invalid params");
        s.course = "invalid id space";
        InvalidParametersException ipe = assertThrows(InvalidParametersException.class, () -> studentsDb.createEntity(s));
        AssertHelper.assertContains(
                getPopulatedErrorMessage(
                        COURSE_ID_ERROR_MESSAGE, s.course,
                        FieldValidator.COURSE_ID_FIELD_NAME, REASON_INCORRECT_FORMAT,
                        FieldValidator.COURSE_ID_MAX_LENGTH),
                ipe.getMessage());
        verifyAbsentInDatastore(s);

        ______TS("success : valid params");
        s.course = "valid-course";

        // remove possibly conflicting entity from the database
        studentsDb.deleteStudent(s.course, s.email);

        studentsDb.createEntity(s);
        verifyPresentInDatastore(s);
        StudentAttributes retrievedStudent = studentsDb.getStudentForGoogleId(s.course, s.googleId);
        assertTrue(retrievedStudent.isEnrollInfoSameAs(s));
        assertNull(studentsDb.getStudentForGoogleId(s.course + "not existing", s.googleId));
        assertNull(studentsDb.getStudentForGoogleId(s.course, s.googleId + "not existing"));
        assertNull(studentsDb.getStudentForGoogleId(s.course + "not existing", s.googleId + "not existing"));

        ______TS("fail : duplicate");
        EntityAlreadyExistsException eaee = assertThrows(EntityAlreadyExistsException.class,
                () -> studentsDb.createEntity(s));
        AssertHelper.assertContains(
                String.format(StudentsDb.ERROR_CREATE_ENTITY_ALREADY_EXISTS, s.getEntityTypeAsString())
                        + s.getIdentificationString(),
                eaee.getMessage());

        ______TS("null params check");
        AssertionError ae = assertThrows(AssertionError.class, () -> studentsDb.createEntity(null));
        assertEquals(Const.StatusCodes.DBLEVEL_NULL_INPUT, ae.getMessage());

    }

    @Test
    public void testGetStudent() throws InvalidParametersException, EntityDoesNotExistException {

        StudentAttributes s = createNewStudent();
        s.googleId = "validGoogleId";
        s.team = "validTeam";
        studentsDb.updateStudent(
                StudentAttributes.updateOptionsBuilder(s.course, s.email)
                        .withGoogleId(s.googleId)
                        .withTeamName(s.team)
                        .build(),
                true, false);

        ______TS("typical success case for getStudentForRegistrationKey: existing student");
        StudentAttributes retrieved = studentsDb.getStudentForEmail(s.course, s.email);
        assertNotNull(retrieved);
        assertNotNull(studentsDb.getStudentForRegistrationKey(StringHelper.encrypt(retrieved.key)));

        assertNull(studentsDb.getStudentForRegistrationKey(StringHelper.encrypt("notExistingKey")));

        ______TS("non existant student case");

        retrieved = studentsDb.getStudentForEmail("any-course-id", "non-existent@email.com");
        assertNull(retrieved);

        StudentAttributes s2 = createNewStudent("one.new@gmail.com");
        s2.googleId = "validGoogleId2";
        studentsDb.updateStudent(
                StudentAttributes.updateOptionsBuilder(s2.course, s2.email)
                        .withGoogleId(s2.googleId)
                        .build(),
                true, false);
        studentsDb.deleteStudentsForGoogleIdWithoutDocument(s2.googleId);
        assertNull(studentsDb.getStudentForGoogleId(s2.course, s2.googleId));

        s2 = createNewStudent("one.new@gmail.com");
        assertTrue(studentsDb.getUnregisteredStudentsForCourse(s2.course).get(0).isEnrollInfoSameAs(s2));

        assertTrue(s.isEnrollInfoSameAs(studentsDb.getStudentsForGoogleId(s.googleId).get(0)));
        assertTrue(studentsDb.getStudentsForCourse(s.course).get(0).isEnrollInfoSameAs(s)
                || studentsDb.getStudentsForCourse(s.course).get(0).isEnrollInfoSameAs(s2));
        assertTrue(studentsDb.getStudentsForTeam(s.team, s.course).get(0).isEnrollInfoSameAs(s));

        ______TS("null params case");
        AssertionError ae = assertThrows(AssertionError.class, () -> studentsDb.getStudentForEmail(null, "valid@email.com"));
        assertEquals(Const.StatusCodes.DBLEVEL_NULL_INPUT, ae.getMessage());

        ae = assertThrows(AssertionError.class, () -> studentsDb.getStudentForEmail("any-course-id", null));
        assertEquals(Const.StatusCodes.DBLEVEL_NULL_INPUT, ae.getMessage());

        studentsDb.deleteStudent(s.course, s.email);
        studentsDb.deleteStudent(s2.course, s2.email);
    }

    @Test
    public void testUpdateStudentWithoutDocument() throws InvalidParametersException, EntityDoesNotExistException {

        // Create a new student with valid attributes
        StudentAttributes s = createNewStudent();
        studentsDb.updateStudent(
                StudentAttributes.updateOptionsBuilder(s.course, s.email)
                        .withGoogleId("new.google.id")
                        .withComment("lorem ipsum dolor si amet")
                        .withNewEmail("new@email.com")
                        .withSectionName("new-section")
                        .withTeamName("new-team")
                        .withName("new-name")
                        .build(),
                true, false);

        ______TS("non-existent case");
        StudentAttributes.UpdateOptions updateOptions =
                StudentAttributes.updateOptionsBuilder("non-existent-course", "non@existent.email")
                        .withName("no-name")
                        .build();
        EntityDoesNotExistException ednee = assertThrows(EntityDoesNotExistException.class,
                () -> studentsDb.updateStudent(updateOptions, true, false));
        assertEquals(StudentsDb.ERROR_UPDATE_NON_EXISTENT_STUDENT + updateOptions,
                ednee.getMessage());

        // Only check first 2 params (course & email) which are used to identify the student entry.
        // The rest are actually allowed to be null.
        ______TS("null course case");
        AssertionError ae = assertThrows(AssertionError.class,
                () -> studentsDb.updateStudent(
                        StudentAttributes.updateOptionsBuilder(null, s.email)
                                .withName("new-name")
                                .build(),
                        true, false));
        assertEquals(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, ae.getMessage());

        ______TS("null email case");
        ae = assertThrows(AssertionError.class,
                () -> studentsDb.updateStudent(
                        StudentAttributes.updateOptionsBuilder(s.course, null)
                                .withName("new-name")
                                .build(),
                        true, false));
        assertEquals(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, ae.getMessage());

        ______TS("duplicate email case");
        StudentAttributes duplicate = createNewStudent();
        // Create a second student with different email address
        StudentAttributes s2 = createNewStudent("valid2@email.com");
        StudentAttributes.UpdateOptions updateOptionsForS2 =
                StudentAttributes.updateOptionsBuilder(duplicate.course, duplicate.email)
                        .withNewEmail(s2.email)
                        .build();
        InvalidParametersException ipe = assertThrows(InvalidParametersException.class,
                () -> studentsDb.updateStudent(updateOptionsForS2, true, false));
        assertEquals(StudentsDb.ERROR_UPDATE_EMAIL_ALREADY_USED + s2.name + "/" + s2.email, ipe.getMessage());

        ______TS("typical success case");
        String originalEmail = s.email;
        s.name = "new-name-2";
        s.team = "new-team-2";
        s.email = "new-email-2@email.com";
        s.googleId = "new-id-2";
        s.comments = "this are new comments";
        studentsDb.updateStudent(
                StudentAttributes.updateOptionsBuilder(s.course, originalEmail)
                        .withNewEmail(s.email)
                        .withName(s.name)
                        .withTeamName(s.team)
                        .withSectionName(s.section)
                        .withGoogleId(s.googleId)
                        .withComment(s.comments)
                        .build(),
                true, false);

        StudentAttributes updatedStudent = studentsDb.getStudentForEmail(s.course, s.email);
        assertTrue(updatedStudent.isEnrollInfoSameAs(s));

    }

    @SuppressWarnings("deprecation")
    @Test
    public void testDeleteStudent() throws InvalidParametersException, EntityDoesNotExistException {
        StudentAttributes s = createNewStudent();
        s.googleId = "validGoogleId";
        studentsDb.updateStudent(
                StudentAttributes.updateOptionsBuilder(s.course, s.email)
                        .withGoogleId(s.googleId)
                        .build(),
                true, false);
        // Delete
        studentsDb.deleteStudentWithoutDocument(s.course, s.email);

        StudentAttributes deleted = studentsDb.getStudentForEmail(s.course, s.email);

        assertNull(deleted);
        studentsDb.deleteStudentsForGoogleIdWithoutDocument(s.googleId);
        assertNull(studentsDb.getStudentForGoogleId(s.course, s.googleId));
        int currentStudentNum = studentsDb.getAllStudents().size();
        s = createNewStudent();
        createNewStudent("secondStudent@mail.com");
        assertEquals(2 + currentStudentNum, studentsDb.getAllStudents().size());
        studentsDb.deleteStudentsForCourseWithoutDocument(s.course);
        assertEquals(currentStudentNum, studentsDb.getAllStudents().size());
        // delete again - should fail silently
        studentsDb.deleteStudentWithoutDocument(s.course, s.email);

        // Null params check:
        StudentAttributes[] finalStudent = new StudentAttributes[] { s };
        AssertionError ae = assertThrows(AssertionError.class,
                () -> studentsDb.deleteStudentWithoutDocument(null, finalStudent[0].email));
        assertEquals(Const.StatusCodes.DBLEVEL_NULL_INPUT, ae.getMessage());

        ae = assertThrows(AssertionError.class,
                () -> studentsDb.deleteStudentWithoutDocument(finalStudent[0].course, null));
        assertEquals(Const.StatusCodes.DBLEVEL_NULL_INPUT, ae.getMessage());

        studentsDb.deleteStudent(s.course, s.email);

    }

    private StudentAttributes createNewStudent() throws InvalidParametersException {
        StudentAttributes s = StudentAttributes
                .builder("valid-course", "valid student", "valid@email.com")
                .withComments("")
                .withTeam("validTeamName")
                .withSection("validSectionName")
                .withGoogleId("")
                .build();

        try {
            studentsDb.createEntity(s);
        } catch (EntityAlreadyExistsException e) {
            // Okay if it's already inside
            ignorePossibleException();
        }

        return s;
    }

    private StudentAttributes createNewStudent(String email) throws InvalidParametersException {
        StudentAttributes s = StudentAttributes
                .builder("valid-course", "valid student 2", email)
                .withComments("")
                .withTeam("valid team name")
                .withSection("valid section name")
                .withGoogleId("")
                .build();

        try {
            studentsDb.createEntity(s);
        } catch (EntityAlreadyExistsException e) {
            // Okay if it's already inside
            ignorePossibleException();
        }

        return s;
    }
}
