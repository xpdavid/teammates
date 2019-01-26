package teammates.logic.api;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import com.google.appengine.api.blobstore.BlobKey;

import teammates.common.datatransfer.CourseDetailsBundle;
import teammates.common.datatransfer.CourseEnrollmentResult;
import teammates.common.datatransfer.CourseSummaryBundle;
import teammates.common.datatransfer.DataBundle;
import teammates.common.datatransfer.FeedbackResponseCommentSearchResultBundle;
import teammates.common.datatransfer.FeedbackSessionDetailsBundle;
import teammates.common.datatransfer.FeedbackSessionQuestionsBundle;
import teammates.common.datatransfer.FeedbackSessionResponseStatus;
import teammates.common.datatransfer.FeedbackSessionResultsBundle;
import teammates.common.datatransfer.InstructorPrivileges;
import teammates.common.datatransfer.InstructorSearchResultBundle;
import teammates.common.datatransfer.SectionDetailsBundle;
import teammates.common.datatransfer.StudentEnrollDetails;
import teammates.common.datatransfer.StudentSearchResultBundle;
import teammates.common.datatransfer.TeamDetailsBundle;
import teammates.common.datatransfer.attributes.AccountAttributes;
import teammates.common.datatransfer.attributes.CourseAttributes;
import teammates.common.datatransfer.attributes.FeedbackQuestionAttributes;
import teammates.common.datatransfer.attributes.FeedbackResponseAttributes;
import teammates.common.datatransfer.attributes.FeedbackResponseCommentAttributes;
import teammates.common.datatransfer.attributes.FeedbackSessionAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.datatransfer.attributes.StudentProfileAttributes;
import teammates.common.exception.EnrollException;
import teammates.common.exception.EntityAlreadyExistsException;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.ExceedingRangeException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.Assumption;
import teammates.common.util.GoogleCloudStorageHelper;
import teammates.common.util.SectionDetail;
import teammates.logic.core.AccountsLogic;
import teammates.logic.core.CoursesLogic;
import teammates.logic.core.DataBundleLogic;
import teammates.logic.core.FeedbackQuestionsLogic;
import teammates.logic.core.FeedbackResponseCommentsLogic;
import teammates.logic.core.FeedbackResponsesLogic;
import teammates.logic.core.FeedbackSessionsLogic;
import teammates.logic.core.InstructorsLogic;
import teammates.logic.core.ProfilesLogic;
import teammates.logic.core.StudentsLogic;

/**
 * Provides the business logic for production usage of the system.
 *
 * <p>This is a Facade class which simply forwards the method to internal classes.
 */
public class Logic {

    private static final AccountsLogic accountsLogic = AccountsLogic.inst();
    private static final StudentsLogic studentsLogic = StudentsLogic.inst();
    private static final InstructorsLogic instructorsLogic = InstructorsLogic.inst();
    private static final CoursesLogic coursesLogic = CoursesLogic.inst();
    private static final FeedbackSessionsLogic feedbackSessionsLogic = FeedbackSessionsLogic.inst();
    private static final FeedbackQuestionsLogic feedbackQuestionsLogic = FeedbackQuestionsLogic.inst();
    private static final FeedbackResponsesLogic feedbackResponsesLogic = FeedbackResponsesLogic.inst();
    private static final FeedbackResponseCommentsLogic feedbackResponseCommentsLogic =
            FeedbackResponseCommentsLogic.inst();
    private static final ProfilesLogic profilesLogic = ProfilesLogic.inst();
    private static final DataBundleLogic dataBundleLogic = DataBundleLogic.inst();

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public AccountAttributes getAccount(String googleId) {
        Assumption.assertNotNull(googleId);

        return accountsLogic.getAccount(googleId);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     *
     * @return Details of accounts with instruction privileges. Returns empty
     *         list if no such accounts are found.
     */
    @Deprecated //Not scalable.
    public List<AccountAttributes> getInstructorAccounts() {

        return accountsLogic.getInstructorAccounts();
    }

    /**
     * Updates/Creates the profile using {@link StudentProfileAttributes.UpdateOptions}.
     *
     * <br/> Preconditions: <br/>
     * * All parameters are non-null.
     *
     * @return updated student profile
     * @throws InvalidParametersException if attributes to update are not valid
     */
    public StudentProfileAttributes updateOrCreateStudentProfile(StudentProfileAttributes.UpdateOptions updateOptions)
            throws InvalidParametersException {

        Assumption.assertNotNull(updateOptions);

        return profilesLogic.updateOrCreateStudentProfile(updateOptions);
    }

    /**
     * Deletes both instructor and student privileges, as long as the account and associated student profile.
     *
     * <ul>
     * <li>Does not delete courses, which can result in orphan courses.</li>
     * <li>Fails silently if no such account.</li>
     * </ul>
     *
     * <p>Preconditions:</p>
     * * All parameters are non-null.
     */
    public void deleteAccount(String googleId) {

        Assumption.assertNotNull(googleId);

        accountsLogic.deleteAccountCascade(googleId);
    }

    /**
     * Delete the picture associated with the {@code key} in Cloud Storage.
     *
     * <br/> Preconditions: <br/>
     * All parameters are non-null.
     *
     * <p>Fails silently if the {@code key} doesn't exist.</p>
     */
    public void deletePicture(BlobKey key) {
        Assumption.assertNotNull(key);

        profilesLogic.deletePicture(key);
    }

    /**
     * Add an instructor for a course. <br>
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    @Deprecated
    public void addInstructor(String courseId, String name, String email, String role)
            throws InvalidParametersException, EntityAlreadyExistsException {

        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(name);
        Assumption.assertNotNull(email);

        InstructorAttributes instructor = InstructorAttributes.builder(null, courseId, name, email)
                .withRole(role).withPrivileges(new InstructorPrivileges(role))
                .build();
        instructorsLogic.createInstructor(instructor);
    }

    public InstructorAttributes createInstructor(InstructorAttributes instructor)
            throws InvalidParametersException, EntityAlreadyExistsException {
        return instructorsLogic.createInstructor(instructor);
    }

    /**
     * This method should be used by admin only since the searching does not restrict the
     * visibility according to the logged-in user's google ID. This is used by admin to
     * search instructors in the whole system.
     * @return Null if no match found.
     */
    public InstructorSearchResultBundle searchInstructorsInWholeSystem(String queryString) {
        Assumption.assertNotNull(queryString);

        return instructorsLogic.searchInstructorsInWholeSystem(queryString);
    }

    /**
     * Batch creates or updates documents for the given Instructors.
     *
     * @see InstructorsLogic#putDocuments(List)
     */
    public void putInstructorDocuments(List<InstructorAttributes> instructors) {
        instructorsLogic.putDocuments(instructors);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     * @return null if not found.
     */
    public InstructorAttributes getInstructorForEmail(String courseId, String email) {

        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(email);

        return instructorsLogic.getInstructorForEmail(courseId, email);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     * @return null if not found.
     */
    public InstructorAttributes getInstructorById(String courseId, String email) {

        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(email);

        return instructorsLogic.getInstructorById(courseId, email);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     * @return null if not found.
     */
    public InstructorAttributes getInstructorForGoogleId(String courseId, String googleId) {

        Assumption.assertNotNull(googleId);
        Assumption.assertNotNull(courseId);

        return instructorsLogic.getInstructorForGoogleId(courseId, googleId);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     * @return null if not found.
     */
    public InstructorAttributes getInstructorForRegistrationKey(String encryptedKey) {

        Assumption.assertNotNull(encryptedKey);

        return instructorsLogic.getInstructorForRegistrationKey(encryptedKey);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     * @return Empty list if none found.
     */
    public List<InstructorAttributes> getInstructorsForGoogleId(String googleId) {

        Assumption.assertNotNull(googleId);

        return instructorsLogic.getInstructorsForGoogleId(googleId);
    }

    public List<InstructorAttributes> getInstructorsForGoogleId(String googleId, boolean omitArchived) {

        Assumption.assertNotNull(googleId);

        return instructorsLogic.getInstructorsForGoogleId(googleId, omitArchived);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     * @return Empty list if none found.
     */
    public List<InstructorAttributes> getInstructorsForCourse(String courseId) {

        Assumption.assertNotNull(courseId);

        return instructorsLogic.getInstructorsForCourse(courseId);
    }

    /**
     * Get the encrypted registration key for the instructor.
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public String getEncryptedKeyForInstructor(String courseId, String email)
            throws EntityDoesNotExistException {

        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(email);

        return instructorsLogic.getEncryptedKeyForInstructor(courseId, email);
    }

    public List<FeedbackSessionAttributes> getAllOngoingSessions(Instant rangeStart, Instant rangeEnd) {

        return feedbackSessionsLogic.getAllOngoingSessions(rangeStart, rangeEnd);
    }

    /**
     * Returns true if this user has instructor privileges.
     */
    public boolean isInstructor(String googleId) {

        return accountsLogic.isAccountAnInstructor(googleId);
    }

    /**
     * Returns true if this user is an instructor of the course.
     */
    public boolean isInstructorOfCourse(String googleId, String courseId) {

        return instructorsLogic.isGoogleIdOfInstructorOfCourse(googleId, courseId);
    }

    /**
     * Returns true if the instructor is a new user.
     *
     * @see InstructorsLogic#isNewInstructor(String)
     */
    public boolean isNewInstructor(String googleId) {

        return instructorsLogic.isNewInstructor(googleId);
    }

    /**
     * Update the name and email address of an instructor with the specific Google ID.
     * Preconditions: <br>
     * * All parameters are non-null.
     * @param instr InstructorAttributes object containing the details to be updated
     */
    public void updateInstructorByGoogleId(String googleId, InstructorAttributes instr)
            throws InvalidParametersException, EntityDoesNotExistException {

        Assumption.assertNotNull(googleId);
        Assumption.assertNotNull(instr);

        instructorsLogic.updateInstructorByGoogleId(googleId, instr);
    }

    public void updateInstructorByEmail(String email, InstructorAttributes instr)
            throws InvalidParametersException, EntityDoesNotExistException {

        Assumption.assertNotNull(email);
        Assumption.assertNotNull(instr);

        instructorsLogic.updateInstructorByEmail(email, instr);
    }

    /**
     * Make the instructor join the course, i.e. associate the Google ID to the instructor.<br>
     * Create an account for the instructor if there is no account exist for him.
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public InstructorAttributes joinCourseForInstructor(String encryptedKey, String googleId, String institute)
            throws InvalidParametersException, EntityDoesNotExistException, EntityAlreadyExistsException {

        Assumption.assertNotNull(googleId);
        Assumption.assertNotNull(encryptedKey);

        return accountsLogic.joinCourseForInstructor(encryptedKey, googleId, institute);
    }

    public void verifyInputForAdminHomePage(String name, String institute, String email)
            throws InvalidParametersException {

        List<String> invalidityInfo = instructorsLogic.getInvalidityInfoForNewInstructorData(name,
                                                                                              institute, email);

        if (!invalidityInfo.isEmpty()) {
            throw new InvalidParametersException(invalidityInfo);
        }
    }

    /**
     * Removes instructor access but does not delete the account.
     * The account will continue to have student access. <br>
     * Fails silently if no match found.<br>
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public void downgradeInstructorToStudentCascade(String googleId) {

        Assumption.assertNotNull(googleId);

        accountsLogic.downgradeInstructorToStudentCascade(googleId);
    }

    /**
     * Fails silently if no match found.
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public void deleteInstructor(String courseId, String email) {

        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(email);

        instructorsLogic.deleteInstructorCascade(courseId, email);
    }

    /**
     * Creates a course and an instructor for it. <br>
     * Preconditions: <br>
     * * All parameters are non-null. <br>
     * * {@code instructorGoogleId} already has instructor privileges.
     */
    public void createCourseAndInstructor(String instructorGoogleId, String courseId, String courseName,
                                          String courseTimeZone)
            throws EntityAlreadyExistsException, InvalidParametersException {

        Assumption.assertNotNull(instructorGoogleId);
        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(courseName);
        Assumption.assertNotNull(courseTimeZone);

        coursesLogic.createCourseAndInstructor(instructorGoogleId, courseId, courseName, courseTimeZone);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     * @return null if not found.
     */
    public CourseAttributes getCourse(String courseId) {

        Assumption.assertNotNull(courseId);

        return coursesLogic.getCourse(courseId);
    }

    /**
     * Returns a detailed version of course data. <br>
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public CourseDetailsBundle getCourseDetails(String courseId) throws EntityDoesNotExistException {
        Assumption.assertNotNull(courseId);
        return coursesLogic.getCourseSummary(courseId);
    }

    /**
     * Returns a course data, including its feedback sessions, according to the instructor passed in.<br>
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public CourseSummaryBundle getCourseSummaryWithFeedbackSessions(InstructorAttributes instructor)
            throws EntityDoesNotExistException {
        Assumption.assertNotNull(instructor);
        return coursesLogic.getCourseSummaryWithFeedbackSessionsForInstructor(instructor);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public List<CourseAttributes> getCoursesForStudentAccount(String googleId) {
        Assumption.assertNotNull(googleId);
        return coursesLogic.getCoursesForStudentAccount(googleId);
    }

    /**
     * Omits archived courses if omitArchived == true<br>
     * Preconditions: <br>
     * * All parameters are non-null.
     * @return A less detailed version of courses for this instructor without stats.
     *         Returns an empty list if none found.
     */
    public Map<String, CourseSummaryBundle> getCourseSummariesWithoutStatsForInstructor(String googleId,
                                                                                            boolean omitArchived) {

        Assumption.assertNotNull(googleId);
        return coursesLogic.getCoursesSummaryWithoutStatsForInstructor(googleId, omitArchived);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     * @return A less detailed version of courses for this instructor.
     *         Returns an empty list if none found.
     */
    public Map<String, CourseDetailsBundle> getCourseSummariesForInstructor(String googleId)
            throws EntityDoesNotExistException {

        Assumption.assertNotNull(googleId);
        return coursesLogic.getCourseSummariesForInstructor(googleId, false);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     *
     * @return Details of courses the student is in. CourseData objects
     *         returned contain details of feedback sessions too (except the ones
     *         still AWAITING).
     */
    public List<CourseDetailsBundle> getCourseDetailsListForStudent(String googleId)
            throws EntityDoesNotExistException {

        Assumption.assertNotNull(googleId);
        return coursesLogic.getCourseDetailsListForStudent(googleId);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     *
     * @return Courses the instructor is in.
     */
    public List<CourseAttributes> getCoursesForInstructor(String googleId) {

        return getCoursesForInstructor(googleId, false);
    }

    /**
     * Omits archived courses if omitArchived == true<br>
     * Preconditions: <br>
     * * All parameters are non-null.
     *
     * @return Courses the instructor is in.
     */
    public List<CourseAttributes> getCoursesForInstructor(String googleId, boolean omitArchived) {

        Assumption.assertNotNull(googleId);
        return coursesLogic.getCoursesForInstructor(googleId, omitArchived);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     *
     * @return Courses the given instructors is in except for courses in Recycle Bin.
     */
    public List<CourseAttributes> getCoursesForInstructor(List<InstructorAttributes> instructorList) {

        Assumption.assertNotNull(instructorList);
        return coursesLogic.getCoursesForInstructor(instructorList);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     *
     * @return Courses in Recycle Bin that the given instructors is in.
     */
    public List<CourseAttributes> getSoftDeletedCoursesForInstructors(List<InstructorAttributes> instructorList) {

        Assumption.assertNotNull(instructorList);
        return coursesLogic.getSoftDeletedCoursesForInstructors(instructorList);
    }

    public CourseAttributes getSoftDeletedCourseForInstructor(InstructorAttributes instructor) {

        Assumption.assertNotNull(instructor);
        return coursesLogic.getSoftDeletedCourseForInstructor(instructor);
    }

    /**
     * Updates the details of a course.
     *
     * @see CoursesLogic#updateCourse(String, String, String)
     */
    public void updateCourse(String courseId, String courseName, String courseTimeZone)
            throws InvalidParametersException, EntityDoesNotExistException {
        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(courseName);
        Assumption.assertNotNull(courseTimeZone);
        coursesLogic.updateCourse(courseId, courseName, courseTimeZone);
    }

    /**
     * Change the archive status of a course for a instructor.<br>
     * Preconditions: <br>
     * * All parameters are non-null.
     * @param courseId The course of which the archive status is to be changed
     * @param archiveStatus The archive status to be set
     */

    public void setArchiveStatusOfInstructor(String googleId, String courseId, boolean archiveStatus)
            throws InvalidParametersException, EntityDoesNotExistException {

        Assumption.assertNotNull(googleId);
        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(archiveStatus);

        instructorsLogic.setArchiveStatusOfInstructor(googleId, courseId, archiveStatus);
    }

    /**
     * Permanently deletes a course and all data related to the course
     * (instructors, students, feedback sessions) from Recycle Bin.
     * Fails silently if no such account. <br>
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public void deleteCourse(String courseId) {
        Assumption.assertNotNull(courseId);
        coursesLogic.deleteCourseCascade(courseId);
    }

    /**
     * Permanently deletes all courses and all data related to these courses
     * (instructors, students, feedback sessions) from Recycle Bin.
     */
    public void deleteAllCourses(List<InstructorAttributes> instructorList) {
        Assumption.assertNotNull(instructorList);
        coursesLogic.deleteAllCoursesCascade(instructorList);
    }

    /**
     * Checks if the course is present in the system based on its courseid.
     */
    public boolean isCoursePresent(String courseid) {
        return coursesLogic.isCoursePresent(courseid);
    }

    /**
     * Moves a course to Recycle Bin by its given corresponding ID.
     * All data related will not be deleted.
     */
    public void moveCourseToRecycleBin(String courseId) throws InvalidParametersException, EntityDoesNotExistException {
        Assumption.assertNotNull(courseId);
        coursesLogic.moveCourseToRecycleBin(courseId);
    }

    /**
     * Restores a course and all data related to the course from Recycle Bin by
     * its given corresponding ID.
     */
    public void restoreCourseFromRecycleBin(String courseId)
            throws InvalidParametersException, EntityDoesNotExistException {
        Assumption.assertNotNull(courseId);
        coursesLogic.restoreCourseFromRecycleBin(courseId);
    }

    /**
     * Restores all courses and all data related to these courses from Recycle Bin.
     */
    public void restoreAllCoursesFromRecycleBin(List<InstructorAttributes> instructorList)
            throws InvalidParametersException, EntityDoesNotExistException {
        Assumption.assertNotNull(instructorList);
        coursesLogic.restoreAllCoursesFromRecycleBin(instructorList);
    }

    /**
     * Creates a student. <br>
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public void createStudent(StudentAttributes student)
            throws EntityAlreadyExistsException, InvalidParametersException, EntityDoesNotExistException {

        Assumption.assertNotNull(student);
        studentsLogic.createStudentCascade(student);
    }

    /**
     * Search for students. Preconditions: all parameters are non-null.
     * @param instructors   a list of InstructorAttributes associated to a googleId,
     *                      used for filtering of search result
     * @return Null if no match found
     */
    public StudentSearchResultBundle searchStudents(String queryString, List<InstructorAttributes> instructors) {
        Assumption.assertNotNull(queryString);
        Assumption.assertNotNull(instructors);
        return studentsLogic.searchStudents(queryString, instructors);
    }

    /**
     * This method should be used by admin only since the searching does not restrict the
     * visibility according to the logged-in user's google ID. This is used by admin to
     * search students in the whole system.
     * @return Null if no match found.
     */
    public StudentSearchResultBundle searchStudentsInWholeSystem(String queryString) {
        Assumption.assertNotNull(queryString);

        return studentsLogic.searchStudentsInWholeSystem(queryString);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     *
     * @return Null if no match found.
     */
    public StudentAttributes getStudentForRegistrationKey(String registrationKey) {
        Assumption.assertNotNull(registrationKey);
        return studentsLogic.getStudentForRegistrationKey(registrationKey);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     *
     * @return Null if no match found.
     */
    public StudentAttributes getStudentForEmail(String courseId, String email) {
        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(email);

        return studentsLogic.getStudentForEmail(courseId, email);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     *
     * @return Null if no match found.
     */
    public StudentAttributes getStudentForGoogleId(String courseId, String googleId) {
        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(googleId);

        return studentsLogic.getStudentForCourseIdAndGoogleId(courseId, googleId);
    }

    /**
     * Gets student profile associated with the {@code googleId}.
     *
     * <br/> Preconditions: <br/>
     * * All parameters are non-null.
     *
     * @return null if no match found.
     */
    public StudentProfileAttributes getStudentProfile(String googleId) {
        Assumption.assertNotNull(googleId);
        return profilesLogic.getStudentProfile(googleId);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     *
     * @return Empty list if no match found.
     */
    public List<StudentAttributes> getStudentsForGoogleId(String googleId) {
        Assumption.assertNotNull(googleId);
        return studentsLogic.getStudentsForGoogleId(googleId);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     * @return Empty list if none found.
     */
    public List<StudentAttributes> getStudentsForCourse(String courseId) {
        Assumption.assertNotNull(courseId);
        return studentsLogic.getStudentsForCourse(courseId);
    }

    /**
     * Returns a list of section names for the course with ID courseId.
     *
     * <p>Preconditions: <br>
     * * All parameters are non-null.
     *
     * @see CoursesLogic#getSectionsNameForCourse(String)
     */
    public List<String> getSectionNamesForCourse(String courseId) throws EntityDoesNotExistException {
        Assumption.assertNotNull(courseId);
        return coursesLogic.getSectionsNameForCourse(courseId);
    }

    /**
     * Returns a list of {@link SectionDetailsBundle} for a given course using courseId.
     *
     * <p>Preconditions: <br>
     * * All parameters are non-null.
     *
     * @see CoursesLogic#getSectionsForCourseWithoutStats(String)
     */
    public List<SectionDetailsBundle> getSectionsForCourse(String courseId) throws EntityDoesNotExistException {
        Assumption.assertNotNull(courseId);
        return coursesLogic.getSectionsForCourseWithoutStats(courseId);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public List<TeamDetailsBundle> getTeamsForCourse(String courseId) throws EntityDoesNotExistException {
        Assumption.assertNotNull(courseId);
        return coursesLogic.getTeamsForCourse(courseId);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public TeamDetailsBundle getTeamDetailsForStudent(StudentAttributes student) {
        Assumption.assertNotNull(student);
        return studentsLogic.getTeamDetailsForStudent(student);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public String getEncryptedKeyForStudent(String courseId, String email) throws EntityDoesNotExistException {

        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(email);

        return studentsLogic.getEncryptedKeyForStudent(courseId, email);
    }

    public void resetStudentGoogleId(String originalEmail, String courseId) throws EntityDoesNotExistException {
        Assumption.assertNotNull(originalEmail);
        Assumption.assertNotNull(courseId);
        studentsLogic.resetStudentGoogleId(originalEmail, courseId);
    }

    public void resetInstructorGoogleId(String originalEmail, String courseId) throws EntityDoesNotExistException {
        Assumption.assertNotNull(originalEmail);
        Assumption.assertNotNull(courseId);
        instructorsLogic.resetInstructorGoogleId(originalEmail, courseId);
    }

    /**
     * All attributes except courseId be changed. Trying to change courseId will
     * be treated as trying to edit a student in a different course.<br>
     * Changing team name will not delete existing submissions under that team <br>
     * Cascade logic: Email changed-> changes email in all existing submissions <br>
     * Team changed-> creates new submissions for the new team, deletes
     * submissions for previous team structure. <br>
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public void updateStudent(String originalEmail, StudentAttributes student)
            throws InvalidParametersException, EntityDoesNotExistException {

        Assumption.assertNotNull(originalEmail);
        Assumption.assertNotNull(student);

        studentsLogic.updateStudentCascade(originalEmail, student);
    }

    /**
     * Make the student join the course, i.e. associate the Google ID to the student.<br>
     * Create an account for the student if there is no account exist for him.
     * Preconditions: <br>
     * * All parameters are non-null.
     * @param key the encrypted registration key
     */
    public StudentAttributes joinCourseForStudent(String key, String googleId)
            throws InvalidParametersException, EntityDoesNotExistException, EntityAlreadyExistsException {

        Assumption.assertNotNull(googleId);
        Assumption.assertNotNull(key);

        return accountsLogic.joinCourseForStudent(key, googleId);

    }

    /**
     * Enrolls new students in the course or modifies existing students. But it
     * will not delete any students. It will not edit email address either. If
     * an existing student was enrolled with a different email address, that
     * student will be treated as a new student.<br>
     * If there is an error in the enrollLines, there will be no changes to the
     * datastore <br>
     * Preconditions: <br>
     * * All parameters are non-null.
     * @return StudentData objects in the return value contains the status of
     *         enrollment. It also includes data for other students in the
     *         course that were not touched by the operation.
     */
    public CourseEnrollmentResult enrollStudents(String enrollLines, String courseId)
            throws EnrollException, EntityDoesNotExistException, InvalidParametersException, EntityAlreadyExistsException {

        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(enrollLines);

        return studentsLogic.enrollStudents(enrollLines.trim(), courseId);

    }

    public List<StudentAttributes> getUnregisteredStudentsForCourse(String courseId) {
        Assumption.assertNotNull(courseId);
        return studentsLogic.getUnregisteredStudentsForCourse(courseId);
    }

    public boolean isFeedbackSessionCompletedByInstructor(FeedbackSessionAttributes fsa, String userEmail)
            throws EntityDoesNotExistException {
        Assumption.assertNotNull(fsa);
        Assumption.assertNotNull(userEmail);
        return feedbackSessionsLogic.isFeedbackSessionCompletedByInstructor(fsa, userEmail);
    }

    public boolean isFeedbackSessionCompletedByStudent(FeedbackSessionAttributes fsa, String userEmail) {
        Assumption.assertNotNull(fsa);
        Assumption.assertNotNull(userEmail);
        return feedbackSessionsLogic.isFeedbackSessionCompletedByStudent(fsa, userEmail);
    }

    /**
     * Deletes the student from the course including any submissions to/from
     * for this student in this course.
     * Fails silently if no match found. <br>
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public void deleteStudent(String courseId, String studentEmail) {

        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(studentEmail);

        studentsLogic.deleteStudentCascade(courseId, studentEmail);
    }

    /**
     * Deletes all the students in the course.
     *
     * @param courseId course id for the students
     */
    public void deleteAllStudentsInCourse(String courseId) {

        Assumption.assertNotNull(courseId);
        studentsLogic.deleteAllStudentsInCourse(courseId);
    }

    /**
     *  Checks if a course has sections for each team
     *  Preconditions: <br>
     *  * All parameters are non-null.
     */
    public boolean hasIndicatedSections(String courseId) throws EntityDoesNotExistException {
        Assumption.assertNotNull(courseId);
        return coursesLogic.hasIndicatedSections(courseId);
    }

    /**
     * Validates sections for any limit violations and teams for any team name violations.
     *
     * <p>Preconditions: <br>
     * * All parameters are non-null.
     *
     * @see StudentsLogic#validateSectionsAndTeams(List, String)
     */
    public void validateSectionsAndTeams(List<StudentAttributes> studentList, String courseId) throws EnrollException {

        Assumption.assertNotNull(studentList);
        Assumption.assertNotNull(courseId);

        studentsLogic.validateSectionsAndTeams(studentList, courseId);
    }

    /**
     * Validates teams for any team name violations.
     *
     * <p>Preconditions: <br>
     * * All parameters are non-null.
     *
     * @see StudentsLogic#validateTeams(List, String)
     */
    public void validateTeams(List<StudentAttributes> studentList, String courseId) throws EnrollException {

        Assumption.assertNotNull(studentList);
        Assumption.assertNotNull(courseId);

        studentsLogic.validateTeams(studentList, courseId);
    }

    /**
     * Gets all students of a team.
     */
    public List<StudentAttributes> getStudentsForTeam(String teamName, String courseId) {
        Assumption.assertNotNull(teamName);
        Assumption.assertNotNull(courseId);

        return studentsLogic.getStudentsForTeam(teamName, courseId);
    }

    /**
     * Batch creates or updates search documents for the given students.
     */
    public void putStudentDocuments(List<StudentAttributes> students) {
        studentsLogic.putDocuments(students);
    }

    /**
     * Generates students list of a course in CSV format. <br>
     * Preconditions: <br>
     * * All parameters are non-null. <br>
     */
    public String getCourseStudentListAsCsv(String courseId, String googleId) throws EntityDoesNotExistException {

        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(googleId);

        return coursesLogic.getCourseStudentListAsCsv(courseId, googleId);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public void createFeedbackSession(FeedbackSessionAttributes feedbackSession)
            throws EntityAlreadyExistsException, InvalidParametersException {
        Assumption.assertNotNull(feedbackSession);
        feedbackSessionsLogic.createFeedbackSession(feedbackSession);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public FeedbackSessionAttributes copyFeedbackSession(String newFeedbackSessionName, String newCourseId,
            ZoneId newTimeZone, String feedbackSessionName, String courseId, String instructorEmail)
            throws EntityAlreadyExistsException, InvalidParametersException, EntityDoesNotExistException {

        Assumption.assertNotNull(newFeedbackSessionName);
        Assumption.assertNotNull(newCourseId);
        Assumption.assertNotNull(newTimeZone);
        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(instructorEmail);

        return feedbackSessionsLogic.copyFeedbackSession(newFeedbackSessionName, newCourseId, newTimeZone,
                feedbackSessionName, courseId, instructorEmail);
    }

    /**
     * Gets a feedback session from the data storage.
     *
     * <br/>Preconditions: <br/>
     * * All parameters are non-null.
     *
     * @return null if not found or in recycle bin.
     */
    public FeedbackSessionAttributes getFeedbackSession(String feedbackSessionName, String courseId) {

        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);

        return feedbackSessionsLogic.getFeedbackSession(feedbackSessionName, courseId);
    }

    /**
     * Gets a feedback session from the recycle bin.
     *
     * <br/>Preconditions: <br/>
     * * All parameters are non-null.
     *
     * @return null if not found.
     */
    public FeedbackSessionAttributes getFeedbackSessionFromRecycleBin(String feedbackSessionName, String courseId) {
        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);

        return feedbackSessionsLogic.getFeedbackSessionFromRecycleBin(feedbackSessionName, courseId);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public List<FeedbackSessionAttributes> getFeedbackSessionsForCourse(String courseId) {
        Assumption.assertNotNull(courseId);
        return feedbackSessionsLogic.getFeedbackSessionsForCourse(courseId);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public FeedbackSessionDetailsBundle getFeedbackSessionDetails(String feedbackSessionName, String courseId)
            throws EntityDoesNotExistException {

        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);

        return feedbackSessionsLogic.getFeedbackSessionDetails(feedbackSessionName, courseId);
    }

    /**
     * Returns a {@code List} of all feedback sessions WITHOUT their response
     * statistics for a instructor given by his googleId.
     *
     * <p>Preconditions: <br>
     * * All parameters are non-null.
     *
     * @see FeedbackSessionsLogic#getFeedbackSessionsListForInstructor(String, boolean)
     */
    public List<FeedbackSessionAttributes> getFeedbackSessionsListForInstructor(String googleId, boolean omitArchived) {
        Assumption.assertNotNull(googleId);
        return feedbackSessionsLogic.getFeedbackSessionsListForInstructor(googleId, omitArchived);
    }

    public List<FeedbackSessionAttributes> getFeedbackSessionsListForInstructor(
            List<InstructorAttributes> instructorList) {
        Assumption.assertNotNull(instructorList);
        return feedbackSessionsLogic.getFeedbackSessionsListForInstructor(instructorList);
    }

    /**
     * Returns a {@code List} of all feedback sessions in Recycle Bin WITHOUT their response
     * statistics for a instructor.
     *
     * <p>Preconditions: <br>
     * * All parameters are non-null.
     */
    public List<FeedbackSessionAttributes> getSoftDeletedFeedbackSessionsListForInstructor(InstructorAttributes instructor) {
        Assumption.assertNotNull(instructor);
        return feedbackSessionsLogic.getSoftDeletedFeedbackSessionsListForInstructor(instructor);
    }

    public List<FeedbackSessionAttributes> getSoftDeletedFeedbackSessionsListForInstructors(
            List<InstructorAttributes> instructorList) {
        Assumption.assertNotNull(instructorList);
        return feedbackSessionsLogic.getSoftDeletedFeedbackSessionsListForInstructors(instructorList);
    }

    /**
     * Gets {@code FeedbackQuestions} and previously filled
     * {@code FeedbackResponses} that an instructor can view/submit as a
     * {@link FeedbackSessionQuestionsBundle}.
     *
     * <p>Preconditions: <br>
     * * All parameters are non-null.
     *
     * @see FeedbackSessionsLogic#getFeedbackSessionQuestionsForInstructor(String, String, String)
     */
    public FeedbackSessionQuestionsBundle getFeedbackSessionQuestionsBundleForInstructor(String feedbackSessionName,
                                                                                         String courseId,
                                                                                         String userEmail)
            throws EntityDoesNotExistException {

        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(userEmail);

        return feedbackSessionsLogic.getFeedbackSessionQuestionsForInstructor(feedbackSessionName, courseId, userEmail);
    }

    /**
     * Gets {@code FeedbackQuestions} and previously filled
     * {@code FeedbackResponses} that a student can view/submit as a
     * {@link FeedbackSessionQuestionsBundle}.
     *
     * <p>Preconditions: <br>
     * * All parameters are non-null.
     *
     * @see FeedbackSessionsLogic#getFeedbackSessionQuestionsForStudent(String, String, String)
     */
    public FeedbackSessionQuestionsBundle getFeedbackSessionQuestionsBundleForStudent(String feedbackSessionName,
                                                                                      String courseId,
                                                                                      String userEmail)
            throws EntityDoesNotExistException {

        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(userEmail);

        return feedbackSessionsLogic.getFeedbackSessionQuestionsForStudent(feedbackSessionName, courseId, userEmail);
    }

    /**
     * Gets the recipients of a feedback question for student.
     *
     * @see FeedbackQuestionsLogic#getRecipientsOfQuestionForStudent(FeedbackQuestionAttributes, String, String)
     */
    public Map<String, String> getRecipientsOfQuestionForStudent(
            FeedbackQuestionAttributes question, String giverEmail, String giverTeam) {
        Assumption.assertNotNull(question);
        Assumption.assertNotNull(giverEmail);
        Assumption.assertNotNull(giverTeam);

        return feedbackQuestionsLogic.getRecipientsOfQuestionForStudent(question, giverEmail, giverTeam);
    }

    /**
     * Gets the recipients of a feedback question for instructor.
     *
     * @see FeedbackQuestionsLogic#getRecipientsOfQuestionForInstructor(FeedbackQuestionAttributes, String)
     */
    public Map<String, String> getRecipientsOfQuestionForInstructor(FeedbackQuestionAttributes question, String giverEmail) {
        Assumption.assertNotNull(question);
        Assumption.assertNotNull(giverEmail);

        return feedbackQuestionsLogic.getRecipientsOfQuestionForInstructor(question, giverEmail);
    }

    public FeedbackQuestionAttributes getFeedbackQuestion(String feedbackSessionName,
                                                          String courseId,
                                                          int questionNumber) {
        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);

        return feedbackQuestionsLogic.getFeedbackQuestion(feedbackSessionName, courseId, questionNumber);
    }

    /**
     * Gets a list of all questions for the given session that
     * students can view/submit.
     */
    public List<FeedbackQuestionAttributes> getFeedbackQuestionsForStudents(
            String feedbackSessionName, String courseId) {
        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);

        return feedbackQuestionsLogic.getFeedbackQuestionsForStudents(feedbackSessionName, courseId);
    }

    /**
     * Gets a {@code List} of all questions for the given session that
     * instructor can view/submit.
     */
    public List<FeedbackQuestionAttributes> getFeedbackQuestionsForInstructors(
            String feedbackSessionName, String courseId, String instructorEmail) throws EntityDoesNotExistException {
        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);

        return feedbackQuestionsLogic.getFeedbackQuestionsForInstructor(feedbackSessionName, courseId, instructorEmail);
    }


    /**
     * Preconditions: <br>
     * * All parameters are non-null. <br>
     *
     */
    public FeedbackQuestionAttributes getFeedbackQuestion(String feedbackQuestionId) {
        Assumption.assertNotNull(feedbackQuestionId);
        return feedbackQuestionsLogic.getFeedbackQuestion(feedbackQuestionId);
    }

    /**
     * Generates summary results (without comments) in CSV format. <br>
     * Preconditions: <br>
     * * All parameters(except questionId) are non-null. <br>
     * @see FeedbackSessionsLogic#getFeedbackSessionResultsSummaryAsCsv(String, String,
     *      String, String, boolean, boolean)
     */
    public String getFeedbackSessionResultSummaryAsCsv(
            String courseId, String feedbackSessionName, String instructorEmail,
            boolean isMissingResponsesShown, boolean isStatsShown, String questionId)
            throws EntityDoesNotExistException, ExceedingRangeException {

        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(feedbackSessionName);

        return feedbackSessionsLogic.getFeedbackSessionResultsSummaryAsCsv(
                feedbackSessionName, courseId, instructorEmail, questionId,
                isMissingResponsesShown, isStatsShown);
    }

    /**
     * Generates summary results (without comments) within a section in CSV format. <br>
     * Preconditions: <br>
     * * All parameters(except questionId) are non-null. <br>
     * @see FeedbackSessionsLogic#getFeedbackSessionResultsSummaryInSectionAsCsv(String, String, String,
     *      String, SectionDetail, String, boolean, boolean)
     */
    public String getFeedbackSessionResultSummaryInSectionAsCsv(
            String courseId, String feedbackSessionName, String instructorEmail, String section,
            SectionDetail sectionDetail, String questionId, boolean isMissingResponsesShown, boolean isStatsShown)
            throws EntityDoesNotExistException, ExceedingRangeException {

        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(section);
        Assumption.assertNotNull(sectionDetail);

        return feedbackSessionsLogic.getFeedbackSessionResultsSummaryInSectionAsCsv(
                feedbackSessionName, courseId, instructorEmail, section, sectionDetail,
                questionId, isMissingResponsesShown, isStatsShown);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     * @return a list of viewable feedback sessions for any user in the course.
     */
    public List<FeedbackSessionAttributes> getFeedbackSessionsForUserInCourse(String courseId, String userEmail)
            throws EntityDoesNotExistException {
        Assumption.assertNotNull(userEmail);
        Assumption.assertNotNull(courseId);

        return feedbackSessionsLogic.getFeedbackSessionsForUserInCourse(courseId, userEmail);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public boolean hasStudentSubmittedFeedback(FeedbackSessionAttributes fsa, String studentEmail) {

        Assumption.assertNotNull(fsa);
        Assumption.assertNotNull(studentEmail);

        return feedbackSessionsLogic.isFeedbackSessionCompletedByStudent(fsa, studentEmail);
    }

    /**
     * Updates the details of a feedback session <br>
     * Does not affect the questions and responses associated with it.
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public void updateFeedbackSession(FeedbackSessionAttributes updatedSession) throws InvalidParametersException,
                                                                                       EntityDoesNotExistException {

        Assumption.assertNotNull(updatedSession);
        feedbackSessionsLogic.updateFeedbackSession(updatedSession);
    }

    public void updateRespondents(String feedbackSessionName, String courseId) throws InvalidParametersException,
                                                                                      EntityDoesNotExistException {
        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);

        feedbackSessionsLogic.updateRespondentsForSession(feedbackSessionName, courseId);
    }

    public void addInstructorRespondent(String email, String feedbackSessionName, String courseId)
            throws EntityDoesNotExistException, InvalidParametersException {

        Assumption.assertNotNull(email);
        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);

        feedbackSessionsLogic.addInstructorRespondent(email, feedbackSessionName, courseId);
    }

    public void addStudentRespondent(String email, String feedbackSessionName, String courseId)
            throws EntityDoesNotExistException, InvalidParametersException {

        Assumption.assertNotNull(email);
        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);

        feedbackSessionsLogic.addStudentRespondent(email, feedbackSessionName, courseId);
    }

    public void deleteInstructorRespondent(String email, String feedbackSessionName, String courseId)
            throws EntityDoesNotExistException, InvalidParametersException {

        Assumption.assertNotNull(email);
        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);

        feedbackSessionsLogic.deleteInstructorRespondent(email, feedbackSessionName, courseId);
    }

    public void deleteStudentRespondent(String email, String feedbackSessionName, String courseId)
            throws EntityDoesNotExistException, InvalidParametersException {

        Assumption.assertNotNull(email);
        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);

        feedbackSessionsLogic.deleteStudentFromRespondentList(email, feedbackSessionName, courseId);
    }

    /**
     * Publishes the feedback session and send email alerts to students.
     * Preconditions: <br>
     * * All parameters are non-null. <br>
     * @throws InvalidParametersException
     *             if the feedback session is not ready to be published.
     */
    public void publishFeedbackSession(FeedbackSessionAttributes session)
            throws EntityDoesNotExistException, InvalidParametersException {

        Assumption.assertNotNull(session);

        feedbackSessionsLogic.publishFeedbackSession(session);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null. <br>
     * @throws InvalidParametersException
     *             if the feedback session is not ready to be unpublished.
     */
    public void unpublishFeedbackSession(FeedbackSessionAttributes session)
            throws EntityDoesNotExistException, InvalidParametersException {

        Assumption.assertNotNull(session);

        feedbackSessionsLogic.unpublishFeedbackSession(session);
    }

    /**
     * Permanently deletes the feedback session in Recycle Bin, but not the questions and
     * responses associated to it.
     * Fails silently if no such feedback session. <br>
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public void deleteFeedbackSession(String feedbackSessionName, String courseId) {

        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);

        feedbackSessionsLogic.deleteFeedbackSessionCascade(feedbackSessionName, courseId);
    }

    /**
     * Permanently deletes feedback sessions in Recycle Bin, but not the questions and
     * responses associated to them.
     * Fails silently if no such feedback session. <br>
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public void deleteAllFeedbackSessions(List<InstructorAttributes> instructorList) {

        Assumption.assertNotNull(instructorList);

        feedbackSessionsLogic.deleteAllFeedbackSessionsCascade(instructorList);
    }

    /**
     * Soft-deletes a specific session to Recycle Bin.
     */
    public void moveFeedbackSessionToRecycleBin(String feedbackSessionName, String courseId)
            throws InvalidParametersException, EntityDoesNotExistException {

        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);

        feedbackSessionsLogic.moveFeedbackSessionToRecycleBin(feedbackSessionName, courseId);
    }

    /**
     * Restores a specific session from Recycle Bin to feedback sessions table.
     */
    public void restoreFeedbackSessionFromRecycleBin(String feedbackSessionName, String courseId)
            throws InvalidParametersException, EntityDoesNotExistException {

        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);

        feedbackSessionsLogic.restoreFeedbackSessionFromRecycleBin(feedbackSessionName, courseId);
    }

    /**
     * Restores all sessions from Recycle Bin to feedback sessions table.
     */
    public void restoreAllFeedbackSessionsFromRecycleBin(List<InstructorAttributes> instructorList)
            throws InvalidParametersException, EntityDoesNotExistException {

        Assumption.assertNotNull(instructorList);

        feedbackSessionsLogic.restoreAllFeedbackSessionsFromRecycleBin(instructorList);
    }

    /**
     * Creates a new feedback question.
     *
     * @return the created question
     */
    public FeedbackQuestionAttributes createFeedbackQuestion(FeedbackQuestionAttributes feedbackQuestion)
            throws InvalidParametersException {
        Assumption.assertNotNull(feedbackQuestion);
        return feedbackQuestionsLogic.createFeedbackQuestion(feedbackQuestion);
    }

    /**
     * Used for creating initial questions for template sessions only.
     * Does not check if feedback session exists.
     * Does not check if question number supplied is valid(does not check for clashes, or make adjustments)
     * Preconditions: <br>
     * * All parameters are non-null.
     * * questionNumber is > 0
     */
    public FeedbackQuestionAttributes createFeedbackQuestionForTemplate(
            FeedbackQuestionAttributes feedbackQuestion, int questionNumber)
            throws InvalidParametersException {

        Assumption.assertNotNull(feedbackQuestion);
        Assumption.assertTrue(questionNumber > 0);
        return feedbackQuestionsLogic.createFeedbackQuestionNoIntegrityCheck(feedbackQuestion, questionNumber);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public FeedbackQuestionAttributes copyFeedbackQuestion(String feedbackQuestionId, String feedbackSessionName,
                                                           String courseId)
            throws InvalidParametersException {

        Assumption.assertNotNull(feedbackQuestionId);
        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);

        return feedbackQuestionsLogic.copyFeedbackQuestion(feedbackQuestionId, feedbackSessionName, courseId);
    }

    /**
     * Updates the question number of a Feedback Question.<br>
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public void updateFeedbackQuestionNumber(FeedbackQuestionAttributes updatedQuestion)
            throws InvalidParametersException, EntityDoesNotExistException {

        Assumption.assertNotNull(updatedQuestion);
        feedbackQuestionsLogic.updateFeedbackQuestionNumber(updatedQuestion);
    }

    /**
     * Updates the details of a Feedback Question.<br>
     * The FeedbackQuestionAttributes should have the updated attributes
     * together with the original ID of the question. Preserves null
     * attributes.
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public void updateFeedbackQuestion(FeedbackQuestionAttributes updatedQuestion)
            throws InvalidParametersException, EntityDoesNotExistException {

        Assumption.assertNotNull(updatedQuestion);
        feedbackQuestionsLogic.updateFeedbackQuestion(updatedQuestion);
    }

    /**
     * Deletes the feedback question and the responses associated to it. Fails
     * silently if there is no such feedback question. <br>
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public void deleteFeedbackQuestion(String questionId) {
        Assumption.assertNotNull(questionId);
        feedbackQuestionsLogic.deleteFeedbackQuestionCascade(questionId);
    }

    /**
     * Returns true if there is at least one response for the given feedback question,
     * false if not.
     * for the session.
     * Preconditions: <br>
     * * All parameters are non-null.
     */

    public boolean areThereResponsesForQuestion(String feedbackQuestionId) {
        return feedbackQuestionsLogic.areThereResponsesForQuestion(feedbackQuestionId);
    }

    /**
     * Gets all copiable questions for an instructor<br>
     * Returns an empty list if they are no questions
     * for the session.
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public List<FeedbackQuestionAttributes> getCopiableFeedbackQuestionsForInstructor(String googleId)
            throws EntityDoesNotExistException {
        Assumption.assertNotNull(googleId);
        return feedbackQuestionsLogic.getCopiableFeedbackQuestionsForInstructor(googleId);
    }

    /**
     * Generates template feedback questions for an instructor in the session.<br>
     * Returns an empty list if there are no questions for the template
     * for the session.
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public List<FeedbackQuestionAttributes> populateFeedbackSessionTemplateQuestions(String templateType, String courseId,
            String feedbackSessionName) {
        Assumption.assertNotNull(templateType);
        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(feedbackSessionName);
        return feedbackQuestionsLogic.getFeedbackSessionTemplateQuestions(templateType, courseId, feedbackSessionName);
    }

    /**
     * Gets all questions for a feedback session.<br>
     * Returns an empty list if they are no questions
     * for the session.
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public List<FeedbackQuestionAttributes> getFeedbackQuestionsForSession(String feedbackSessionName, String courseId) {
        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);

        return feedbackQuestionsLogic.getFeedbackQuestionsForSession(feedbackSessionName, courseId);
    }

    /**
     * Gets the response rate status for a session.
     *
     * <p>Preconditions: <br>
     * * All parameters are non-null.
     *
     * @see FeedbackSessionsLogic#getFeedbackSessionResponseStatus(String, String)
     */
    public FeedbackSessionResponseStatus getFeedbackSessionResponseStatus(String feedbackSessionName, String courseId)
            throws EntityDoesNotExistException {

        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);

        return feedbackSessionsLogic.getFeedbackSessionResponseStatus(feedbackSessionName, courseId);
    }

    /**
     * Gets a question+response bundle for questions with responses that
     * is visible to the student for a feedback session.
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public FeedbackSessionResultsBundle getFeedbackSessionResultsForStudent(String feedbackSessionName,
                                                                            String courseId, String userEmail)
            throws EntityDoesNotExistException {
        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(userEmail);

        return feedbackSessionsLogic.getFeedbackSessionResultsForStudent(feedbackSessionName, courseId, userEmail);
    }

    /**
     * Gets a question+response bundle for questions with responses that
     * is visible to the instructor for a feedback session within the given range
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public FeedbackSessionResultsBundle getFeedbackSessionResultsForInstructorWithinRangeFromView(
            String feedbackSessionName, String courseId, String userEmail, int range, String viewType)
            throws EntityDoesNotExistException {

        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(userEmail);
        Assumption.assertNotNull(viewType);

        return feedbackSessionsLogic.getFeedbackSessionResultsForInstructorWithinRangeFromView(feedbackSessionName,
                                                                                               courseId, userEmail,
                                                                                               range, viewType);
    }

    /**
     * Gets a question+response bundle for questions with responses that
     * is visible to the instructor for a feedback session from a section within the given range
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public FeedbackSessionResultsBundle getFeedbackSessionResultsForInstructorFromSectionWithinRange(
            String feedbackSessionName, String courseId, String userEmail, String section, int range)
            throws EntityDoesNotExistException {

        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(userEmail);

        return feedbackSessionsLogic.getFeedbackSessionResultsForInstructorFromSectionWithinRange(
                                        feedbackSessionName, courseId, userEmail, section, range);
    }

    /**
     * Gets a question+response bundle for questions with responses that
     * is visible to the instructor for a feedback session to a section within the given range
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public FeedbackSessionResultsBundle getFeedbackSessionResultsForInstructorToSectionWithinRange(
            String feedbackSessionName, String courseId, String userEmail, String section, int range)
            throws EntityDoesNotExistException {

        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(userEmail);

        return feedbackSessionsLogic.getFeedbackSessionResultsForInstructorToSectionWithinRange(
                                        feedbackSessionName, courseId, userEmail, section, range);
    }

    /**
     * Gets a question+response bundle for questions with responses that
     * is visible to the instructor for a feedback session from a given question number
     * This will not retrieve the list of comments for this question
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public FeedbackSessionResultsBundle getFeedbackSessionResultsForInstructorFromQuestion(
            String feedbackSessionName, String courseId, String userEmail, String questionId)
            throws EntityDoesNotExistException {

        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(userEmail);

        return feedbackSessionsLogic.getFeedbackSessionResultsForInstructorFromQuestion(feedbackSessionName, courseId,
                                                                                        userEmail, questionId);
    }

    /**
     * Gets a question+response bundle for questions with responses that
     * is visible to the instructor for a feedback session from a given question number
     * in a given section.
     * This will not retrieve the list of comments for this question
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public FeedbackSessionResultsBundle getFeedbackSessionResultsForInstructorFromQuestionInSection(
                                    String feedbackSessionName, String courseId, String userEmail,
                                    String questionId, String selectedSection, SectionDetail selectedSectionDetail)
            throws EntityDoesNotExistException {

        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(userEmail);
        Assumption.assertNotNull(selectedSection);
        Assumption.assertNotNull(selectedSectionDetail);

        return feedbackSessionsLogic.getFeedbackSessionResultsForInstructorFromQuestionInSection(
                                            feedbackSessionName, courseId, userEmail,
                                            questionId, selectedSection, selectedSectionDetail);
    }

    /**
     * Gets a question+response bundle for questions with responses that
     * is visible to the instructor for a feedback session.
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public FeedbackSessionResultsBundle getFeedbackSessionResultsForInstructor(
            String feedbackSessionName, String courseId, String userEmail)
            throws EntityDoesNotExistException {

        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(userEmail);

        return feedbackSessionsLogic.getFeedbackSessionResultsForInstructor(feedbackSessionName, courseId, userEmail);
    }

    /**
     * Gets a question+response bundle for questions with responses that
     * is visible to the instructor for a feedback session in a specific section.
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public FeedbackSessionResultsBundle getFeedbackSessionResultsForInstructorInSection(String feedbackSessionName,
            String courseId, String userEmail, String section, SectionDetail sectionDetail)
            throws EntityDoesNotExistException {

        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(userEmail);
        Assumption.assertNotNull(section);
        Assumption.assertNotNull(sectionDetail);

        return feedbackSessionsLogic.getFeedbackSessionResultsForInstructorInSection(feedbackSessionName, courseId,
                                                                                     userEmail, section, sectionDetail);
    }

    /**
     * Get existing feedback responses from student or his team for the given question.
     */
    public List<FeedbackResponseAttributes> getFeedbackResponsesFromStudentOrTeamForQuestion(
            FeedbackQuestionAttributes question, StudentAttributes student) {
        Assumption.assertNotNull(question);
        Assumption.assertNotNull(student);

        return feedbackResponsesLogic.getFeedbackResponsesFromStudentOrTeamForQuestion(question, student);
    }

    /**
     * Get existing feedback responses from instructor for the given question.
     */
    public List<FeedbackResponseAttributes> getFeedbackResponsesFromInstructorForQuestion(
            FeedbackQuestionAttributes question, InstructorAttributes instructorAttributes) {
        Assumption.assertNotNull(question);
        Assumption.assertNotNull(instructorAttributes);

        return feedbackResponsesLogic.getFeedbackResponsesFromGiverForQuestion(
                question.getFeedbackQuestionId(), instructorAttributes.getEmail());
    }

    public FeedbackResponseAttributes getFeedbackResponse(String feedbackResponseId) {
        Assumption.assertNotNull(feedbackResponseId);
        return feedbackResponsesLogic.getFeedbackResponse(feedbackResponseId);
    }

    public FeedbackResponseAttributes getFeedbackResponse(String feedbackQuestionId,
                                                          String giverEmail,
                                                          String recipient) {

        Assumption.assertNotNull(feedbackQuestionId);
        Assumption.assertNotNull(giverEmail);
        Assumption.assertNotNull(recipient);

        return feedbackResponsesLogic.getFeedbackResponse(feedbackQuestionId, giverEmail, recipient);
    }

    public void createFeedbackResponses(List<FeedbackResponseAttributes> feedbackResponses)
            throws InvalidParametersException {

        Assumption.assertNotNull(feedbackResponses);
        feedbackResponsesLogic.createFeedbackResponses(feedbackResponses);
    }

    public boolean hasGiverRespondedForSession(String userEmail, String feedbackSessionName, String courseId) {
        Assumption.assertNotNull(userEmail);
        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);

        return feedbackResponsesLogic.hasGiverRespondedForSession(userEmail, feedbackSessionName, courseId);
    }

    public boolean hasResponsesForCourse(String courseId) {
        return feedbackResponsesLogic.hasResponsesForCourse(courseId);
    }

    public boolean isOpenOrPublishedEmailSentForTheCourse(String courseId) {
        Assumption.assertNotNull(courseId);
        return feedbackSessionsLogic.isOpenOrPublishedEmailSentForTheCourse(courseId);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public void updateFeedbackResponse(FeedbackResponseAttributes feedbackResponse)
            throws InvalidParametersException, EntityDoesNotExistException, EntityAlreadyExistsException {

        Assumption.assertNotNull(feedbackResponse);
        feedbackResponsesLogic.updateFeedbackResponse(feedbackResponse);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public void deleteFeedbackResponse(FeedbackResponseAttributes feedbackResponse) {
        Assumption.assertNotNull(feedbackResponse);
        feedbackResponsesLogic.deleteFeedbackResponseAndCascade(feedbackResponse);
    }

    /**
     * Create a feedback response comment, and return the created comment.
     *
     * <br/>Preconditions: <br/>
     * * All parameters are non-null.
     */
    public FeedbackResponseCommentAttributes createFeedbackResponseComment(
            FeedbackResponseCommentAttributes feedbackResponseComment)
            throws InvalidParametersException, EntityDoesNotExistException, EntityAlreadyExistsException {
        Assumption.assertNotNull(feedbackResponseComment);
        return feedbackResponseCommentsLogic.createFeedbackResponseComment(feedbackResponseComment);
    }

    public FeedbackResponseCommentAttributes getFeedbackResponseComment(Long feedbackResponseCommentId) {
        Assumption.assertNotNull(feedbackResponseCommentId);
        return feedbackResponseCommentsLogic.getFeedbackResponseComment(feedbackResponseCommentId);
    }

    public List<FeedbackResponseCommentAttributes> getFeedbackResponseCommentForGiver(String courseId,
                                                                                      String giverEmail) {
        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(giverEmail);

        return feedbackResponseCommentsLogic.getFeedbackResponseCommentsForGiver(courseId, giverEmail);
    }

    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     */
    public FeedbackResponseCommentAttributes getFeedbackResponseComment(
            String responseId, String giverEmail, Instant creationDate) {
        Assumption.assertNotNull(responseId);
        Assumption.assertNotNull(giverEmail);
        Assumption.assertNotNull(creationDate);

        return feedbackResponseCommentsLogic.getFeedbackResponseComment(responseId, giverEmail, creationDate);
    }

    /**
     * Creates or updates document for the given comment.
     *
     * @see FeedbackResponseCommentsLogic#putDocument(FeedbackResponseCommentAttributes)
     */
    public void putDocument(FeedbackResponseCommentAttributes comment) {
        feedbackResponseCommentsLogic.putDocument(comment);
    }

    /**
     * Batch creates or updates documents for the given comments.
     *
     * @see FeedbackResponseCommentsLogic#putDocuments(List)
     */
    public void putFeedbackResponseCommentDocuments(List<FeedbackResponseCommentAttributes> comments) {
        feedbackResponseCommentsLogic.putDocuments(comments);
    }

    /**
     * Removes document for the comment by given id.
     *
     * @see FeedbackResponseCommentsLogic#deleteDocumentByCommentId(long)
     */
    public void deleteDocumentByCommentId(long commentId) {
        feedbackResponseCommentsLogic.deleteDocumentByCommentId(commentId);
    }

    /**
     * Search for FeedbackResponseComment. Preconditions: all parameters are non-null.
     * @param instructors   a list of InstructorAttributes associated to a googleId,
     *                      used for filtering of search result
     * @return Null if no match found
     */
    public FeedbackResponseCommentSearchResultBundle searchFeedbackResponseComments(String queryString,
                                                                         List<InstructorAttributes> instructors) {
        Assumption.assertNotNull(queryString);
        Assumption.assertNotNull(instructors);
        return feedbackResponseCommentsLogic.searchFeedbackResponseComments(queryString, instructors);
    }

    /**
     * Updates a feedback response comment by {@link FeedbackResponseCommentAttributes.UpdateOptions}.
     *
     * <br/>Preconditions: <br/>
     * * All parameters are non-null.
     *
     * @return updated comment
     * @throws InvalidParametersException if attributes to update are not valid
     * @throws EntityDoesNotExistException if the comment cannot be found
     */
    public FeedbackResponseCommentAttributes updateFeedbackResponseComment(
            FeedbackResponseCommentAttributes.UpdateOptions updateOptions)
            throws EntityDoesNotExistException, InvalidParametersException {
        Assumption.assertNotNull(updateOptions);

        return feedbackResponseCommentsLogic.updateFeedbackResponseComment(updateOptions);
    }

    /**
     * Preconditions: <br>
     * * Id of comment is not null.
     */
    public void deleteFeedbackResponseCommentById(Long commentId) {
        Assumption.assertNotNull(commentId);
        feedbackResponseCommentsLogic.deleteFeedbackResponseCommentById(commentId);
    }

    /**
     * Deletes uploaded file.
     * @param key the GCS blobkey used to fetch the file in Google Cloud Storage
     */
    public void deleteUploadedFile(BlobKey key) {
        Assumption.assertNotNull(key);
        GoogleCloudStorageHelper.deleteFile(key);
    }

    public List<String> getArchivedCourseIds(List<CourseAttributes> allCourses,
                                             Map<String, InstructorAttributes> instructorsForCourses) {
        Assumption.assertNotNull(allCourses);
        Assumption.assertNotNull(instructorsForCourses);
        return coursesLogic.getArchivedCourseIds(allCourses, instructorsForCourses);
    }

    public List<FeedbackResponseAttributes>
            getFeedbackResponsesForSession(String feedbackSessionName, String courseId) {
        Assumption.assertNotNull(feedbackSessionName);
        Assumption.assertNotNull(courseId);
        return feedbackResponsesLogic.getFeedbackResponsesForSession(feedbackSessionName, courseId);
    }

    public void adjustFeedbackResponseForEnrollments(List<StudentEnrollDetails> enrollmentList,
                                                     FeedbackResponseAttributes response)
            throws InvalidParametersException, EntityDoesNotExistException {
        Assumption.assertNotNull(enrollmentList);
        Assumption.assertNotNull(response);
        studentsLogic.adjustFeedbackResponseForEnrollments(enrollmentList, response);
    }

    /**
     * Returns returns a list of sessions that were closed within past hour.
     *
     * @see FeedbackSessionsLogic#getFeedbackSessionsClosedWithinThePastHour()
     */
    public List<FeedbackSessionAttributes> getFeedbackSessionsClosedWithinThePastHour() {
        return feedbackSessionsLogic.getFeedbackSessionsClosedWithinThePastHour();
    }

    public List<FeedbackSessionAttributes> getFeedbackSessionsClosingWithinTimeLimit() {
        return feedbackSessionsLogic.getFeedbackSessionsClosingWithinTimeLimit();
    }

    /**
     * Returns a list of sessions that require automated emails to be sent as they are published.
     *
     * @see FeedbackSessionsLogic#getFeedbackSessionsWhichNeedAutomatedPublishedEmailsToBeSent()
     */
    public List<FeedbackSessionAttributes> getFeedbackSessionsWhichNeedAutomatedPublishedEmailsToBeSent() {
        return feedbackSessionsLogic.getFeedbackSessionsWhichNeedAutomatedPublishedEmailsToBeSent();
    }

    public List<FeedbackSessionAttributes> getFeedbackSessionsWhichNeedOpenEmailsToBeSent() {
        return feedbackSessionsLogic.getFeedbackSessionsWhichNeedOpenEmailsToBeSent();
    }

    public String getSectionForTeam(String courseId, String teamName) {
        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(teamName);
        return studentsLogic.getSectionForTeam(courseId, teamName);
    }

    /**
     * Persists the given data bundle to the datastore.
     *
     * @see DataBundleLogic#persistDataBundle(DataBundle)
     */
    public void persistDataBundle(DataBundle dataBundle) throws InvalidParametersException {
        dataBundleLogic.persistDataBundle(dataBundle);
    }

    /**
     * Removes the given data bundle from the datastore.
     *
     * @see DataBundleLogic#removeDataBundle(DataBundle)
     */
    public void removeDataBundle(DataBundle dataBundle) {
        dataBundleLogic.removeDataBundle(dataBundle);
    }

    /**
     * Puts searchable documents from the data bundle to the datastore.
     *
     * @see DataBundleLogic#putDocuments(DataBundle)
     */
    public void putDocuments(DataBundle dataBundle) {
        dataBundleLogic.putDocuments(dataBundle);
    }

}
