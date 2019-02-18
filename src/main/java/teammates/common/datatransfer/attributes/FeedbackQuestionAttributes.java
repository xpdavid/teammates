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

    protected FeedbackQuestionAttributes() {
        //attributes to be built by Builder
    }

    public static Builder builder() {
        return new Builder();
    }

    public static FeedbackQuestionAttributes valueOf(FeedbackQuestion fq) {
        return builder()
                .withFeedbackSessionName(fq.getFeedbackSessionName())
                .withCourseId(fq.getCourseId())
                .withQuestionMetaData(fq.getQuestionMetaData())
                .withQuestionDescription(fq.getQuestionDescription())
                .withQuestionNumber(fq.getQuestionNumber())
                .withQuestionType(fq.getQuestionType())
                .withGiverType(fq.getGiverType())
                .withRecipientType(fq.getRecipientType())
                .withNumOfEntitiesToGiveFeedbackTo(fq.getNumberOfEntitiesToGiveFeedbackTo())
                .withShowResponseTo(fq.getShowResponsesTo())
                .withShowGiverNameTo(fq.getShowGiverNameTo())
                .withShowRecipientNameTo(fq.getShowRecipientNameTo())
                .withCreatedAt(fq.getCreatedAt())
                .withUpdatedAt(fq.getUpdatedAt())
                .withFeedbackQuestionId(fq.getId())
                .build();

    }

    public Instant getCreatedAt() {
        return createdAt == null ? Const.TIME_REPRESENTS_DEFAULT_TIMESTAMP : createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt == null ? Const.TIME_REPRESENTS_DEFAULT_TIMESTAMP : updatedAt;
    }

    public String getId() {
        return feedbackQuestionId;
    }

    /** NOTE: Only use this to match and search for the ID of a known existing question entity. */
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
        return builder()
                .withFeedbackSessionName(getFeedbackSessionName())
                .withCourseId(getCourseId())
                .withQuestionMetaData(getQuestionMetaData())
                .withQuestionDescription(getQuestionDescription())
                .withQuestionNumber(getQuestionNumber())
                .withQuestionType(getQuestionType())
                .withGiverType(getGiverType())
                .withRecipientType(getRecipientType())
                .withNumOfEntitiesToGiveFeedbackTo(getNumberOfEntitiesToGiveFeedbackTo())
                .withShowResponseTo(new ArrayList<>(getShowResponsesTo()))
                .withShowGiverNameTo(new ArrayList<>(getShowGiverNameTo()))
                .withShowRecipientNameTo(new ArrayList<>(getShowRecipientNameTo()))
                .withCreatedAt(getCreatedAt())
                .withUpdatedAt(getUpdatedAt())
                .withFeedbackQuestionId(getId())
                .build();
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
    public String getJsonString() {
        return JsonUtils.toJson(this, FeedbackQuestionAttributes.class);
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

    // TODO: move following methods to PageData?
    // Answer: OK to move to the respective PageData class. Unit test this thoroughly.
    public List<String> getVisibilityMessage() {
        List<String> message = new ArrayList<>();

        for (FeedbackParticipantType participant : showResponsesTo) {
            StringBuilder line = new StringBuilder(100);

            // Exceptional case: self feedback
            if (participant == FeedbackParticipantType.RECEIVER
                    && recipientType == FeedbackParticipantType.SELF) {
                message.add("You can see your own feedback in the results page later on.");
                continue;
            }

            // Front fragment: e.g. Other students in the course..., The receiving.., etc.
            line.append(participant.toVisibilityString()).append(' ');

            // Recipient fragment: e.g. student, instructor, etc.
            if (participant == FeedbackParticipantType.RECEIVER) {
                line.append(recipientType.toSingularFormString());

                if (numberOfEntitiesToGiveFeedbackTo > 1) {
                    line.append('s');
                }

                line.append(' ');
            }

            line.append("can see your response");

            // Visibility fragment: e.g. can see your name, but not...
            if (showRecipientNameTo.contains(participant)) {
                if (participant != FeedbackParticipantType.RECEIVER
                        && recipientType != FeedbackParticipantType.NONE) {
                    line.append(", the name of the recipient");
                }

                if (showGiverNameTo.contains(participant)) {
                    line.append(", and your name");
                } else {
                    line.append(", but not your name");
                }
            } else {
                if (showGiverNameTo.contains(participant)) {
                    line.append(", and your name");
                }

                if (recipientType == FeedbackParticipantType.NONE) {
                    if (!showGiverNameTo.contains(participant)) {
                        line.append(", but not your name");
                    }
                } else {
                    line.append(", but not the name of the recipient");

                    if (!showGiverNameTo.contains(participant)) {
                        line.append(", or your name");
                    }
                }

            }

            line.append('.');
            message.add(line.toString());
        }

        if (message.isEmpty()) {
            message.add("No-one can see your responses.");
        }

        return message;
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
    public static class Builder {
        private final FeedbackQuestionAttributes feedbackQuestionAttributes;

        public Builder() {
            feedbackQuestionAttributes = new FeedbackQuestionAttributes();
        }

        public Builder withFeedbackSessionName(String feedbackSessionName) {
            if (feedbackSessionName != null) {
                feedbackQuestionAttributes.feedbackSessionName = feedbackSessionName;
            }
            return this;
        }

        public Builder withCourseId(String courseId) {
            if (courseId != null) {
                feedbackQuestionAttributes.courseId = courseId;
            }
            return this;
        }

        public Builder withQuestionMetaData(String questionMetaData) {
            if (questionMetaData != null) {
                feedbackQuestionAttributes.questionMetaData = questionMetaData;
            }
            return this;
        }

        public Builder withQuestionMetaData(FeedbackQuestionDetails questionDetails) {
            if (questionDetails != null) {
                feedbackQuestionAttributes.setQuestionDetails(questionDetails);
            }
            return this;
        }

        public Builder withQuestionDescription(String questionDescription) {
            if (questionDescription != null) {
                feedbackQuestionAttributes.setQuestionDescription(questionDescription);
            }
            return this;
        }

        public Builder withQuestionNumber(int questionNumber) {
            feedbackQuestionAttributes.questionNumber = questionNumber;
            return this;
        }

        public Builder withQuestionType(FeedbackQuestionType questionType) {
            if (questionType != null) {
                feedbackQuestionAttributes.questionType = questionType;
            }
            return this;
        }

        public Builder withGiverType(FeedbackParticipantType giverType) {
            if (giverType != null) {
                feedbackQuestionAttributes.giverType = giverType;
            }
            return this;
        }

        public Builder withRecipientType(FeedbackParticipantType recipientType) {
            if (recipientType != null) {
                feedbackQuestionAttributes.recipientType = recipientType;
            }
            return this;
        }

        public Builder withNumOfEntitiesToGiveFeedbackTo(int numOfEntitiesToGiveFeedbackTo) {
            feedbackQuestionAttributes.numberOfEntitiesToGiveFeedbackTo = numOfEntitiesToGiveFeedbackTo;
            return this;
        }

        public Builder withShowResponseTo(List<FeedbackParticipantType> showResponseTo) {
            feedbackQuestionAttributes.showResponsesTo =
                    showResponseTo == null ? new ArrayList<>()
                            : new ArrayList<>(showResponseTo);
            return this;
        }

        public Builder withShowGiverNameTo(List<FeedbackParticipantType> showGiverNameTo) {
            feedbackQuestionAttributes.showGiverNameTo =
                    showGiverNameTo == null ? new ArrayList<>()
                            : new ArrayList<>(showGiverNameTo);
            return this;
        }

        public Builder withShowRecipientNameTo(List<FeedbackParticipantType> showRecipientNameTo) {
            feedbackQuestionAttributes.showRecipientNameTo =
                    showRecipientNameTo == null ? new ArrayList<>()
                            : new ArrayList<>(showRecipientNameTo);
            return this;
        }

        public Builder withCreatedAt(Instant createdAt) {
            if (createdAt != null) {
                feedbackQuestionAttributes.createdAt = createdAt;
            }
            return this;
        }

        public Builder withUpdatedAt(Instant updatedAt) {
            if (updatedAt != null) {
                feedbackQuestionAttributes.updatedAt = updatedAt;
            }
            return this;
        }

        public Builder withFeedbackQuestionId(String feedbackQuestionId) {
            if (feedbackQuestionId != null) {
                feedbackQuestionAttributes.feedbackQuestionId = feedbackQuestionId;
            }
            return this;
        }

        public FeedbackQuestionAttributes build() {
            feedbackQuestionAttributes.questionDescription =
                    SanitizationHelper.sanitizeForRichText(feedbackQuestionAttributes.questionDescription);
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
            Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, feedbackQuestionId);

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
        public static class Builder {
            private FeedbackQuestionAttributes.UpdateOptions updateOptions;

            private Builder(String feedbackQuestionId) {
                updateOptions = new FeedbackQuestionAttributes.UpdateOptions(feedbackQuestionId);
            }

            public FeedbackQuestionAttributes.UpdateOptions.Builder withQuestionDetails(
                    FeedbackQuestionDetails questionDetails) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, questionDetails);

                updateOptions.questionDetailsOption = UpdateOption.of(questionDetails);
                return this;
            }

            public FeedbackQuestionAttributes.UpdateOptions.Builder withQuestionDescription(String questionDescription) {
                // questionDescription can be null
                updateOptions.questionDescriptionOption = UpdateOption.of(questionDescription);
                return this;
            }

            public FeedbackQuestionAttributes.UpdateOptions.Builder withQuestionNumber(int questionNumber) {
                updateOptions.questionNumberOption = UpdateOption.of(questionNumber);
                return this;
            }

            public FeedbackQuestionAttributes.UpdateOptions.Builder withGiverType(FeedbackParticipantType giverType) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, giverType);

                updateOptions.giverTypeOption = UpdateOption.of(giverType);
                return this;
            }

            public FeedbackQuestionAttributes.UpdateOptions.Builder withRecipientType(
                    FeedbackParticipantType recipientType) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, recipientType);

                updateOptions.recipientTypeOption = UpdateOption.of(recipientType);
                return this;
            }

            public FeedbackQuestionAttributes.UpdateOptions.Builder withNumberOfEntitiesToGiveFeedbackTo(
                    int numberOfEntitiesToGiveFeedbackTo) {
                updateOptions.numberOfEntitiesToGiveFeedbackToOption = UpdateOption.of(numberOfEntitiesToGiveFeedbackTo);
                return this;
            }

            public FeedbackQuestionAttributes.UpdateOptions.Builder withShowResponsesTo(
                    List<FeedbackParticipantType> showResponsesTo) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, showResponsesTo);
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, (Object[]) showResponsesTo.toArray());

                updateOptions.showResponsesToOption = UpdateOption.of(new ArrayList<>(showResponsesTo));
                return this;
            }

            public FeedbackQuestionAttributes.UpdateOptions.Builder withShowGiveNameTo(
                    List<FeedbackParticipantType> showGiveNameTo) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, showGiveNameTo);
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, (Object[]) showGiveNameTo.toArray());

                updateOptions.showGiverNameToOption = UpdateOption.of(new ArrayList<>(showGiveNameTo));
                return this;
            }

            public FeedbackQuestionAttributes.UpdateOptions.Builder withShowRecipientNameTo(
                    List<FeedbackParticipantType> showRecipientNameTo) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, showRecipientNameTo);
                Assumption.assertNotNull(
                        Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, (Object[]) showRecipientNameTo.toArray());

                updateOptions.showRecipientNameToOption = UpdateOption.of(new ArrayList<>(showRecipientNameTo));
                return this;
            }

            public FeedbackQuestionAttributes.UpdateOptions build() {
                return updateOptions;
            }

        }

    }
}
