package teammates.ui.webapi.output;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import teammates.common.datatransfer.FeedbackParticipantType;
import teammates.common.datatransfer.FeedbackSessionResultsBundle;
import teammates.common.datatransfer.attributes.FeedbackQuestionAttributes;
import teammates.common.datatransfer.attributes.FeedbackResponseAttributes;
import teammates.common.datatransfer.attributes.FeedbackResponseCommentAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.datatransfer.questions.FeedbackQuestionDetails;
import teammates.common.datatransfer.questions.FeedbackResponseDetails;
import teammates.common.util.Assumption;
import teammates.common.util.Const;

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
            QuestionOutput qnOutput = new QuestionOutput(question.getId(), question.questionNumber, questionDetails,
                    questionDetails.getQuestionResultStatisticsJson(responses, question, instructor.email, bundle,
                            false), question.showResponsesTo);

            List<ResponseOutput> allResponses = buildResponses(responses, bundle);
            for (ResponseOutput respOutput : allResponses) {
                qnOutput.allResponses.add(respOutput);
            }

            questions.add(qnOutput);
        });
    }

    public SessionResultsData(FeedbackSessionResultsBundle bundle, StudentAttributes student) {
        Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>> questionsWithResponses =
                bundle.getQuestionResponseMapSortedByRecipient();

        questionsWithResponses.forEach((question, responses) -> {
            FeedbackQuestionDetails questionDetails = question.getQuestionDetails();
            QuestionOutput qnOutput = new QuestionOutput(question.getId(), question.questionNumber, questionDetails,
                    questionDetails.getQuestionResultStatisticsJson(responses, question, student.email, bundle,
                            true), question.showResponsesTo);

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

                // TODO fetch feedback response comments

                // Student does not need to know the teams for giver and/or recipient
                output.add(new ResponseOutput(response.getId(), displayedGiverName, null, null, response.giverSection,
                        recipientName, null, response.recipientSection, response.responseDetails));
            }

        });
        return output;
    }

    private List<ResponseOutput> buildResponses(
            List<FeedbackResponseAttributes> responses, FeedbackSessionResultsBundle bundle) {
        Map<String, List<FeedbackResponseAttributes>> responsesMap = new HashMap<>();
        Map<String, List<FeedbackResponseCommentAttributes>> commentsMap = bundle.getResponseComments();

        for (FeedbackResponseAttributes response : responses) {
            responsesMap.computeIfAbsent(response.recipient, k -> new ArrayList<>()).add(response);
        }

        List<ResponseOutput> output = new ArrayList<>();

        responsesMap.forEach((recipient, responsesForRecipient) -> {
            String recipientName = removeAnonymousHash(bundle.getNameForEmail(recipient));
            String recipientTeam = bundle.getTeamNameForEmail(recipient);

            for (FeedbackResponseAttributes response : responsesForRecipient) {
                String giverName = removeAnonymousHash(bundle.getGiverNameForResponse(response));
                Map<String, Set<String>> teamNameToMembersEmailTable = bundle.rosterTeamNameMembersTable;
                String relatedGiverEmail = teamNameToMembersEmailTable.containsKey(response.giver)
                        ? teamNameToMembersEmailTable.get(response.giver).iterator().next() : response.giver;

                String giverTeam = bundle.getTeamNameForEmail(response.giver);

                List<FeedbackResponseCommentAttributes> comments = commentsMap.get(response.getId());
                ResponseOutput responseOutput = new ResponseOutput(response.getId(), giverName, giverTeam, relatedGiverEmail,
                        response.giverSection, recipientName, recipientTeam, response.recipientSection,
                        response.responseDetails);
                List<FeedbackResponseCommentData> commentOutputs = buildComments(comments, bundle);
                for (FeedbackResponseCommentData commentOutput : commentOutputs) {
                    if (commentOutput.isCommentFromFeedbackParticipant()) {
                        responseOutput.commentFromParticipant = commentOutput;
                    } else {
                        responseOutput.commentFromInstructors.add(commentOutput);
                    }
                }
                responseOutput.allComments = commentOutputs;

                output.add(responseOutput);
            }

        });
        return output;
    }

    private List<FeedbackResponseCommentData> buildComments(
            List<FeedbackResponseCommentAttributes> comments, FeedbackSessionResultsBundle bundle) {
        List<FeedbackResponseCommentData> output = new ArrayList<>();

        if (comments == null) {
            return output;
        }

        for (FeedbackResponseCommentAttributes comment : comments) {
            FeedbackResponseCommentData commentOutput = new FeedbackResponseCommentData(comment);
            output.add(commentOutput);
        }
        return output;
    }

    private static class QuestionOutput {

        private final String questionId;
        private final FeedbackQuestionDetails questionDetails;
        private final int questionNumber;
        private final String questionStatistics;

        // For instructor view
        private List<ResponseOutput> allResponses = new ArrayList<>();
        private List<FeedbackVisibilityType> showResponsesTo;

        // For student view
        private List<ResponseOutput> responsesToSelf = new ArrayList<>();
        private List<ResponseOutput> responsesFromSelf = new ArrayList<>();
        private List<List<ResponseOutput>> otherResponses = new ArrayList<>();

        QuestionOutput(String questionId, int questionNumber,
                       FeedbackQuestionDetails questionDetails, String questionStatistics,
                       List<FeedbackParticipantType> showResponsesTo) {
            this.questionId = questionId;
            this.questionNumber = questionNumber;
            this.questionDetails = questionDetails;
            this.questionStatistics = questionStatistics;
            this.showResponsesTo = convertToFeedbackVisibilityType(showResponsesTo);
        }

        public String getQuestionId() {
            return questionId;
        }

        public FeedbackQuestionDetails getQuestionDetails() {
            return questionDetails;
        }

        public int getQuestionNumber() {
            return questionNumber;
        }

        public String getQuestionStatistics() {
            return questionStatistics;
        }

        public List<ResponseOutput> getAllResponses() {
            return allResponses;
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

        /**
         * Converts a list of feedback participant type to a list of visibility type.
         */
        private List<FeedbackVisibilityType> convertToFeedbackVisibilityType(
                List<FeedbackParticipantType> feedbackParticipantTypeList) {
            return feedbackParticipantTypeList.stream().map(feedbackParticipantType -> {
                switch (feedbackParticipantType) {
                case STUDENTS:
                    return FeedbackVisibilityType.STUDENTS;
                case INSTRUCTORS:
                    return FeedbackVisibilityType.INSTRUCTORS;
                case RECEIVER:
                    return FeedbackVisibilityType.RECIPIENT;
                case OWN_TEAM_MEMBERS:
                    return FeedbackVisibilityType.GIVER_TEAM_MEMBERS;
                case RECEIVER_TEAM_MEMBERS:
                    return FeedbackVisibilityType.RECIPIENT_TEAM_MEMBERS;
                default:
                    Assumption.fail("Unknown feedbackParticipantType" + feedbackParticipantType);
                    break;
                }
                return null;
            }).collect(Collectors.toList());
        }
    }

    private static class ResponseOutput {

        private final String responseId;
        private final String giver;
        /**
         * Depending on the question giver type, {@code giverIdentifier} may contain the giver's email, any team member's
         * email or "anonymous".
         */
        private final String relatedGiverEmail;
        private final String giverTeam;
        private final String giverSection;
        private String recipient;
        private final String recipientTeam;
        private final String recipientSection;
        private final FeedbackResponseDetails responseDetails;
        //for instructor view
        private List<FeedbackResponseCommentData> allComments = new ArrayList<>();
        //for students view
        private FeedbackResponseCommentData commentFromParticipant;
        private List<FeedbackResponseCommentData> commentFromInstructors = new ArrayList<>();

        ResponseOutput(String responseId, String giver, String giverTeam, String relatedGiverEmail, String giverSection,
                       String recipient, String recipientTeam, String recipientSection,
                       FeedbackResponseDetails responseDetails) {
            this.responseId = responseId;
            this.giver = giver;
            this.relatedGiverEmail = relatedGiverEmail;
            this.giverTeam = giverTeam;
            this.giverSection = giverSection;
            this.recipient = recipient;
            this.recipientTeam = recipientTeam;
            this.recipientSection = recipientSection;
            this.responseDetails = responseDetails;
        }

        public String getResponseId() {
            return responseId;
        }

        public String getGiver() {
            return giver;
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

        public String getRecipientSection() {
            return recipientSection;
        }

        public FeedbackResponseDetails getResponseDetails() {
            return responseDetails;
        }

        public List<FeedbackResponseCommentData> getAllComments() {
            return allComments;
        }

        public List<FeedbackResponseCommentData> getCommentFromInstructors() {
            return commentFromInstructors;
        }

        public FeedbackResponseCommentData getCommentFromParicipant() {
            return commentFromParticipant;
        }

    }

}
