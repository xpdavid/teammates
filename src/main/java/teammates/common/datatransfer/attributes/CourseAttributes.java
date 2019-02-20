package teammates.common.datatransfer.attributes;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import teammates.common.util.Assumption;
import teammates.common.util.Const;
import teammates.common.util.FieldValidator;
import teammates.common.util.Logger;
import teammates.common.util.TimeHelper;
import teammates.storage.entity.Course;

/**
 * The data transfer object for Course entities.
 */
public class CourseAttributes extends EntityAttributes<Course> implements Comparable<CourseAttributes> {

    private static final Logger log = Logger.getLogger();

    private static final String COURSE_BACKUP_LOG_MSG = "Recently modified course::";

    public Instant createdAt;
    public Instant deletedAt;
    private String id;
    private String name;
    private ZoneId timeZone;

    CourseAttributes(String courseId) {
        this.id = courseId;
        this.timeZone = Const.DEFAULT_TIME_ZONE;
        this.createdAt = Instant.now();
        this.deletedAt = null;
    }

    public static CourseAttributes valueOf(Course course) {
        CourseAttributes courseAttributes = new CourseAttributes(course.getUniqueId());

        courseAttributes.name = course.getName();

        ZoneId courseTimeZone;
        try {
            courseTimeZone = ZoneId.of(course.getTimeZone());
        } catch (DateTimeException e) {
            log.severe("Timezone '" + course.getTimeZone() + "' of course '" + course.getUniqueId()
                    + "' is not supported. UTC will be used instead.");
            courseTimeZone = Const.DEFAULT_TIME_ZONE;
        }
        courseAttributes.timeZone = courseTimeZone;

        if (course.getCreatedAt() != null) {
            courseAttributes.createdAt = course.getCreatedAt();
        }
        courseAttributes.deletedAt = course.getDeletedAt();

        return courseAttributes;
    }

    /**
     * Returns a builder for {@link CourseAttributes}.
     */
    public static Builder builder(String courseId) {
        return new Builder(courseId);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ZoneId getTimeZone() {
        return timeZone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public String getCreatedAtDateString() {
        return TimeHelper.formatDateForInstructorPages(createdAt, timeZone);
    }

    public String getCreatedAtDateStamp() {
        return TimeHelper.formatDateTimeToIso8601Utc(createdAt);
    }

    public String getCreatedAtFullDateTimeString() {
        LocalDateTime localDateTime = TimeHelper.convertInstantToLocalDateTime(createdAt, timeZone);
        return TimeHelper.formatDateTimeForDisplay(localDateTime);
    }

    public String getDeletedAtDateString() {
        if (this.deletedAt == null) {
            return Const.DELETION_DATE_NOT_APPLICABLE;
        }
        return TimeHelper.formatDateForInstructorPages(deletedAt, timeZone);
    }

    public String getDeletedAtDateStamp() {
        if (this.deletedAt == null) {
            return Const.DELETION_DATE_NOT_APPLICABLE;
        }
        return TimeHelper.formatDateTimeToIso8601Utc(deletedAt);
    }

    public String getDeletedAtFullDateTimeString() {
        if (this.deletedAt == null) {
            return Const.DELETION_DATE_NOT_APPLICABLE;
        }
        LocalDateTime localDateTime = TimeHelper.convertInstantToLocalDateTime(deletedAt, timeZone);
        return TimeHelper.formatDateTimeForDisplay(localDateTime);
    }

    public void resetDeletedAt() {
        this.deletedAt = null;
    }

    public boolean isCourseDeleted() {
        return this.deletedAt != null;
    }

    public void setTimeZone(ZoneId timeZone) {
        this.timeZone = timeZone;
    }

    @Override
    public List<String> getInvalidityInfo() {

        FieldValidator validator = new FieldValidator();
        List<String> errors = new ArrayList<>();

        addNonEmptyError(validator.getInvalidityInfoForCourseId(getId()), errors);

        addNonEmptyError(validator.getInvalidityInfoForCourseName(getName()), errors);

        return errors;
    }

    @Override
    public Course toEntity() {
        return new Course(getId(), getName(), getTimeZone().getId(), createdAt, deletedAt);
    }

    @Override
    public String toString() {
        return "[" + CourseAttributes.class.getSimpleName() + "] id: " + getId() + " name: " + getName()
               + " timeZone: " + getTimeZone();
    }

    @Override
    public String getBackupIdentifier() {
        return COURSE_BACKUP_LOG_MSG + getId();
    }

    @Override
    public void sanitizeForSaving() {
        // no additional sanitization required
    }

    @Override
    public int compareTo(CourseAttributes o) {
        if (o == null) {
            return 0;
        }
        return o.createdAt.compareTo(createdAt);
    }

    public static void sortById(List<CourseAttributes> courses) {
        courses.sort(Comparator.comparing(CourseAttributes::getId));
    }

    public static void sortByCreatedDate(List<CourseAttributes> courses) {
        courses.sort(Comparator.comparing((CourseAttributes course) -> course.createdAt).reversed()
                .thenComparing(course -> course.getId()));
    }

    /**
     * Updates with {@link UpdateOptions}.
     */
    public void update(UpdateOptions updateOptions) {
        updateOptions.deletedAtOption.ifPresent(s -> deletedAt = s);
        updateOptions.nameOption.ifPresent(s -> name = s);
        updateOptions.timeZoneOption.ifPresent(s -> timeZone = s);
    }

    /**
     * Returns a {@link UpdateOptions.Builder} to build {@link UpdateOptions} for a course.
     */
    public static UpdateOptions.Builder updateOptionsBuilder(String courseId) {
        return new UpdateOptions.Builder(courseId);
    }

    /**
     * A builder for {@link CourseAttributes}.
     */
    public static class Builder extends BasicBuilder<CourseAttributes, Builder> {

        private final CourseAttributes courseAttributes;

        private Builder(String courseId) {
            super(new UpdateOptions(courseId));
            thisBuilder = this;

            courseAttributes = new CourseAttributes(courseId);
        }

        @Override
        public CourseAttributes build() {
            courseAttributes.update(updateOptions);

            return courseAttributes;
        }
    }

    /**
     * Helper class to specific the fields to update in {@link AccountAttributes}.
     */
    public static class UpdateOptions {
        private String courseId;

        private UpdateOption<Instant> deletedAtOption = UpdateOption.empty();
        private UpdateOption<String> nameOption = UpdateOption.empty();
        private UpdateOption<ZoneId> timeZoneOption = UpdateOption.empty();

        private UpdateOptions(String courseId) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, courseId);

            this.courseId = courseId;
        }

        public String getCourseId() {
            return courseId;
        }

        @Override
        public String toString() {
            return "CourseAttributes.UpdateOptions ["
                    + "courseId = " + courseId
                    + ", name = " + nameOption
                    + ", deletedAt = " + deletedAtOption
                    + ", timezone = " + timeZoneOption
                    + "]";
        }

        /**
         * Builder class to build {@link UpdateOptions}.
         */
        public static class Builder extends BasicBuilder<UpdateOptions, Builder> {

            private Builder(String courseId) {
                super(new UpdateOptions(courseId));
                thisBuilder = this;
            }

            @Override
            public UpdateOptions build() {
                return updateOptions;
            }

        }

    }

    /**
     * Basic builder to build {@link CourseAttributes} related classes.
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

        public B withTimezone(ZoneId timezone) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, timezone);

            updateOptions.timeZoneOption = UpdateOption.of(timezone);
            return thisBuilder;
        }

        public abstract T build();

    }
}
