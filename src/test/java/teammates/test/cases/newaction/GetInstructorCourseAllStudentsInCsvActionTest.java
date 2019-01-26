package teammates.test.cases.newaction;

import org.testng.annotations.Test;

import teammates.common.datatransfer.attributes.CourseAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.util.Const;
import teammates.logic.core.StudentsLogic;
import teammates.test.driver.CsvChecker;
import teammates.ui.newcontroller.CsvResult;
import teammates.ui.newcontroller.GetInstructorCourseAllStudentsInCsvAction;

/**
 * SUT: {@link GetInstructorCourseAllStudentsInCsvAction}.
 */
public class GetInstructorCourseAllStudentsInCsvActionTest extends
        BaseActionTest<GetInstructorCourseAllStudentsInCsvAction> {

    @Override
    protected String getActionUri() {
        return Const.ResourceURIs.INSTRUCTOR_COURSE_DETAILS_ALL_STUDENTS_CSV;
    }

    @Override
    protected String getRequestMethod() {
        return GET;
    }

    @Override
    @Test
    public void testExecute() throws Exception {
        String instructorId = typicalBundle.instructors.get("instructor1OfCourse1").googleId;
        CourseAttributes course = typicalBundle.courses.get("typicalCourse1");

        gaeSimulation.loginAsInstructor(instructorId);

        ______TS("Invalid params");
        String[] submissionParams = {};
        verifyHttpParameterFailure(submissionParams);

        submissionParams = new String[] {
                Const.ParamsNames.COURSE_ID, course.getId()
        };

        ______TS("Typical case: student list downloaded successfully");
        GetInstructorCourseAllStudentsInCsvAction downloadAction = getAction(submissionParams);
        CsvResult result = getCsvResult(downloadAction);
        String content = result.getContent();

        CsvChecker.verifyCsvContent(content, "/courseStudentList_actionTest.csv");

        ______TS("Typical case: student list downloaded successfully with student last name specified within braces");

        StudentAttributes student1InCourse1 = typicalBundle.students.get("student1InCourse1");
        student1InCourse1.name = "new name {new last name}";
        StudentsLogic.inst().updateStudentCascade(
                StudentAttributes.updateOptionsBuilder(student1InCourse1.course, student1InCourse1.email)
                        .withName(student1InCourse1.name)
                        .build()
        );

        downloadAction = getAction(submissionParams);
        result = getCsvResult(downloadAction);
        content = result.getContent();

        CsvChecker.verifyCsvContent(content, "/courseStudentListStudentLastName_actionTest.csv");

        removeAndRestoreTypicalDataBundle();

        ______TS("Typical case: student list downloaded successfully with special team name");

        student1InCourse1 = StudentsLogic.inst().getStudentForEmail("idOfTypicalCourse1", "student1InCourse1@gmail.tmt");
        student1InCourse1.team = "N/A";
        StudentsLogic.inst().updateStudentCascade(
                StudentAttributes.updateOptionsBuilder(student1InCourse1.course, student1InCourse1.email)
                        .withTeamName(student1InCourse1.team)
                        .build()
        );

        downloadAction = getAction(submissionParams);
        result = getCsvResult(downloadAction);
        content = result.getContent();

        CsvChecker.verifyCsvContent(content, "/courseStudentListSpecialTeamName_actionTest.csv");
    }

    @Override
    @Test
    protected void testAccessControl() throws Exception {
        CourseAttributes course = typicalBundle.courses.get("typicalCourse1");

        String[] submissionParams = new String[] {
                Const.ParamsNames.COURSE_ID, course.getId()
        };

        verifyOnlyInstructorsOfTheSameCourseCanAccess(submissionParams);
    }
}
