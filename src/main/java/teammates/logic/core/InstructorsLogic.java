package teammates.logic.core;

import java.util.ArrayList;
import java.util.List;

import teammates.common.datatransfer.FeedbackParticipantType;
import teammates.common.datatransfer.InstructorSearchResultBundle;
import teammates.common.datatransfer.attributes.FeedbackQuestionAttributes;
import teammates.common.datatransfer.attributes.FeedbackResponseAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.exception.EntityAlreadyExistsException;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.Assumption;
import teammates.common.util.FieldValidator;
import teammates.common.util.Logger;
import teammates.common.util.StringHelper;
import teammates.storage.api.InstructorsDb;

/**
 * Handles operations related to instructors.
 *
 * @see InstructorAttributes
 * @see InstructorsDb
 */
public final class InstructorsLogic {

    private static final Logger log = Logger.getLogger();

    private static InstructorsLogic instance = new InstructorsLogic();

    private static final InstructorsDb instructorsDb = new InstructorsDb();

    private static final AccountsLogic accountsLogic = AccountsLogic.inst();
    private static final CoursesLogic coursesLogic = CoursesLogic.inst();
    private static final FeedbackResponsesLogic frLogic = FeedbackResponsesLogic.inst();
    private static final FeedbackResponseCommentsLogic frcLogic = FeedbackResponseCommentsLogic.inst();
    private static final FeedbackQuestionsLogic fqLogic = FeedbackQuestionsLogic.inst();
    private static final FeedbackSessionsLogic fsLogic = FeedbackSessionsLogic.inst();

    private InstructorsLogic() {
        // prevent initialization
    }

    public static InstructorsLogic inst() {
        return instance;
    }

    /* ====================================
     * methods related to google search API
     * ====================================
     */

    /**
     * Batch creates or updates documents for the given Instructors.
     * @param instructors a list of instructors to be put into documents
     */
    public void putDocuments(List<InstructorAttributes> instructors) {
        instructorsDb.putDocuments(instructors);
    }

    /**
     * This method should be used by admin only since the searching does not restrict the
     * visibility according to the logged-in user's google ID. This is used by admin to
     * search instructors in the whole system.
     * @return null if no result found
     */
    public InstructorSearchResultBundle searchInstructorsInWholeSystem(String queryString) {
        return instructorsDb.searchInstructorsInWholeSystem(queryString);
    }

    /* ====================================
     * ====================================
     */

    /**
     * Creates an instructor.
     *
     * @return the created instructor
     * @throws InvalidParametersException if the instructor is not valid
     * @throws EntityAlreadyExistsException if the instructor already exists in the Datastore
     */
    public InstructorAttributes createInstructor(InstructorAttributes instructorToAdd)
            throws InvalidParametersException, EntityAlreadyExistsException {
        return instructorsDb.createInstructor(instructorToAdd);
    }

    /**
     * Sets the archive status of an instructor (i.e. whether the instructor
     * decides to archive the associated course or not).
     */
    public void setArchiveStatusOfInstructor(String googleId, String courseId, boolean archiveStatus)
            throws InvalidParametersException, EntityDoesNotExistException {
        instructorsDb.updateInstructorByGoogleId(
                InstructorAttributes.updateOptionsWithGoogleIdBuilder(courseId, googleId)
                        .withIsArchived(archiveStatus)
                        .build()
        );
    }

    public InstructorAttributes getInstructorForEmail(String courseId, String email) {

        return instructorsDb.getInstructorForEmail(courseId, email);
    }

    public InstructorAttributes getInstructorById(String courseId, String email) {

        return instructorsDb.getInstructorById(courseId, email);
    }

    public InstructorAttributes getInstructorForGoogleId(String courseId, String googleId) {

        return instructorsDb.getInstructorForGoogleId(courseId, googleId);
    }

    public InstructorAttributes getInstructorForRegistrationKey(String encryptedKey) {

        return instructorsDb.getInstructorForRegistrationKey(encryptedKey);
    }

    public List<InstructorAttributes> getInstructorsForCourse(String courseId) {
        List<InstructorAttributes> instructorReturnList = instructorsDb.getInstructorsForCourse(courseId);
        instructorReturnList.sort(InstructorAttributes.COMPARE_BY_NAME);

        return instructorReturnList;
    }

    public List<InstructorAttributes> getInstructorsForGoogleId(String googleId) {

        return getInstructorsForGoogleId(googleId, false);
    }

    public List<InstructorAttributes> getInstructorsForGoogleId(String googleId, boolean omitArchived) {

        return instructorsDb.getInstructorsForGoogleId(googleId, omitArchived);
    }

    public String getEncryptedKeyForInstructor(String courseId, String email)
            throws EntityDoesNotExistException {

        verifyIsEmailOfInstructorOfCourse(email, courseId);

        InstructorAttributes instructor = getInstructorForEmail(courseId, email);

        return StringHelper.encrypt(instructor.key);
    }

    public boolean isGoogleIdOfInstructorOfCourse(String instructorId, String courseId) {

        return instructorsDb.getInstructorForGoogleId(courseId, instructorId) != null;
    }

    public boolean isEmailOfInstructorOfCourse(String instructorEmail, String courseId) {

        return instructorsDb.getInstructorForEmail(courseId, instructorEmail) != null;
    }

    /**
     * Returns whether the instructor is a new user, according to one of the following criteria:
     * <ul>
     * <li>There is only a sample course (created by system) for the instructor.</li>
     * <li>There is no any course for the instructor.</li>
     * </ul>
     */
    public boolean isNewInstructor(String googleId) {
        List<InstructorAttributes> instructorList = getInstructorsForGoogleId(googleId);
        return instructorList.isEmpty()
               || instructorList.size() == 1 && coursesLogic.isSampleCourse(instructorList.get(0).courseId);
    }

    public void verifyInstructorExists(String instructorId)
            throws EntityDoesNotExistException {

        if (!accountsLogic.isAccountAnInstructor(instructorId)) {
            throw new EntityDoesNotExistException("Instructor does not exist :"
                    + instructorId);
        }
    }

    public void verifyIsEmailOfInstructorOfCourse(String instructorEmail, String courseId)
            throws EntityDoesNotExistException {

        if (!isEmailOfInstructorOfCourse(instructorEmail, courseId)) {
            throw new EntityDoesNotExistException("Instructor " + instructorEmail
                    + " does not belong to course " + courseId);
        }
    }

    public void verifyAtLeastOneInstructorIsDisplayed(String courseId, boolean isOriginalInstructorDisplayed,
                                                      boolean isEditedInstructorDisplayed)
            throws InvalidParametersException {
        List<InstructorAttributes> instructorsDisplayed = instructorsDb.getInstructorsDisplayedToStudents(courseId);
        boolean isEditedInstructorChangedToNonVisible = isOriginalInstructorDisplayed && !isEditedInstructorDisplayed;
        boolean isNoInstructorMadeVisible = instructorsDisplayed.isEmpty() && !isEditedInstructorDisplayed;

        if (isNoInstructorMadeVisible || (instructorsDisplayed.size() == 1
                && isEditedInstructorChangedToNonVisible)) {
            throw new InvalidParametersException("At least one instructor must be displayed to students");
        }
    }

    /**
     * Updates an instructor by {@link InstructorAttributes.UpdateOptionsWithGoogleId}.
     *
     * <p>Cascade update the comments and responses given by the instructor.
     *
     * @return updated instructor
     * @throws InvalidParametersException if attributes to update are not valid
     * @throws EntityDoesNotExistException if the instructor cannot be found
     */
    public InstructorAttributes updateInstructorByGoogleIdCascade(
            InstructorAttributes.UpdateOptionsWithGoogleId updateOptions)
            throws InvalidParametersException, EntityDoesNotExistException {

        InstructorAttributes originalInstructor =
                instructorsDb.getInstructorForGoogleId(updateOptions.getCourseId(), updateOptions.getGoogleId());

        if (originalInstructor == null) {
            throw new EntityDoesNotExistException("Trying to update non-existent Entity: " + updateOptions);
        }

        InstructorAttributes newInstructor = originalInstructor.getCopy();
        newInstructor.update(updateOptions);

        boolean isOriginalInstructorDisplayed = originalInstructor.isDisplayedToStudents();
        verifyAtLeastOneInstructorIsDisplayed(originalInstructor.courseId, isOriginalInstructorDisplayed,
                newInstructor.isDisplayedToStudents());

        InstructorAttributes updatedInstructor = instructorsDb.updateInstructorByGoogleId(updateOptions);

        if (!originalInstructor.email.equals(updatedInstructor.email)) {
            // cascade responses
            List<FeedbackResponseAttributes> responsesFromUser =
                    frLogic.getFeedbackResponsesFromGiverForCourse(
                            originalInstructor.getCourseId(), originalInstructor.getEmail());
            for (FeedbackResponseAttributes responseFromUser : responsesFromUser) {
                FeedbackQuestionAttributes question = fqLogic.getFeedbackQuestion(responseFromUser.feedbackQuestionId);
                if (question.getGiverType() == FeedbackParticipantType.INSTRUCTORS
                        || question.getGiverType() == FeedbackParticipantType.SELF) {
                    try {
                        frLogic.updateFeedbackResponseCascade(
                                FeedbackResponseAttributes.updateOptionsBuilder(responseFromUser.getId())
                                        .withGiver(updatedInstructor.getEmail())
                                        .build());
                    } catch (EntityAlreadyExistsException e) {
                        log.severe("Fail to adjust 'from' responses when updating instructor: " + e.getMessage());
                    }
                }
            }
            List<FeedbackResponseAttributes> responsesToUser =
                    frLogic.getFeedbackResponsesForReceiverForCourse(
                            originalInstructor.getCourseId(), originalInstructor.getEmail());
            for (FeedbackResponseAttributes responseToUser : responsesToUser) {
                FeedbackQuestionAttributes question = fqLogic.getFeedbackQuestion(responseToUser.feedbackQuestionId);
                if (question.getRecipientType() == FeedbackParticipantType.INSTRUCTORS
                        || (question.getGiverType() == FeedbackParticipantType.INSTRUCTORS
                        && question.getRecipientType() == FeedbackParticipantType.SELF)) {
                    try {
                        frLogic.updateFeedbackResponseCascade(
                                FeedbackResponseAttributes.updateOptionsBuilder(responseToUser.getId())
                                        .withRecipient(updatedInstructor.getEmail())
                                        .build());
                    } catch (EntityAlreadyExistsException e) {
                        log.severe("Fail to adjust 'to' responses when updating instructor: " + e.getMessage());
                    }
                }
            }
            // cascade comments
            frcLogic.updateFeedbackResponseCommentsEmails(
                    updatedInstructor.courseId, originalInstructor.email, updatedInstructor.email);
            // cascade respondents
            fsLogic.updateRespondentsForInstructor(
                    originalInstructor.email, updatedInstructor.email, updatedInstructor.courseId);
        }

        return updatedInstructor;
    }

    /**
     * Updates an instructor by {@link InstructorAttributes.UpdateOptionsWithEmail}.
     *
     * @return updated instructor
     * @throws InvalidParametersException if attributes to update are not valid
     * @throws EntityDoesNotExistException if the instructor cannot be found
     */
    public InstructorAttributes updateInstructorByEmail(InstructorAttributes.UpdateOptionsWithEmail updateOptions)
            throws InvalidParametersException, EntityDoesNotExistException {
        Assumption.assertNotNull("Supplied parameter was null", updateOptions);

        InstructorAttributes originalInstructor =
                instructorsDb.getInstructorForEmail(updateOptions.getCourseId(), updateOptions.getEmail());

        if (originalInstructor == null) {
            throw new EntityDoesNotExistException("Trying to update non-existent Entity: " + updateOptions);
        }

        InstructorAttributes newInstructor = originalInstructor.getCopy();
        newInstructor.update(updateOptions);

        boolean isOriginalInstructorDisplayed = originalInstructor.isDisplayedToStudents();
        verifyAtLeastOneInstructorIsDisplayed(originalInstructor.courseId, isOriginalInstructorDisplayed,
                newInstructor.isDisplayedToStudents());

        return instructorsDb.updateInstructorByEmail(updateOptions);
    }

    public List<String> getInvalidityInfoForNewInstructorData(String name,
                                                              String institute, String email) {

        FieldValidator validator = new FieldValidator();
        List<String> errors = new ArrayList<>();
        String error;

        error = validator.getInvalidityInfoForPersonName(name);
        if (!error.isEmpty()) {
            errors.add(error);
        }

        error = validator.getInvalidityInfoForEmail(email);
        if (!error.isEmpty()) {
            errors.add(error);
        }

        error = validator.getInvalidityInfoForInstituteName(institute);
        if (!error.isEmpty()) {
            errors.add(error);
        }

        //No validation for isInstructor and createdAt fields.
        return errors;
    }

    public void deleteInstructorCascade(String courseId, String email) {
        fsLogic.deleteInstructorFromRespondentsList(getInstructorForEmail(courseId, email));
        instructorsDb.deleteInstructor(courseId, email);
    }

    public void deleteInstructorsForGoogleIdAndCascade(String googleId) {
        List<InstructorAttributes> instructors = instructorsDb.getInstructorsForGoogleId(googleId, false);

        //Cascade delete instructors
        for (InstructorAttributes instructor : instructors) {
            deleteInstructorCascade(instructor.courseId, instructor.email);
        }
    }

    // this method is only being used in course logic. cascade to comments is therefore not necessary
    // as it it taken care of when deleting course
    public void deleteInstructorsForCourse(String courseId) {

        instructorsDb.deleteInstructorsForCourse(courseId);
    }

    public List<InstructorAttributes> getCoOwnersForCourse(String courseId) {
        List<InstructorAttributes> instructors = getInstructorsForCourse(courseId);
        List<InstructorAttributes> instructorsWithCoOwnerPrivileges = new ArrayList<>();
        for (InstructorAttributes instructor : instructors) {
            if (!instructor.hasCoownerPrivileges()) {
                continue;
            }
            instructorsWithCoOwnerPrivileges.add(instructor);
        }
        return instructorsWithCoOwnerPrivileges;
    }

    /**
     * Resets the associated googleId of an instructor.
     */
    public void resetInstructorGoogleId(String originalEmail, String courseId) throws EntityDoesNotExistException {
        try {
            instructorsDb.updateInstructorByEmail(
                    InstructorAttributes.updateOptionsWithEmailBuilder(originalEmail, originalEmail)
                            .withGoogleId(null)
                            .build());
        } catch (InvalidParametersException e) {
            Assumption.fail("Unexpected invalid parameter.");
        }
    }

}
