package teammates.common.datatransfer.attributes;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import teammates.common.util.Assumption;
import teammates.common.util.Const;
import teammates.common.util.FieldValidator;
import teammates.common.util.JsonUtils;
import teammates.common.util.SanitizationHelper;
import teammates.common.util.TimeHelper;
import teammates.storage.entity.FeedbackSession;

public class FeedbackSessionAttributes extends EntityAttributes<FeedbackSession> {

    /**
     * Comparator to sort SessionAttributes on DESCENDING order based on
     * end time, followed by start time and session name.
     */
    public static final Comparator<FeedbackSessionAttributes> DESCENDING_ORDER = (session1, session2) -> {

        Assumption.assertNotNull(session1.getFeedbackSessionName());
        Assumption.assertNotNull(session1.getStartTime());
        Assumption.assertNotNull(session1.getEndTime());
        Assumption.assertNotNull(session2.getFeedbackSessionName());
        Assumption.assertNotNull(session2.getStartTime());
        Assumption.assertNotNull(session2.getEndTime());

        // Compares end times
        int result = session1.getEndTime().isAfter(session2.getEndTime()) ? -1
                : session1.getEndTime().isBefore(session2.getEndTime()) ? 1 : 0;

        // If the end time is same, compares start times
        if (result == 0) {
            result = session1.getStartTime().isAfter(session2.getStartTime()) ? -1
                    : session1.getStartTime().isBefore(session2.getStartTime()) ? 1 : 0;
        }

        // If both end and start time is same, compares session name
        if (result == 0) {
            result = session1.getFeedbackSessionName().compareTo(session2.getFeedbackSessionName());
        }
        return result;
    };

    private static final String FEEDBACK_SESSION_BACKUP_LOG_MSG = "Recently modified feedback session::";

    // Required fields
    private String feedbackSessionName;
    private String courseId;
    private String creatorEmail;

    // Optional fields
    private String instructions;
    private Instant createdTime;
    private Instant deletedTime;
    private Instant startTime;
    private Instant endTime;
    private Instant sessionVisibleFromTime;
    private Instant resultsVisibleFromTime;
    private ZoneId timeZone;
    private Duration gracePeriod;
    private boolean sentOpenEmail;
    private boolean sentClosingEmail;
    private boolean sentClosedEmail;
    private boolean sentPublishedEmail;
    private boolean isOpeningEmailEnabled;
    private boolean isClosingEmailEnabled;
    private boolean isPublishedEmailEnabled;
    private transient Set<String> respondingInstructorList;
    private transient Set<String> respondingStudentList;

    FeedbackSessionAttributes() {
        isOpeningEmailEnabled = true;
        isClosingEmailEnabled = true;
        isPublishedEmailEnabled = true;
        respondingInstructorList = new HashSet<>();
        respondingStudentList = new HashSet<>();

        timeZone = Const.DEFAULT_TIME_ZONE;
        gracePeriod = Duration.ZERO;

        instructions = "";
    }

    public static FeedbackSessionAttributes valueOf(FeedbackSession fs) {
        return builder(fs.getFeedbackSessionName(), fs.getCourseId(), fs.getCreatorEmail())
                .withInstructions(fs.getInstructions())
                .withCreatedTime(fs.getCreatedTime())
                .withDeletedTime(fs.getDeletedTime())
                .withStartTime(fs.getStartTime())
                .withEndTime(fs.getEndTime())
                .withSessionVisibleFromTime(fs.getSessionVisibleFromTime())
                .withResultsVisibleFromTime(fs.getResultsVisibleFromTime())
                .withTimeZone(ZoneId.of(fs.getTimeZone()))
                .withGracePeriodMinutes(fs.getGracePeriod())
                .withSentOpenEmail(fs.isSentOpenEmail())
                .withSentClosingEmail(fs.isSentClosingEmail())
                .withSentClosedEmail(fs.isSentClosedEmail())
                .withSentPublishedEmail(fs.isSentPublishedEmail())
                .withOpeningEmailEnabled(fs.isOpeningEmailEnabled())
                .withPublishedEmailEnabled(fs.isPublishedEmailEnabled())
                .withRespondingInstructorList(fs.getRespondingInstructorList())
                .withRespondingStudentList(fs.getRespondingStudentList())
                .withClosingEmailEnabled(fs.isClosingEmailEnabled())
                .build();
    }

    /**
     * Returns new builder instance with default values for optional fields.
     *
     * <p>Following default values are set to corresponding attributes:
     * <ul>
     * <li>{@code isOpeningEmailEnabled = true}</li>
     * <li>{@code isClosingEmailEnabled = true}</li>
     * <li>{@code isPublishedEmailEnabled = true}</li>
     * <li>{@code respondingInstructorList = new HashSet<>()}</li>
     * <li>{@code respondingStudentList = new HashSet<>()}</li>
     * </ul>
     */
    public static Builder builder(String feedbackSessionName, String courseId, String creatorEmail) {
        return new Builder(feedbackSessionName, courseId, creatorEmail);
    }

    public FeedbackSessionAttributes getCopy() {
        return valueOf(toEntity());
    }

    public String getCourseId() {
        return courseId;
    }

    public String getFeedbackSessionName() {
        return feedbackSessionName;
    }

    public String getStartTimeString() {
        return TimeHelper.formatDateTimeForDisplay(startTime, timeZone);
    }

    public String getStartTimeInIso8601UtcFormat() {
        return TimeHelper.formatDateTimeToIso8601Utc(startTime);
    }

    public String getEndTimeString() {
        return TimeHelper.formatDateTimeForDisplay(endTime, timeZone);
    }

    public String getEndTimeInIso8601UtcFormat() {
        return TimeHelper.formatDateTimeToIso8601Utc(endTime);
    }

    public String getInstructionsString() {
        if (instructions == null) {
            return null;
        }

        return SanitizationHelper.sanitizeForRichText(instructions);
    }

    @Override
    public FeedbackSession toEntity() {
        return new FeedbackSession(feedbackSessionName, courseId, creatorEmail, instructions,
                createdTime, deletedTime, startTime, endTime, sessionVisibleFromTime, resultsVisibleFromTime,
                timeZone.getId(), getGracePeriodMinutes(),
                sentOpenEmail, sentClosingEmail, sentClosedEmail, sentPublishedEmail,
                isOpeningEmailEnabled, isClosingEmailEnabled, isPublishedEmailEnabled,
                respondingInstructorList, respondingStudentList);
    }

    @Override
    public String getBackupIdentifier() {
        return FEEDBACK_SESSION_BACKUP_LOG_MSG + getCourseId() + "::" + getFeedbackSessionName();
    }

    @Override
    public String getJsonString() {
        return JsonUtils.toJson(this, FeedbackSessionAttributes.class);
    }

    @Override
    public List<String> getInvalidityInfo() {
        FieldValidator validator = new FieldValidator();
        List<String> errors = new ArrayList<>();

        // Check for null fields.

        addNonEmptyError(validator.getValidityInfoForNonNullField(
                FieldValidator.FEEDBACK_SESSION_NAME_FIELD_NAME, feedbackSessionName), errors);

        addNonEmptyError(validator.getValidityInfoForNonNullField(FieldValidator.COURSE_ID_FIELD_NAME, courseId), errors);

        addNonEmptyError(validator.getValidityInfoForNonNullField("instructions to students", instructions), errors);

        addNonEmptyError(validator.getValidityInfoForNonNullField(
                "time for the session to become visible", sessionVisibleFromTime), errors);

        addNonEmptyError(validator.getValidityInfoForNonNullField("session time zone", timeZone), errors);

        addNonEmptyError(validator.getValidityInfoForNonNullField("creator's email", creatorEmail), errors);

        addNonEmptyError(validator.getValidityInfoForNonNullField("session creation time", createdTime), errors);

        // Early return if any null fields
        if (!errors.isEmpty()) {
            return errors;
        }

        addNonEmptyError(validator.getInvalidityInfoForFeedbackSessionName(feedbackSessionName), errors);

        addNonEmptyError(validator.getInvalidityInfoForCourseId(courseId), errors);

        addNonEmptyError(validator.getInvalidityInfoForEmail(creatorEmail), errors);

        addNonEmptyError(validator.getInvalidityInfoForGracePeriod(gracePeriod), errors);

        addNonEmptyError(validator.getValidityInfoForNonNullField("submission opening time", startTime), errors);

        addNonEmptyError(validator.getValidityInfoForNonNullField("submission closing time", endTime), errors);

        addNonEmptyError(validator.getValidityInfoForNonNullField(
                "time for the responses to become visible", resultsVisibleFromTime), errors);

        // Early return if any null fields
        if (!errors.isEmpty()) {
            return errors;
        }

        addNonEmptyError(validator.getInvalidityInfoForTimeForSessionStartAndEnd(startTime, endTime), errors);

        addNonEmptyError(validator.getInvalidityInfoForTimeForVisibilityStartAndSessionStart(
                sessionVisibleFromTime, startTime), errors);

        Instant actualSessionVisibleFromTime = sessionVisibleFromTime;

        if (actualSessionVisibleFromTime.equals(Const.TIME_REPRESENTS_FOLLOW_OPENING)) {
            actualSessionVisibleFromTime = startTime;
        }

        addNonEmptyError(validator.getInvalidityInfoForTimeForVisibilityStartAndResultsPublish(
                actualSessionVisibleFromTime, resultsVisibleFromTime), errors);

        return errors;
    }

    @Override
    public boolean isValid() {
        return getInvalidityInfo().isEmpty();
    }

    public boolean isClosedAfter(long hours) {
        return Instant.now().plus(Duration.ofHours(hours)).isAfter(endTime);
    }

    public boolean isClosingWithinTimeLimit(long hours) {
        Instant now = Instant.now();
        Duration difference = Duration.between(now, endTime);
        // If now and start are almost similar, it means the feedback session
        // is open for only 24 hours.
        // Hence we do not send a reminder e-mail for feedback session.
        return now.isAfter(startTime)
               && difference.compareTo(Duration.ofHours(hours - 1)) >= 0
               && difference.compareTo(Duration.ofHours(hours)) < 0;
    }

    /**
     * Checks if the session closed some time in the last one hour from calling this function.
     *
     * @return true if the session closed within the past hour; false otherwise.
     */
    public boolean isClosedWithinPastHour() {
        Instant now = Instant.now();
        Instant given = endTime.plus(gracePeriod);
        return given.isBefore(now) && Duration.between(given, now).compareTo(Duration.ofHours(1)) < 0;
    }

    /**
     * Returns {@code true} if it is after the closing time of this feedback session; {@code false} if not.
     */
    public boolean isClosed() {
        return Instant.now().isAfter(endTime.plus(gracePeriod));
    }

    /**
     * Returns true if the session is currently open and accepting responses.
     */
    public boolean isOpened() {
        Instant now = Instant.now();
        return (now.isAfter(startTime) || now.equals(startTime)) && now.isBefore(endTime);
    }

    /**
     * Returns true if the session is currently close but is still accept responses.
     */
    public boolean isInGracePeriod() {
        Instant now = Instant.now();
        Instant gracedEnd = endTime.plus(gracePeriod);
        return (now.isAfter(endTime) || now.equals(endTime)) && (now.isBefore(gracedEnd) || now.equals(gracedEnd));
    }

    /**
     * Returns {@code true} has not opened before and is waiting to open,
     * {@code false} if session has opened before.
     */
    public boolean isWaitingToOpen() {
        return Instant.now().isBefore(startTime);
    }

    /**
     * Returns {@code true} if the session is visible; {@code false} if not.
     *         Does not care if the session has started or not.
     */
    public boolean isVisible() {
        Instant visibleTime = this.sessionVisibleFromTime;

        if (visibleTime.equals(Const.TIME_REPRESENTS_FOLLOW_OPENING)) {
            visibleTime = this.startTime;
        }

        Instant now = Instant.now();
        return now.isAfter(visibleTime) || now.equals(visibleTime);
    }

    /**
     * Returns {@code true} if the results of the feedback session is visible; {@code false} if not.
     *         Does not care if the session has ended or not.
     */
    public boolean isPublished() {
        Instant publishTime = this.resultsVisibleFromTime;

        if (publishTime.equals(Const.TIME_REPRESENTS_FOLLOW_VISIBLE)) {
            return isVisible();
        }
        if (publishTime.equals(Const.TIME_REPRESENTS_LATER)) {
            return false;
        }
        if (publishTime.equals(Const.TIME_REPRESENTS_NOW)) {
            return true;
        }

        Instant now = Instant.now();
        return now.isAfter(publishTime) || now.equals(publishTime);
    }

    /**
     * Returns {@code true} if the session has been set by the creator to be manually published.
     */
    public boolean isManuallyPublished() {
        return resultsVisibleFromTime.equals(Const.TIME_REPRESENTS_LATER)
               || resultsVisibleFromTime.equals(Const.TIME_REPRESENTS_NOW);
    }

    public boolean isCreator(String instructorEmail) {
        return creatorEmail.equals(instructorEmail);
    }

    @Override
    public void sanitizeForSaving() {
        this.instructions = SanitizationHelper.sanitizeForRichText(instructions);
    }

    @Override
    public String toString() {
        return "FeedbackSessionAttributes [feedbackSessionName="
               + feedbackSessionName + ", courseId=" + courseId
               + ", creatorEmail=" + creatorEmail + ", instructions=" + instructions
               + ", startTime=" + startTime
               + ", endTime=" + endTime + ", sessionVisibleFromTime="
               + sessionVisibleFromTime + ", resultsVisibleFromTime="
               + resultsVisibleFromTime + ", timeZone=" + timeZone
               + ", gracePeriod=" + getGracePeriodMinutes() + "min"
               + ", sentOpenEmail=" + sentOpenEmail
               + ", sentPublishedEmail=" + sentPublishedEmail
               + ", isOpeningEmailEnabled=" + isOpeningEmailEnabled
               + ", isClosingEmailEnabled=" + isClosingEmailEnabled
               + ", isPublishedEmailEnabled=" + isPublishedEmailEnabled + "]";
    }

    /**
     * Sorts feedback session based courseID (ascending), then by create time (ascending), deadline
     * (ascending), then by start time (ascending), then by feedback session name
     * (ascending). The sort by CourseID part is to cater the case when this
     * method is called with combined feedback sessions from many courses
     */
    public static void sortFeedbackSessionsByCreationTime(List<FeedbackSessionAttributes> sessions) {
        sessions.sort(Comparator.comparing((FeedbackSessionAttributes session) -> session.courseId)
                .thenComparing(session -> session.createdTime)
                .thenComparing(session -> session.endTime)
                .thenComparing(session -> session.startTime)
                .thenComparing(session -> session.feedbackSessionName));
    }

    /**
     * Sorts feedback session based on create time (descending), deadline
     * (descending), then by start time (descending),then by courseID (ascending),then by feedback session name
     * (ascending). The sort by CourseID part is to cater the case when this
     * method is called with combined feedback sessions from many courses
     */
    public static void sortFeedbackSessionsByCreationTimeDescending(List<FeedbackSessionAttributes> sessions) {
        sessions.sort(Comparator.comparing((FeedbackSessionAttributes session) ->
                session.createdTime, Comparator.reverseOrder())
                .thenComparing(session -> session.endTime, Comparator.nullsFirst(Comparator.reverseOrder()))
                .thenComparing(session -> session.startTime, Comparator.reverseOrder())
                .thenComparing(session -> session.courseId)
                .thenComparing(session -> session.feedbackSessionName));
    }

    public void setFeedbackSessionName(String feedbackSessionName) {
        this.feedbackSessionName = feedbackSessionName;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getCreatorEmail() {
        return creatorEmail;
    }

    public void setCreatorEmail(String creatorEmail) {
        this.creatorEmail = creatorEmail;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getCreatedTimeDateString() {
        return TimeHelper.formatDateForInstructorPages(createdTime, timeZone);
    }

    public String getCreatedTimeDateStamp() {
        return TimeHelper.formatDateTimeToIso8601Utc(createdTime);
    }

    public String getCreatedTimeFullDateTimeString() {
        LocalDateTime localDateTime = TimeHelper.convertInstantToLocalDateTime(createdTime, timeZone);
        return TimeHelper.formatDateTimeForDisplay(localDateTime);
    }

    public String getDeletedTimeDateString() {
        if (this.deletedTime == null) {
            return Const.DELETION_DATE_NOT_APPLICABLE;
        }
        return TimeHelper.formatDateForInstructorPages(deletedTime, timeZone);
    }

    public String getDeletedTimeDateStamp() {
        if (this.deletedTime == null) {
            return Const.DELETION_DATE_NOT_APPLICABLE;
        }
        return TimeHelper.formatDateTimeToIso8601Utc(deletedTime);
    }

    public String getDeletedTimeFullDateTimeString() {
        if (this.deletedTime == null) {
            return Const.DELETION_DATE_NOT_APPLICABLE;
        }
        LocalDateTime localDateTime = TimeHelper.convertInstantToLocalDateTime(deletedTime, timeZone);
        return TimeHelper.formatDateTimeForDisplay(localDateTime);
    }

    public Instant getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Instant createdTime) {
        this.createdTime = createdTime;
    }

    public Instant getDeletedTime() {
        return deletedTime;
    }

    public void setDeletedTime(Instant deletedTime) {
        this.deletedTime = deletedTime;
    }

    public void resetDeletedTime() {
        this.deletedTime = null;
    }

    public boolean isSessionDeleted() {
        return this.deletedTime != null;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public LocalDateTime getStartTimeLocal() {
        return TimeHelper.convertInstantToLocalDateTime(startTime, timeZone);
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public LocalDateTime getEndTimeLocal() {
        return TimeHelper.convertInstantToLocalDateTime(endTime, timeZone);
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public Instant getSessionVisibleFromTime() {
        return sessionVisibleFromTime;
    }

    public LocalDateTime getSessionVisibleFromTimeLocal() {
        return TimeHelper.convertInstantToLocalDateTime(sessionVisibleFromTime, timeZone);
    }

    public void setSessionVisibleFromTime(Instant sessionVisibleFromTime) {
        this.sessionVisibleFromTime = sessionVisibleFromTime;
    }

    public Instant getResultsVisibleFromTime() {
        return resultsVisibleFromTime;
    }

    public LocalDateTime getResultsVisibleFromTimeLocal() {
        return TimeHelper.convertInstantToLocalDateTime(resultsVisibleFromTime, timeZone);
    }

    public void setResultsVisibleFromTime(Instant resultsVisibleFromTime) {
        this.resultsVisibleFromTime = resultsVisibleFromTime;
    }

    public ZoneId getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(ZoneId timeZone) {
        this.timeZone = timeZone;
    }

    public long getGracePeriodMinutes() {
        return gracePeriod.toMinutes();
    }

    public void setGracePeriodMinutes(long gracePeriodMinutes) {
        this.gracePeriod = Duration.ofMinutes(gracePeriodMinutes);
    }

    public boolean isSentOpenEmail() {
        return sentOpenEmail;
    }

    public void setSentOpenEmail(boolean sentOpenEmail) {
        this.sentOpenEmail = sentOpenEmail;
    }

    public boolean isSentClosingEmail() {
        return sentClosingEmail;
    }

    public void setSentClosingEmail(boolean sentClosingEmail) {
        this.sentClosingEmail = sentClosingEmail;
    }

    public boolean isSentClosedEmail() {
        return sentClosedEmail;
    }

    public void setSentClosedEmail(boolean sentClosedEmail) {
        this.sentClosedEmail = sentClosedEmail;
    }

    public boolean isSentPublishedEmail() {
        return sentPublishedEmail;
    }

    public void setSentPublishedEmail(boolean sentPublishedEmail) {
        this.sentPublishedEmail = sentPublishedEmail;
    }

    public boolean isOpeningEmailEnabled() {
        return isOpeningEmailEnabled;
    }

    public void setOpeningEmailEnabled(boolean isOpeningEmailEnabled) {
        this.isOpeningEmailEnabled = isOpeningEmailEnabled;
    }

    public boolean isClosingEmailEnabled() {
        return isClosingEmailEnabled;
    }

    public void setClosingEmailEnabled(boolean isClosingEmailEnabled) {
        this.isClosingEmailEnabled = isClosingEmailEnabled;
    }

    public boolean isPublishedEmailEnabled() {
        return isPublishedEmailEnabled;
    }

    public void setPublishedEmailEnabled(boolean isPublishedEmailEnabled) {
        this.isPublishedEmailEnabled = isPublishedEmailEnabled;
    }

    public Set<String> getRespondingInstructorList() {
        return respondingInstructorList;
    }

    public void setRespondingInstructorList(Set<String> respondingInstructorList) {
        this.respondingInstructorList = respondingInstructorList;
    }

    public Set<String> getRespondingStudentList() {
        return respondingStudentList;
    }

    public void setRespondingStudentList(Set<String> respondingStudentList) {
        this.respondingStudentList = respondingStudentList;
    }

    /**
     * Updates with {@link UpdateOptions}.
     */
    public void update(UpdateOptions updateOptions) {
        updateOptions.instructionsOption.ifPresent(s -> instructions = s);
        updateOptions.deletedTimeOption.ifPresent(s -> deletedTime = s);
        updateOptions.startTimeOption.ifPresent(s -> startTime = s);
        updateOptions.endTimeOption.ifPresent(s -> endTime = s);
        updateOptions.sessionVisibleFromTimeOption.ifPresent(s -> sessionVisibleFromTime = s);
        updateOptions.resultsVisibleFromTimeOption.ifPresent(s -> resultsVisibleFromTime = s);
        updateOptions.timeZoneOption.ifPresent(s -> timeZone = s);
        updateOptions.gracePeriodOption.ifPresent(s -> gracePeriod = s);
        updateOptions.sentOpenEmailOption.ifPresent(s -> sentOpenEmail = s);
        updateOptions.sentClosingEmailOption.ifPresent(s -> sentClosingEmail = s);
        updateOptions.sentClosedEmailOption.ifPresent(s -> sentClosedEmail = s);
        updateOptions.sentPublishedEmailOption.ifPresent(s -> sentPublishedEmail = s);
        updateOptions.isClosingEmailEnabledOption.ifPresent(s -> isClosingEmailEnabled = s);
        updateOptions.isPublishedEmailEnabledOption.ifPresent(s -> isPublishedEmailEnabled = s);

        updateOptions.addingStudentRespondentOption.ifPresent(s -> respondingStudentList.add(s));
        updateOptions.removingStudentRespondentOption.ifPresent(s -> respondingStudentList.remove(s));
        updateOptions.addingInstructorRespondentOption.ifPresent(s -> respondingInstructorList.add(s));
        updateOptions.removingInstructorRespondentOption.ifPresent(s -> respondingInstructorList.remove(s));

        updateOptions.updatingStudentRespondentOption.ifPresent(s -> {
            if (respondingStudentList.contains(s.getOldEmail())) {
                respondingStudentList.remove(s.getOldEmail());
                respondingStudentList.add(s.getNewEmail());
            }
        });

        updateOptions.updatingInstructorRespondentOption.ifPresent(s -> {
            if (respondingInstructorList.contains(s.getOldEmail())) {
                respondingInstructorList.remove(s.getOldEmail());
                respondingInstructorList.add(s.getNewEmail());
            }
        });
    }

    /**
     * Returns a {@link UpdateOptions.Builder} to build {@link UpdateOptions} for a session.
     */
    public static UpdateOptions.Builder updateOptionsBuilder(String feedbackSessionName, String courseId) {
        return new UpdateOptions.Builder(feedbackSessionName, courseId);
    }

    /**
     * Returns a {@link UpdateOptions.Builder} to build on top of {@code updateOptions}.
     */
    public static UpdateOptions.Builder updateOptionsBuilder(UpdateOptions updateOptions) {
        return new UpdateOptions.Builder(updateOptions);
    }

    /**
     * A Builder for {@link FeedbackSessionAttributes}.
     */
    public static class Builder {
        private final FeedbackSessionAttributes feedbackSessionAttributes;

        public Builder(String feedbackSessionName, String courseId, String creatorEmail) {
            feedbackSessionAttributes = new FeedbackSessionAttributes();

            feedbackSessionAttributes.setFeedbackSessionName(feedbackSessionName);
            feedbackSessionAttributes.setCourseId(courseId);
            feedbackSessionAttributes.setCreatorEmail(creatorEmail);
        }

        public Builder withInstructions(String instructions) {
            feedbackSessionAttributes.setInstructions(instructions == null ? "" : instructions);
            return this;
        }

        public Builder withCreatedTime(Instant createdTime) {
            if (createdTime != null) {
                feedbackSessionAttributes.setCreatedTime(createdTime);
            }
            return this;
        }

        public Builder withDeletedTime(Instant deletedTime) {
            if (deletedTime != null) {
                feedbackSessionAttributes.setDeletedTime(deletedTime);
            }
            return this;
        }

        public Builder withStartTime(Instant startTime) {
            if (startTime != null) {
                feedbackSessionAttributes.setStartTime(startTime);
            }
            return this;
        }

        public Builder withEndTime(Instant endTime) {
            if (endTime != null) {
                feedbackSessionAttributes.setEndTime(endTime);
            }
            return this;
        }

        public Builder withSessionVisibleFromTime(Instant sessionVisibleFromTime) {
            if (sessionVisibleFromTime != null) {
                feedbackSessionAttributes.setSessionVisibleFromTime(sessionVisibleFromTime);
            }
            return this;
        }

        public Builder withResultsVisibleFromTime(Instant resultsVisibleFromTime) {
            if (resultsVisibleFromTime != null) {
                feedbackSessionAttributes.setResultsVisibleFromTime(resultsVisibleFromTime);
            }
            return this;
        }

        public Builder withTimeZone(ZoneId timeZone) {
            feedbackSessionAttributes.setTimeZone(timeZone);
            return this;
        }

        public Builder withGracePeriodMinutes(long gracePeriodMinutes) {
            feedbackSessionAttributes.setGracePeriodMinutes(gracePeriodMinutes);
            return this;
        }

        public Builder withSentOpenEmail(boolean sentOpenEmail) {
            feedbackSessionAttributes.setSentOpenEmail(sentOpenEmail);
            return this;
        }

        public Builder withSentClosingEmail(boolean sentClosingEmail) {
            feedbackSessionAttributes.setSentClosingEmail(sentClosingEmail);
            return this;
        }

        public Builder withSentClosedEmail(boolean sentClosedEmail) {
            feedbackSessionAttributes.setSentClosedEmail(sentClosedEmail);
            return this;
        }

        public Builder withSentPublishedEmail(boolean sentPublishedEmail) {
            feedbackSessionAttributes.setSentPublishedEmail(sentPublishedEmail);
            return this;
        }

        public Builder withOpeningEmailEnabled(boolean openingEmailEnabled) {
            feedbackSessionAttributes.setOpeningEmailEnabled(openingEmailEnabled);
            return this;
        }

        public Builder withClosingEmailEnabled(boolean closingEmailEnabled) {
            feedbackSessionAttributes.setClosingEmailEnabled(closingEmailEnabled);
            return this;
        }

        public Builder withPublishedEmailEnabled(boolean publishedEmailEnabled) {
            feedbackSessionAttributes.setPublishedEmailEnabled(publishedEmailEnabled);
            return this;
        }

        public Builder withRespondingInstructorList(Set<String> respondingInstructorList) {
            Set<String> respondingList = respondingInstructorList == null
                    ? new HashSet<>()
                    : respondingInstructorList;
            feedbackSessionAttributes.setRespondingInstructorList(respondingList);
            return this;
        }

        public Builder withRespondingStudentList(Set<String> respondingStudentList) {
            Set<String> respondingList = respondingStudentList == null
                    ? new HashSet<>()
                    : respondingStudentList;
            feedbackSessionAttributes.setRespondingStudentList(respondingList);
            return this;
        }

        public FeedbackSessionAttributes build() {
            return feedbackSessionAttributes;
        }
    }

    /**
     * Helper class to specific the fields to update in {@link FeedbackSessionAttributes}.
     */
    public static class UpdateOptions {
        private String courseId;
        private String feedbackSessionName;

        private UpdateOption<String> instructionsOption = UpdateOption.empty();
        private UpdateOption<Instant> deletedTimeOption = UpdateOption.empty();
        private UpdateOption<Instant> startTimeOption = UpdateOption.empty();
        private UpdateOption<Instant> endTimeOption = UpdateOption.empty();
        private UpdateOption<Instant> sessionVisibleFromTimeOption = UpdateOption.empty();
        private UpdateOption<Instant> resultsVisibleFromTimeOption = UpdateOption.empty();
        private UpdateOption<ZoneId> timeZoneOption = UpdateOption.empty();
        private UpdateOption<Duration> gracePeriodOption = UpdateOption.empty();
        private UpdateOption<Boolean> sentOpenEmailOption = UpdateOption.empty();
        private UpdateOption<Boolean> sentClosingEmailOption = UpdateOption.empty();
        private UpdateOption<Boolean> sentClosedEmailOption = UpdateOption.empty();
        private UpdateOption<Boolean> sentPublishedEmailOption = UpdateOption.empty();
        private UpdateOption<Boolean> isClosingEmailEnabledOption = UpdateOption.empty();
        private UpdateOption<Boolean> isPublishedEmailEnabledOption = UpdateOption.empty();

        private UpdateOption<String> addingStudentRespondentOption = UpdateOption.empty();
        private UpdateOption<String> removingStudentRespondentOption = UpdateOption.empty();
        private UpdateOption<String> addingInstructorRespondentOption = UpdateOption.empty();
        private UpdateOption<String> removingInstructorRespondentOption = UpdateOption.empty();
        private UpdateOption<EmailChange> updatingStudentRespondentOption = UpdateOption.empty();
        private UpdateOption<EmailChange> updatingInstructorRespondentOption = UpdateOption.empty();

        private UpdateOptions(String feedbackSessionName, String courseId) {
            Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, feedbackSessionName);
            Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, courseId);

            this.feedbackSessionName = feedbackSessionName;
            this.courseId = courseId;
        }

        public String getCourseId() {
            return courseId;
        }

        public String getFeedbackSessionName() {
            return feedbackSessionName;
        }

        @Override
        public String toString() {
            return "StudentAttributes.UpdateOptions ["
                    + "feedbackSessionName = " + feedbackSessionName
                    + ", courseId = " + courseId
                    + ", instructions = " + instructionsOption
                    + ", startTime = " + startTimeOption
                    + ", endTime = " + endTimeOption
                    + ", sessionVisibleFromTime = " + sessionVisibleFromTimeOption
                    + ", resultsVisibleFromTime = " + resultsVisibleFromTimeOption
                    + ", timeZone = " + timeZoneOption
                    + ", gracePeriod = " + gracePeriodOption
                    + ", sentOpenEmail = " + sentOpenEmailOption
                    + ", sentClosingEmail = " + sentClosingEmailOption
                    + ", sentClosedEmail = " + sentClosedEmailOption
                    + ", sentPublishedEmail = " + sentPublishedEmailOption
                    + ", isClosingEmailEnabled = " + isClosingEmailEnabledOption
                    + ", isPublishedEmailEnabled = " + isPublishedEmailEnabledOption
                    + ", addingStudentRespondent = " + addingStudentRespondentOption
                    + ", removingStudentRespondent = " + removingStudentRespondentOption
                    + ", addingInstructorRespondent = " + addingInstructorRespondentOption
                    + ", removingInstructorRespondent = " + removingInstructorRespondentOption
                    + ", updatingStudentRespondent = " + updatingStudentRespondentOption
                    + ", updatingInstructorRespondent = " + updatingInstructorRespondentOption
                    + "]";
        }

        /**
         * Represents the change of email for an(a) instructor/student.
         */
        private static class EmailChange {

            private String oldEmail;
            private String newEmail;

            private EmailChange(String oldEmail, String newEmail) {
                this.oldEmail = oldEmail;
                this.newEmail = newEmail;
            }

            private String getOldEmail() {
                return oldEmail;
            }

            private String getNewEmail() {
                return newEmail;
            }
        }

        /**
         * Builder class to build {@link UpdateOptions}.
         */
        public static class Builder {
            private UpdateOptions updateOptions;

            private Builder(UpdateOptions updateOptions) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, updateOptions);

                this.updateOptions = updateOptions;
            }

            private Builder(String feedbackSessionName, String courseId) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, feedbackSessionName);
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, courseId);

                updateOptions = new UpdateOptions(feedbackSessionName, courseId);
            }

            public Builder withInstructions(String instruction) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, instruction);

                updateOptions.instructionsOption = UpdateOption.of(instruction);
                return this;
            }

            public Builder withStartTime(Instant startTime) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, startTime);

                updateOptions.startTimeOption = UpdateOption.of(startTime);
                return this;
            }

            public Builder withEndTime(Instant gender) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, gender);

                updateOptions.endTimeOption = UpdateOption.of(gender);
                return this;
            }

            public Builder withSessionVisibleFromTime(Instant sessionVisibleFromTime) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, sessionVisibleFromTime);

                updateOptions.sessionVisibleFromTimeOption = UpdateOption.of(sessionVisibleFromTime);
                return this;
            }

            public Builder withResultsVisibleFromTime(Instant resultsVisibleFromTime) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, resultsVisibleFromTime);

                updateOptions.resultsVisibleFromTimeOption = UpdateOption.of(resultsVisibleFromTime);
                return this;
            }

            public Builder withTimeZone(ZoneId timeZone) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, timeZone);

                updateOptions.timeZoneOption = UpdateOption.of(timeZone);
                return this;
            }

            public Builder withGracePeriod(Duration gracePeriod) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, gracePeriod);

                updateOptions.gracePeriodOption = UpdateOption.of(gracePeriod);
                return this;
            }

            public Builder withSentOpenEmail(boolean sentOpenEmail) {
                updateOptions.sentOpenEmailOption = UpdateOption.of(sentOpenEmail);
                return this;
            }

            public Builder withSentClosingEmail(boolean sentClosingEmail) {
                updateOptions.sentClosingEmailOption = UpdateOption.of(sentClosingEmail);
                return this;
            }

            public Builder withSentClosedEmail(boolean sentClosedEmail) {
                updateOptions.sentClosedEmailOption = UpdateOption.of(sentClosedEmail);
                return this;
            }

            public Builder withSentPublishedEmail(boolean sentPublishedEmail) {
                updateOptions.sentPublishedEmailOption = UpdateOption.of(sentPublishedEmail);
                return this;
            }

            public Builder withIsClosingEmailEnabled(boolean isClosingEmailEnabled) {
                updateOptions.isClosingEmailEnabledOption = UpdateOption.of(isClosingEmailEnabled);
                return this;
            }

            public Builder withIsPublishedEmailEnabled(boolean isPublishedEmailEnabled) {
                updateOptions.isPublishedEmailEnabledOption = UpdateOption.of(isPublishedEmailEnabled);
                return this;
            }

            public Builder withAddingStudentRespondent(String email) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, email);

                updateOptions.addingStudentRespondentOption = UpdateOption.of(email);
                return this;
            }

            public Builder withRemovingStudentRespondent(String email) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, email);

                updateOptions.removingStudentRespondentOption = UpdateOption.of(email);
                return this;
            }

            public Builder withAddingInstructorRespondent(String email) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, email);

                updateOptions.addingInstructorRespondentOption = UpdateOption.of(email);
                return this;
            }

            public Builder withRemovingInstructorRespondent(String email) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, email);

                updateOptions.removingInstructorRespondentOption = UpdateOption.of(email);
                return this;
            }

            public Builder withUpdatingStudentRespondent(String oldEmail, String newEmail) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, oldEmail);
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, newEmail);

                updateOptions.updatingStudentRespondentOption = UpdateOption.of(new EmailChange(oldEmail, newEmail));
                return this;
            }

            public Builder withUpdatingInstructorRespondent(String oldEmail, String newEmail) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, oldEmail);
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, newEmail);

                updateOptions.updatingInstructorRespondentOption = UpdateOption.of(new EmailChange(oldEmail, newEmail));
                return this;
            }

            public UpdateOptions build() {
                return updateOptions;
            }

        }

    }
}
