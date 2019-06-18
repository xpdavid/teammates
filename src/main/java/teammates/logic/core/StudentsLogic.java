package teammates.logic.core;

import java.util.ArrayList;
import java.util.List;

import teammates.common.datatransfer.AttributesDeletionQuery;
import teammates.common.datatransfer.StudentSearchResultBundle;
import teammates.common.datatransfer.TeamDetailsBundle;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.exception.EnrollException;
import teammates.common.exception.EntityAlreadyExistsException;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.Assumption;
import teammates.common.util.Const;
import teammates.common.util.SanitizationHelper;
import teammates.common.util.StringHelper;
import teammates.storage.api.StudentsDb;

/**
 * Handles operations related to students.
 *
 * @see StudentAttributes
 * @see StudentsDb
 */
public final class StudentsLogic {

    private static final int SECTION_SIZE_LIMIT = 100;

    private static StudentsLogic instance = new StudentsLogic();

    private static final StudentsDb studentsDb = new StudentsDb();

    private static final FeedbackResponsesLogic frLogic = FeedbackResponsesLogic.inst();

    private StudentsLogic() {
        // prevent initialization
    }

    public static StudentsLogic inst() {
        return instance;
    }

    /**
     * Creates a student.
     *
     * @return the created student
     * @throws InvalidParametersException if the student is not valid
     * @throws EntityAlreadyExistsException if the student already exists in the Datastore
     */
    public StudentAttributes createStudent(StudentAttributes studentData)
            throws InvalidParametersException, EntityAlreadyExistsException {
        return studentsDb.createEntity(studentData);
    }

    public StudentAttributes getStudentForEmail(String courseId, String email) {
        return studentsDb.getStudentForEmail(courseId, email);
    }

    public List<StudentAttributes> getAllStudentsForEmail(String email) {
        return studentsDb.getAllStudentsForEmail(email);
    }

    public StudentAttributes getStudentForCourseIdAndGoogleId(String courseId, String googleId) {
        return studentsDb.getStudentForGoogleId(courseId, googleId);
    }

    public StudentAttributes getStudentForRegistrationKey(String registrationKey) {
        return studentsDb.getStudentForRegistrationKey(registrationKey);
    }

    public List<StudentAttributes> getStudentsForGoogleId(String googleId) {
        return studentsDb.getStudentsForGoogleId(googleId);
    }

    public List<StudentAttributes> getStudentsForCourse(String courseId) {
        return studentsDb.getStudentsForCourse(courseId);
    }

    /**
     * Gets all students of a team.
     */
    public List<StudentAttributes> getStudentsForTeam(String teamName, String courseId) {
        return studentsDb.getStudentsForTeam(teamName, courseId);
    }

    public List<StudentAttributes> getStudentsForSection(String sectionName, String courseId) {
        return studentsDb.getStudentsForSection(sectionName, courseId);
    }

    public List<StudentAttributes> getUnregisteredStudentsForCourse(String courseId) {
        return studentsDb.getUnregisteredStudentsForCourse(courseId);
    }

    public StudentSearchResultBundle searchStudents(String queryString, List<InstructorAttributes> instructors) {
        return studentsDb.search(queryString, instructors);
    }

    /**
     * This method should be used by admin only since the searching does not restrict the
     * visibility according to the logged-in user's google ID. This is used by admin to
     * search students in the whole system.
     * @return null if no result found
     */
    public StudentSearchResultBundle searchStudentsInWholeSystem(String queryString) {
        return studentsDb.searchStudentsInWholeSystem(queryString);
    }

    public String getEncryptedKeyForStudent(String courseId, String email) throws EntityDoesNotExistException {

        StudentAttributes studentData = getStudentForEmail(courseId, email);

        if (studentData == null) {
            throw new EntityDoesNotExistException("Student does not exist: [" + courseId + "/" + email + "]");
        }

        return StringHelper.encrypt(studentData.key);
    }

    public boolean isStudentInAnyCourse(String googleId) {
        return studentsDb.getStudentsForGoogleId(googleId).size() != 0;
    }

    public boolean isStudentInCourse(String courseId, String studentEmail) {
        return studentsDb.getStudentForEmail(courseId, studentEmail) != null;
    }

    public boolean isStudentInTeam(String courseId, String teamName, String studentEmail) {

        StudentAttributes student = getStudentForEmail(courseId, studentEmail);
        if (student == null) {
            return false;
        }

        List<StudentAttributes> teammates = getStudentsForTeam(teamName, courseId);
        for (StudentAttributes teammate : teammates) {
            if (teammate.email.equals(student.email)) {
                return true;
            }
        }
        return false;
    }

    public boolean isStudentsInSameTeam(String courseId, String student1Email, String student2Email) {
        StudentAttributes student1 = getStudentForEmail(courseId, student1Email);
        if (student1 == null) {
            return false;
        }
        return isStudentInTeam(courseId, student1.team, student2Email);
    }

    /**
     * Updates a student by {@link StudentAttributes.UpdateOptions}.
     *
     * <p>If email changed, update by recreating the student and cascade update all responses the student gives/receives.
     *
     * <p>If team changed, cascade delete all responses the student gives/receives within that team.
     *
     * <p>If section changed, cascade update all responses the student gives/receives.
     *
     * @return updated student
     * @throws InvalidParametersException if attributes to update are not valid
     * @throws EntityDoesNotExistException if the student cannot be found
     * @throws EntityAlreadyExistsException if the student cannot be updated
     *         by recreation because of an existent student
     */
    public StudentAttributes updateStudentCascade(StudentAttributes.UpdateOptions updateOptions)
            throws InvalidParametersException, EntityDoesNotExistException, EntityAlreadyExistsException {
        StudentAttributes originalStudent = getStudentForEmail(updateOptions.getCourseId(), updateOptions.getEmail());
        StudentAttributes updatedStudent = studentsDb.updateStudent(updateOptions);

        // cascade email change, if any
        if (!originalStudent.email.equals(updatedStudent.email)) {
            frLogic.updateFeedbackResponsesForChangingEmail(
                    updatedStudent.course, originalStudent.email, updatedStudent.email);
        }

        // adjust submissions if moving to a different team
        if (isTeamChanged(originalStudent.team, updatedStudent.team)) {
            frLogic.updateFeedbackResponsesForChangingTeam(updatedStudent.course, updatedStudent.email,
                    originalStudent.team, updatedStudent.team);
        }

        // update the new section name in responses
        if (isSectionChanged(originalStudent.section, updatedStudent.section)) {
            frLogic.updateFeedbackResponsesForChangingSection(updatedStudent.course, updatedStudent.email,
                    originalStudent.section, updatedStudent.section);
        }

        // TODO: check to delete comments for this section/team if the section/team is no longer existent in the course

        return updatedStudent;
    }

    /**
     * Resets the googleId associated with the student.
     */
    public void resetStudentGoogleId(String originalEmail, String courseId)
            throws EntityDoesNotExistException {
        try {
            updateStudentCascade(
                    StudentAttributes.updateOptionsBuilder(courseId, originalEmail)
                            .withGoogleId(null)
                            .build());
        } catch (InvalidParametersException | EntityAlreadyExistsException e) {
            Assumption.fail("Resting google ID shall not cause: " + e.getMessage());
        }
    }

    /**
     * Validates sections for any limit violations and teams for any team name violations.
     */
    public void validateSectionsAndTeams(List<StudentAttributes> studentList, String courseId) throws EnrollException {

        List<StudentAttributes> mergedList = getMergedList(studentList, courseId);

        if (mergedList.size() < 2) { // no conflicts
            return;
        }

        String errorMessage = getSectionInvalidityInfo(mergedList) + getTeamInvalidityInfo(mergedList);

        if (!errorMessage.isEmpty()) {
            throw new EnrollException(errorMessage);
        }

    }

    /**
     * Validates teams for any team name violations.
     */
    public void validateTeams(List<StudentAttributes> studentList, String courseId) throws EnrollException {

        List<StudentAttributes> mergedList = getMergedList(studentList, courseId);

        if (mergedList.size() < 2) { // no conflicts
            return;
        }

        String errorMessage = getTeamInvalidityInfo(mergedList);

        if (errorMessage.length() > 0) {
            throw new EnrollException(errorMessage);
        }

    }

    private List<StudentAttributes> getMergedList(List<StudentAttributes> studentList, String courseId) {

        List<StudentAttributes> mergedList = new ArrayList<>();
        List<StudentAttributes> studentsInCourse = getStudentsForCourse(courseId);

        for (StudentAttributes student : studentList) {
            mergedList.add(student);
        }

        for (StudentAttributes student : studentsInCourse) {
            if (!isInEnrollList(student, mergedList)) {
                mergedList.add(student);
            }
        }
        return mergedList;
    }

    public String getSectionForTeam(String courseId, String teamName) {

        List<StudentAttributes> students = getStudentsForTeam(teamName, courseId);
        if (students.isEmpty()) {
            return Const.DEFAULT_SECTION;
        }
        return students.get(0).section;
    }

    private String getSectionInvalidityInfo(List<StudentAttributes> mergedList) {

        StudentAttributes.sortBySectionName(mergedList);

        List<String> invalidSectionList = new ArrayList<>();
        int studentsCount = 1;
        for (int i = 1; i < mergedList.size(); i++) {
            StudentAttributes currentStudent = mergedList.get(i);
            StudentAttributes previousStudent = mergedList.get(i - 1);
            if (currentStudent.section.equals(previousStudent.section)) {
                studentsCount++;
            } else {
                if (studentsCount > SECTION_SIZE_LIMIT) {
                    invalidSectionList.add(previousStudent.section);
                }
                studentsCount = 1;
            }

            if (i == mergedList.size() - 1 && studentsCount > SECTION_SIZE_LIMIT) {
                invalidSectionList.add(currentStudent.section);
            }
        }

        StringBuilder errorMessage = new StringBuilder();
        for (String section : invalidSectionList) {
            errorMessage.append(String.format(Const.StatusMessages.SECTION_QUOTA_EXCEED, section));
        }

        return errorMessage.toString();
    }

    private String getTeamInvalidityInfo(List<StudentAttributes> mergedList) {

        StudentAttributes.sortByTeamName(mergedList);

        List<String> invalidTeamList = new ArrayList<>();
        for (int i = 1; i < mergedList.size(); i++) {
            StudentAttributes currentStudent = mergedList.get(i);
            StudentAttributes previousStudent = mergedList.get(i - 1);
            if (currentStudent.team.equals(previousStudent.team)
                    && !currentStudent.section.equals(previousStudent.section)
                    && !invalidTeamList.contains(currentStudent.team)) {
                invalidTeamList.add(currentStudent.team);
            }
        }

        StringBuilder errorMessage = new StringBuilder(100);
        for (String team : invalidTeamList) {
            errorMessage.append(String.format(Const.StatusMessages.TEAM_INVALID_SECTION_EDIT,
                                              SanitizationHelper.sanitizeForHtml(team)));
        }

        if (errorMessage.length() != 0) {
            errorMessage.append("Please use the enroll page to edit multiple students");
        }

        return errorMessage.toString();
    }

    /**
     * Deletes all the students in the course cascade their associated responses and comments.
     */
    public void deleteStudentsInCourseCascade(String courseId) {
        List<StudentAttributes> studentsInCourse = getStudentsForCourse(courseId);
        for (StudentAttributes student : studentsInCourse) {
            deleteStudentCascade(courseId, student.email);
        }
    }

    /**
     * Deletes a student cascade its associated feedback responses and comments.
     *
     * <p>Fails silently if the student does not exist.
     */
    public void deleteStudentCascade(String courseId, String studentEmail) {
        StudentAttributes student = getStudentForEmail(courseId, studentEmail);
        if (student == null) {
            return;
        }

        frLogic.deleteFeedbackResponsesInvolvedEntityOfCourseCascade(courseId, studentEmail);
        if (studentsDb.getStudentsForTeam(student.getTeam(), student.getCourse()).size() == 1) {
            // the student is the only student in the team
            frLogic.deleteFeedbackResponsesInvolvedEntityOfCourseCascade(student.getCourse(), student.getTeam());
        }
        studentsDb.deleteStudent(courseId, studentEmail);
    }

    /**
     * Deletes all students associated a googleId and cascade its associated feedback responses and comments.
     */
    public void deleteStudentsForGoogleIdCascade(String googleId) {
        List<StudentAttributes> students = studentsDb.getStudentsForGoogleId(googleId);

        // Cascade delete students
        for (StudentAttributes student : students) {
            deleteStudentCascade(student.course, student.email);
        }
    }

    /**
     * Deletes students using {@link AttributesDeletionQuery}.
     */
    public void deleteStudents(AttributesDeletionQuery query) {
        studentsDb.deleteStudents(query);
    }

    /**
     * Batch creates or updates documents for the given students.
     */
    public void putDocuments(List<StudentAttributes> students) {
        studentsDb.putDocuments(students);
    }

    private boolean isInEnrollList(StudentAttributes student,
            List<StudentAttributes> studentInfoList) {
        for (StudentAttributes studentInfo : studentInfoList) {
            if (studentInfo.email.equalsIgnoreCase(student.email)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTeamChanged(String originalTeam, String newTeam) {
        return newTeam != null && originalTeam != null
                && !originalTeam.equals(newTeam);
    }

    private boolean isSectionChanged(String originalSection, String newSection) {
        return newSection != null && originalSection != null
                && !originalSection.equals(newSection);
    }

    public TeamDetailsBundle getTeamDetailsForStudent(StudentAttributes student) {
        if (student != null) {
            TeamDetailsBundle teamResult = new TeamDetailsBundle();
            teamResult.name = student.team;
            teamResult.students = getStudentsForTeam(student.team, student.course);
            StudentAttributes.sortByNameAndThenByEmail(teamResult.students);
            return teamResult;
        }
        return null;
    }

}
