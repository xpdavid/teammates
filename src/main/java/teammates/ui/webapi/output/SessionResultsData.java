package teammates.ui.webapi.output;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;
import javax.xml.ws.Response;

import teammates.common.datatransfer.CourseRoster;
import teammates.common.datatransfer.FeedbackParticipantType;
import teammates.common.datatransfer.FeedbackSessionResultsBundle;
import teammates.common.datatransfer.attributes.FeedbackQuestionAttributes;
import teammates.common.datatransfer.attributes.FeedbackResponseAttributes;
import teammates.common.datatransfer.attributes.FeedbackResponseCommentAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.datatransfer.questions.FeedbackQuestionDetails;
import teammates.common.datatransfer.questions.FeedbackResponseDetails;
import teammates.common.util.Const;
import teammates.storage.entity.FeedbackResponse;

/**
 * API output format for session results, including statistics.
 */
public class SessionResultsData extends ApiOutput {

    private static final String REGEX_ANONYMOUS_PARTICIPANT_HASH = "[0-9]{1,10}";

    private final List<QuestionOutput> questions = new ArrayList<>();

    public SessionResultsData(FeedbackSessionResultsBundle bundle, InstructorAttributes instructor) {
        Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>> questionsWithResponses =
                bundle.getQuestionResponseMapSortedByRecipient();

        questionsWithResponses.forEach((question, responses) -> {
            FeedbackQuestionDetails questionDetails = question.getQuestionDetails();
            QuestionOutput qnOutput = new QuestionOutput(question,
                    questionDetails.getQuestionResultStatisticsJson(responses, question, instructor.email, bundle, false));

            List<ResponseOutput> allResponses = buildResponses(responses, bundle);
            for (ResponseOutput respOutput : allResponses) {
                qnOutput.allResponses.add(respOutput);
            }

            // missing responses
            qnOutput.missingResponses = buildMissingResponse(bundle.getMissingResponses(), bundle);

            questions.add(qnOutput);
        });
    }

    public SessionResultsData(FeedbackSessionResultsBundle bundle, StudentAttributes student) {
        Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>> questionsWithResponses =
                bundle.getQuestionResponseMapSortedByRecipient();

        questionsWithResponses.forEach((question, responses) -> {
            FeedbackQuestionDetails questionDetails = question.getQuestionDetails();
            QuestionOutput qnOutput = new QuestionOutput(question,
                    questionDetails.getQuestionResultStatisticsJson(responses, question, student.email, bundle, true));

            Map<String, List<ResponseOutput>> otherResponsesMap = new HashMap<>();
            if (questionDetails.isIndividualResponsesShownToStudents()) {
                List<ResponseOutput> allResponses = buildResponses(question, responses, bundle, student);
                for (ResponseOutput respOutput : allResponses) {
                    if ("You".equals(respOutput.giver)) {
                        qnOutput.responsesFromSelf.add(respOutput);
                    } else if ("You".equals(respOutput.recipient)) {
                        qnOutput.responsesToSelf.add(respOutput);
                    } else {
                        String recipientNameWithHash = respOutput.recipient;
                        respOutput.recipient = removeAnonymousHash(respOutput.recipient);
                        otherResponsesMap.computeIfAbsent(recipientNameWithHash, k -> new ArrayList<>()).add(respOutput);
                    }
                }
            }
            qnOutput.otherResponses = new ArrayList<>(otherResponsesMap.values());

            questions.add(qnOutput);
        });
    }

    public List<QuestionOutput> getQuestions() {
        return questions;
    }

    private static String removeAnonymousHash(String identifier) {
        return identifier.replaceAll(Const.DISPLAYED_NAME_FOR_ANONYMOUS_PARTICIPANT + " (student|instructor|team) "
                + REGEX_ANONYMOUS_PARTICIPANT_HASH, Const.DISPLAYED_NAME_FOR_ANONYMOUS_PARTICIPANT + " $1");
    }

    private List<ResponseOutput> buildResponses(
            FeedbackQuestionAttributes question, List<FeedbackResponseAttributes> responses,
            FeedbackSessionResultsBundle bundle, StudentAttributes student) {
        Map<String, List<FeedbackResponseAttributes>> responsesMap = new HashMap<>();

        for (FeedbackResponseAttributes response : responses) {
            responsesMap.computeIfAbsent(response.recipient, k -> new ArrayList<>()).add(response);
        }

        List<ResponseOutput> output = new ArrayList<>();

        responsesMap.forEach((recipient, responsesForRecipient) -> {
            boolean isUserRecipient = student.email.equals(recipient);
            boolean isUserTeamRecipient = question.recipientType == FeedbackParticipantType.TEAMS
                    && student.team.equals(recipient);
            String recipientName;
            if (isUserRecipient) {
                recipientName = "You";
            } else if (isUserTeamRecipient) {
                recipientName = String.format("Your Team (%s)", bundle.getNameForEmail(recipient));
            } else {
                recipientName = bundle.getNameForEmail(recipient);
            }

            for (FeedbackResponseAttributes response : responsesForRecipient) {
                String giverName = bundle.getGiverNameForResponse(response);
                String displayedGiverName;

                boolean isUserGiver = student.email.equals(response.giver);
                boolean isUserPartOfGiverTeam = student.team.equals(giverName);
                if (question.giverType == FeedbackParticipantType.TEAMS && isUserPartOfGiverTeam) {
                    displayedGiverName = "Your Team (" + giverName + ")";
                } else if (isUserGiver) {
                    displayedGiverName = "You";
                } else {
                    displayedGiverName = removeAnonymousHash(giverName);
                }

                if (isUserGiver && !isUserRecipient) {
                    // If the giver is the user, show the real name of the recipient
                    // since the giver would know which recipient he/she gave the response to
                    recipientName = bundle.getNameForEmail(response.recipient);
                }

                // get associated comments
                List<FeedbackResponseCommentAttributes> feedbackResponseComments =
                        bundle.getResponseComments().getOrDefault(response.getId(), Collections.emptyList());
                Queue<CommentOutput> comments = buildComments(feedbackResponseComments, bundle);

                // Student does not need to know the teams for giver and/or recipient
                output.add(new ResponseOutput(response.getId(), displayedGiverName, null, null,
                        null, response.giverSection,
                        recipientName, null, null, response.recipientSection,
                        response.responseDetails, comments.poll(), new ArrayList<>(comments)));
            }

        });
        return output;
    }

    private List<ResponseOutput> buildResponses(
            List<FeedbackResponseAttributes> responses, FeedbackSessionResultsBundle bundle) {
        Map<String, List<FeedbackResponseAttributes>> responsesMap = new HashMap<>();

        for (FeedbackResponseAttributes response : responses) {
            responsesMap.computeIfAbsent(response.recipient, k -> new ArrayList<>()).add(response);
        }

        List<ResponseOutput> output = new ArrayList<>();

        responsesMap.forEach((recipient, responsesForRecipient) -> {
            String recipientName = bundle.getNameForEmail(recipient);
            String recipientTeam = bundle.getTeamNameForEmail(recipient);

            // TODO use the same process as buildMissingResponse
            for (FeedbackResponseAttributes response : responsesForRecipient) {
                String giverName = bundle.getGiverNameForResponse(response);
                String giverEmail = bundle.isGiverVisible(response)
                        ? (bundle.rosterTeamNameMembersTable.containsKey(response.giver) ? null : response.giver)
                        : null;
                String recipientEmail = bundle.isRecipientVisible(response)
                        ? (bundle.rosterTeamNameMembersTable.containsKey(response.recipient) ? null : response.recipient)
                        : null;
                Map<String, Set<String>> teamNameToMembersEmailTable = bundle.rosterTeamNameMembersTable;
                String relatedGiverEmail = teamNameToMembersEmailTable.containsKey(response.giver)
                        ? teamNameToMembersEmailTable.get(response.giver).iterator().next() : response.giver;

                String giverTeam = bundle.getTeamNameForEmail(response.giver);

                // get associated comments
                List<FeedbackResponseCommentAttributes> feedbackResponseComments =
                        bundle.getResponseComments().getOrDefault(response.getId(), Collections.emptyList());
                Queue<CommentOutput> comments = buildComments(feedbackResponseComments, bundle);

                output.add(new ResponseOutput(response.getId(), giverName, giverTeam, giverEmail, relatedGiverEmail,
                        response.giverSection, recipientName, recipientTeam, recipientEmail, response.recipientSection,
                        response.responseDetails, comments.poll(), new ArrayList<>(comments)));
            }

        });
        return output;
    }

    private Queue<CommentOutput> buildComments(List<FeedbackResponseCommentAttributes> feedbackResponseComments,
                                               FeedbackSessionResultsBundle bundle) {
        LinkedList<CommentOutput> outputs = new LinkedList<>();

        CommentOutput participantComment = null;
        for (FeedbackResponseCommentAttributes comment : feedbackResponseComments) {
            if (comment.isCommentFromFeedbackParticipant()) {
                participantComment = new CommentOutput(comment,
                        bundle.commentGiverEmailToNameTable.get(comment.commentGiver),
                        bundle.commentGiverEmailToNameTable.get(comment.lastEditorEmail));
            } else {
                outputs.add(new CommentOutput(comment,
                        bundle.commentGiverEmailToNameTable.get(comment.commentGiver),
                        bundle.commentGiverEmailToNameTable.get(comment.lastEditorEmail)));
            }
        }
        outputs.addFirst(participantComment);

        return outputs;
    }

    /**
     * Builds missing response output.
     */
    private List<ResponseOutput> buildMissingResponse(
            List<FeedbackResponseAttributes> responses, FeedbackSessionResultsBundle bundle) {
        List<ResponseOutput> output = new ArrayList<>();

        for (FeedbackResponseAttributes response : responses) {
            String giverEmail = response.getGiver();
            String relatedGiverEmail = response.getGiver();
            if (bundle.getRoster().isTeamInCourse(giverEmail)) {
                // team name is not an email
                giverEmail = null;
                relatedGiverEmail =
                        bundle.getRoster().getTeamToMembersTable().get(giverEmail).iterator().next().getEmail();
            }
            if (!bundle.isGiverVisible(response)) {
                giverEmail = null;
                relatedGiverEmail = null;
            }
            String giverName = this.getGiverNameOfResponse(response, bundle);
            String giverTeam = bundle.getRoster().getInfoForIdentifier(response.getGiver())[CourseRoster.INFO_TEAM_NAME];

            String recipientEmail = response.getRecipient();
            String recipientName = this.getRecipientNameOfResponse(response, bundle);
            String recipientTeam =
                    bundle.getRoster().getInfoForIdentifier(response.getRecipient())[CourseRoster.INFO_TEAM_NAME];
            if (bundle.getRoster().isTeamInCourse(recipientEmail)) {
                // team name is not an email
                recipientEmail = null;
            }
            if (!bundle.isGiverVisible(response)) {
                recipientEmail = null;
            }

            output.add(new ResponseOutput(response.getId(), giverName, giverTeam, giverEmail, relatedGiverEmail,
                    response.getGiverSection(), recipientName, recipientTeam, recipientEmail, response.getRecipientSection(),
                    response.getResponseDetails(), null, Collections.emptyList()));
        }

        return output;
    }

    /**
     * Gets giver name of a response from the bundle.
     *
     * <p>Anonymized the name if necessary.
     */
    private String getGiverNameOfResponse(FeedbackResponseAttributes response, FeedbackSessionResultsBundle bundle) {
        FeedbackQuestionAttributes question = bundle.getQuestions().get(response.getFeedbackQuestionId());
        FeedbackParticipantType participantType = question.giverType;

        String name = bundle.getRoster().getInfoForIdentifier(response.getGiver())[CourseRoster.INFO_NAME];
        if (!bundle.isGiverVisible(response)) {
            name = FeedbackSessionResultsBundle.getAnonName(participantType, name);
        }

        return name;
    }

    /**
     * Gets recipient name of a response from the bundle.
     *
     * <p>Anonymized the name if necessary.
     */
    private String getRecipientNameOfResponse(FeedbackResponseAttributes response, FeedbackSessionResultsBundle bundle) {
        FeedbackQuestionAttributes question = bundle.getQuestions().get(response.getFeedbackQuestionId());
        FeedbackParticipantType participantType = question.getRecipientType();
        if (participantType == FeedbackParticipantType.SELF) {
            // recipient type for self-feedback is the same as the giver type
            participantType = question.getGiverType();
        }

        String name = bundle.getRoster().getInfoForIdentifier(response.getRecipient())[CourseRoster.INFO_NAME];
        if (!bundle.isRecipientVisible(response)) {
            name = FeedbackSessionResultsBundle.getAnonName(participantType, name);
        }

        return name;
    }

    /**
     * API output format for questions in session results.
     */
    public static class QuestionOutput {

        private final FeedbackQuestionData feedbackQuestion;
        private final String questionStatistics;

        // For instructor view
        private List<ResponseOutput> allResponses = new ArrayList<>();
        private List<ResponseOutput> missingResponses = new ArrayList<>();

        // For student view
        private List<ResponseOutput> responsesToSelf = new ArrayList<>();
        private List<ResponseOutput> responsesFromSelf = new ArrayList<>();
        private List<List<ResponseOutput>> otherResponses = new ArrayList<>();

        QuestionOutput(FeedbackQuestionAttributes feedbackQuestionAttributes, String questionStatistics) {
            this.feedbackQuestion = new FeedbackQuestionData(feedbackQuestionAttributes);
            this.questionStatistics = questionStatistics;
        }

        public FeedbackQuestionData getFeedbackQuestion() {
            return feedbackQuestion;
        }

        public String getQuestionStatistics() {
            return questionStatistics;
        }

        public List<ResponseOutput> getAllResponses() {
            return allResponses;
        }

        public List<ResponseOutput> getMissingResponses() {
            return missingResponses;
        }

        public List<ResponseOutput> getResponsesFromSelf() {
            return responsesFromSelf;
        }

        public List<ResponseOutput> getResponsesToSelf() {
            return responsesToSelf;
        }

        public List<List<ResponseOutput>> getOtherResponses() {
            return otherResponses;
        }

    }

    /**
     * API output format for question responses.
     */
    public static class ResponseOutput {

        // TODO: security risk: responseId can expose giver and recipient email
        private final String responseId;

        private final String giver;
        /**
         * Depending on the question giver type, {@code giverIdentifier} may contain the giver's email, any team member's
         * email or "anonymous".
         */
        private final String relatedGiverEmail; // TODO: security risk: relatedGiverEmail can expose giver email
        private final String giverTeam;
        @Nullable
        private final String giverEmail;
        private final String giverSection;
        private String recipient;
        private final String recipientTeam;
        @Nullable
        private final String recipientEmail;
        private final String recipientSection;
        private final FeedbackResponseDetails responseDetails;

        // comments
        @Nullable
        private CommentOutput participantComment;
        private final List<CommentOutput> instructorComments;

        ResponseOutput(String responseId, String giver, String giverTeam, String giverEmail, String relatedGiverEmail,
                       String giverSection, String recipient, String recipientTeam, String recipientEmail,
                       String recipientSection, FeedbackResponseDetails responseDetails,
                       CommentOutput participantComment, List<CommentOutput> instructorComments) {
            this.responseId = responseId;
            this.giver = giver;
            this.relatedGiverEmail = relatedGiverEmail;
            this.giverEmail = giverEmail;
            this.giverTeam = giverTeam;
            this.giverSection = giverSection;
            this.recipient = recipient;
            this.recipientTeam = recipientTeam;
            this.recipientEmail = recipientEmail;
            this.recipientSection = recipientSection;
            this.responseDetails = responseDetails;
            this.participantComment = participantComment;
            this.instructorComments = instructorComments;
        }

        public String getResponseId() {
            return responseId;
        }

        public String getGiver() {
            return giver;
        }

        public String getGiverEmail() {
            return giverEmail;
        }

        public String getRelatedGiverEmail() {
            return relatedGiverEmail;
        }

        public String getGiverTeam() {
            return giverTeam;
        }

        public String getGiverSection() {
            return giverSection;
        }

        public String getRecipient() {
            return recipient;
        }

        public String getRecipientTeam() {
            return recipientTeam;
        }

        public String getRecipientEmail() {
            return recipientEmail;
        }

        public String getRecipientSection() {
            return recipientSection;
        }

        public FeedbackResponseDetails getResponseDetails() {
            return responseDetails;
        }

        @Nullable
        public CommentOutput getParticipantComment() {
            return participantComment;
        }

        public List<CommentOutput> getInstructorComments() {
            return instructorComments;
        }
    }


    /**
     * API output format for response comments.
     */
    public static class CommentOutput extends FeedbackResponseCommentData {

        @Nullable
        private final String commentGiverName;
        @Nullable
        private final String lastEditorName;

        public CommentOutput(FeedbackResponseCommentAttributes frc, String commentGiverName, String lastEditorName) {
            super(frc);
            this.commentGiverName = commentGiverName;
            this.lastEditorName = lastEditorName;
        }
    }

}
