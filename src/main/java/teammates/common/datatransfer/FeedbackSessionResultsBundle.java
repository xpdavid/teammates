package teammates.common.datatransfer;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import teammates.common.datatransfer.attributes.FeedbackQuestionAttributes;
import teammates.common.datatransfer.attributes.FeedbackResponseAttributes;
import teammates.common.datatransfer.attributes.FeedbackResponseCommentAttributes;
import teammates.common.datatransfer.attributes.FeedbackSessionAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.datatransfer.questions.FeedbackTextQuestionDetails;
import teammates.common.datatransfer.questions.FeedbackTextResponseDetails;
import teammates.common.util.Assumption;
import teammates.common.util.Const;
import teammates.common.util.Logger;
import teammates.common.util.SanitizationHelper;
import teammates.common.util.StringHelper;

/**
 * Represents detailed results for an feedback session.
 * <br> Contains:
 * <br> * The basic {@link FeedbackSessionAttributes}
 * <br> * {@link List} of viewable responses as {@link FeedbackResponseAttributes} objects.
 */
public class FeedbackSessionResultsBundle {

    private static final Logger log = Logger.getLogger();

    public FeedbackSessionAttributes feedbackSession;
    public List<FeedbackResponseAttributes> responses;
    public Map<String, FeedbackQuestionAttributes> questions;
    public Map<String, String> emailNameTable;
    public Map<String, String> emailLastNameTable;
    public Map<String, String> emailTeamNameTable;
    public Map<String, String> commentGiverEmailToNameTable;
    public Map<String, Set<String>> rosterTeamNameMembersTable;
    public Map<String, Set<String>> rosterSectionTeamNameTable;
    public Map<String, boolean[]> visibilityTable;
    public List<FeedbackResponseAttributes> missingResponses;
    public CourseRoster roster;
    public Map<String, List<FeedbackResponseCommentAttributes>> responseComments;
    public boolean isComplete;

    /**
     * Responses with identities of giver/recipients NOT hidden.
     * To be used for anonymous result calculation only, and identities hidden before showing to users.
     */
    public List<FeedbackResponseAttributes> actualResponses;

    // For contribution questions.
    // Key is questionId, value is a map of student email to StudentResultSummary
    public Map<String, Map<String, StudentResultSummary>> contributionQuestionStudentResultSummary = new HashMap<>();
    // Key is questionId, value is a map of team name to TeamEvalResult
    public Map<String, Map<String, TeamEvalResult>> contributionQuestionTeamEvalResults = new HashMap<>();

    /*
     * sectionTeamNameTable takes into account the section viewing privileges of the logged-in instructor
     * and the selected section for viewing
     * whereas rosterSectionTeamNameTable doesn't.
     * As a result, sectionTeamNameTable only contains sections viewable to the logged-in instructor
     * whereas rosterSectionTeamNameTable contains all sections in the course.
     * As sectionTeamNameTable is dependent on instructor privileges,
     * it can only be used for instructor pages and not for student pages
     */
    public Map<String, Set<String>> sectionTeamNameTable;

    private Comparator<FeedbackResponseAttributes> compareByGiverSection =
            Comparator.comparing(fra -> fra.giverSection);

    private Comparator<FeedbackResponseAttributes> compareByRecipientSection =
            Comparator.comparing(fra -> fra.recipientSection);

    private Comparator<FeedbackResponseAttributes> compareByGiverName =
            (fra1, fra2) ->
                    compareByNames(emailNameTable.get(fra1.giver), emailNameTable.get(fra2.giver),
                            isGiverVisible(fra1), isGiverVisible(fra2));

    private Comparator<FeedbackResponseAttributes> compareByRecipientName =
            (fra1, fra2) ->
                    compareByNames(emailNameTable.get(fra1.recipient), emailNameTable.get(fra2.recipient),
                            isRecipientVisible(fra1), isRecipientVisible(fra2));

    private Comparator<FeedbackResponseAttributes> compareByGiverEmail =
            (fra1, fra2) -> compareByNames(fra1.giver, fra2.giver, isGiverVisible(fra1), isGiverVisible(fra2));

    private Comparator<FeedbackResponseAttributes> compareByRecipientEmail =
            (fra1, fra2) ->
                    compareByNames(fra1.recipient, fra2.recipient, isRecipientVisible(fra1), isRecipientVisible(fra2));

    private Comparator<FeedbackResponseAttributes> compareByGiverDisplayName =
            (fra1, fra2) ->
                    compareByNames(getNameForEmail(fra1.giver), getNameForEmail(fra2.giver),
                            isGiverVisible(fra1), isGiverVisible(fra2));

    private Comparator<FeedbackResponseAttributes> compareByRecipientDisplayName =
            (fra1, fra2) ->
                    compareByNames(getNameForEmail(fra1.recipient), getNameForEmail(fra2.recipient),
                            isRecipientVisible(fra1), isRecipientVisible(fra2));

    private Comparator<FeedbackResponseAttributes> compareByQuestionNumber = (r1, r2) -> {
        FeedbackQuestionAttributes q1 = questions.get(r1.feedbackQuestionId);
        FeedbackQuestionAttributes q2 = questions.get(r2.feedbackQuestionId);
        if (q1 == null || q2 == null) {
            return 0;
        }
        return q1.compareTo(q2);
    };

    private Comparator<FeedbackResponseAttributes> compareByGiverTeam = (o1, o2) -> {

        boolean isGiverVisible1 = isGiverVisible(o1);
        boolean isGiverVisible2 = isGiverVisible(o2);

        String t1 = getTeamNameForEmail(o1.giver).isEmpty() ? getNameForEmail(o1.giver)
                : getTeamNameForEmail(o1.giver);
        String t2 = getTeamNameForEmail(o2.giver).isEmpty() ? getNameForEmail(o2.giver)
                : getTeamNameForEmail(o2.giver);
        return compareByNames(t1, t2, isGiverVisible1, isGiverVisible2);
    };

    private Comparator<FeedbackResponseAttributes> compareByRecipientTeam = (o1, o2) -> {

        boolean isRecipientVisible1 = isRecipientVisible(o1);
        boolean isRecipientVisible2 = isRecipientVisible(o2);

        String t1 = getTeamNameForEmail(o1.recipient).isEmpty() ? getNameForEmail(o1.recipient)
                : getTeamNameForEmail(o1.recipient);
        String t2 = getTeamNameForEmail(o2.recipient).isEmpty() ? getNameForEmail(o2.recipient)
                : getTeamNameForEmail(o2.recipient);
        return compareByNames(t1, t2, isRecipientVisible1, isRecipientVisible2);

    };

    private Comparator<FeedbackResponseAttributes> compareByResponseString =
            Comparator.comparing(fra -> fra.getResponseDetails().getAnswerString());

    private Comparator<FeedbackResponseAttributes> compareByFeedbackResponseAttributeId =
            Comparator.comparing(fra -> fra.getId());

    // Sorts by giverName > recipientName > qnNumber
    // General questions and team questions at the bottom.
    public Comparator<FeedbackResponseAttributes> compareByGiverRecipientQuestion = compareByGiverSection
            .thenComparing(compareByGiverName)
            .thenComparing(compareByRecipientName)
            .thenComparing(compareByQuestionNumber)
            .thenComparing(compareByResponseString)
            .thenComparing(compareByFeedbackResponseAttributeId);

    // Sorts by giverName > recipientName
    private Comparator<FeedbackResponseAttributes> compareByGiverRecipient = compareByGiverName
            .thenComparing(compareByRecipientName)
            .thenComparing(compareByResponseString)
            .thenComparing(compareByFeedbackResponseAttributeId);

    // Sorts by teamName > giverName > recipientName > qnNumber
    private Comparator<FeedbackResponseAttributes> compareByTeamGiverRecipientQuestion = compareByGiverSection
            .thenComparing(compareByGiverTeam)
            .thenComparing(compareByGiverName)
            .thenComparing(compareByRecipientName)
            .thenComparing(compareByQuestionNumber)
            .thenComparing(compareByResponseString)
            .thenComparing(compareByFeedbackResponseAttributeId);

    // Sorts by recipientName > giverName > qnNumber
    private Comparator<FeedbackResponseAttributes> compareByRecipientGiverQuestion = compareByRecipientSection
            .thenComparing(compareByRecipientName)
            .thenComparing(compareByGiverName)
            .thenComparing(compareByQuestionNumber)
            .thenComparing(compareByResponseString)
            .thenComparing(compareByFeedbackResponseAttributeId);

    // Sorts by teamName > recipientName > giverName > qnNumber
    private Comparator<FeedbackResponseAttributes> compareByTeamRecipientGiverQuestion = compareByRecipientSection
            .thenComparing(compareByRecipientTeam)
            .thenComparing(compareByRecipientName)
            .thenComparing(compareByGiverName)
            .thenComparing(compareByQuestionNumber)
            .thenComparing(compareByResponseString)
            .thenComparing(compareByFeedbackResponseAttributeId);

    // Sorts by giverName > question > recipientTeam > recipientName
    private Comparator<FeedbackResponseAttributes> compareByGiverQuestionTeamRecipient = compareByGiverSection
            .thenComparing(compareByGiverName)
            .thenComparing(compareByQuestionNumber)
            .thenComparing(compareByRecipientTeam)
            .thenComparing(compareByRecipientName)
            .thenComparing(compareByResponseString)
            .thenComparing(compareByFeedbackResponseAttributeId);

    // Sorts by giverTeam > giverName > question > recipientTeam > recipientName
    private Comparator<FeedbackResponseAttributes> compareByTeamGiverQuestionTeamRecipient = compareByGiverSection
            .thenComparing(compareByGiverTeam)
            .thenComparing(compareByGiverName)
            .thenComparing(compareByQuestionNumber)
            .thenComparing(compareByRecipientTeam)
            .thenComparing(compareByRecipientName)
            .thenComparing(compareByResponseString)
            .thenComparing(compareByFeedbackResponseAttributeId);

    // Sorts by recipientName > question > giverTeam > giverName
    private Comparator<FeedbackResponseAttributes> compareByRecipientQuestionTeamGiver = compareByRecipientSection
            .thenComparing(compareByRecipientName)
            .thenComparing(compareByQuestionNumber)
            .thenComparing(compareByGiverTeam)
            .thenComparing(compareByGiverName)
            .thenComparing(compareByResponseString)
            .thenComparing(compareByFeedbackResponseAttributeId);

    // Sorts by recipientTeam > recipientName > question > giverTeam > giverName
    private Comparator<FeedbackResponseAttributes> compareByTeamRecipientQuestionTeamGiver = compareByRecipientSection
            .thenComparing(compareByRecipientTeam)
            .thenComparing(compareByRecipientName)
            .thenComparing(compareByQuestionNumber)
            .thenComparing(compareByGiverTeam)
            .thenComparing(compareByGiverName)
            .thenComparing(compareByResponseString)
            .thenComparing(compareByFeedbackResponseAttributeId);

    // Sorts by recipientTeam > question > recipientName > giverTeam > giverName
    private Comparator<FeedbackResponseAttributes> compareByTeamQuestionRecipientTeamGiver = compareByRecipientTeam
            .thenComparing(compareByQuestionNumber)
            .thenComparing(compareByRecipientName)
            .thenComparing(compareByGiverTeam)
            .thenComparing(compareByGiverName)
            .thenComparing(compareByResponseString)
            .thenComparing(compareByFeedbackResponseAttributeId);

    // Sorts by giverTeam > question > giverName > recipientTeam > recipientName
    private Comparator<FeedbackResponseAttributes> compareByTeamQuestionGiverTeamRecipient = compareByGiverTeam
            .thenComparing(compareByQuestionNumber)
            .thenComparing(compareByGiverName)
            .thenComparing(compareByRecipientTeam)
            .thenComparing(compareByRecipientName)
            .thenComparing(compareByResponseString)
            .thenComparing(compareByFeedbackResponseAttributeId);

    // Sorts by recipientName > recipientEmail > giverName > giverEmail
    private Comparator<FeedbackResponseAttributes> compareByRecipientNameEmailGiverNameEmail =
            compareByRecipientDisplayName.thenComparing(compareByRecipientEmail)
                    .thenComparing(compareByGiverDisplayName)
                    .thenComparing(compareByGiverEmail)
                    .thenComparing(compareByResponseString)
                    .thenComparing(compareByFeedbackResponseAttributeId);

    public FeedbackSessionResultsBundle(FeedbackSessionAttributes feedbackSession,
                                        Map<String, FeedbackQuestionAttributes> questions, CourseRoster roster) {
        this(feedbackSession, new ArrayList<>(), questions, new HashMap<>(),
                new HashMap<>(), new HashMap<>(), new HashMap<>(),
                new HashMap<>(), roster,
                new HashMap<>());
    }

    public FeedbackSessionResultsBundle(FeedbackSessionAttributes feedbackSession,
                                        List<FeedbackResponseAttributes> responses,
                                        Map<String, FeedbackQuestionAttributes> questions,
                                        Map<String, String> emailNameTable,
                                        Map<String, String> emailLastNameTable,
                                        Map<String, String> emailTeamNameTable,
                                        Map<String, Set<String>> sectionTeamNameTable,
                                        Map<String, boolean[]> visibilityTable,
                                        CourseRoster roster,
                                        Map<String, List<FeedbackResponseCommentAttributes>> responseComments) {
        this(feedbackSession, responses, questions, emailNameTable, emailLastNameTable,
                emailTeamNameTable, sectionTeamNameTable, visibilityTable, new HashMap<>(), roster, responseComments, true);
    }

    public FeedbackSessionResultsBundle(FeedbackSessionAttributes feedbackSession,
                                        List<FeedbackResponseAttributes> responses,
                                        Map<String, FeedbackQuestionAttributes> questions,
                                        Map<String, String> emailNameTable,
                                        Map<String, String> emailLastNameTable,
                                        Map<String, String> emailTeamNameTable,
                                        Map<String, Set<String>> sectionTeamNameTable,
                                        Map<String, boolean[]> visibilityTable,
                                        Map<String, Map<String, Set<String>>> completeGiverRecipientMap,
                                        CourseRoster roster,
                                        Map<String, List<FeedbackResponseCommentAttributes>> responseComments,
                                        boolean isComplete) {
        this.feedbackSession = feedbackSession;
        this.questions = questions;
        this.responses = responses;

        // TODO we can get rid of the table below and use CourseRoster only
        this.emailNameTable = emailNameTable;
        this.emailLastNameTable = emailLastNameTable;
        this.emailTeamNameTable = emailTeamNameTable;
        this.commentGiverEmailToNameTable = roster.getEmailToNameTableFromRoster();
        this.sectionTeamNameTable = sectionTeamNameTable;

        this.visibilityTable = visibilityTable;
        this.roster = roster;
        this.responseComments = responseComments;
        this.actualResponses = new ArrayList<>();

        // TODO we can merge responses and actualResponses later
        // We change user email to team name here for display purposes.
        for (FeedbackResponseAttributes response : responses) {
            if (questions.get(response.feedbackQuestionId).giverType == FeedbackParticipantType.TEAMS
                    && roster.isStudentInCourse(response.giver)) {
                // for TEAMS giver type, for older responses,
                // the giverEmail is stored as the student giver's email in the database
                // so we convert it to the team name for use in FeedbackSessionResultsBundle
                response.giver = emailNameTable.get(response.giver + Const.TEAM_OF_EMAIL_OWNER);
            }
            // Copy the data before hiding response recipient and giver.
            FeedbackResponseAttributes fraCopy = new FeedbackResponseAttributes(response);
            actualResponses.add(fraCopy);
        }
        this.isComplete = isComplete;

        buildMissingResponses(completeGiverRecipientMap);
        // TODO move the hiding process to action layer
        hideResponsesGiverRecipient();
        // unlike emailTeamNameTable, emailLastNameTable and emailTeamNameTable,
        // roster.*Table is populated using the CourseRoster data directly
        this.rosterTeamNameMembersTable = getTeamNameToEmailsTableFromRoster(roster);
        this.rosterSectionTeamNameTable = getSectionToTeamNamesFromRoster(roster);
    }

    /**
     * Builds missing response based on the {@code completeGiverRecipientMap}.
     *
     * @param completeGiverRecipientMap the complete giver recipient map for questions
     */
    private void buildMissingResponses(Map<String, Map<String, Set<String>>> completeGiverRecipientMap) {
        this.missingResponses = new ArrayList<>();

        // remove entries that already have responses
        for (FeedbackResponseAttributes response : this.actualResponses) {
            Map<String, Set<String>> currQuestionGiverRecipientMap =
                    completeGiverRecipientMap.get(response.getFeedbackQuestionId());
            if (currQuestionGiverRecipientMap == null) {
                continue;
            }
            Set<String> recipientSet = currQuestionGiverRecipientMap.get(response.getGiver());
            if (recipientSet == null) {
                continue;
            }
            recipientSet.remove(response.getRecipient());
        }

        FeedbackTextResponseDetails missingResponseDetail =
                new FeedbackTextResponseDetails(Const.INSTRUCTOR_FEEDBACK_RESULTS_MISSING_RESPONSE);

        // rebuilt pseud-response that is missing
        for (Map.Entry<String, Map<String, Set<String>>> questionGiverRecipientMapEntry
                : completeGiverRecipientMap.entrySet()) {
            for (Map.Entry<String, Set<String>> giverRecipientMap : questionGiverRecipientMapEntry.getValue().entrySet()) {
                String giver = giverRecipientMap.getKey();
                String giverSection = roster.getInfoForIdentifier(giver)[CourseRoster.INFO_SECTION_NAME];

                for (String recipient : giverRecipientMap.getValue()) {
                    // get proper section
                    String recipientSection = roster.getInfoForIdentifier(recipient)[CourseRoster.INFO_SECTION_NAME];

                    this.missingResponses.add(
                            FeedbackResponseAttributes
                                    .builder(questionGiverRecipientMapEntry.getKey(), giver, recipient)
                                    .withCourseId(this.feedbackSession.getCourseId())
                                    .withFeedbackSessionName(this.feedbackSession.getFeedbackSessionName())
                                    .withGiverSection(giverSection)
                                    .withRecipientSection(recipientSection)
                                    .withResponseDetails(missingResponseDetail)
                                    .build());
                }
            }
        }
    }

    /**
     * Hides response names/emails and teams that are not visible to the current user.
     * Replaces the giver/recipient email in responses to an email with two "@@"s
     * to indicate it is invalid and should not be displayed.
     */
    private void hideResponsesGiverRecipient() {
        for (FeedbackResponseAttributes response : responses) {
            // Hide recipient details if its not visible to the current user
            String name = emailNameTable.get(response.recipient);
            FeedbackQuestionAttributes question = questions.get(response.feedbackQuestionId);
            FeedbackParticipantType participantType = question.recipientType;
            if (participantType == FeedbackParticipantType.SELF) {
                // recipient type for self-feedback is the same as the giver type
                participantType = question.giverType;
            }

            if (!isRecipientVisible(response)) {
                String anonEmail = getAnonEmail(participantType, name);
                name = getAnonName(participantType, name);

                emailNameTable.put(anonEmail, name);
                emailTeamNameTable.put(anonEmail, name + Const.TEAM_OF_EMAIL_OWNER);

                response.recipient = anonEmail;
            }

            // Hide giver details if its not visible to the current user
            name = emailNameTable.get(response.giver);
            participantType = question.giverType;

            if (!isGiverVisible(response)) {
                String anonEmail = getAnonEmail(participantType, name);
                name = getAnonName(participantType, name);

                emailNameTable.put(anonEmail, name);
                emailTeamNameTable.put(anonEmail, name + Const.TEAM_OF_EMAIL_OWNER);
                if (participantType == FeedbackParticipantType.TEAMS) {
                    emailTeamNameTable.put(anonEmail, name);
                }
                response.giver = anonEmail;
            }
        }
    }

    /**
     * Checks if the giver/recipient for a response is visible/hidden from the current user.
     */
    public boolean isFeedbackParticipantVisible(boolean isGiver, FeedbackResponseAttributes response) {
        FeedbackQuestionAttributes question = questions.get(response.feedbackQuestionId);
        FeedbackParticipantType participantType;
        String responseId = response.getId();

        boolean isVisible;
        if (isGiver) {
            isVisible = visibilityTable.get(responseId)[Const.VISIBILITY_TABLE_GIVER];
            participantType = question.giverType;
        } else {
            isVisible = visibilityTable.get(responseId)[Const.VISIBILITY_TABLE_RECIPIENT];
            participantType = question.recipientType;
        }
        boolean isTypeNone = participantType == FeedbackParticipantType.NONE;

        return isVisible || isTypeNone;
    }

    /**
     * Returns true if the recipient from a response is visible to the current user.
     * Returns false otherwise.
     */
    public boolean isRecipientVisible(FeedbackResponseAttributes response) {
        return isFeedbackParticipantVisible(false, response);
    }

    /**
     * Returns true if the giver from a response is visible to the current user.
     * Returns false otherwise.
     */
    public boolean isGiverVisible(FeedbackResponseAttributes response) {
        return isFeedbackParticipantVisible(true, response);
    }

    public static String getAnonEmail(FeedbackParticipantType type, String name) {
        String anonName = getAnonName(type, name);
        return anonName + "@@" + anonName + ".com";
    }

    public String getAnonEmailFromStudentEmail(String studentEmail) {
        String name = roster.getStudentForEmail(studentEmail).name;
        return getAnonEmail(FeedbackParticipantType.STUDENTS, name);
    }

    public static String getAnonName(FeedbackParticipantType type, String name) {
        String hashedEncryptedName = getHashOfName(getEncryptedName(name));
        String participantType = type.toSingularFormString();
        return String.format(
                Const.DISPLAYED_NAME_FOR_ANONYMOUS_PARTICIPANT + " %s %s", participantType, hashedEncryptedName);
    }

    private static String getEncryptedName(String name) {
        return StringHelper.encrypt(name);
    }

    private static String getHashOfName(String name) {
        return Long.toString(Math.abs((long) name.hashCode()));
    }

    private String getNameFromRoster(String participantIdentifier, boolean isFullName) {
        if (participantIdentifier.equals(Const.GENERAL_QUESTION)) {
            return Const.USER_NOBODY_TEXT;
        }

        // return person name if participant is a student
        if (isParticipantIdentifierStudent(participantIdentifier)) {
            StudentAttributes student = roster.getStudentForEmail(participantIdentifier);
            if (isFullName) {
                return student.name;
            }
            return student.lastName;
        }

        // return person name if participant is an instructor
        if (isParticipantIdentifierInstructor(participantIdentifier)) {
            return roster.getInstructorForEmail(participantIdentifier)
                    .name;
        }

        // return team name if participantIdentifier is a team name
        boolean isTeamName = rosterTeamNameMembersTable.containsKey(participantIdentifier);
        if (isTeamName) {
            return participantIdentifier;
        }

        // return team name if participant is team identified by a member
        boolean isNameRepresentingStudentsTeam = participantIdentifier.contains(Const.TEAM_OF_EMAIL_OWNER);
        if (isNameRepresentingStudentsTeam) {
            int index = participantIdentifier.indexOf(Const.TEAM_OF_EMAIL_OWNER);
            return getTeamNameFromRoster(participantIdentifier.substring(0, index));
        }

        return "";
    }

    /**
     * Get the displayable full name from an email.
     *
     * <p>This function is different from {@link #getNameForEmail} as it obtains the name
     * using the class roster, instead of from the responses.
     * @return the full name of a student, if participantIdentifier is the email of a student, <br>
     *         the name of an instructor, if participantIdentifier is the email of an instructor, <br>
     *         or the team name, if participantIdentifier represents a team. <br>
     *         Otherwise, return an empty string
     */
    public String getFullNameFromRoster(String participantIdentifier) {
        return getNameFromRoster(participantIdentifier, true);
    }

    /**
     * Get the displayable last name from an email.
     *
     * <p>This function is different from {@link #getLastNameForEmail} as it obtains the name
     * using the class roster, instead of from the responses.
     * @return the last name of a student, if participantIdentifier is the email of a student, <br>
     *         the name of an instructor, if participantIdentifier is the email of an instructor, <br>
     *         or the team name, if participantIdentifier represents a team. <br>
     *         Otherwise, return an empty string
     */
    public String getLastNameFromRoster(String participantIdentifier) {
        return getNameFromRoster(participantIdentifier, false);
    }

    /**
     * Return true if the participantIdentifier is an email of either a student
     * or instructor in the course roster. Otherwise, return false.
     *
     * @return true if the participantIdentifier is an email of either a student
     *         or instructor in the course roster, false otherwise.
     */
    public boolean isEmailOfPersonFromRoster(String participantIdentifier) {
        boolean isStudent = isParticipantIdentifierStudent(participantIdentifier);
        boolean isInstructor = isParticipantIdentifierInstructor(participantIdentifier);
        return isStudent || isInstructor;
    }

    /**
     * If the participantIdentifier identifies a student or instructor,
     * the participantIdentifier is returned.
     *
     * <p>Otherwise, Const.USER_NOBODY_TEXT is returned.
     * @see #getDisplayableEmail
     */
    public String getDisplayableEmailFromRoster(String participantIdentifier) {
        if (isEmailOfPersonFromRoster(participantIdentifier)) {
            return participantIdentifier;
        }
        return Const.USER_NOBODY_TEXT;
    }

    /**
     * Get the displayable team name from an email.
     * If the email is not an email of someone in the class roster, an empty string is returned.
     *
     * <p>This function is different from {@link #getTeamNameForEmail} as it obtains the name
     * using the class roster, instead of from the responses.
     */
    public String getTeamNameFromRoster(String participantIdentifier) {
        if (participantIdentifier.equals(Const.GENERAL_QUESTION)) {
            return Const.USER_NOBODY_TEXT;
        }
        if (isParticipantIdentifierStudent(participantIdentifier)) {
            return roster.getStudentForEmail(participantIdentifier).team;
        } else if (isParticipantIdentifierInstructor(participantIdentifier)) {
            return Const.USER_TEAM_FOR_INSTRUCTOR;
        } else {
            return "";
        }
    }

    /**
     * Get the displayable section name from an email.
     *
     * <p>If the email is not an email of someone in the class roster, an empty string is returned.
     *
     * <p>If the email of an instructor or "%GENERAL%" is passed in, "No specific recipient" is returned.
     */
    public String getSectionFromRoster(String participantIdentifier) {
        boolean isStudent = isParticipantIdentifierStudent(participantIdentifier);
        boolean isInstructor = isParticipantIdentifierInstructor(participantIdentifier);
        boolean participantIsGeneral = participantIdentifier.equals(Const.GENERAL_QUESTION);

        if (isStudent) {
            return roster.getStudentForEmail(participantIdentifier)
                    .section;
        } else if (isInstructor || participantIsGeneral) {
            return Const.NO_SPECIFIC_SECTION;
        } else {
            return "";
        }
    }

    /**
     * Get the emails of the students given a teamName,
     * if teamName is "Instructors", returns the list of instructors.
     * @return a set of emails of the students in the team
     */
    public Set<String> getTeamMembersFromRoster(String teamName) {
        return new HashSet<>(rosterTeamNameMembersTable.getOrDefault(teamName, new HashSet<>()));
    }

    /**
     * Get the team names in a section. <br>
     *
     * <p>Instructors are not contained in any section.
     * @return a set of team names of the teams in the section
     */
    public Set<String> getTeamsInSectionFromRoster(String sectionName) {
        return new HashSet<>(rosterSectionTeamNameTable.getOrDefault(sectionName, new HashSet<>()));
    }

    public boolean isParticipantIdentifierStudent(String participantIdentifier) {
        StudentAttributes student = roster.getStudentForEmail(participantIdentifier);
        return student != null;
    }

    public boolean isParticipantIdentifierInstructor(String participantIdentifier) {
        InstructorAttributes instructor = roster.getInstructorForEmail(participantIdentifier);
        return instructor != null;
    }

    /**
     * Used for instructor feedback results views.
     */
    public String getResponseAnswerHtml(FeedbackResponseAttributes response,
                                        FeedbackQuestionAttributes question) {
        return ""; // response.getResponseDetails().getAnswerHtml(response, question, this);
    }

    public String getResponseAnswerCsv(FeedbackResponseAttributes response,
                                       FeedbackQuestionAttributes question) {
        return response.getResponseDetails().getAnswerCsv(response, question, this);
    }

    public FeedbackResponseAttributes getActualResponse(FeedbackResponseAttributes response) {
        FeedbackResponseAttributes actualResponse = null;
        for (FeedbackResponseAttributes resp : actualResponses) {
            if (resp.getId().equals(response.getId())) {
                actualResponse = resp;
                break;
            }
        }
        return actualResponse;
    }

    public String getNameForEmail(String email) {
        String name = emailNameTable.get(email);
        if (name == null || name.equals(Const.USER_IS_MISSING)) {
            return Const.USER_UNKNOWN_TEXT;
        } else if (name.equals(Const.USER_IS_NOBODY)) {
            return Const.USER_NOBODY_TEXT;
        } else if (name.equals(Const.USER_IS_TEAM)) {
            return getTeamNameForEmail(email);
        } else {
            return name;
        }
    }

    public String getLastNameForEmail(String email) {
        String name = emailLastNameTable.get(email);
        if (name == null || name.equals(Const.USER_IS_MISSING)) {
            return Const.USER_UNKNOWN_TEXT;
        } else if (name.equals(Const.USER_IS_NOBODY)) {
            return Const.USER_NOBODY_TEXT;
        } else if (name.equals(Const.USER_IS_TEAM)) {
            return getTeamNameForEmail(email);
        } else {
            return name;
        }
    }

    public String getTeamNameForEmail(String email) {
        String teamName = emailTeamNameTable.get(email);
        if (teamName == null || email.equals(Const.GENERAL_QUESTION)) {
            return Const.USER_NOBODY_TEXT;
        }
        return teamName;
    }

    /**
     * Returns displayable email if the email of a giver/recipient in the course
     * and it is allowed to be displayed.
     * Returns Const.USER_NOBODY_TEXT otherwise.
     */
    public String getDisplayableEmail(boolean isGiver, FeedbackResponseAttributes response) {
        String participantIdentifier;
        if (isGiver) {
            participantIdentifier = response.giver;
        } else {
            participantIdentifier = response.recipient;
        }

        if (isEmailOfPerson(participantIdentifier) && isFeedbackParticipantVisible(isGiver, response)) {
            return participantIdentifier;
        }
        return Const.USER_NOBODY_TEXT;
    }

    /**
     * Returns displayable email if the email of a recipient in the course
     * and it is allowed to be displayed.
     * Returns Const.USER_NOBODY_TEXT otherwise.
     */
    public String getDisplayableEmailRecipient(FeedbackResponseAttributes response) {
        return getDisplayableEmail(false, response);
    }

    /**
     * Returns displayable email if the email of a giver in the course
     * and it is allowed to be displayed.
     * Returns Const.USER_NOBODY_TEXT otherwise.
     */
    public String getDisplayableEmailGiver(FeedbackResponseAttributes response) {
        return getDisplayableEmail(true, response);
    }

    /**
     * Returns true if the given identifier is an email of a person in the course.
     * Returns false otherwise.
     */
    public boolean isEmailOfPerson(String participantIdentifier) {
        // An email must at least contains '@' character
        boolean isIdentifierEmail = participantIdentifier.contains("@");

        /*
         * However, a team name may also contains '@'
         * To differentiate a team name and an email of a person,
         * we check against the name & team name associated by the participant identifier
         */
        String name = emailNameTable.get(participantIdentifier);
        boolean isIdentifierName = name != null && name.equals(participantIdentifier);
        boolean isIdentifierTeam = name != null && name.equals(Const.USER_IS_TEAM);

        String teamName = emailTeamNameTable.get(participantIdentifier);
        boolean isIdentifierTeamName = teamName != null && teamName.equals(participantIdentifier);
        return isIdentifierEmail && !(isIdentifierName || isIdentifierTeamName || isIdentifierTeam);
    }

    public String getRecipientNameForResponse(FeedbackResponseAttributes response) {
        String name = emailNameTable.get(response.recipient);
        if (name == null || name.equals(Const.USER_IS_MISSING)) {
            return Const.USER_UNKNOWN_TEXT;
        } else if (name.equals(Const.USER_IS_NOBODY)) {
            return Const.USER_NOBODY_TEXT;
        } else {
            return name;
        }
    }

    public String getGiverNameForResponse(FeedbackResponseAttributes response) {
        String name = emailNameTable.get(response.giver);
        if (name == null || name.equals(Const.USER_IS_MISSING)) {
            return Const.USER_UNKNOWN_TEXT;
        } else if (name.equals(Const.USER_IS_NOBODY)) {
            return Const.USER_NOBODY_TEXT;
        } else {
            return name;
        }
    }

    public String appendTeamNameToName(String name, String teamName) {
        String outputName;
        if (name.contains(Const.DISPLAYED_NAME_FOR_ANONYMOUS_PARTICIPANT) || name.equals(Const.USER_UNKNOWN_TEXT)
                || name.equals(Const.USER_NOBODY_TEXT) || teamName.isEmpty()) {
            outputName = name;
        } else {
            outputName = name + " (" + teamName + ")";
        }
        return outputName;
    }

    // TODO consider removing this to increase cohesion
    public String getQuestionText(String feedbackQuestionId) {
        return SanitizationHelper.sanitizeForHtml(questions.get(feedbackQuestionId)
                .getQuestionDetails()
                .getQuestionText());
    }

    // TODO: make responses to the student calling this method always on top.
    /**
     * Gets the questions and responses in this bundle as a map.
     *
     * @return An ordered {@code Map} with keys as {@link FeedbackQuestionAttributes}
     *         sorted by questionNumber.
     *         The mapped values for each key are the corresponding
     *         {@link FeedbackResponseAttributes} as a {@code List}.
     */
    public Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>> getQuestionResponseMap() {
        if (questions == null || responses == null) {
            return null;
        }

        Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>> sortedMap = new LinkedHashMap<>();
        List<FeedbackQuestionAttributes> sortedQuestions = new ArrayList<>(questions.values());
        // sorts the questions by its natural ordering, which is by question number
        sortedQuestions.sort(null);
        for (FeedbackQuestionAttributes question : sortedQuestions) {
            sortedMap.put(question, new ArrayList<>());
        }

        for (FeedbackResponseAttributes response : responses) {
            FeedbackQuestionAttributes question = questions.get(response.feedbackQuestionId);
            List<FeedbackResponseAttributes> responsesForQuestion = sortedMap.get(question);
            responsesForQuestion.add(response);
        }

        for (List<FeedbackResponseAttributes> responsesForQuestion : sortedMap.values()) {
            responsesForQuestion.sort(compareByGiverRecipient);
        }

        return sortedMap;
    }

    public Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>> getQuestionResponseMapSortedByRecipient() {
        if (questions == null || responses == null) {
            return null;
        }

        Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>> sortedMap = new LinkedHashMap<>();

        List<FeedbackQuestionAttributes> sortedQuestions = new ArrayList<>(questions.values());
        // sorts the questions by its natural ordering, which is by question number
        sortedQuestions.sort(null);
        for (FeedbackQuestionAttributes question : sortedQuestions) {
            sortedMap.put(question, new ArrayList<>());
        }

        for (FeedbackResponseAttributes response : responses) {
            FeedbackQuestionAttributes question = questions.get(response.feedbackQuestionId);
            List<FeedbackResponseAttributes> responsesForQuestion = sortedMap.get(question);
            responsesForQuestion.add(response);
        }

        for (List<FeedbackResponseAttributes> responsesForQuestion : sortedMap.values()) {
            responsesForQuestion.sort(compareByRecipientNameEmailGiverNameEmail);
        }

        return sortedMap;
    }

    /**
     * Returns an ordered Map with {@code recipientTeam} name as key
     * sorted by recipientTeam > question > recipientName > giverTeam > giverName.
     */
    public Map<String, Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>>>
            getQuestionResponseMapByRecipientTeam() {
        Map<String, Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>>> sortedMap = new LinkedHashMap<>();

        responses.sort(compareByTeamQuestionRecipientTeamGiver);

        for (FeedbackResponseAttributes response : responses) {
            String recipientTeam = getTeamNameForEmail(response.recipient);
            if (recipientTeam.isEmpty()) {
                recipientTeam = getNameForEmail(response.recipient);
            }

            Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>> responsesForOneRecipient =
                    sortedMap.computeIfAbsent(recipientTeam, key -> new LinkedHashMap<>());

            FeedbackQuestionAttributes question = questions.get(response.feedbackQuestionId);

            List<FeedbackResponseAttributes> responsesForOneRecipientOneQuestion =
                    responsesForOneRecipient.computeIfAbsent(question, key -> new ArrayList<>());

            responsesForOneRecipientOneQuestion.add(response);
        }

        return sortedMap;
    }

    /**
     * Returns an ordered Map with {@code giverTeam} name as key
     * sorted by giverTeam > question > giverName > recipientTeam > recipientName.
     */
    public Map<String, Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>>>
            getQuestionResponseMapByGiverTeam() {
        Map<String, Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>>> sortedMap = new LinkedHashMap<>();

        responses.sort(compareByTeamQuestionGiverTeamRecipient);

        for (FeedbackResponseAttributes response : responses) {
            String giverTeam = getTeamNameForEmail(response.giver);
            if (giverTeam.isEmpty()) {
                giverTeam = getNameForEmail(response.giver);
            }

            Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>> responsesFromOneGiver =
                    sortedMap.computeIfAbsent(giverTeam, key -> new LinkedHashMap<>());

            FeedbackQuestionAttributes question = questions.get(response.feedbackQuestionId);

            responsesFromOneGiver.computeIfAbsent(question, key -> new ArrayList<>())
                                 .add(response);
        }

        return sortedMap;
    }

    /**
     * Returns responses as a {@code Map<recipientName, Map<question, List<response>>>}
     * Where the responses are sorted in the order of recipient, question, giver.
     * @return responses sorted by Recipient > Question > Giver
     */
    public Map<String, Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>>>
            getResponsesSortedByRecipientQuestionGiver(boolean sortByTeam) {
        Map<String, Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>>> sortedMap = new LinkedHashMap<>();

        if (sortByTeam) {
            responses.sort(compareByTeamRecipientQuestionTeamGiver);
        } else {
            responses.sort(compareByRecipientQuestionTeamGiver);
        }

        for (FeedbackResponseAttributes response : responses) {
            String recipientEmail = response.recipient;
            Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>> responsesForOneRecipient =
                    sortedMap.computeIfAbsent(recipientEmail, key -> new LinkedHashMap<>());

            FeedbackQuestionAttributes question = questions.get(response.feedbackQuestionId);
            responsesForOneRecipient.computeIfAbsent(question, key -> new ArrayList<>())
                                    .add(response);
        }

        return sortedMap;
    }

    /**
     * Returns the responses in this bundle as a {@code Tree} structure with no base node
     * using a {@code LinkedHashMap} implementation.
     * <br>The tree is sorted by recipientName > giverName > questionNumber.
     * <br>The key of each map represents the parent node, while the value represents the leaf.
     * <br>The top-most parent {@code String recipientName} is the recipient's name of all it's leafs.
     * <br>The inner parent {@code String giverName} is the giver's name of all it's leafs.
     * <br>The inner-most child is a {@code List<FeedbackResponseAttributes} of all the responses
     * <br>with attributes corresponding to it's parents.
     * @return The responses in this bundle sorted by recipient's name > giver's name > question number.
     */
    public Map<String, Map<String, List<FeedbackResponseAttributes>>> getResponsesSortedByRecipient() {
        return getResponsesSortedByRecipient(false);
    }

    public Map<String, Map<String, List<FeedbackResponseAttributes>>>
            getResponsesSortedByRecipient(boolean sortByTeam) {
        Map<String, Map<String, List<FeedbackResponseAttributes>>> sortedMap = new LinkedHashMap<>();

        if (sortByTeam) {
            responses.sort(compareByTeamRecipientGiverQuestion);
        } else {
            responses.sort(compareByRecipientGiverQuestion);
        }

        for (FeedbackResponseAttributes response : responses) {
            String recipientName = this.getRecipientNameForResponse(response);
            String recipientTeamName = this.getTeamNameForEmail(response.recipient);
            String recipientNameWithTeam = this.appendTeamNameToName(recipientName, recipientTeamName);

            Map<String, List<FeedbackResponseAttributes>> responsesToOneRecipient =
                    sortedMap.computeIfAbsent(recipientNameWithTeam, key -> new LinkedHashMap<>());

            String giverName = this.getGiverNameForResponse(response);
            String giverTeamName = this.getTeamNameForEmail(response.giver);
            String giverNameWithTeam = this.appendTeamNameToName(giverName, giverTeamName);

            responsesToOneRecipient.computeIfAbsent(giverNameWithTeam, key -> new ArrayList<>())
                                   .add(response);
        }

        return sortedMap;
    }

    /**
     * Returns the responses in this bundle as a {@code Tree} structure with no base node
     * using a {@code LinkedHashMap} implementation.
     * <br>The tree is sorted by recipientName > giverName > questionNumber.
     * <br>The key of each map represents the parent node, while the value represents the leaf.
     * <br>The top-most parent {@code String recipientName} is the recipient's name of all it's leafs.
     * <br>The inner parent {@code String giverName} is the giver's name of all it's leafs.
     * <br>The inner-most child is a {@code List<FeedbackResponseAttributes} of all the responses
     * <br>with attributes corresponding to it's parents.
     * @return The responses in this bundle sorted by recipient identifier > giver identifier > question number.
     * @see #getResponsesSortedByRecipient
     */
    public Map<String, Map<String, List<FeedbackResponseAttributes>>>
            getResponsesSortedByRecipientGiverQuestion(boolean sortByTeam) {

        Map<String, Map<String, List<FeedbackResponseAttributes>>> sortedMap = new LinkedHashMap<>();

        if (sortByTeam) {
            responses.sort(compareByTeamRecipientGiverQuestion);
        } else {
            responses.sort(compareByRecipientGiverQuestion);
        }

        for (FeedbackResponseAttributes response : responses) {
            String recipientEmail = response.recipient;

            Map<String, List<FeedbackResponseAttributes>> responsesToOneRecipient =
                    sortedMap.computeIfAbsent(recipientEmail, key -> new LinkedHashMap<>());

            String giverEmail = response.giver;
            responsesToOneRecipient.computeIfAbsent(giverEmail, key -> new ArrayList<>())
                                   .add(response);
        }

        return sortedMap;
    }

    /**
     * Returns responses as a {@code Map<giverName, Map<question, List<response>>>}
     * Where the responses are sorted in the order of giver, question, recipient.
     * @return responses sorted by Giver > Question > Recipient
     */
    public Map<String, Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>>>
            getResponsesSortedByGiverQuestionRecipient(boolean sortByTeam) {
        Map<String, Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>>> sortedMap = new LinkedHashMap<>();

        if (sortByTeam) {
            responses.sort(compareByTeamGiverQuestionTeamRecipient);
        } else {
            responses.sort(compareByGiverQuestionTeamRecipient);
        }

        for (FeedbackResponseAttributes response : responses) {
            String giverEmail = response.giver;

            Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>> responsesFromOneGiver =
                    sortedMap.computeIfAbsent(giverEmail, key -> new LinkedHashMap<>());

            FeedbackQuestionAttributes question = questions.get(response.feedbackQuestionId);
            responsesFromOneGiver.computeIfAbsent(question, key -> new ArrayList<>())
                                 .add(response);
        }

        return sortedMap;
    }

    /**
     * Returns the responses in this bundle as a {@code Tree} structure with no base node
     * using a {@code LinkedHashMap} implementation.
     * <br>The tree is sorted by giverName > recipientName > questionNumber.
     * <br>The key of each map represents the parent node, while the value represents the leaf.
     * <br>The top-most parent {@code String giverName} is the recipient's name of all it's leafs.
     * <br>The inner parent {@code String recipientName} is the giver's name of all it's leafs.
     * <br>The inner-most child is a {@code List<FeedbackResponseAttributes} of all the responses
     * <br>with attributes corresponding to it's parents.
     * @return The responses in this bundle sorted by giver's name > recipient's name > question number.
     */
    public Map<String, Map<String, List<FeedbackResponseAttributes>>> getResponsesSortedByGiver() {
        return getResponsesSortedByGiver(false);
    }

    public Map<String, Map<String, List<FeedbackResponseAttributes>>>
            getResponsesSortedByGiver(boolean sortByTeam) {
        Map<String, Map<String, List<FeedbackResponseAttributes>>> sortedMap = new LinkedHashMap<>();

        if (sortByTeam) {
            responses.sort(compareByTeamGiverRecipientQuestion);
        } else {
            responses.sort(compareByGiverRecipientQuestion);
        }

        for (FeedbackResponseAttributes response : responses) {
            String giverName = this.getGiverNameForResponse(response);
            String giverTeamName = this.getTeamNameForEmail(response.giver);
            String giverNameWithTeam = this.appendTeamNameToName(giverName, giverTeamName);

            Map<String, List<FeedbackResponseAttributes>> responsesFromOneGiver =
                    sortedMap.computeIfAbsent(giverNameWithTeam, key -> new LinkedHashMap<>());

            String recipientName = this.getRecipientNameForResponse(response);
            String recipientTeamName = this.getTeamNameForEmail(response.recipient);
            String recipientNameWithTeam = this.appendTeamNameToName(recipientName, recipientTeamName);

            responsesFromOneGiver.computeIfAbsent(recipientNameWithTeam, key -> new ArrayList<>())
                                 .add(response);
        }

        return sortedMap;
    }

    /**
     * Returns the responses in this bundle as a {@code Tree} structure with no base node
     * using a {@code LinkedHashMap} implementation.
     * <br>The tree is sorted by giverName > recipientName > questionNumber.
     * <br>The key of each map represents the parent node, while the value represents the leaf.
     * <br>The top-most parent {@code String giverName} is the recipient's name of all it's leafs.
     * <br>The inner parent {@code String recipientName} is the giver's name of all it's leafs.
     * <br>The inner-most child is a {@code List<FeedbackResponseAttributes} of all the responses
     * <br>with attributes corresponding to it's parents.
     * @return The responses in this bundle sorted by giver's identifier > recipient's identifier > question number.
     * @see #getResponsesSortedByGiver
     */
    public Map<String, Map<String, List<FeedbackResponseAttributes>>>
            getResponsesSortedByGiverRecipientQuestion(boolean sortByTeam) {
        if (sortByTeam) {
            responses.sort(compareByTeamGiverRecipientQuestion);
        } else {
            responses.sort(compareByGiverRecipientQuestion);
        }

        Map<String, Map<String, List<FeedbackResponseAttributes>>> sortedMap = new LinkedHashMap<>();

        for (FeedbackResponseAttributes response : responses) {
            String giverEmail = response.giver;

            Map<String, List<FeedbackResponseAttributes>> responsesFromOneGiver =
                    sortedMap.computeIfAbsent(giverEmail, key -> new LinkedHashMap<>());

            String recipientEmail = response.recipient;
            responsesFromOneGiver.computeIfAbsent(recipientEmail, key -> new ArrayList<>())
                                 .add(response);
        }

        return sortedMap;
    }

    public boolean isStudentHasSomethingNewToSee(StudentAttributes student) {
        for (FeedbackResponseAttributes response : responses) {
            // There is a response not written by the student
            // which is visible to the student
            if (!response.giver.equals(student.email)) {
                return true;
            }
            // There is a response comment visible to the student
            if (responseComments.containsKey(response.getId())) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Set<String>> getTeamNameToEmailsTableFromRoster(CourseRoster courseroster) {
        List<StudentAttributes> students = courseroster.getStudents();
        Map<String, Set<String>> teamNameToEmails = new HashMap<>();

        for (StudentAttributes student : students) {
            String studentTeam = student.team;
            Set<String> studentEmails = teamNameToEmails.getOrDefault(studentTeam, new TreeSet<>());

            studentEmails.add(student.email);
            teamNameToEmails.put(studentTeam, studentEmails);
        }

        List<InstructorAttributes> instructors = courseroster.getInstructors();
        String instructorsTeam = Const.USER_TEAM_FOR_INSTRUCTOR;
        Set<String> instructorEmails = new HashSet<>();

        for (InstructorAttributes instructor : instructors) {
            instructorEmails.add(instructor.email);
            teamNameToEmails.put(instructorsTeam, instructorEmails);
        }

        return teamNameToEmails;
    }

    private Map<String, Set<String>> getSectionToTeamNamesFromRoster(CourseRoster courseroster) {
        List<StudentAttributes> students = courseroster.getStudents();
        Map<String, Set<String>> sectionToTeam = new HashMap<>();

        for (StudentAttributes student : students) {
            String studentSection = student.section;
            String studentTeam = student.team;
            Set<String> teamNames = sectionToTeam.getOrDefault(studentSection, new HashSet<>());

            teamNames.add(studentTeam);
            sectionToTeam.put(studentSection, teamNames);
        }

        return sectionToTeam;
    }

    /**
     * Compares the values of {@code name1} and {@code name2}.
     * Anonymous names are ordered later than non-anonymous names.
     * @param isFirstNameVisible  true if the first name should be visible to the user
     * @param isSecondNameVisible true if the second name should be visible to the user
     */
    private int compareByNames(String name1, String name2,
                               boolean isFirstNameVisible, boolean isSecondNameVisible) {
        if (!isFirstNameVisible && !isSecondNameVisible) {
            return 0;
        }
        if (!isFirstNameVisible && isSecondNameVisible) {
            return 1;
        } else if (isFirstNameVisible && !isSecondNameVisible) {
            return -1;
        }

        // Make class feedback always appear on top, and team responses at bottom.
        int n1Priority = 0;
        int n2Priority = 0;

        if (name1.equals(Const.USER_IS_NOBODY)) {
            n1Priority = -1;
        } else if (name1.equals(Const.USER_IS_TEAM)) {
            n1Priority = 1;
        }
        if (name2.equals(Const.USER_IS_NOBODY)) {
            n2Priority = -1;
        } else if (name2.equals(Const.USER_IS_TEAM)) {
            n2Priority = 1;
        }

        int order = Integer.compare(n1Priority, n2Priority);
        return order == 0 ? name1.compareTo(name2) : order;
    }

    public FeedbackSessionAttributes getFeedbackSession() {
        return feedbackSession;
    }

    public List<FeedbackResponseAttributes> getResponses() {
        return responses;
    }

    public Map<String, FeedbackQuestionAttributes> getQuestions() {
        return questions;
    }

    public Map<String, String> getEmailNameTable() {
        return emailNameTable;
    }

    public Map<String, String> getEmailLastNameTable() {
        return emailLastNameTable;
    }

    public Map<String, String> getEmailTeamNameTable() {
        return emailTeamNameTable;
    }

    public Map<String, Set<String>> getRosterTeamNameMembersTable() {
        return rosterTeamNameMembersTable;
    }

    public Set<String> sectionsInCourse() {
        return new HashSet<>(rosterSectionTeamNameTable.keySet());
    }

    public Map<String, Set<String>> getRosterSectionTeamNameTable() {
        return rosterSectionTeamNameTable;
    }

    public Map<String, boolean[]> getVisibilityTable() {
        return visibilityTable;
    }

    public CourseRoster getRoster() {
        Assumption.assertNotNull(roster);
        return roster;
    }

    public List<FeedbackResponseAttributes> getMissingResponses() {
        return missingResponses;
    }

    public Map<String, List<FeedbackResponseCommentAttributes>> getResponseComments() {
        return responseComments;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public ZoneId getTimeZone() {
        return feedbackSession.getTimeZone();
    }

    /**
     * Returns string of all instructor comments on a response appended to their names for csv.
     *
     * @param response feedback response from which comments are taken
     * @return string of format ", name of instructor, comment"
     */
    public String getCsvDetailedInstructorFeedbackResponseComments(FeedbackResponseAttributes response) {
        if (!this.responseComments.containsKey(response.getId())) {
            return "";
        }
        StringBuilder commentRow = new StringBuilder(200);
        List<FeedbackResponseCommentAttributes> frcList = this.responseComments.get(response.getId());
        for (FeedbackResponseCommentAttributes frc : frcList) {
            if (!frc.isCommentFromFeedbackParticipant) {
                commentRow.append("," + commentGiverEmailToNameTable.get(frc.commentGiver) + ","
                                          + frc.getCommentAsCsvString());
            }
        }
        return commentRow.toString();
    }

    /**
     * Returns string of comment by feedback participant on a response for csv.
     *
     * @param response feedback response on which comment is given
     * @return comment by feedback participant in form of string
     */
    public String getCsvDetailedFeedbackParticipantCommentOnResponse(FeedbackResponseAttributes response) {
        if (!this.responseComments.containsKey(response.getId())) {
            return "";
        }
        List<FeedbackResponseCommentAttributes> frcList = this.responseComments.get(response.getId());
        for (FeedbackResponseCommentAttributes frc : frcList) {
            if (frc.isCommentFromFeedbackParticipant) {
                return frc.getCommentAsCsvString();
            }
        }
        return "";
    }

    /**
     * Returns true if bundle contains response from Instructor.
     */
    public boolean hasResponseFromInstructor() {
        for (FeedbackResponseAttributes response : responses) {
            String giverIdentifier = response.giver;
            // If a instructor is also a student, the response should be considered as student's response.
            if (isParticipantIdentifierInstructor(giverIdentifier) && !isParticipantIdentifierStudent(giverIdentifier)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if bundle contains response to Instructor or General.
     */
    public boolean hasResponseToInstructorOrGeneral() {
        for (FeedbackResponseAttributes response : responses) {
            String recipientIdentifier = response.recipient;
            // getSectionFromRoster will return NO_SPECIFIC_SECTION for recipient who is Instructor or Nobody specific.
            if (Const.NO_SPECIFIC_SECTION.equals(getSectionFromRoster(recipientIdentifier))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns list of responses sorted by giverName > recipientName > qnNumber with identities
     * of giver/recipients NOT hidden which is used for anonymous result calculation.
     *
     * @param question question whose responses are required
     * @return list of responses
     */
    public List<FeedbackResponseAttributes> getActualResponsesSortedByGqr(FeedbackQuestionAttributes question) {
        List<FeedbackResponseAttributes> responses = getActualUnsortedResponses(question);
        responses.sort(compareByGiverRecipientQuestion);
        return responses;
    }

    /**
     * Returns list of unsorted responses with identities of giver/recipients NOT hidden which is used for
     * anonymous result calculation.
     *
     * @param question question whose responses are required
     * @return list of responses
     */
    public List<FeedbackResponseAttributes> getActualUnsortedResponses(FeedbackQuestionAttributes question) {
        return actualResponses.stream()
                            .filter(response -> response.feedbackQuestionId.equals(question.getId()))
                            .collect(Collectors.toList());
    }
}
