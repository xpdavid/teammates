package teammates.common.datatransfer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.util.Const;

/**
 * Contains a list of students and instructors in a course. Useful for caching
 * a copy of student and instructor details of a course instead of reading
 * them from the database multiple times.
 */
public class CourseRoster {

    private final Map<String, StudentAttributes> studentListByEmail = new HashMap<>();
    private final Map<String, InstructorAttributes> instructorListByEmail = new HashMap<>();
    private final Map<String, List<StudentAttributes>> teamToMembersTable = new HashMap<>();

    public CourseRoster(List<StudentAttributes> students, List<InstructorAttributes> instructors) {
        populateStudentListByEmail(students);
        populateInstructorListByEmail(instructors);
        buildTeamToMembersTable();
    }

    public List<StudentAttributes> getStudents() {
        return new ArrayList<>(studentListByEmail.values());
    }

    public List<InstructorAttributes> getInstructors() {
        return new ArrayList<>(instructorListByEmail.values());
    }

    public Map<String, List<StudentAttributes>> getTeamToMembersTable() {
        return teamToMembersTable;
    }

    /**
     * Checks if an instructor is the instructor of a course by providing an email address.
     * @param instructorEmail email of the instructor to be checked.
     * @return true if the instructor is an instructor of the course
     */
    public boolean isInstructorOfCourse(String instructorEmail) {
        return instructorListByEmail.containsKey(instructorEmail);
    }

    public boolean isStudentInCourse(String studentEmail) {
        return studentListByEmail.containsKey(studentEmail);
    }

    /**
     * Checks whether a team is in course.
     */
    public boolean isTeamInCourse(String teamName) {
        return teamToMembersTable.containsKey(teamName);
    }

    public boolean isStudentInTeam(String studentEmail, String targetTeamName) {
        StudentAttributes student = studentListByEmail.get(studentEmail);
        return student != null && student.team.equals(targetTeamName);
    }

    public boolean isStudentsInSameTeam(String studentEmail1, String studentEmail2) {
        StudentAttributes student1 = studentListByEmail.get(studentEmail1);
        StudentAttributes student2 = studentListByEmail.get(studentEmail2);
        return student1 != null && student2 != null
                && student1.team != null && student1.team.equals(student2.team);
    }

    public StudentAttributes getStudentForEmail(String email) {
        return studentListByEmail.get(email);
    }

    public InstructorAttributes getInstructorForEmail(String email) {
        return instructorListByEmail.get(email);
    }

    /**
     * Returns a map of email mapped to name of instructors and students of the course.
     *
     * @return Map in which key is email of student/instructor and value is name.
     */
    public Map<String, String> getEmailToNameTableFromRoster() {
        Map<String, String> emailToNameTable = new HashMap<>();
        List<InstructorAttributes> instructorList = getInstructors();
        for (InstructorAttributes instructor : instructorList) {
            emailToNameTable.put(instructor.email, instructor.name);
        }

        List<StudentAttributes> studentList = getStudents();
        for (StudentAttributes student : studentList) {
            emailToNameTable.put(student.email, student.name);
        }
        return emailToNameTable;
    }

    private void populateStudentListByEmail(List<StudentAttributes> students) {

        if (students == null) {
            return;
        }

        for (StudentAttributes s : students) {
            studentListByEmail.put(s.email, s);
        }
    }

    private void populateInstructorListByEmail(List<InstructorAttributes> instructors) {

        if (instructors == null) {
            return;
        }

        for (InstructorAttributes i : instructors) {
            instructorListByEmail.put(i.email, i);
        }
    }

    private void buildTeamToMembersTable() {
        // group students by team
        for (StudentAttributes studentAttributes : getStudents()) {
            teamToMembersTable.computeIfAbsent(studentAttributes.getTeam(), key -> new ArrayList<>())
                    .add(studentAttributes);
        }
    }

    /**
     * Gets info of a participant associated with an identifier in the course.
     *
     * @return an object {@link ParticipantInfo} containing the name, teamName and the sectionName.
     */
    public ParticipantInfo getInfoForIdentifier(String identifier) {
        String name = Const.USER_NOBODY_TEXT;
        String teamName = Const.USER_NOBODY_TEXT;
        String sectionName = Const.DEFAULT_SECTION;

        boolean isStudent = getStudentForEmail(identifier) != null;
        boolean isInstructor = getInstructorForEmail(identifier) != null;
        boolean isTeam = getTeamToMembersTable().containsKey(identifier);
        if (isStudent) {
            StudentAttributes student = getStudentForEmail(identifier);

            name = student.getName();
            teamName = student.getTeam();
            sectionName = student.getSection();
        } else if (isInstructor) {
            InstructorAttributes instructor = getInstructorForEmail(identifier);

            name = instructor.getName();
            teamName = Const.USER_TEAM_FOR_INSTRUCTOR;
            sectionName = Const.DEFAULT_SECTION;
        } else if (isTeam) {
            StudentAttributes teamMember = getTeamToMembersTable().get(identifier).iterator().next();

            name = identifier;
            teamName = identifier;
            sectionName = teamMember.getSection();
        }

        return new ParticipantInfo(name, teamName, sectionName);
    }

    /**
     * Simple data transfer object containing the information of a participant.
     */
    public static class ParticipantInfo {

        private final String name;
        private final String teamName;
        private final String sectionName;

        public ParticipantInfo(String name, String teamName, String sectionName) {
            this.name = name;
            this.teamName = teamName;
            this.sectionName = sectionName;
        }

        public String getName() {
            return name;
        }

        public String getTeamName() {
            return teamName;
        }

        public String getSectionName() {
            return sectionName;
        }
    }
}
