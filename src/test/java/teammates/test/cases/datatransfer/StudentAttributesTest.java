package teammates.test.cases.datatransfer;

import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

import teammates.common.datatransfer.StudentUpdateStatus;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.util.Config;
import teammates.common.util.Const;
import teammates.common.util.FieldValidator;
import teammates.common.util.StringHelper;
import teammates.storage.entity.CourseStudent;
import teammates.test.cases.BaseTestCaseWithMinimalGaeEnvironment;
import teammates.test.driver.StringHelperExtension;

/**
 * SUT: {@link StudentAttributes}.
 */
public class StudentAttributesTest extends BaseTestCaseWithMinimalGaeEnvironment {

    @Test
    public void testBuilder_buildNothing_shouldUseDefaultValues() {
        StudentAttributes student = StudentAttributes
                .builder("courseId", "e@e.com")
                .build();

        assertEquals("courseId", student.course);
        assertEquals("e@e.com", student.email);

        assertNull(student.name);
        assertNull(student.lastName);
        assertEquals("", student.googleId);
        assertNull(student.team);
        assertEquals(Const.DEFAULT_SECTION, student.section);
        assertNull(student.comments);
        assertNull(student.key);
        assertEquals(StudentUpdateStatus.UNKNOWN, student.updateStatus);
        assertEquals(Const.TIME_REPRESENTS_DEFAULT_TIMESTAMP, student.getCreatedAt());
        assertEquals(Const.TIME_REPRESENTS_DEFAULT_TIMESTAMP, student.getUpdatedAt());
    }

    @Test
    public void testBuilder_nullValues_shouldThrowException() {
        assertThrows(AssertionError.class, () -> {
            StudentAttributes
                    .builder(null, "email@email.com")
                    .build();
        });

        assertThrows(AssertionError.class, () -> {
            StudentAttributes
                    .builder("course", null)
                    .build();
        });

        assertThrows(AssertionError.class, () -> {
            StudentAttributes
                    .builder("course", "email@email.com")
                    .withName(null)
                    .build();
        });

        assertThrows(AssertionError.class, () -> {
            StudentAttributes
                    .builder("course", "email@email.com")
                    .withLastName(null)
                    .build();
        });

        assertThrows(AssertionError.class, () -> {
            StudentAttributes
                    .builder("course", "email@email.com")
                    .withTeamName(null)
                    .build();
        });

        assertThrows(AssertionError.class, () -> {
            StudentAttributes
                    .builder("course", "email@email.com")
                    .withSectionName(null)
                    .build();
        });

        assertThrows(AssertionError.class, () -> {
            StudentAttributes
                    .builder("course", "email@email.com")
                    .withComment(null)
                    .build();
        });
    }

    @Test
    public void testGetCopy() {
        CourseStudent student = new CourseStudent("email@email.com", "name 1", "googleId.1",
                "comment 1", "courseId1", "team 1", "sect 1");
        StudentAttributes originalStudent = StudentAttributes.valueOf(student);

        StudentAttributes copyStudent = originalStudent.getCopy();

        assertEquals(originalStudent.course, copyStudent.course);
        assertEquals(originalStudent.name, copyStudent.name);
        assertEquals(originalStudent.email, copyStudent.email);
        assertEquals(originalStudent.googleId, copyStudent.googleId);
        assertEquals(originalStudent.comments, copyStudent.comments);
        assertEquals(originalStudent.key, copyStudent.key);
        assertEquals(originalStudent.updateStatus, copyStudent.updateStatus);
        assertEquals(originalStudent.lastName, copyStudent.lastName);
        assertEquals(originalStudent.section, copyStudent.section);
        assertEquals(originalStudent.team, copyStudent.team);
        assertEquals(originalStudent.getCreatedAt(), copyStudent.getCreatedAt());
        assertEquals(originalStudent.getUpdatedAt(), copyStudent.getUpdatedAt());
    }

    @Test
    public void testValueOf_withAllFieldPopulatedCourseStudent_shouldGenerateAttributesCorrectly() {
        CourseStudent originalStudent = new CourseStudent("email@email.com", "name 1", "googleId.1",
                "comment 1", "courseId1", "team 1", "sect 1");
        StudentAttributes copyStudent = StudentAttributes.valueOf(originalStudent);

        assertEquals(originalStudent.getCourseId(), copyStudent.course);
        assertEquals(originalStudent.getName(), copyStudent.name);
        assertEquals(originalStudent.getEmail(), copyStudent.email);
        assertEquals(originalStudent.getGoogleId(), copyStudent.googleId);
        assertEquals(originalStudent.getComments(), copyStudent.comments);
        assertEquals(originalStudent.getRegistrationKey(), copyStudent.key);
        assertEquals(originalStudent.getLastName(), copyStudent.lastName);
        assertEquals(originalStudent.getSectionName(), copyStudent.section);
        assertEquals(originalStudent.getTeamName(), copyStudent.team);
        assertEquals(originalStudent.getCreatedAt(), copyStudent.getCreatedAt());
        assertEquals(originalStudent.getUpdatedAt(), copyStudent.getUpdatedAt());
    }

    @Test
    public void testValueOf_withSomeFieldsPopulatedAsNull_shouldUseDefaultValues() {
        CourseStudent originalStudent = new CourseStudent("email@email.com", "name 1", null,
                "comment 1", "courseId1", "team 1", null);
        originalStudent.setCreatedAt(null);
        originalStudent.setLastUpdate(null);
        StudentAttributes copyStudent = StudentAttributes.valueOf(originalStudent);

        assertEquals(originalStudent.getCourseId(), copyStudent.course);
        assertEquals(originalStudent.getName(), copyStudent.name);
        assertEquals(originalStudent.getEmail(), copyStudent.email);
        assertEquals("", copyStudent.googleId);
        assertEquals(originalStudent.getComments(), copyStudent.comments);
        assertEquals(originalStudent.getRegistrationKey(), copyStudent.key);
        assertEquals(originalStudent.getLastName(), copyStudent.lastName);
        assertEquals(Const.DEFAULT_SECTION, copyStudent.section);
        assertEquals(originalStudent.getTeamName(), copyStudent.team);
        assertEquals(Const.TIME_REPRESENTS_DEFAULT_TIMESTAMP, copyStudent.getCreatedAt());
        assertEquals(Const.TIME_REPRESENTS_DEFAULT_TIMESTAMP, copyStudent.getUpdatedAt());
    }

    @Test
    public void testUpdateStatusEnum() {
        assertEquals(StudentUpdateStatus.ERROR, StudentUpdateStatus.enumRepresentation(0));
        assertEquals(StudentUpdateStatus.NEW, StudentUpdateStatus.enumRepresentation(1));
        assertEquals(StudentUpdateStatus.MODIFIED, StudentUpdateStatus.enumRepresentation(2));
        assertEquals(StudentUpdateStatus.UNMODIFIED, StudentUpdateStatus.enumRepresentation(3));
        assertEquals(StudentUpdateStatus.NOT_IN_ENROLL_LIST, StudentUpdateStatus.enumRepresentation(4));
        assertEquals(StudentUpdateStatus.UNKNOWN, StudentUpdateStatus.enumRepresentation(5));
        assertEquals(StudentUpdateStatus.UNKNOWN, StudentUpdateStatus.enumRepresentation(-1));
    }

    @Test
    public void testBuilder_withTypicalData_shouldBuildAttributeWithCorrectValue() throws Exception {
        CourseStudent expected = generateTypicalStudentObject();

        StudentAttributes studentUnderTest = StudentAttributes
                .builder(expected.getCourseId(), expected.getEmail())
                .withName(expected.getName())
                .withLastName(expected.getLastName())
                .withComment(expected.getComments())
                .withTeamName(expected.getTeamName())
                .withSectionName(expected.getSectionName())
                .withGoogleId(expected.getGoogleId())
                .build();

        assertEquals(expected.getCourseId(), studentUnderTest.getCourse());
        assertEquals(expected.getName(), studentUnderTest.getName());
        assertEquals(expected.getLastName(), studentUnderTest.getLastName());
        assertEquals(expected.getComments(), studentUnderTest.getComments());
        assertEquals(expected.getSectionName(), studentUnderTest.getSection());
        assertEquals(expected.getTeamName(), studentUnderTest.getTeam());
        assertEquals(expected.getGoogleId(), studentUnderTest.getGoogleId());

        assertEquals(Const.TIME_REPRESENTS_DEFAULT_TIMESTAMP, studentUnderTest.getCreatedAt());
        assertEquals(Const.TIME_REPRESENTS_DEFAULT_TIMESTAMP, studentUnderTest.getUpdatedAt());
    }

    @Test
    public void testValidate() throws Exception {

        ______TS("Typical cases: multiple invalid fields");
        StudentAttributes s = generateValidStudentAttributesObject();

        assertTrue("valid value", s.isValid());

        s.googleId = "invalid@google@id";
        s.name = "";
        s.email = "invalid email";
        s.course = "";
        s.comments = StringHelperExtension.generateStringOfLength(FieldValidator.STUDENT_ROLE_COMMENTS_MAX_LENGTH + 1);
        s.team = StringHelperExtension.generateStringOfLength(FieldValidator.TEAM_NAME_MAX_LENGTH + 1);

        assertFalse("invalid value", s.isValid());
        String errorMessage =
                getPopulatedErrorMessage(
                    FieldValidator.GOOGLE_ID_ERROR_MESSAGE, "invalid@google@id",
                    FieldValidator.GOOGLE_ID_FIELD_NAME, FieldValidator.REASON_INCORRECT_FORMAT,
                    FieldValidator.GOOGLE_ID_MAX_LENGTH) + System.lineSeparator()
                + getPopulatedEmptyStringErrorMessage(
                      FieldValidator.COURSE_ID_ERROR_MESSAGE_EMPTY_STRING,
                      FieldValidator.COURSE_ID_FIELD_NAME, FieldValidator.COURSE_ID_MAX_LENGTH) + System.lineSeparator()
                + getPopulatedErrorMessage(
                      FieldValidator.EMAIL_ERROR_MESSAGE, "invalid email",
                      FieldValidator.EMAIL_FIELD_NAME, FieldValidator.REASON_INCORRECT_FORMAT,
                      FieldValidator.EMAIL_MAX_LENGTH) + System.lineSeparator()
                + getPopulatedErrorMessage(
                      FieldValidator.SIZE_CAPPED_NON_EMPTY_STRING_ERROR_MESSAGE,
                      "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                      FieldValidator.TEAM_NAME_FIELD_NAME, FieldValidator.REASON_TOO_LONG,
                      FieldValidator.TEAM_NAME_MAX_LENGTH) + System.lineSeparator()
                + getPopulatedErrorMessage(
                      FieldValidator.SIZE_CAPPED_POSSIBLY_EMPTY_STRING_ERROR_MESSAGE, s.comments,
                      FieldValidator.STUDENT_ROLE_COMMENTS_FIELD_NAME, FieldValidator.REASON_TOO_LONG,
                      FieldValidator.STUDENT_ROLE_COMMENTS_MAX_LENGTH) + System.lineSeparator()
                + getPopulatedEmptyStringErrorMessage(
                      FieldValidator.SIZE_CAPPED_NON_EMPTY_STRING_ERROR_MESSAGE_EMPTY_STRING,
                      FieldValidator.PERSON_NAME_FIELD_NAME, FieldValidator.PERSON_NAME_MAX_LENGTH);
        assertEquals("invalid value", errorMessage, StringHelper.toString(s.getInvalidityInfo()));

        ______TS("Failure case: student name too long");
        String longStudentName = StringHelperExtension
                .generateStringOfLength(FieldValidator.PERSON_NAME_MAX_LENGTH + 1);
        StudentAttributes invalidStudent = StudentAttributes
                .builder("courseId", "e@e.com")
                .withName(longStudentName)
                .withSectionName("sect")
                .withComment("c")
                .withTeamName("t1")
                .build();

        assertFalse(invalidStudent.isValid());
        assertEquals(getPopulatedErrorMessage(
                FieldValidator.SIZE_CAPPED_NON_EMPTY_STRING_ERROR_MESSAGE, longStudentName,
                FieldValidator.PERSON_NAME_FIELD_NAME, FieldValidator.REASON_TOO_LONG,
                FieldValidator.PERSON_NAME_MAX_LENGTH),
                invalidStudent.getInvalidityInfo().get(0));

        ______TS("Failure case: section name too long");
        String longSectionName = StringHelperExtension
                .generateStringOfLength(FieldValidator.SECTION_NAME_MAX_LENGTH + 1);
        invalidStudent = StudentAttributes
                .builder("courseId", "e@e.com")
                .withName("")
                .withSectionName(longSectionName)
                .withComment("c")
                .withTeamName("t1")
                .build();

        assertFalse(invalidStudent.isValid());
        assertEquals(getPopulatedErrorMessage(
                FieldValidator.SIZE_CAPPED_NON_EMPTY_STRING_ERROR_MESSAGE, longSectionName,
                FieldValidator.SECTION_NAME_FIELD_NAME, FieldValidator.REASON_TOO_LONG,
                FieldValidator.SECTION_NAME_MAX_LENGTH),
                invalidStudent.getInvalidityInfo().get(0));

        ______TS("Failure case: empty email");
        invalidStudent = StudentAttributes
                .builder("course", "")
                .withName("n")
                .withSectionName("sect")
                .withComment("c")
                .withTeamName("t1")
                .build();

        assertFalse(invalidStudent.isValid());
        assertEquals(getPopulatedEmptyStringErrorMessage(
                FieldValidator.EMAIL_ERROR_MESSAGE_EMPTY_STRING,
                FieldValidator.EMAIL_FIELD_NAME, FieldValidator.EMAIL_MAX_LENGTH),
                invalidStudent.getInvalidityInfo().get(0));

        ______TS("Failure case: empty name");
        invalidStudent = StudentAttributes
                .builder("course", "e@e.com")
                .withName("")
                .withSectionName("sect")
                .withComment("c")
                .withTeamName("t1")
                .build();

        assertFalse(invalidStudent.isValid());
        assertEquals(invalidStudent.getInvalidityInfo().get(0),
                getPopulatedEmptyStringErrorMessage(
                        FieldValidator.SIZE_CAPPED_NON_EMPTY_STRING_ERROR_MESSAGE_EMPTY_STRING,
                        FieldValidator.PERSON_NAME_FIELD_NAME, FieldValidator.PERSON_NAME_MAX_LENGTH));

        ______TS("Failure case: invalid course id");
        invalidStudent = StudentAttributes
                .builder("Course Id with space", "e@e.com")
                .withName("name")
                .withSectionName("section")
                .withComment("c")
                .withTeamName("team")
                .build();

        assertFalse(invalidStudent.isValid());
        assertEquals(getPopulatedErrorMessage(
                FieldValidator.COURSE_ID_ERROR_MESSAGE, invalidStudent.course,
                FieldValidator.COURSE_ID_FIELD_NAME, FieldValidator.REASON_INCORRECT_FORMAT,
                FieldValidator.COURSE_ID_MAX_LENGTH),
                invalidStudent.getInvalidityInfo().get(0));

    }

    @Test
    public void testUpdateOptions_withTypicalUpdateOptions_shouldUpdateAttributeCorrectly() {
        StudentAttributes.UpdateOptions updateOptions =
                StudentAttributes.updateOptionsBuilder("courseId", "email@email.com")
                        .withNewEmail("new@email.com")
                        .withName("John Doe")
                        .withLastName("Wu")
                        .withComment("Comment")
                        .withGoogleId("googleId")
                        .withTeamName("teamName")
                        .withSectionName("sectionName")
                        .build();

        assertEquals("courseId", updateOptions.getCourseId());
        assertEquals("email@email.com", updateOptions.getEmail());

        StudentAttributes studentAttributes =
                StudentAttributes.builder("course", "alice@gmail.tmt")
                        .withName("Alice")
                        .withLastName("Li")
                        .withComment("Comment B")
                        .withGoogleId("googleIdC")
                        .withTeamName("TEAM B")
                        .withSectionName("Section C")
                        .build();

        // last name is specified in updateOptions, use the value.
        studentAttributes.update(updateOptions);

        assertEquals("new@email.com", studentAttributes.getEmail());
        assertEquals("John Doe", studentAttributes.getName());
        assertEquals("Wu", studentAttributes.getLastName());
        assertEquals("Comment", studentAttributes.getComments());
        assertEquals("googleId", studentAttributes.googleId);
        assertEquals("teamName", studentAttributes.getTeam());
        assertEquals("sectionName", studentAttributes.getSection());

        updateOptions =
                StudentAttributes.updateOptionsBuilder("courseId", "new@email.com")
                        .withName("John Doe")
                        .build();

        // last name not specified in updateOptions, split the name.
        studentAttributes.update(updateOptions);
        assertEquals("Doe", studentAttributes.getLastName());
    }

    @Test
    public void testUpdateOptionsBuilder_withNullInput_shouldFailWithAssertionError() {
        assertThrows(AssertionError.class, () ->
                StudentAttributes.updateOptionsBuilder(null, "email@email.com"));
        assertThrows(AssertionError.class, () ->
                StudentAttributes.updateOptionsBuilder("course", null));
        assertThrows(AssertionError.class, () ->
                StudentAttributes.updateOptionsBuilder("course", "email@email.com")
                        .withNewEmail(null));
        assertThrows(AssertionError.class, () ->
                StudentAttributes.updateOptionsBuilder("course", "email@email.com")
                        .withName(null));
        assertThrows(AssertionError.class, () ->
                StudentAttributes.updateOptionsBuilder("course", "email@email.com")
                        .withLastName(null));
        assertThrows(AssertionError.class, () ->
                StudentAttributes.updateOptionsBuilder("course", "email@email.com")
                        .withComment(null));
        assertThrows(AssertionError.class, () ->
                StudentAttributes.updateOptionsBuilder("course", "email@email.com")
                        .withTeamName(null));
        assertThrows(AssertionError.class, () ->
                StudentAttributes.updateOptionsBuilder("course", "email@email.com")
                        .withSectionName(null));
    }

    @Test
    public void testIsEnrollInfoSameAs() {
        StudentAttributes student = StudentAttributes.valueOf(generateTypicalStudentObject());
        StudentAttributes other = StudentAttributes.valueOf(generateTypicalStudentObject());

        ______TS("Typical case: Same enroll info");
        assertTrue(student.isEnrollInfoSameAs(other));

        ______TS("Typical case: Compare to null");
        assertFalse(student.isEnrollInfoSameAs(null));

        ______TS("Typical case: Different in email");
        other.email = "other@email.com";
        assertFalse(student.isEnrollInfoSameAs(other));

        ______TS("Typical case: Different in name");
        other = StudentAttributes.valueOf(generateTypicalStudentObject());
        other.name = "otherName";
        assertFalse(student.isEnrollInfoSameAs(other));

        ______TS("Typical case: Different in course id");
        other = StudentAttributes.valueOf(generateTypicalStudentObject());
        other.course = "otherCourse";
        assertFalse(student.isEnrollInfoSameAs(other));

        ______TS("Typical case: Different in comment");
        other = StudentAttributes.valueOf(generateTypicalStudentObject());
        other.comments = "otherComments";
        assertFalse(student.isEnrollInfoSameAs(other));

        ______TS("Typical case: Different in team");
        other = StudentAttributes.valueOf(generateTypicalStudentObject());
        other.team = "otherTeam";
        assertFalse(student.isEnrollInfoSameAs(other));

        ______TS("Typical case: Different in section");
        other = StudentAttributes.valueOf(generateStudentWithoutSectionObject());
        assertFalse(student.isEnrollInfoSameAs(other));
    }

    @Test
    public void testSortByNameAndThenByEmail() {
        List<StudentAttributes> sortedList = generateTypicalStudentAttributesList();
        StudentAttributes.sortByNameAndThenByEmail(sortedList);
        List<StudentAttributes> unsortedList = generateTypicalStudentAttributesList();
        assertEquals(sortedList.get(0).toEnrollmentString(), unsortedList.get(0).toEnrollmentString());
        assertEquals(sortedList.get(1).toEnrollmentString(), unsortedList.get(3).toEnrollmentString());
        assertEquals(sortedList.get(2).toEnrollmentString(), unsortedList.get(2).toEnrollmentString());
        assertEquals(sortedList.get(3).toEnrollmentString(), unsortedList.get(1).toEnrollmentString());
    }

    @Test
    public void testSortByTeam() {
        List<StudentAttributes> sortedList = generateTypicalStudentAttributesList();
        StudentAttributes.sortByTeamName(sortedList);
        List<StudentAttributes> unsortedList = generateTypicalStudentAttributesList();
        assertEquals(sortedList.get(0).toEnrollmentString(),
                     unsortedList.get(2).toEnrollmentString());
        assertEquals(sortedList.get(1).toEnrollmentString(),
                     unsortedList.get(0).toEnrollmentString());
        assertEquals(sortedList.get(2).toEnrollmentString(),
                     unsortedList.get(1).toEnrollmentString());
        assertEquals(sortedList.get(3).toEnrollmentString(),
                     unsortedList.get(3).toEnrollmentString());
    }

    @Test
    public void testSortBySection() {
        List<StudentAttributes> sortedList = generateTypicalStudentAttributesList();
        StudentAttributes.sortBySectionName(sortedList);
        List<StudentAttributes> unsortedList = generateTypicalStudentAttributesList();
        assertEquals(sortedList.get(0).toEnrollmentString(),
                     unsortedList.get(3).toEnrollmentString());
        assertEquals(sortedList.get(1).toEnrollmentString(),
                     unsortedList.get(0).toEnrollmentString());
        assertEquals(sortedList.get(2).toEnrollmentString(),
                     unsortedList.get(1).toEnrollmentString());
        assertEquals(sortedList.get(3).toEnrollmentString(),
                     unsortedList.get(2).toEnrollmentString());
    }

    @Test
    public void testIsRegistered() {
        StudentAttributes sd = StudentAttributes
                .builder("course1", "email@email.com")
                .withName("name 1")
                .withSectionName("sect 1")
                .withComment("comment 1")
                .withTeamName("team 1")
                .build();

        // Id is not given yet
        assertFalse(sd.isRegistered());

        // Id empty
        sd.googleId = "";
        assertFalse(sd.isRegistered());

        // Id given
        sd.googleId = "googleId.1";
        assertTrue(sd.isRegistered());
    }

    @Test
    public void testToString() {
        StudentAttributes sd = StudentAttributes
                .builder("course1", "email@email.com")
                .withName("name 1")
                .withSectionName("sect 1")
                .withComment("comment 1")
                .withTeamName("team 1")
                .build();

        assertEquals("Student:name 1[email@email.com]" + System.lineSeparator(), sd.toString());
        assertEquals("    Student:name 1[email@email.com]" + System.lineSeparator(), sd.toString(4));
    }

    @Test
    public void testToEnrollmentString() {
        StudentAttributes sd = StudentAttributes
                .builder("course1", "email@email.com")
                .withName("name 1")
                .withSectionName("sect 1")
                .withComment("comment 1")
                .withTeamName("team 1")
                .build();

        assertEquals("sect 1|team 1|name 1|email@email.com|comment 1", sd.toEnrollmentString());
    }

    @Test
    public void testGetRegistrationLink() {
        StudentAttributes sd = StudentAttributes
                .builder("course1", "email@email.com")
                .withName("name 1")
                .withSectionName("sect 1")
                .withComment("comment 1")
                .withTeamName("team 1")
                .build();

        sd.key = "testkey";
        String regUrl = Config.getFrontEndAppUrl(Const.WebPageURIs.JOIN_PAGE)
                .withRegistrationKey(StringHelper.encrypt("testkey"))
                .withStudentEmail("email@email.com")
                .withCourseId("course1")
                .withParam(Const.ParamsNames.ENTITY_TYPE, Const.EntityType.STUDENT)
                .toString();
        assertEquals(regUrl, sd.getRegistrationUrl());
    }

    @Test
    public void testGetPublicProfilePictureUrl() {
        StudentAttributes studentAttributes = StudentAttributes
                .builder("course1", "email@email.com")
                .withName("name 1")
                .withSectionName("sect 1")
                .withComment("comment 1")
                .withTeamName("team 1")
                .build();
        String profilePicUrl = Config.getBackEndAppUrl(Const.ActionURIs.STUDENT_PROFILE_PICTURE)
                .withStudentEmail(StringHelper.encrypt("email@email.com"))
                .withCourseId(StringHelper.encrypt("course1"))
                .toString();
        assertEquals(profilePicUrl, studentAttributes.getPublicProfilePictureUrl());
    }

    @Test
    public void testGetBackUpIdentifier() {
        StudentAttributes studentAttributes = generateValidStudentAttributesObject();
        String expectedBackUpIdentifierMessage = "Recently modified student::" + studentAttributes.getId();

        assertEquals(expectedBackUpIdentifierMessage, studentAttributes.getBackupIdentifier());
    }

    private CourseStudent generateTypicalStudentObject() {
        return new CourseStudent("email@email.com", "name 1", "googleId.1", "comment 1", "courseId1", "team 1", "sect 1");
    }

    private CourseStudent generateStudentWithoutSectionObject() {
        return new CourseStudent("email@email.com", "name 1", "googleId.1", "comment 1", "courseId1", "team 1", null);
    }

    private List<StudentAttributes> generateTypicalStudentAttributesList() {
        StudentAttributes studentAttributes1 = StudentAttributes
                .builder("courseId", "email 1")
                .withName("name 1")
                .withSectionName("sect 2")
                .withComment("comment 1")
                .withTeamName("team 2")
                .build();
        StudentAttributes studentAttributes2 = StudentAttributes
                .builder("courseId", "email 2")
                .withName("name 2")
                .withSectionName("sect 1")
                .withComment("comment 2")
                .withTeamName("team 3")
                .build();
        StudentAttributes studentAttributes3 = StudentAttributes
                .builder("courseId", "email 3")
                .withName("name 2")
                .withSectionName("sect 3")
                .withComment("comment 3")
                .withTeamName("team 1")
                .build();
        StudentAttributes studentAttributes4 = StudentAttributes
                .builder("courseId", "email 4")
                .withName("name 4")
                .withSectionName("sect 2")
                .withComment("comment 4")
                .withTeamName("team 2")
                .build();

        return Arrays.asList(studentAttributes1, studentAttributes4, studentAttributes3, studentAttributes2);
    }

    private StudentAttributes generateValidStudentAttributesObject() {
        return StudentAttributes.builder("valid-course-id", "valid@email.com")
                .withName("valid name")
                .withGoogleId("valid.google.id")
                .withTeamName("valid team")
                .withSectionName("valid section")
                .withComment("")
                .build();
    }

}
