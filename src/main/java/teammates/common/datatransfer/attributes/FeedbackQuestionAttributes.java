package teammates.common.datatransfer.attributes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import teammates.common.datatransfer.FeedbackParticipantType;
import teammates.common.datatransfer.questions.FeedbackQuestionDetails;
import teammates.common.datatransfer.questions.FeedbackQuestionType;
import teammates.common.datatransfer.questions.FeedbackTextQuestionDetails;
import teammates.common.util.Assumption;
import teammates.common.util.Const;
import teammates.common.util.FieldValidator;
import teammates.common.util.JsonUtils;
import teammates.common.util.SanitizationHelper;
import teammates.storage.entity.FeedbackQuestion;

public class FeedbackQuestionAttributes extends EntityAttributes<FeedbackQuestion>
        implements Comparable<FeedbackQuestionAttributes> {

    private static final String FEEDBACK_QUESTION_BACKUP_LOG_MSG = "Recently modified feedback question::";

    public String feedbackSessionName;
    public String courseId;
    /**
     * Contains the JSON formatted string that holds the information of the question details.
     *
     * <p>Don't use directly unless for storing/loading from data store.<br>
     * To get the question text use {@code getQuestionDetails().questionText}
     */
    public String questionMetaData;
    public String questionDescription;
    public int questionNumber;
    public FeedbackQuestionType questionType;
    public FeedbackParticipantType giverType;
    public FeedbackParticipantType recipientType;
    public int numberOfEntitiesToGiveFeedbackTo;
    public List<FeedbackParticipantType> showResponsesTo;
    public List<FeedbackParticipantType> showGiverNameTo;
    public List<FeedbackParticipantType> showRecipientNameTo;
    protected transient Instant createdAt;
    protected transient Instant updatedAt;
    private String feedbackQuestionId;

    FeedbackQuestionAttributes() {
        this.showResponsesTo = new ArrayList<>();
        this.showGiverNameTo = new ArrayList<>();
        this.showRecipientNameTo = new ArrayList<>();
    }

    /**
     * Returns a builder for {@link FeedbackQuestionAttributes}.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static FeedbackQuestionAttributes valueOf(FeedbackQuestion fq) {
        FeedbackQuestionAttributes faq = new FeedbackQuestionAttributes();

        faq.feedbackSessionName = fq.getFeedbackSessionName();
        faq.courseId = fq.getCourseId();
        faq.questionMetaData = fq.getQuestionMetaData();
        faq.questionDescription = fq.getQuestionDescription();
        faq.questionNumber = fq.getQuestionNumber();
        faq.questionType = fq.getQuestionType();
        faq.giverType = fq.getGiverType();
        faq.recipientType = fq.getRecipientType();
        faq.numberOfEntitiesToGiveFeedbackTo = fq.getNumberOfEntitiesToGiveFeedbackTo();
        if (fq.getShowResponsesTo() != null) {
            faq.showResponsesTo = fq.getShowResponsesTo();
        }
        if (fq.getShowGiverNameTo() != null) {
            faq.showGiverNameTo = fq.getShowGiverNameTo();
        }
        if (fq.getShowRecipientNameTo() != null) {
            faq.showRecipientNameTo = fq.getShowRecipientNameTo();
        }
        faq.createdAt = fq.getCreatedAt();
        faq.updatedAt = fq.getUpdatedAt();
        faq.feedbackQuestionId = fq.getId();

        return faq;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getId() {
        return feedbackQuestionId;
    }

    /** NOTE: Only use this to match and search for the ID of a known existing question entity. */
    @Deprecated
    public void setId(String id) {
        this.feedbackQuestionId = id;
    }

    @Override
    public FeedbackQuestion toEntity() {
        return new FeedbackQuestion(feedbackSessionName, courseId,
                                    questionMetaData, questionDescription, questionNumber, questionType, giverType,
                                    recipientType, numberOfEntitiesToGiveFeedbackTo,
                                    showResponsesTo, showGiverNameTo, showRecipientNameTo);
    }

    public FeedbackQuestionAttributes getCopy() {
        FeedbackQuestionAttributes faq = new FeedbackQuestionAttributes();

        faq.feedbackSessionName = this.feedbackSessionName;
        faq.courseId = this.courseId;
        faq.questionMetaData = this.questionMetaData;
        faq.questionDescription = this.questionDescription;
        faq.questionNumber = this.questionNumber;
        faq.questionType = this.questionType;
        faq.giverType = this.giverType;
        faq.recipientType = this.recipientType;
        faq.numberOfEntitiesToGiveFeedbackTo = this.numberOfEntitiesToGiveFeedbackTo;
        faq.showResponsesTo = new ArrayList<>(this.showResponsesTo);
        faq.showGiverNameTo = new ArrayList<>(this.showGiverNameTo);
        faq.showRecipientNameTo = new ArrayList<>(this.showRecipientNameTo);
        faq.createdAt = this.createdAt;
        faq.updatedAt = this.updatedAt;
        faq.feedbackQuestionId = this.feedbackQuestionId;

        return faq;
    }

    @Override
    public String toString() {
        return "FeedbackQuestionAttributes [feedbackSessionName="
               + feedbackSessionName + ", courseId=" + courseId
               + ", questionText="
               + questionMetaData + ", questionDescription=" + questionDescription
               + ", questionNumber=" + questionNumber
               + ", questionType=" + questionType + ", giverType=" + giverType
               + ", recipientType=" + recipientType
               + ", numberOfEntitiesToGiveFeedbackTo="
               + numberOfEntitiesToGiveFeedbackTo + ", showResponsesTo="
               + showResponsesTo + ", showGiverNameTo=" + showGiverNameTo
               + ", showRecipientNameTo=" + showRecipientNameTo + "]";
    }

    @Override
    public String getBackupIdentifier() {
        return FEEDBACK_QUESTION_BACKUP_LOG_MSG + getId();
    }

    @Override
    public List<String> getInvalidityInfo() {
        FieldValidator validator = new FieldValidator();
        List<String> errors = new ArrayList<>();

        addNonEmptyError(validator.getInvalidityInfoForFeedbackSessionName(feedbackSessionName), errors);

        addNonEmptyError(validator.getInvalidityInfoForCourseId(courseId), errors);

        errors.addAll(validator.getValidityInfoForFeedbackParticipantType(giverType, recipientType));

        errors.addAll(validator.getValidityInfoForFeedbackResponseVisibility(showResponsesTo,
                                                                             showGiverNameTo,
                                                                             showRecipientNameTo));

        return errors;
    }

    @Override
    public boolean isValid() {
        return getInvalidityInfo().isEmpty();
    }

    public boolean isGiverAStudent() {
        return giverType == FeedbackParticipantType.SELF
               || giverType == FeedbackParticipantType.STUDENTS;
    }

    public boolean isRecipientNameHidden() {
        return recipientType == FeedbackParticipantType.NONE
               || recipientType == FeedbackParticipantType.SELF;
    }

    public boolean isRecipientAStudent() {
        return recipientType == FeedbackParticipantType.SELF
               || recipientType == FeedbackParticipantType.STUDENTS
               || recipientType == FeedbackParticipantType.OWN_TEAM_MEMBERS
               || recipientType == FeedbackParticipantType.OWN_TEAM_MEMBERS_INCLUDING_SELF;
    }

    public boolean isRecipientInstructor() {
        return recipientType == FeedbackParticipantType.INSTRUCTORS;
    }

    public boolean isResponseVisibleTo(FeedbackParticipantType userType) {
        return showResponsesTo.contains(userType);
    }

    /**
     * Checks if updating this question to the {@code newAttributes} will
     * require the responses to be deleted for consistency.
     * Does not check if any responses exist.
     */
    public boolean areResponseDeletionsRequiredForChanges(FeedbackQuestionAttributes newAttributes) {
        if (!newAttributes.giverType.equals(this.giverType)
                || !newAttributes.recipientType.equals(this.recipientType)) {
            return true;
        }

        return this.getQuestionDetails().shouldChangesRequireResponseDeletion(newAttributes.getQuestionDetails());
    }

    @Override
    public int compareTo(FeedbackQuestionAttributes o) {
        if (o == null) {
            return 1;
        }

        if (this.questionNumber != o.questionNumber) {
            return Integer.compare(this.questionNumber, o.questionNumber);
        }
        /**
         * Although question numbers ought to be unique in a feedback session,
         * eventual consistency can result in duplicate questions numbers.
         * Therefore, to ensure that the question order is always consistent to the user,
         * compare feedbackQuestionId, which is guaranteed to be unique,
         * when the questionNumbers are the same.
         */
        return this.feedbackQuestionId.compareTo(o.feedbackQuestionId);
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;

        result = prime * result + (courseId == null ? 0 : courseId.hashCode());

        result = prime * result + (feedbackSessionName == null ? 0 : feedbackSessionName.hashCode());

        result = prime * result + (giverType == null ? 0 : giverType.hashCode());

        result = prime * result + numberOfEntitiesToGiveFeedbackTo;

        result = prime * result + questionNumber;

        result = prime * result + (questionMetaData == null ? 0 : questionMetaData.hashCode());

        result = prime * result + (questionDescription == null ? 0 : questionDescription.hashCode());

        result = prime * result + (questionType == null ? 0 : questionType.hashCode());

        result = prime * result + (recipientType == null ? 0 : recipientType.hashCode());

        result = prime * result + (showGiverNameTo == null ? 0 : showGiverNameTo.hashCode());

        result = prime * result + (showRecipientNameTo == null ? 0 : showRecipientNameTo.hashCode());

        result = prime * result + (showResponsesTo == null ? 0 : showResponsesTo.hashCode());

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        FeedbackQuestionAttributes other = (FeedbackQuestionAttributes) obj;

        if (courseId == null) {
            if (other.courseId != null) {
                return false;
            }
        } else if (!courseId.equals(other.courseId)) {
            return false;
        }

        if (feedbackSessionName == null) {
            if (other.feedbackSessionName != null) {
                return false;
            }
        } else if (!feedbackSessionName.equals(other.feedbackSessionName)) {
            return false;
        }

        if (giverType != other.giverType) {
            return false;
        }

        if (numberOfEntitiesToGiveFeedbackTo != other.numberOfEntitiesToGiveFeedbackTo) {
            return false;
        }

        if (questionNumber != other.questionNumber) {
            return false;
        }

        if (questionMetaData == null) {
            if (other.questionMetaData != null) {
                return false;
            }
        } else if (!questionMetaData.equals(other.questionMetaData)) {
            return false;
        }

        if (questionDescription == null) {
            if (other.questionDescription != null) {
                return false;
            }
        } else if (!questionDescription.equals(other.questionDescription)) {
            return false;
        }

        if (questionType != other.questionType) {
            return false;
        }

        if (recipientType != other.recipientType) {
            return false;
        }

        if (showGiverNameTo == null) {
            if (other.showGiverNameTo != null) {
                return false;
            }
        } else if (!showGiverNameTo.equals(other.showGiverNameTo)) {
            return false;
        }

        if (showRecipientNameTo == null) {
            if (other.showRecipientNameTo != null) {
                return false;
            }
        } else if (!showRecipientNameTo.equals(other.showRecipientNameTo)) {
            return false;
        }

        if (showResponsesTo == null) {
            if (other.showResponsesTo != null) {
                return false;
            }
        } else if (!showResponsesTo.equals(other.showResponsesTo)) {
            return false;
        }

        return true;
    }

    public void updateValues(FeedbackQuestionAttributes newAttributes) {
        // These can't be changed anyway. Copy values to defensively avoid invalid parameters.
        newAttributes.feedbackSessionName = this.feedbackSessionName;
        newAttributes.courseId = this.courseId;

        if (newAttributes.questionMetaData == null) {
            newAttributes.questionMetaData = this.questionMetaData;
        }

        if (newAttributes.questionDescription == null) {
            newAttributes.questionDescription = this.questionDescription;
        }

        if (newAttributes.questionType == null) {
            newAttributes.questionType = this.questionType;
        }

        if (newAttributes.giverType == null) {
            newAttributes.giverType = this.giverType;
        }

        if (newAttributes.recipientType == null) {
            newAttributes.recipientType = this.recipientType;
        }

        if (newAttributes.showResponsesTo == null) {
            newAttributes.showResponsesTo = this.showResponsesTo;
        }

        if (newAttributes.showGiverNameTo == null) {
            newAttributes.showGiverNameTo = this.showGiverNameTo;
        }

        if (newAttributes.showRecipientNameTo == null) {
            newAttributes.showRecipientNameTo = this.showRecipientNameTo;
        }
    }

    public void removeIrrelevantVisibilityOptions() {
        List<FeedbackParticipantType> optionsToRemove = new ArrayList<>();

        if (recipientType != null) {
            switch (recipientType) {
            case NONE:
                optionsToRemove.add(FeedbackParticipantType.RECEIVER);
                optionsToRemove.add(FeedbackParticipantType.RECEIVER_TEAM_MEMBERS);
                break;
            case TEAMS:
            case INSTRUCTORS:
            case OWN_TEAM:
            case OWN_TEAM_MEMBERS:
                optionsToRemove.add(FeedbackParticipantType.RECEIVER_TEAM_MEMBERS);
                break;
            default:
                break;
            }
        }

        if (giverType != null) {
            switch (giverType) {
            case TEAMS:
            case INSTRUCTORS:
                optionsToRemove.add(FeedbackParticipantType.OWN_TEAM_MEMBERS);
                break;
            default:
                break;
            }
        }

        removeVisibilities(optionsToRemove);
    }

    private void removeVisibilities(List<FeedbackParticipantType> optionsToRemove) {
        if (showRecipientNameTo != null) {
            showResponsesTo.removeAll(optionsToRemove);
        }

        if (showGiverNameTo != null) {
            showGiverNameTo.removeAll(optionsToRemove);
        }

        if (showRecipientNameTo != null) {
            showRecipientNameTo.removeAll(optionsToRemove);
        }
    }

    @Override
    public void sanitizeForSaving() {
        this.questionDescription = SanitizationHelper.sanitizeForRichText(this.questionDescription);
    }

    private boolean isValidJsonString(String jsonString) {
        try {
            new JSONObject(jsonString);
        } catch (JSONException e) {
            return false;
        }
        return true;
    }

    /**
     * Converts the given Feedback*QuestionDetails object to JSON for storing.
     */
    public void setQuestionDetails(FeedbackQuestionDetails questionDetails) {
        questionMetaData = JsonUtils.toJson(questionDetails, getFeedbackQuestionDetailsClass());
    }

    /**
     * Retrieves the Feedback*QuestionDetails object for this question.
     *
     * @return The Feedback*QuestionDetails object representing the question's details
     */
    public FeedbackQuestionDetails getQuestionDetails() {
        // For old Text questions, the questionText simply contains the question, not a JSON
        if (questionType == FeedbackQuestionType.TEXT && !isValidJsonString(questionMetaData)) {
            return new FeedbackTextQuestionDetails(questionMetaData);
        }
        return JsonUtils.fromJson(questionMetaData, getFeedbackQuestionDetailsClass());
    }

    /**
     * This method gets the appropriate class type for the Feedback*QuestionDetails object for this question.
     *
     * @return The Feedback*QuestionDetails class type appropriate for this question.
     */
    private Class<? extends FeedbackQuestionDetails> getFeedbackQuestionDetailsClass() {
        return questionType.getQuestionDetailsClass();
    }

    public String getFeedbackQuestionId() {
        return feedbackQuestionId;
    }

    public String getFeedbackSessionName() {
        return feedbackSessionName;
    }

    public String getCourseId() {
        return courseId;
    }

    public String getQuestionMetaData() {
        return questionMetaData;
    }

    public String getQuestionDescription() {
        return questionDescription;
    }

    public void setQuestionDescription(String questionDescription) {
        this.questionDescription = questionDescription;
    }

    public int getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(int questionNumber) {
        this.questionNumber = questionNumber;
    }

    public FeedbackQuestionType getQuestionType() {
        return questionType;
    }

    public FeedbackParticipantType getGiverType() {
        return giverType;
    }

    public void setGiverType(FeedbackParticipantType giverType) {
        this.giverType = giverType;
    }

    public FeedbackParticipantType getRecipientType() {
        return recipientType;
    }

    public void setRecipientType(FeedbackParticipantType recipientType) {
        this.recipientType = recipientType;
    }

    public int getNumberOfEntitiesToGiveFeedbackTo() {
        return numberOfEntitiesToGiveFeedbackTo;
    }

    public void setNumberOfEntitiesToGiveFeedbackTo(int numberOfEntitiesToGiveFeedbackTo) {
        this.numberOfEntitiesToGiveFeedbackTo = numberOfEntitiesToGiveFeedbackTo;
    }

    public List<FeedbackParticipantType> getShowResponsesTo() {
        return showResponsesTo;
    }

    public void setShowResponsesTo(List<FeedbackParticipantType> showResponsesTo) {
        this.showResponsesTo = showResponsesTo;
    }

    public List<FeedbackParticipantType> getShowGiverNameTo() {
        return showGiverNameTo;
    }

    public void setShowGiverNameTo(List<FeedbackParticipantType> showGiverNameTo) {
        this.showGiverNameTo = showGiverNameTo;
    }

    public List<FeedbackParticipantType> getShowRecipientNameTo() {
        return showRecipientNameTo;
    }

    public void setShowRecipientNameTo(List<FeedbackParticipantType> showRecipientNameTo) {
        this.showRecipientNameTo = showRecipientNameTo;
    }

    public String getQuestionAdditionalInfoHtml() {
        return getQuestionDetails().getQuestionAdditionalInfoHtml(questionNumber, "");
    }

    /**
     * Updates with {@link UpdateOptions}.
     */
    public void update(FeedbackQuestionAttributes.UpdateOptions updateOptions) {
        updateOptions.questionNumberOption.ifPresent(s -> questionNumber = s);
        updateOptions.questionDetailsOption.ifPresent(
                s -> questionMetaData = JsonUtils.toJson(s, getFeedbackQuestionDetailsClass()));
        updateOptions.questionDescriptionOption.ifPresent(s -> questionDescription = s);
        updateOptions.giverTypeOption.ifPresent(s -> giverType = s);
        updateOptions.recipientTypeOption.ifPresent(s -> recipientType = s);
        updateOptions.numberOfEntitiesToGiveFeedbackToOption.ifPresent(s -> numberOfEntitiesToGiveFeedbackTo = s);
        updateOptions.showResponsesToOption.ifPresent(s -> showResponsesTo = s);
        updateOptions.showGiverNameToOption.ifPresent(s -> showGiverNameTo = s);
        updateOptions.showRecipientNameToOption.ifPresent(s -> showRecipientNameTo = s);

        removeIrrelevantVisibilityOptions();
    }

    /**
     * Returns a {@link UpdateOptions.Builder}
     * to build {@link UpdateOptions} for a question.
     */
    public static FeedbackQuestionAttributes.UpdateOptions.Builder updateOptionsBuilder(String feedbackQuestionId) {
        return new FeedbackQuestionAttributes.UpdateOptions.Builder(feedbackQuestionId);
    }

    /**
     * A Builder class for {@link FeedbackQuestionAttributes}.
     */
    public static class Builder extends BasicBuilder<FeedbackQuestionAttributes, Builder> {
        private final FeedbackQuestionAttributes feedbackQuestionAttributes;

        private Builder() {
            super(new UpdateOptions(""));
            thisBuilder = this;

            feedbackQuestionAttributes = new FeedbackQuestionAttributes();
        }

        public Builder withFeedbackSessionName(String feedbackSessionName) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, feedbackSessionName);

            feedbackQuestionAttributes.feedbackSessionName = feedbackSessionName;
            return this;
        }

        public Builder withCourseId(String courseId) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, courseId);

            feedbackQuestionAttributes.courseId = courseId;
            return this;
        }

        public Builder withQuestionType(FeedbackQuestionType questionType) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, questionType);

            if (questionType != null) {
                feedbackQuestionAttributes.questionType = questionType;
            }
            return this;
        }

        @Override
        public FeedbackQuestionAttributes build() {
            feedbackQuestionAttributes.update(updateOptions);
            feedbackQuestionAttributes.removeIrrelevantVisibilityOptions();

            return feedbackQuestionAttributes;
        }
    }

    /**
     * Helper class to specific the fields to update in {@link FeedbackQuestionAttributes}.
     */
    public static class UpdateOptions {
        private String feedbackQuestionId;

        private UpdateOption<FeedbackQuestionDetails> questionDetailsOption = UpdateOption.empty();
        private UpdateOption<String> questionDescriptionOption = UpdateOption.empty();
        private UpdateOption<Integer> questionNumberOption = UpdateOption.empty();
        private UpdateOption<FeedbackParticipantType> giverTypeOption = UpdateOption.empty();
        private UpdateOption<FeedbackParticipantType> recipientTypeOption = UpdateOption.empty();
        private UpdateOption<Integer> numberOfEntitiesToGiveFeedbackToOption = UpdateOption.empty();
        private UpdateOption<List<FeedbackParticipantType>> showResponsesToOption = UpdateOption.empty();
        private UpdateOption<List<FeedbackParticipantType>> showGiverNameToOption = UpdateOption.empty();
        private UpdateOption<List<FeedbackParticipantType>> showRecipientNameToOption = UpdateOption.empty();

        private UpdateOptions(String feedbackQuestionId) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, feedbackQuestionId);

            this.feedbackQuestionId = feedbackQuestionId;
        }

        public String getFeedbackQuestionId() {
            return feedbackQuestionId;
        }

        @Override
        public String toString() {
            return "FeedbackQuestionAttributes.UpdateOptions ["
                    + "feedbackQuestionId = " + feedbackQuestionId
                    + ", questionDetails = " + JsonUtils.toJson(questionDetailsOption)
                    + ", questionDescription = " + questionDescriptionOption
                    + ", questionNumber = " + questionNumberOption
                    + ", giverType = " + giverTypeOption
                    + ", recipientType = " + recipientTypeOption
                    + ", numberOfEntitiesToGiveFeedbackTo = " + numberOfEntitiesToGiveFeedbackToOption
                    + ", showResponsesTo = " + showResponsesToOption
                    + ", showGiverNameTo = " + showGiverNameToOption
                    + ", showRecipientNameTo = " + showRecipientNameToOption
                    + "]";
        }

        /**
         * Builder class to build {@link UpdateOptions}.
         */
        public static class Builder extends BasicBuilder<UpdateOptions, Builder> {

            private Builder(String feedbackQuestionId) {
                super(new UpdateOptions(feedbackQuestionId));
                thisBuilder = this;
            }

            @Override
            public UpdateOptions build() {
                return updateOptions;
            }

        }

    }

    /**
     * Basic builder to build {@link FeedbackQuestionAttributes} related classes.
     *
     * @param <T> type to be built
     * @param <B> type of the builder
     */
    private abstract static class BasicBuilder<T, B extends BasicBuilder<T, B>> {

        protected FeedbackQuestionAttributes.UpdateOptions updateOptions;
        protected B thisBuilder;

        protected BasicBuilder(UpdateOptions updateOptions) {
            this.updateOptions = updateOptions;
        }

        public B withQuestionDetails(FeedbackQuestionDetails questionDetails) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, questionDetails);

            updateOptions.questionDetailsOption = UpdateOption.of(questionDetails);
            return thisBuilder;
        }

        public B withQuestionDescription(String questionDescription) {
            // questionDescription can be null

            updateOptions.questionDescriptionOption = UpdateOption.of(questionDescription);
            return thisBuilder;
        }

        public B withQuestionNumber(int questionNumber) {
            updateOptions.questionNumberOption = UpdateOption.of(questionNumber);
            return thisBuilder;
        }

        public B withGiverType(FeedbackParticipantType giverType) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, giverType);

            updateOptions.giverTypeOption = UpdateOption.of(giverType);
            return thisBuilder;
        }

        public B withRecipientType(FeedbackParticipantType recipientType) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, recipientType);

            updateOptions.recipientTypeOption = UpdateOption.of(recipientType);
            return thisBuilder;
        }

        public B withNumberOfEntitiesToGiveFeedbackTo(int numberOfEntitiesToGiveFeedbackTo) {
            updateOptions.numberOfEntitiesToGiveFeedbackToOption = UpdateOption.of(numberOfEntitiesToGiveFeedbackTo);
            return thisBuilder;
        }

        public B withShowResponsesTo(List<FeedbackParticipantType> showResponsesTo) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, showResponsesTo);
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, (Object[]) showResponsesTo.toArray());

            updateOptions.showResponsesToOption = UpdateOption.of(new ArrayList<>(showResponsesTo));
            return thisBuilder;
        }

        public B withShowGiverNameTo(List<FeedbackParticipantType> showGiverNameTo) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, showGiverNameTo);
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, (Object[]) showGiverNameTo.toArray());

            updateOptions.showGiverNameToOption = UpdateOption.of(new ArrayList<>(showGiverNameTo));
            return thisBuilder;
        }

        public B withShowRecipientNameTo(List<FeedbackParticipantType> showRecipientNameTo) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, showRecipientNameTo);
            Assumption.assertNotNull(
                    Const.StatusCodes.NULL_PARAMETER, (Object[]) showRecipientNameTo.toArray());

            updateOptions.showRecipientNameToOption = UpdateOption.of(new ArrayList<>(showRecipientNameTo));
            return thisBuilder;
        }

        public abstract T build();

    }
}
