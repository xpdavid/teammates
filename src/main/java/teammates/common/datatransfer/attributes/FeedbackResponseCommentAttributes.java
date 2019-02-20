package teammates.common.datatransfer.attributes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import teammates.common.datatransfer.FeedbackParticipantType;
import teammates.common.util.Assumption;
import teammates.common.util.Const;
import teammates.common.util.FieldValidator;
import teammates.common.util.SanitizationHelper;
import teammates.storage.entity.FeedbackResponseComment;

/**
 * Represents a data transfer object for {@link FeedbackResponseComment} entities.
 */
public class FeedbackResponseCommentAttributes extends EntityAttributes<FeedbackResponseComment> {

    private static final String FEEDBACK_RESPONSE_COMMENT_BACKUP_LOG_MSG = "Recently modified feedback response comment::";

    public String courseId;
    public String feedbackSessionName;
    /**
     * Contains the email of student/instructor if comment giver is student/instructor
     * and name of team if comment giver is a team.
     */
    public String commentGiver;
    public String commentText;

    public String feedbackResponseId;
    public String feedbackQuestionId;
    public List<FeedbackParticipantType> showCommentTo;
    public List<FeedbackParticipantType> showGiverNameTo;
    public boolean isVisibilityFollowingFeedbackQuestion;
    public Instant createdAt;
    public String lastEditorEmail;
    public Instant lastEditedAt;
    public Long feedbackResponseCommentId;
    public String giverSection;
    public String receiverSection;
    // Determines the type of comment giver- instructor, student, or team
    public FeedbackParticipantType commentGiverType;
    // true if comment is given by response giver
    public boolean isCommentFromFeedbackParticipant;

    FeedbackResponseCommentAttributes() {
        giverSection = Const.DEFAULT_SECTION;
        receiverSection = Const.DEFAULT_SECTION;
        showCommentTo = new ArrayList<>();
        showGiverNameTo = new ArrayList<>();
        isVisibilityFollowingFeedbackQuestion = true;
        createdAt = Instant.now();
        commentGiverType = FeedbackParticipantType.INSTRUCTORS;
        isCommentFromFeedbackParticipant = false;
    }

    public static FeedbackResponseCommentAttributes valueOf(FeedbackResponseComment comment) {
        FeedbackResponseCommentAttributes frca = new FeedbackResponseCommentAttributes();
        frca.courseId = comment.getCourseId();
        frca.feedbackSessionName = comment.getFeedbackSessionName();
        frca.commentGiver = comment.getGiverEmail();
        frca.commentText = comment.getCommentText();
        frca.feedbackResponseId = comment.getFeedbackResponseId();
        frca.feedbackQuestionId = comment.getFeedbackQuestionId();
        if (comment.getShowCommentTo() != null) {
            frca.showCommentTo = comment.getShowCommentTo();
        }
        if (comment.getShowGiverNameTo() != null) {
            frca.showGiverNameTo = comment.getShowGiverNameTo();
        }
        frca.isVisibilityFollowingFeedbackQuestion = comment.getIsVisibilityFollowingFeedbackQuestion();
        if (comment.getCreatedAt() != null) {
            frca.createdAt = comment.getCreatedAt();
        }
        if (comment.getLastEditorEmail() == null) {
            frca.lastEditorEmail = frca.getCommentGiver();
        } else {
            frca.lastEditorEmail = comment.getLastEditorEmail();
        }
        if (comment.getLastEditedAt() == null) {
            frca.lastEditedAt = frca.getCreatedAt();
        } else {
            frca.lastEditedAt = comment.getLastEditedAt();
        }
        frca.feedbackResponseCommentId = comment.getFeedbackResponseCommentId();
        if (comment.getGiverSection() != null) {
            frca.giverSection = comment.getGiverSection();
        }
        if (comment.getReceiverSection() != null) {
            frca.receiverSection = comment.getReceiverSection();
        }
        frca.commentGiverType = comment.getCommentGiverType();
        frca.isCommentFromFeedbackParticipant = comment.getIsCommentFromFeedbackParticipant();

        return frca;
    }

    /**
     * Returns a builder for {@link FeedbackResponseCommentAttributes}.
     */
    public static Builder builder() {
        return new Builder();
    }

    public boolean isVisibleTo(FeedbackParticipantType viewerType) {
        return showCommentTo.contains(viewerType);
    }

    public Long getId() {
        return feedbackResponseCommentId;
    }

    public String getCourseId() {
        return courseId;
    }

    public String getFeedbackSessionName() {
        return feedbackSessionName;
    }

    public String getCommentGiver() {
        return commentGiver;
    }

    public String getCommentText() {
        return commentText;
    }

    public String getFeedbackResponseId() {
        return feedbackResponseId;
    }

    public String getFeedbackQuestionId() {
        return feedbackQuestionId;
    }

    public List<FeedbackParticipantType> getShowCommentTo() {
        return showCommentTo;
    }

    public List<FeedbackParticipantType> getShowGiverNameTo() {
        return showGiverNameTo;
    }

    public boolean isVisibilityFollowingFeedbackQuestion() {
        return isVisibilityFollowingFeedbackQuestion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getLastEditorEmail() {
        return lastEditorEmail;
    }

    public Instant getLastEditedAt() {
        return lastEditedAt;
    }

    public String getGiverSection() {
        return giverSection;
    }

    public String getReceiverSection() {
        return receiverSection;
    }

    public FeedbackParticipantType getCommentGiverType() {
        return commentGiverType;
    }

    public boolean isCommentFromFeedbackParticipant() {
        return isCommentFromFeedbackParticipant;
    }

    /**
     * Converts comment text in form of string for csv i.e if it contains image, changes it into link.
     *
     * @return Comment in form of string
     */
    public String getCommentAsCsvString() {
        String htmlText = commentText;
        StringBuilder comment = new StringBuilder(200);
        comment.append(Jsoup.parse(htmlText).text());
        convertImageToLinkInComment(comment, htmlText);
        return SanitizationHelper.sanitizeForCsv(comment.toString());
    }

    /**
     * Converts comment text in form of string.
     *
     * @return Comment in form of string
     */
    public String getCommentAsHtmlString() {
        String htmlText = commentText;
        StringBuilder comment = new StringBuilder(200);
        comment.append(Jsoup.parse(htmlText).text());
        convertImageToLinkInComment(comment, htmlText);
        return SanitizationHelper.sanitizeForHtml(comment.toString());
    }

    // Converts image in comment text to link.
    private void convertImageToLinkInComment(StringBuilder comment, String htmlText) {
        if (!(Jsoup.parse(htmlText).getElementsByTag("img").isEmpty())) {
            comment.append(" Images Link: ");
            Elements ele = Jsoup.parse(htmlText).getElementsByTag("img");
            for (Element element : ele) {
                comment.append(element.absUrl("src") + ' ');
            }
        }
    }

    /**
     * Use only to match existing and known Comment.
     */
    public void setId(Long id) {
        this.feedbackResponseCommentId = id;
    }

    @Override
    public List<String> getInvalidityInfo() {
        FieldValidator validator = new FieldValidator();
        List<String> errors = new ArrayList<>();

        addNonEmptyError(validator.getInvalidityInfoForCourseId(courseId), errors);

        addNonEmptyError(validator.getInvalidityInfoForFeedbackSessionName(feedbackSessionName), errors);

        addNonEmptyError(validator.getInvalidityInfoForCommentGiverType(commentGiverType), errors);

        addNonEmptyError(validator.getInvalidityInfoForVisibilityOfFeedbackParticipantComments(
                isCommentFromFeedbackParticipant, isVisibilityFollowingFeedbackQuestion), errors);

        //TODO: handle the new attributes showCommentTo and showGiverNameTo

        return errors;
    }

    @Override
    public FeedbackResponseComment toEntity() {
        return new FeedbackResponseComment(courseId, feedbackSessionName, feedbackQuestionId, commentGiver,
                commentGiverType, feedbackResponseId, createdAt, commentText, giverSection, receiverSection,
                showCommentTo, showGiverNameTo, lastEditorEmail, lastEditedAt, isCommentFromFeedbackParticipant,
                isVisibilityFollowingFeedbackQuestion);
    }

    @Override
    public String getBackupIdentifier() {
        return FEEDBACK_RESPONSE_COMMENT_BACKUP_LOG_MSG + getId();
    }

    @Override
    public void sanitizeForSaving() {
        this.commentText = SanitizationHelper.sanitizeForRichText(this.commentText);
    }

    @Override
    public String toString() {
        return "FeedbackResponseCommentAttributes ["
                + "feedbackResponseCommentId = " + feedbackResponseCommentId
                + ", courseId = " + courseId
                + ", feedbackSessionName = " + feedbackSessionName
                + ", feedbackQuestionId = " + feedbackQuestionId
                + ", commentGiver = " + commentGiver
                + ", feedbackResponseId = " + feedbackResponseId
                + ", commentText = " + commentText
                + ", createdAt = " + createdAt
                + ", lastEditorEmail = " + lastEditorEmail
                + ", lastEditedAt = " + lastEditedAt
                + ", giverSection = " + giverSection
                + ", receiverSection = " + receiverSection
                + ", showCommentTo = " + showCommentTo
                + ", showGiverNameTo = " + showGiverNameTo
                + ", commentGiverType = " + commentGiverType
                + ", isVisibilityFollowingFeedbackQuestion = " + isVisibilityFollowingFeedbackQuestion
                + ", isCommentFromFeedbackParticipant = " + isCommentFromFeedbackParticipant
                + "]";
    }

    public static void sortFeedbackResponseCommentsByCreationTime(List<FeedbackResponseCommentAttributes> frcs) {
        frcs.sort(Comparator.comparing(frc -> frc.createdAt));
    }

    /**
     * Updates with {@link UpdateOptions}.
     */
    public void update(UpdateOptions updateOptions) {
        updateOptions.feedbackResponseIdOption.ifPresent(s -> feedbackResponseId = s);
        updateOptions.commentTextOption.ifPresent(s -> commentText = s);
        updateOptions.showCommentToOption.ifPresent(s -> showCommentTo = s);
        updateOptions.showGiverNameToOption.ifPresent(s -> showGiverNameTo = s);
        updateOptions.lastEditorEmailOption.ifPresent(s -> lastEditorEmail = s);
        updateOptions.lastEditedAtOption.ifPresent(s -> lastEditedAt = s);
        updateOptions.giverSectionOption.ifPresent(s -> giverSection = s);
        updateOptions.receiverSectionOption.ifPresent(s -> receiverSection = s);
    }

    /**
     * Returns a {@link UpdateOptions.Builder} to build {@link UpdateOptions} for a comment.
     */
    public static UpdateOptions.Builder updateOptionsBuilder(long feedbackResponseCommentId) {
        return new UpdateOptions.Builder(feedbackResponseCommentId);
    }

    /**
     * A builder for {@link FeedbackResponseCommentAttributes}.
     */
    public static class Builder extends BasicBuilder<FeedbackResponseCommentAttributes, Builder> {
        private final FeedbackResponseCommentAttributes frca;

        private Builder() {
            super(new UpdateOptions(0L));
            thisBuilder = this;

            frca = new FeedbackResponseCommentAttributes();
        }

        public Builder withCourseId(String courseId) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, courseId);
            frca.courseId = courseId;

            return this;
        }

        public Builder withFeedbackSessionName(String feedbackSessionName) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, feedbackSessionName);
            frca.feedbackSessionName = feedbackSessionName;

            return this;
        }

        public Builder withCommentGiver(String commentGiver) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, commentGiver);
            frca.commentGiver = commentGiver;

            return this;
        }

        public Builder withFeedbackQuestionId(String feedbackQuestionId) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, feedbackQuestionId);
            frca.feedbackQuestionId = feedbackQuestionId;

            return this;
        }

        public Builder withVisibilityFollowingFeedbackQuestion(boolean visibilityFollowingFeedbackQuestion) {
            frca.isVisibilityFollowingFeedbackQuestion = visibilityFollowingFeedbackQuestion;
            return this;
        }

        public Builder withCommentGiverType(FeedbackParticipantType commentGiverType) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, commentGiverType);

            frca.commentGiverType = commentGiverType;
            return this;
        }

        public Builder withCommentFromFeedbackParticipant(boolean isCommentFromFeedbackParticipant) {
            frca.isCommentFromFeedbackParticipant = isCommentFromFeedbackParticipant;
            return this;
        }

        @Override
        public FeedbackResponseCommentAttributes build() {
            frca.update(updateOptions);

            return frca;
        }
    }

    /**
     * Helper class to specific the fields to update in {@link FeedbackResponseCommentAttributes}.
     */
    public static class UpdateOptions {
        private long feedbackResponseCommentId;

        private UpdateOption<String> feedbackResponseIdOption = UpdateOption.empty();
        private UpdateOption<String> commentTextOption = UpdateOption.empty();
        private UpdateOption<List<FeedbackParticipantType>> showCommentToOption = UpdateOption.empty();
        private UpdateOption<List<FeedbackParticipantType>> showGiverNameToOption = UpdateOption.empty();
        private UpdateOption<String> lastEditorEmailOption = UpdateOption.empty();
        private UpdateOption<Instant> lastEditedAtOption = UpdateOption.empty();
        private UpdateOption<String> giverSectionOption = UpdateOption.empty();
        private UpdateOption<String> receiverSectionOption = UpdateOption.empty();

        private UpdateOptions(long feedbackResponseCommentId) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, feedbackResponseCommentId);

            this.feedbackResponseCommentId = feedbackResponseCommentId;
        }

        public long getFeedbackResponseCommentId() {
            return feedbackResponseCommentId;
        }

        @Override
        public String toString() {
            return "FeedbackResponseCommentAttributes.UpdateOptions ["
                    + "feedbackResponseCommentId = " + feedbackResponseCommentId
                    + ", commentText = " + commentTextOption
                    + ", showCommentTo = " + showCommentToOption
                    + ", showGiverNameTo = " + showGiverNameToOption
                    + ", lastEditorEmail = " + lastEditorEmailOption
                    + ", giverSection = " + giverSectionOption
                    + ", receiverSection = " + receiverSectionOption
                    + "]";
        }

        /**
         * Builder class to build {@link UpdateOptions}.
         */
        public static class Builder extends BasicBuilder<UpdateOptions, Builder> {

            private Builder(Long feedbackResponseCommentId) {
                super(new UpdateOptions(feedbackResponseCommentId));
                thisBuilder = this;
            }

            public Builder withLastEditorEmail(String lastEditorEmail) {
                Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, lastEditorEmail);

                updateOptions.lastEditorEmailOption = UpdateOption.of(lastEditorEmail);
                return this;
            }

            public Builder withLastEditorAt(Instant lastEditedAt) {
                Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, lastEditedAt);

                updateOptions.lastEditedAtOption = UpdateOption.of(lastEditedAt);
                return this;
            }

            @Override
            public UpdateOptions build() {
                return updateOptions;
            }

        }

    }

    /**
     * Basic builder to build {@link FeedbackResponseCommentAttributes} related classes.
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

        public B withFeedbackResponseId(String feedbackResponseId) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, feedbackResponseId);

            updateOptions.feedbackResponseIdOption = UpdateOption.of(feedbackResponseId);
            return thisBuilder;
        }

        public B withCommentText(String commentText) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, commentText);

            updateOptions.commentTextOption = UpdateOption.of(commentText);
            return thisBuilder;
        }

        public B withShowCommentTo(List<FeedbackParticipantType> showCommentTo) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, showCommentTo);

            updateOptions.showCommentToOption = UpdateOption.of(showCommentTo);
            return thisBuilder;
        }

        public B withShowGiverNameTo(List<FeedbackParticipantType> showGiverNameTo) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, showGiverNameTo);

            updateOptions.showGiverNameToOption = UpdateOption.of(showGiverNameTo);
            return thisBuilder;
        }

        public B withGiverSection(String giverSection) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, giverSection);

            updateOptions.giverSectionOption = UpdateOption.of(giverSection);
            return thisBuilder;
        }

        public B withReceiverSection(String receiverSection) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, receiverSection);

            updateOptions.receiverSectionOption = UpdateOption.of(receiverSection);
            return thisBuilder;
        }

        public abstract T build();

    }
}
