package teammates.test.cases.newaction;

import org.apache.http.HttpStatus;
import org.testng.annotations.Test;

import teammates.common.datatransfer.attributes.FeedbackSessionAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.util.Const;
import teammates.ui.newcontroller.FeedbackSessionInfo;
import teammates.ui.newcontroller.GetFeedbackSessionAction;
import teammates.ui.newcontroller.Intent;
import teammates.ui.newcontroller.JsonResult;

/**
 * SUT: {@link GetFeedbackSessionAction}.
 */
public class GetFeedbackSessionActionTest extends BaseActionTest<GetFeedbackSessionAction> {

    @Override
    protected String getActionUri() {
        return Const.ResourceURIs.SESSION;
    }

    @Override
    protected String getRequestMethod() {
        return GET;
    }

    @Override
    @Test
    protected void testExecute() throws Exception {
        InstructorAttributes instructor1OfCourse1 = typicalBundle.instructors.get("instructor1OfCourse1");
        FeedbackSessionAttributes feedbackSessionAttributes = typicalBundle.feedbackSessions.get("session1InCourse1");

        loginAsInstructor(instructor1OfCourse1.googleId);

        ______TS("Not enough parameters");

        verifyHttpParameterFailure();
        verifyHttpParameterFailure(Const.ParamsNames.COURSE_ID, feedbackSessionAttributes.getCourseId());
        verifyHttpParameterFailure(Const.ParamsNames.FEEDBACK_SESSION_NAME,
                feedbackSessionAttributes.getFeedbackSessionName());
        verifyHttpParameterFailure(Const.ParamsNames.COURSE_ID, feedbackSessionAttributes.getCourseId(),
                Const.ParamsNames.FEEDBACK_SESSION_NAME, feedbackSessionAttributes.getFeedbackSessionName());

        ______TS("typical success case");

        String[] params = {
                Const.ParamsNames.COURSE_ID, feedbackSessionAttributes.getCourseId(),
                Const.ParamsNames.FEEDBACK_SESSION_NAME, feedbackSessionAttributes.getFeedbackSessionName(),
                Const.ParamsNames.INTENT, Intent.FULL_DETAIL.toString()
        };
        GetFeedbackSessionAction a = getAction(params);
        JsonResult r = getJsonResult(a);

        assertEquals(HttpStatus.SC_OK, r.getStatusCode());
        FeedbackSessionInfo.FeedbackSessionResponse response = (FeedbackSessionInfo.FeedbackSessionResponse) r.getOutput();
        assertEquals(feedbackSessionAttributes.getCourseId(), response.getCourseId());
        assertEquals(feedbackSessionAttributes.getFeedbackSessionName(), response.getFeedbackSessionName());
        assertEquals(feedbackSessionAttributes.getTimeZone().getId(), response.getTimeZone());
        assertEquals(feedbackSessionAttributes.getInstructions(), response.getInstructions());

        assertEquals(feedbackSessionAttributes.getStartTime().toEpochMilli(), response.getSubmissionStartTimestamp());
        assertEquals(feedbackSessionAttributes.getEndTime().toEpochMilli(), response.getSubmissionEndTimestamp());
        assertEquals(feedbackSessionAttributes.getGracePeriodMinutes(), response.getGracePeriod());

        assertEquals(FeedbackSessionInfo.SessionVisibleSetting.CUSTOM, response.getSessionVisibleSetting());
        assertEquals(feedbackSessionAttributes.getSessionVisibleFromTime().toEpochMilli(),
                response.getCustomSessionVisibleTimestamp().longValue());

        assertEquals(FeedbackSessionInfo.ResponseVisibleSetting.CUSTOM, response.getResponseVisibleSetting());
        assertEquals(feedbackSessionAttributes.getResultsVisibleFromTime().toEpochMilli(),
                response.getCustomResponseVisibleTimestamp().longValue());

        assertEquals(FeedbackSessionInfo.FeedbackSessionSubmissionStatus.OPEN, response.getSubmissionStatus());
        assertEquals("Not Published", response.getPublishStatus());

        assertEquals(feedbackSessionAttributes.isClosingEmailEnabled(), response.isClosingEmailEnabled());
        assertEquals(feedbackSessionAttributes.isPublishedEmailEnabled(), response.isPublishedEmailEnabled());
    }

    @Override
    @Test
    protected void testAccessControl() throws Exception {
        InstructorAttributes instructor1OfCourse1 = typicalBundle.instructors.get("instructor1OfCourse1");
        FeedbackSessionAttributes fs = typicalBundle.feedbackSessions.get("session1InCourse1");

        ______TS("non-existent feedback session");

        String[] submissionParams = new String[] {
                Const.ParamsNames.COURSE_ID, fs.getCourseId(),
                Const.ParamsNames.FEEDBACK_SESSION_NAME, "randomName for Session123",
                Const.ParamsNames.INTENT, Intent.FULL_DETAIL.toString()
        };

        loginAsInstructor(instructor1OfCourse1.googleId);
        verifyCannotAccess(submissionParams);

        ______TS("only instructors of the same course can access");

        submissionParams = new String[] {
                Const.ParamsNames.COURSE_ID, fs.getCourseId(),
                Const.ParamsNames.FEEDBACK_SESSION_NAME, fs.getFeedbackSessionName(),
                Const.ParamsNames.INTENT, Intent.FULL_DETAIL.toString()
        };

        verifyAccessibleForInstructorsOfTheSameCourse(submissionParams);
        verifyAccessibleForAdminToMasqueradeAsInstructor(submissionParams);
    }
}
