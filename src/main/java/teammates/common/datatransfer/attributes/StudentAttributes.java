package teammates.common.datatransfer.attributes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import teammates.common.datatransfer.StudentUpdateStatus;
import teammates.common.util.Assumption;
import teammates.common.util.Config;
import teammates.common.util.Const;
import teammates.common.util.FieldValidator;
import teammates.common.util.SanitizationHelper;
import teammates.common.util.StringHelper;
import teammates.storage.entity.CourseStudent;

public class StudentAttributes extends EntityAttributes<CourseStudent> {

    private static final String STUDENT_BACKUP_LOG_MSG = "Recently modified student::";

    // Required fields
    public String email;
    public String course;

    // Optional values
    public String name;
    public String googleId;
    public String lastName;
    public String comments;
    public String team;
    public String section;
    public String key;

    // update specific attribute should not be inside DTO
    @Deprecated
    public transient StudentUpdateStatus updateStatus;

    private transient Instant createdAt;
    private transient Instant updatedAt;

    StudentAttributes(String courseId, String email) {
        this.course = courseId;
        this.email = email;

        this.googleId = "";
        this.section = Const.DEFAULT_SECTION;
        this.updateStatus = StudentUpdateStatus.UNKNOWN;
        this.createdAt = Const.TIME_REPRESENTS_DEFAULT_TIMESTAMP;
        this.updatedAt = Const.TIME_REPRESENTS_DEFAULT_TIMESTAMP;
    }

    public static StudentAttributes valueOf(CourseStudent student) {
        StudentAttributes studentAttributes = new StudentAttributes(student.getCourseId(), student.getEmail());
        studentAttributes.name = student.getName();
        studentAttributes.lastName = student.getLastName();
        if (student.getGoogleId() != null) {
            studentAttributes.googleId = student.getGoogleId();
        }
        studentAttributes.team = student.getTeamName();
        if (student.getSectionName() != null) {
            studentAttributes.section = student.getSectionName();
        }
        studentAttributes.comments = student.getComments();
        studentAttributes.key = student.getRegistrationKey();
        if (student.getCreatedAt() != null) {
            studentAttributes.createdAt = student.getCreatedAt();
        }
        if (student.getUpdatedAt() != null) {
            studentAttributes.updatedAt = student.getUpdatedAt();
        }

        return studentAttributes;
    }

    /**
     * Return a builder for {@link StudentAttributes}.
     */
    public static Builder builder(String courseId, String email) {
        return new Builder(courseId, email);
    }

    public StudentAttributes getCopy() {
        StudentAttributes studentAttributes = new StudentAttributes(course, email);

        studentAttributes.name = name;
        studentAttributes.lastName = lastName;
        studentAttributes.googleId = googleId;
        studentAttributes.team = team;
        studentAttributes.section = section;
        studentAttributes.comments = comments;
        studentAttributes.key = key;
        studentAttributes.createdAt = createdAt;
        studentAttributes.updatedAt = updatedAt;

        studentAttributes.updateStatus = updateStatus;

        return studentAttributes;
    }

    public String toEnrollmentString() {
        String enrollmentStringSeparator = "|";

        return this.section + enrollmentStringSeparator
             + this.team + enrollmentStringSeparator
             + this.name + enrollmentStringSeparator
             + this.email + enrollmentStringSeparator
             + this.comments;
    }

    public boolean isRegistered() {
        return googleId != null && !googleId.trim().isEmpty();
    }

    public String getRegistrationUrl() {
        return Config.getFrontEndAppUrl(Const.WebPageURIs.JOIN_PAGE)
                .withRegistrationKey(StringHelper.encrypt(key))
                .withStudentEmail(email)
                .withCourseId(course)
                .withParam(Const.ParamsNames.ENTITY_TYPE, Const.EntityType.STUDENT)
                .toString();
    }

    public String getPublicProfilePictureUrl() {
        return Config.getBackEndAppUrl(Const.ActionURIs.STUDENT_PROFILE_PICTURE)
                .withStudentEmail(StringHelper.encrypt(email))
                .withCourseId(StringHelper.encrypt(course))
                .toString();
    }

    public String getName() {
        return name;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getCourse() {
        return course;
    }

    public String getGoogleId() {
        return googleId;
    }

    public String getKey() {
        return key;
    }

    /**
     * Format: email%courseId e.g., adam@gmail.com%cs1101.
     */
    public String getId() {
        return email + "%" + course;
    }

    public String getSection() {
        return section;
    }

    public String getTeam() {
        return team;
    }

    public String getComments() {
        return comments;
    }

    public boolean isEnrollInfoSameAs(StudentAttributes otherStudent) {
        return otherStudent != null && otherStudent.email.equals(this.email)
               && otherStudent.course.equals(this.course)
               && otherStudent.name.equals(this.name)
               && otherStudent.comments.equals(this.comments)
               && otherStudent.team.equals(this.team)
               && otherStudent.section.equals(this.section);
    }

    @Override
    public List<String> getInvalidityInfo() {
        // id is allowed to be null when the student is not registered
        Assumption.assertNotNull(team);
        Assumption.assertNotNull(comments);

        FieldValidator validator = new FieldValidator();
        List<String> errors = new ArrayList<>();

        if (isRegistered()) {
            addNonEmptyError(validator.getInvalidityInfoForGoogleId(googleId), errors);
        }

        addNonEmptyError(validator.getInvalidityInfoForCourseId(course), errors);

        addNonEmptyError(validator.getInvalidityInfoForEmail(email), errors);

        addNonEmptyError(validator.getInvalidityInfoForTeamName(team), errors);

        addNonEmptyError(validator.getInvalidityInfoForSectionName(section), errors);

        addNonEmptyError(validator.getInvalidityInfoForStudentRoleComments(comments), errors);

        addNonEmptyError(validator.getInvalidityInfoForPersonName(name), errors);

        return errors;
    }

    public static void sortBySectionName(List<StudentAttributes> students) {
        students.sort(Comparator.comparing((StudentAttributes student) -> student.section)
                .thenComparing(student -> student.team)
                .thenComparing(student -> student.name));
    }

    public static void sortByTeamName(List<StudentAttributes> students) {
        students.sort(Comparator.comparing((StudentAttributes student) -> student.team)
                .thenComparing(student -> student.name));
    }

    public static void sortByNameAndThenByEmail(List<StudentAttributes> students) {
        students.sort(Comparator.comparing((StudentAttributes student) -> student.name)
                .thenComparing(student -> student.email));
    }

    public void updateWithExistingRecord(StudentAttributes originalStudent) {
        if (this.email == null) {
            this.email = originalStudent.email;
        }

        if (this.name == null) {
            this.name = originalStudent.name;
        }

        if (this.googleId == null) {
            this.googleId = originalStudent.googleId;
        }

        if (this.team == null) {
            this.team = originalStudent.team;
        }

        if (this.comments == null) {
            this.comments = originalStudent.comments;
        }

        if (this.section == null) {
            this.section = originalStudent.section;
        }
    }

    @Override
    public CourseStudent toEntity() {
        return new CourseStudent(email, name, googleId, comments, course, team, section);
    }

    @Override
    public String toString() {
        return toString(0);
    }

    public String toString(int indent) {
        String indentString = StringHelper.getIndent(indent);
        StringBuilder sb = new StringBuilder();
        sb.append(indentString + "Student:" + name + "[" + email + "]" + System.lineSeparator());

        return sb.toString();
    }

    @Override
    public String getBackupIdentifier() {
        return STUDENT_BACKUP_LOG_MSG + getId();
    }

    @Override
    public void sanitizeForSaving() {
        googleId = SanitizationHelper.sanitizeGoogleId(googleId);
        name = SanitizationHelper.sanitizeName(name);
        comments = SanitizationHelper.sanitizeTextField(comments);
    }

    public String getStudentStatus() {
        if (isRegistered()) {
            return Const.STUDENT_COURSE_STATUS_JOINED;
        }
        return Const.STUDENT_COURSE_STATUS_YET_TO_JOIN;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Returns true if section value has changed from its original value.
     */
    public boolean isSectionChanged(StudentAttributes originalStudentAttribute) {
        return this.section != null && !this.section.equals(originalStudentAttribute.section);
    }

    /**
     * Returns true if team value has changed from its original value.
     */
    public boolean isTeamChanged(StudentAttributes originalStudentAttribute) {
        return this.team != null && !this.team.equals(originalStudentAttribute.team);
    }

    /**
     * Returns true if email value has changed from its original value.
     */
    public boolean isEmailChanged(StudentAttributes originalStudentAttribute) {
        return this.email != null && !this.email.equals(originalStudentAttribute.email);
    }

    /**
     * Updates with {@link UpdateOptions}.
     */
    public void update(UpdateOptions updateOptions) {
        updateOptions.newEmailOption.ifPresent(s -> email = s);
        updateOptions.nameOption.ifPresent(s -> {
            name = s;
            lastName = StringHelper.splitName(s)[1];
        });
        updateOptions.lastNameOption.ifPresent(s -> lastName = s);
        updateOptions.commentOption.ifPresent(s -> comments = s);
        updateOptions.googleIdOption.ifPresent(s -> googleId = s);
        updateOptions.teamNameOption.ifPresent(s -> team = s);
        updateOptions.sectionNameOption.ifPresent(s -> section = s);
    }

    /**
     * Returns a {@link UpdateOptions.Builder} to build {@link UpdateOptions} for a student.
     */
    public static UpdateOptions.Builder updateOptionsBuilder(String courseId, String email) {
        return new UpdateOptions.Builder(courseId, email);
    }

    /**
     * A builder class for {@link StudentAttributes}.
     */
    public static class Builder extends BasicBuilder<StudentAttributes, Builder> {

        private final StudentAttributes studentAttributes;

        private Builder(String courseId, String email) {
            super(new UpdateOptions(courseId, email));
            thisBuilder = this;

            studentAttributes = new StudentAttributes(courseId, email);
        }

        @Override
        public StudentAttributes build() {
            studentAttributes.update(updateOptions);

            return studentAttributes;
        }
    }

    /**
     * Helper class to specific the fields to update in {@link StudentAttributes}.
     */
    public static class UpdateOptions {
        private String courseId;
        private String email;

        private UpdateOption<String> newEmailOption = UpdateOption.empty();
        private UpdateOption<String> nameOption = UpdateOption.empty();
        private UpdateOption<String> lastNameOption = UpdateOption.empty();
        private UpdateOption<String> commentOption = UpdateOption.empty();
        private UpdateOption<String> googleIdOption = UpdateOption.empty();
        private UpdateOption<String> teamNameOption = UpdateOption.empty();
        private UpdateOption<String> sectionNameOption = UpdateOption.empty();

        private UpdateOptions(String courseId, String email) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, courseId);
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, email);

            this.courseId = courseId;
            this.email = email;
        }

        public String getCourseId() {
            return courseId;
        }

        public String getEmail() {
            return email;
        }

        @Override
        public String toString() {
            return "StudentAttributes.UpdateOptions ["
                    + "courseId = " + courseId
                    + ", email = " + email
                    + ", newEmail = " + newEmailOption
                    + ", name = " + nameOption
                    + ", lastName = " + lastNameOption
                    + ", comment = " + commentOption
                    + ", googleId = " + googleIdOption
                    + ", teamName = " + teamNameOption
                    + ", sectionName = " + sectionNameOption
                    + "]";
        }

        /**
         * Builder class to build {@link UpdateOptions}.
         */
        public static class Builder extends BasicBuilder<UpdateOptions, Builder> {

            private Builder(String courseId, String email) {
                super(new UpdateOptions(courseId, email));
                thisBuilder = this;
            }

            public Builder withNewEmail(String email) {
                Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, email);

                updateOptions.newEmailOption = UpdateOption.of(email);
                return thisBuilder;
            }

            @Override
            public UpdateOptions build() {
                return updateOptions;
            }

        }

    }

    /**
     * Basic builder to build {@link StudentAttributes} related classes.
     *
     * @param <T> type to be built
     * @param <B> type of the builder
     */
    private abstract static class BasicBuilder<T, B extends BasicBuilder<T, B>> {

        protected UpdateOptions updateOptions;
        protected B thisBuilder;

        protected BasicBuilder(UpdateOptions updateOptions) {
            this.updateOptions = updateOptions;
        }

        public B withName(String name) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, name);

            updateOptions.nameOption = UpdateOption.of(name);
            return thisBuilder;
        }

        public B withLastName(String name) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, name);

            updateOptions.lastNameOption = UpdateOption.of(name);
            return thisBuilder;
        }

        public B withComment(String comment) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, comment);

            updateOptions.commentOption = UpdateOption.of(comment);
            return thisBuilder;
        }

        public B withGoogleId(String googleId) {
            // google id can be set to null
            updateOptions.googleIdOption = UpdateOption.of(googleId);
            return thisBuilder;
        }

        public B withTeamName(String teamName) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, teamName);

            updateOptions.teamNameOption = UpdateOption.of(teamName);
            return thisBuilder;
        }

        public B withSectionName(String sectionName) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, sectionName);

            updateOptions.sectionNameOption = UpdateOption.of(sectionName);
            return thisBuilder;
        }

        public abstract T build();

    }
}
