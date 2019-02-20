package teammates.test.cases.datatransfer;

import org.testng.annotations.Test;

import teammates.common.datatransfer.InstructorPrivileges;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.util.Const;
import teammates.common.util.FieldValidator;
import teammates.common.util.JsonUtils;
import teammates.common.util.SanitizationHelper;
import teammates.common.util.StringHelper;
import teammates.storage.entity.Instructor;

/**
 * SUT: {@link InstructorAttributes}.
 */
public class InstructorAttributesTest extends BaseAttributesTest {

    @Test
    public void testBuilder_buildNothing_shouldUseDefaultValues() {
        InstructorAttributes instructor = InstructorAttributes
                .builder("valid-course-id", "valid@email.com")
                .build();

        assertEquals("valid-course-id", instructor.getCourseId());
        assertEquals("valid@email.com", instructor.getEmail());

        // Check default values for optional params
        assertNull(instructor.getName());
        assertNull(instructor.getGoogleId());
        assertNull(instructor.getKey());
        assertEquals(Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_COOWNER, instructor.getRole());
        assertEquals(InstructorAttributes.DEFAULT_DISPLAY_NAME, instructor.getDisplayedName());
        assertFalse(instructor.isArchived());
        assertTrue(instructor.isDisplayedToStudents());
        assertEquals(new InstructorPrivileges(Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_COOWNER),
                instructor.getPrivileges());
    }

    @Test
    public void testBuilder_withNullArguments_shouldThrowException() {
        assertThrows(AssertionError.class, () -> {
            InstructorAttributes
                    .builder("courseId", null)
                    .build();
        });

        assertThrows(AssertionError.class, () -> {
            InstructorAttributes
                    .builder(null, "email@email.com")
                    .build();
        });

        assertThrows(AssertionError.class, () -> {
            InstructorAttributes
                    .builder("courseId", "email@email.com")
                    .withName(null)
                    .build();
        });

        assertThrows(AssertionError.class, () -> {
            InstructorAttributes
                    .builder("courseId", "email@email.com")
                    .withGoogleId(null)
                    .build();
        });

        assertThrows(AssertionError.class, () -> {
            InstructorAttributes
                    .builder("courseId", "email@email.com")
                    .withRole(null)
                    .build();
        });

        assertThrows(AssertionError.class, () -> {
            InstructorAttributes
                    .builder("courseId", "email@email.com")
                    .withDisplayedName(null)
                    .build();
        });

        assertThrows(AssertionError.class, () -> {
            InstructorAttributes
                    .builder("courseId", "email@email.com")
                    .withPrivileges(null)
                    .build();
        });
    }

    @Test
    public void testBuilder_withTypicalData_shouldBuildCorrectAttribute() {
        InstructorAttributes instructor = InstructorAttributes
                .builder("valid-course-id", "valid@email.com")
                .withName("valid name")
                .withGoogleId("valid.google.id")
                .withRole(Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_MANAGER)
                .withDisplayedName("instructor A")
                .withIsArchived(false)
                .withIsDisplayedToStudents(false)
                .withPrivileges(
                        new InstructorPrivileges(Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_MANAGER))
                .build();

        assertEquals("valid-course-id", instructor.getCourseId());
        assertEquals("valid@email.com", instructor.getEmail());
        assertEquals("valid name", instructor.getName());
        assertEquals("valid.google.id", instructor.getGoogleId());
        assertEquals("instructor A", instructor.getDisplayedName());
        assertFalse(instructor.isArchived());
        assertFalse(instructor.isDisplayedToStudents());
        assertEquals(Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_MANAGER, instructor.getRole());
        assertEquals(new InstructorPrivileges(Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_MANAGER),
                instructor.getPrivileges());
        // key remains
        assertNull(instructor.getKey());
    }

    @Test
    public void testValueOf_withAllFieldPopulatedInstructor_shouldGenerateAttributesCorrectly() {
        InstructorPrivileges privileges =
                new InstructorPrivileges(Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_COOWNER);
        Instructor instructor = new Instructor("valid.google.id", "valid-course-id", false,
                "valid name", "valid@email.com", Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_COOWNER,
                true, "Tutor", JsonUtils.toJson(privileges, InstructorPrivileges.class));
        InstructorAttributes instructorAttributes = InstructorAttributes.valueOf(instructor);

        assertEquals(instructor.getGoogleId(), instructorAttributes.getGoogleId());
        assertEquals(instructor.getCourseId(), instructorAttributes.getCourseId());
        assertEquals(instructor.getIsArchived(), instructorAttributes.isArchived());
        assertEquals(instructor.getName(), instructorAttributes.getName());
        assertEquals(instructor.getEmail(), instructorAttributes.getEmail());
        assertEquals(instructor.getRegistrationKey(), instructorAttributes.getKey());
        assertEquals(instructor.getRole(), instructorAttributes.getRole());
        assertEquals(instructor.isDisplayedToStudents(), instructorAttributes.isDisplayedToStudents());
        assertEquals(instructor.getDisplayedName(), instructorAttributes.getDisplayedName());
        assertEquals(instructor.getInstructorPrivilegesAsText(),
                JsonUtils.toJson(instructorAttributes.getPrivileges(), InstructorPrivileges.class));
    }

    @Test
    public void testValueOf_withSomeFieldsPopulatedAsNull_shouldUseDefaultValues() {
        Instructor instructor = new Instructor("valid.google.id", "valid-course-id", false,
                "valid name", "valid@email.com", null,
                true, null, null);
        InstructorAttributes instructorAttributes = InstructorAttributes.valueOf(instructor);

        assertEquals(instructor.getGoogleId(), instructorAttributes.getGoogleId());
        assertEquals(instructor.getCourseId(), instructorAttributes.getCourseId());
        assertEquals(instructor.getIsArchived(), instructorAttributes.isArchived());
        assertEquals(instructor.getName(), instructorAttributes.getName());
        assertEquals(instructor.getEmail(), instructorAttributes.getEmail());
        assertEquals(instructor.getRegistrationKey(), instructorAttributes.getKey());
        assertEquals(Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_COOWNER, instructorAttributes.getRole());
        assertEquals(instructor.isDisplayedToStudents(), instructorAttributes.isDisplayedToStudents());
        assertEquals(InstructorAttributes.DEFAULT_DISPLAY_NAME, instructorAttributes.getDisplayedName());
        assertEquals(new InstructorPrivileges(Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_COOWNER),
                instructorAttributes.getPrivileges());
    }

    @Test
    public void testIsRegistered() {
        InstructorAttributes instructor = InstructorAttributes
                .builder("valid-course-id", "valid@email.com")
                .withName("valid name")
                .withGoogleId("valid.google.id")
                .build();

        assertTrue(instructor.isRegistered());

        instructor.googleId = null;
        assertFalse(instructor.isRegistered());
    }

    @Override
    @Test
    public void testToEntity() {
        String googleId = "valid.googleId";
        String courseId = "courseId";
        String name = "name";
        String email = "email@google.com";
        String roleName = Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_COOWNER;
        String displayedName = Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_COOWNER;
        InstructorPrivileges privileges =
                new InstructorPrivileges(Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_COOWNER);
        InstructorAttributes instructor = InstructorAttributes.builder(courseId, email)
                .withName(name)
                .withGoogleId(googleId)
                .withRole(roleName)
                .withDisplayedName(displayedName)
                .withPrivileges(privileges)
                .build();
        String key = "randomKey";
        instructor.key = key;

        Instructor entity = instructor.toEntity();

        // regKey should not carried
        assertNotEquals(key, entity.getRegistrationKey());
    }

    @Test
    public void testGetInvalidityInfo() throws Exception {

        InstructorAttributes i = InstructorAttributes
                .builder("valid-course-id", "valid@email.com")
                .withName("valid name")
                .withGoogleId("valid.google.id")
                .build();

        assertTrue(i.isValid());

        i.googleId = "invalid@google@id";
        i.name = "";
        i.email = "invalid email";
        i.courseId = "";
        i.role = "invalidRole";

        assertFalse("invalid value", i.isValid());
        String errorMessage =
                getPopulatedErrorMessage(
                    FieldValidator.GOOGLE_ID_ERROR_MESSAGE, i.googleId,
                    FieldValidator.GOOGLE_ID_FIELD_NAME, FieldValidator.REASON_INCORRECT_FORMAT,
                    FieldValidator.GOOGLE_ID_MAX_LENGTH) + System.lineSeparator()
                + getPopulatedEmptyStringErrorMessage(
                      FieldValidator.COURSE_ID_ERROR_MESSAGE_EMPTY_STRING,
                      FieldValidator.COURSE_ID_FIELD_NAME, FieldValidator.COURSE_ID_MAX_LENGTH) + System.lineSeparator()
                + getPopulatedEmptyStringErrorMessage(
                      FieldValidator.SIZE_CAPPED_NON_EMPTY_STRING_ERROR_MESSAGE_EMPTY_STRING,
                      FieldValidator.PERSON_NAME_FIELD_NAME, FieldValidator.PERSON_NAME_MAX_LENGTH) + System.lineSeparator()
                + getPopulatedErrorMessage(
                      FieldValidator.EMAIL_ERROR_MESSAGE, i.email,
                      FieldValidator.EMAIL_FIELD_NAME, FieldValidator.REASON_INCORRECT_FORMAT,
                      FieldValidator.EMAIL_MAX_LENGTH) + System.lineSeparator()
                + String.format(FieldValidator.ROLE_ERROR_MESSAGE, i.role);
        assertEquals("invalid value", errorMessage, StringHelper.toString(i.getInvalidityInfo()));

        i.googleId = null;

        assertFalse("invalid value", i.isValid());
        errorMessage =
                getPopulatedEmptyStringErrorMessage(
                    FieldValidator.COURSE_ID_ERROR_MESSAGE_EMPTY_STRING,
                    FieldValidator.COURSE_ID_FIELD_NAME, FieldValidator.COURSE_ID_MAX_LENGTH) + System.lineSeparator()
                + getPopulatedEmptyStringErrorMessage(
                      FieldValidator.SIZE_CAPPED_NON_EMPTY_STRING_ERROR_MESSAGE_EMPTY_STRING,
                      FieldValidator.PERSON_NAME_FIELD_NAME, FieldValidator.PERSON_NAME_MAX_LENGTH) + System.lineSeparator()
                + getPopulatedErrorMessage(
                      FieldValidator.EMAIL_ERROR_MESSAGE, i.email,
                      FieldValidator.EMAIL_FIELD_NAME, FieldValidator.REASON_INCORRECT_FORMAT,
                      FieldValidator.EMAIL_MAX_LENGTH) + System.lineSeparator()
                + String.format(FieldValidator.ROLE_ERROR_MESSAGE, i.role);
        assertEquals("invalid value", errorMessage, StringHelper.toString(i.getInvalidityInfo()));
    }

    @Test
    public void testSanitizeForSaving() {
        String googleId = "\t\tvalid.goo    gleId  \t\n";
        String courseId = "\t\n  co      urseId";
        String name = "\t\t\tna    me<><>";
        String email = "\n      my'email@google.com";
        String roleName = Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_COOWNER;
        String displayedName = Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_COOWNER;
        InstructorPrivileges privileges =
                new InstructorPrivileges(Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_COOWNER);
        InstructorAttributes instructor = InstructorAttributes.builder(courseId, email)
                .withName(name)
                .withGoogleId(googleId)
                .withRole(roleName)
                .withDisplayedName(displayedName)
                .withPrivileges(privileges)
                .build();

        instructor.sanitizeForSaving();
        assertEquals(privileges, instructor.privileges);
        assertEquals(SanitizationHelper.sanitizeGoogleId(googleId), instructor.googleId);
        assertEquals(SanitizationHelper.sanitizeTitle(courseId), instructor.courseId);
        assertEquals(SanitizationHelper.sanitizeName(name), instructor.name);
        assertEquals(SanitizationHelper.sanitizeEmail(email), instructor.email);

        instructor.role = null;
        instructor.displayedName = null;
        instructor.privileges = null;
        instructor.sanitizeForSaving();
        assertEquals(privileges, instructor.privileges);
    }

    @Test
    public void testIsAllowedForPrivilege() {
        String googleId = "valid.googleId";
        String courseId = "courseId";
        String name = "name";
        String email = "email@google.com";
        String roleName = Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_COOWNER;
        String displayedName = Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_COOWNER;
        InstructorPrivileges privileges =
                new InstructorPrivileges(Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_MANAGER);
        InstructorAttributes instructor = InstructorAttributes.builder(courseId, email)
                .withName(name)
                .withGoogleId(googleId)
                .withRole(roleName)
                .withDisplayedName(displayedName)
                .withPrivileges(privileges)
                .build();

        assertFalse(instructor.isAllowedForPrivilege(Const.ParamsNames.INSTRUCTOR_PERMISSION_MODIFY_COURSE));
        instructor.privileges = null;
        assertTrue(instructor.isAllowedForPrivilege(Const.ParamsNames.INSTRUCTOR_PERMISSION_MODIFY_COURSE));

        String sectionId = "sectionId";
        String sessionId = "sessionId";
        assertTrue(instructor.isAllowedForPrivilege(sectionId, sessionId,
                Const.ParamsNames.INSTRUCTOR_PERMISSION_MODIFY_SESSION_COMMENT_IN_SECTIONS));
        instructor.privileges = null;
        assertTrue(instructor.isAllowedForPrivilege(sectionId, sessionId,
                Const.ParamsNames.INSTRUCTOR_PERMISSION_MODIFY_SESSION_COMMENT_IN_SECTIONS));
    }

    @Test
    public void testGetBackUpIdentifier() {
        String googleId = "valid.googleId";
        String courseId = "courseId";
        String name = "name";
        String email = "valid@email.com";
        String expectedBackUpIdentifierMessage = "Recently modified instructor::" + courseId + "::" + email;

        InstructorAttributes instructorAttributes = InstructorAttributes
                .builder(courseId, email)
                .withGoogleId(googleId)
                .withName(name)
                .build();

        assertEquals(expectedBackUpIdentifierMessage, instructorAttributes.getBackupIdentifier());
    }

    @Test
    public void testUpdateOptionsWithEmail_withTypicalData_shouldUpdateAttributeCorrectly() {
        InstructorAttributes.UpdateOptionsWithEmail updateOptionsWithEmail =
                InstructorAttributes.updateOptionsWithEmailBuilder("courseId", "test@test.com")
                        .withName("test")
                        .withDisplayedName("Instructor")
                        .withIsArchived(false)
                        .withPrivileges(new InstructorPrivileges(
                                Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_MANAGER))
                        .withIsDisplayedToStudents(false)
                        .withGoogleId("googleId")
                        .withRole(Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_MANAGER)
                        .build();

        assertEquals("courseId", updateOptionsWithEmail.getCourseId());
        assertEquals("test@test.com", updateOptionsWithEmail.getEmail());

        InstructorAttributes instructorAttributes =
                InstructorAttributes.builder("courseId", "test@test.com")
                        .withGoogleId("testGoogleId")
                        .withName("test2")
                        .withDisplayedName("Tutor")
                        .withIsArchived(true)
                        .withPrivileges(new InstructorPrivileges(
                                Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_COOWNER))
                        .withIsDisplayedToStudents(true)
                        .withRole(Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_COOWNER)
                        .build();

        instructorAttributes.update(updateOptionsWithEmail);

        assertEquals("test", instructorAttributes.getName());
        assertEquals("Instructor", instructorAttributes.getDisplayedName());
        assertFalse(instructorAttributes.isArchived);
        assertTrue(instructorAttributes.privileges.hasManagerPrivileges());
        assertFalse(instructorAttributes.isDisplayedToStudents());
        assertEquals("googleId", instructorAttributes.getGoogleId());
        assertEquals(instructorAttributes.getRole(), Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_MANAGER);
    }

    @Test
    public void testUpdateOptionsWithGoogleId_withTypicalData_shouldUpdateAttributeCorrectly() {
        InstructorAttributes.UpdateOptionsWithGoogleId updateOptionsWithGoogleId =
                InstructorAttributes.updateOptionsWithGoogleIdBuilder("courseId", "googleId")
                        .withName("test")
                        .withEmail("test@email.com")
                        .withDisplayedName("Instructor")
                        .withIsArchived(false)
                        .withPrivileges(new InstructorPrivileges(
                                Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_MANAGER))
                        .withIsDisplayedToStudents(false)
                        .withRole(Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_MANAGER)
                        .build();

        assertEquals("courseId", updateOptionsWithGoogleId.getCourseId());
        assertEquals("googleId", updateOptionsWithGoogleId.getGoogleId());

        InstructorAttributes instructorAttributes =
                InstructorAttributes.builder("courseId", "test@test.com")
                        .withGoogleId("googleId")
                        .withName("test2")
                        .withDisplayedName("Tutor")
                        .withIsArchived(true)
                        .withPrivileges(new InstructorPrivileges(
                                Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_COOWNER))
                        .withIsDisplayedToStudents(true)
                        .withRole(Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_COOWNER)
                        .build();

        instructorAttributes.update(updateOptionsWithGoogleId);

        assertEquals("test", instructorAttributes.getName());
        assertEquals("Instructor", instructorAttributes.getDisplayedName());
        assertFalse(instructorAttributes.isArchived);
        assertTrue(instructorAttributes.privileges.hasManagerPrivileges());
        assertFalse(instructorAttributes.isDisplayedToStudents());
        assertEquals("test@email.com", instructorAttributes.getEmail());
        assertEquals(instructorAttributes.getRole(), Const.InstructorPermissionRoleNames.INSTRUCTOR_PERMISSION_ROLE_MANAGER);
    }

    @Test
    public void testUpdateOptionsBuilder_withNullInput_shouldFailWithAssertionError() {
        assertThrows(AssertionError.class, () ->
                InstructorAttributes.updateOptionsWithEmailBuilder(null, "email@email.com"));
        assertThrows(AssertionError.class, () ->
                InstructorAttributes.updateOptionsWithEmailBuilder("courseId", null));
        assertThrows(AssertionError.class, () ->
                InstructorAttributes.updateOptionsWithEmailBuilder("courseId", "email@email.com")
                        .withName(null));
        assertThrows(AssertionError.class, () ->
                InstructorAttributes.updateOptionsWithEmailBuilder("courseId", "email@email.com")
                        .withDisplayedName(null));
        assertThrows(AssertionError.class, () ->
                InstructorAttributes.updateOptionsWithEmailBuilder("courseId", "email@email.com")
                        .withPrivileges(null));
        assertThrows(AssertionError.class, () ->
                InstructorAttributes.updateOptionsWithEmailBuilder("courseId", "email@email.com")
                        .withRole(null));

        assertThrows(AssertionError.class, () ->
                InstructorAttributes.updateOptionsWithGoogleIdBuilder(null, "googleId"));
        assertThrows(AssertionError.class, () ->
                InstructorAttributes.updateOptionsWithGoogleIdBuilder("courseId", null));
        assertThrows(AssertionError.class, () ->
                InstructorAttributes.updateOptionsWithGoogleIdBuilder("courseId", "googleId")
                        .withName(null));
        assertThrows(AssertionError.class, () ->
                InstructorAttributes.updateOptionsWithGoogleIdBuilder("courseId", "googleId")
                        .withEmail(null));
        assertThrows(AssertionError.class, () ->
                InstructorAttributes.updateOptionsWithGoogleIdBuilder("courseId", "googleId")
                        .withDisplayedName(null));
        assertThrows(AssertionError.class, () ->
                InstructorAttributes.updateOptionsWithGoogleIdBuilder("courseId", "googleId")
                        .withPrivileges(null));
        assertThrows(AssertionError.class, () ->
                InstructorAttributes.updateOptionsWithGoogleIdBuilder("courseId", "googleId")
                        .withRole(null));
    }
}
