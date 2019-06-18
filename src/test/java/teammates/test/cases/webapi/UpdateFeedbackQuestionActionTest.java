package teammates.test.cases.webapi;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.http.HttpStatus;
import org.testng.annotations.Test;

import teammates.common.datatransfer.DataBundle;
import teammates.common.datatransfer.FeedbackParticipantType;
import teammates.common.datatransfer.attributes.FeedbackQuestionAttributes;
import teammates.common.datatransfer.attributes.FeedbackSessionAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.questions.FeedbackContributionQuestionDetails;
import teammates.common.datatransfer.questions.FeedbackQuestionType;
import teammates.common.datatransfer.questions.FeedbackTextQuestionDetails;
import teammates.common.exception.EntityNotFoundException;
import teammates.common.exception.InvalidHttpRequestBodyException;
import teammates.common.util.Const;
import teammates.common.util.JsonUtils;
import teammates.storage.api.FeedbackResponsesDb;
import teammates.ui.webapi.action.JsonResult;
import teammates.ui.webapi.action.UpdateFeedbackQuestionAction;
import teammates.ui.webapi.output.FeedbackQuestionData;
import teammates.ui.webapi.output.FeedbackVisibilityType;
import teammates.ui.webapi.output.NumberOfEntitiesToGiveFeedbackToSetting;
import teammates.ui.webapi.request.FeedbackQuestionUpdateRequest;

/**
 * SUT: {@link UpdateFeedbackQuestionAction}.
 */
public class UpdateFeedbackQuestionActionTest extends BaseActionTest<UpdateFeedbackQuestionAction> {

    @Override
    protected String getActionUri() {
        return Const.ResourceURIs.QUESTION;
    }

    @Override
    protected String getRequestMethod() {
        return PUT;
    }

    @Override
    @Test
    protected void testExecute() throws Exception {
        InstructorAttributes instructor1ofCourse1 = typicalBundle.instructors.get("instructor1OfCourse1");
        FeedbackSessionAttributes session = typicalBundle.feedbackSessions.get("session1InCourse1");
        FeedbackQuestionAttributes typicalQuestion =
                logic.getFeedbackQuestion(session.getFeedbackSessionName(), session.getCourseId(), 1);
        assertEquals(FeedbackQuestionType.TEXT, typicalQuestion.getQuestionType());

        loginAsInstructor(instructor1ofCourse1.getGoogleId());

        ______TS("Not enough parameters");

        verifyHttpParameterFailure();

        ______TS("success: Typical case");

        String[] param = new String[] {
                Const.ParamsNames.FEEDBACK_QUESTION_ID, typicalQuestion.getFeedbackQuestionId(),
        };
        FeedbackQuestionUpdateRequest updateRequest = getTypicalTextQuestionUpdateRequest();

        UpdateFeedbackQuestionAction a = getAction(updateRequest, param);
        JsonResult r = getJsonResult(a);

        assertEquals(HttpStatus.SC_OK, r.getStatusCode());
        FeedbackQuestionData response = (FeedbackQuestionData) r.getOutput();

        typicalQuestion = logic.getFeedbackQuestion(typicalQuestion.getId());
        assertEquals(typicalQuestion.getQuestionNumber(), response.getQuestionNumber());
        assertEquals(2, typicalQuestion.getQuestionNumber());

        assertEquals(typicalQuestion.getQuestionDetails().getQuestionText(), response.getQuestionBrief());
        assertEquals("this is the brief", typicalQuestion.getQuestionDetails().getQuestionText());

        assertEquals(typicalQuestion.getQuestionDescription(), response.getQuestionDescription());
        assertEquals("this is the description", typicalQuestion.getQuestionDescription());

        assertEquals(typicalQuestion.getQuestionType(), response.getQuestionType());
        assertEquals(FeedbackQuestionType.TEXT, typicalQuestion.getQuestionType());

        assertEquals(JsonUtils.toJson(typicalQuestion.getQuestionDetails()),
                JsonUtils.toJson(response.getQuestionDetails()));
        assertEquals(800, ((FeedbackTextQuestionDetails)
                typicalQuestion.getQuestionDetails()).getRecommendedLength());

        assertEquals(typicalQuestion.getGiverType(), typicalQuestion.getGiverType());
        assertEquals(FeedbackParticipantType.STUDENTS, typicalQuestion.getGiverType());

        assertEquals(typicalQuestion.getRecipientType(), typicalQuestion.getRecipientType());
        assertEquals(FeedbackParticipantType.INSTRUCTORS, typicalQuestion.getRecipientType());

        assertEquals(NumberOfEntitiesToGiveFeedbackToSetting.UNLIMITED,
                response.getNumberOfEntitiesToGiveFeedbackToSetting());
        assertEquals(Const.MAX_POSSIBLE_RECIPIENTS, typicalQuestion.getNumberOfEntitiesToGiveFeedbackTo());

        assertNull(response.getCustomNumberOfEntitiesToGiveFeedbackTo());

        assertTrue(response.getShowResponsesTo().isEmpty());
        assertTrue(typicalQuestion.getShowResponsesTo().isEmpty());
        assertTrue(response.getShowGiverNameTo().isEmpty());
        assertTrue(typicalQuestion.getShowGiverNameTo().isEmpty());
        assertTrue(response.getShowRecipientNameTo().isEmpty());
        assertTrue(typicalQuestion.getShowRecipientNameTo().isEmpty());
    }

    @Test
    public void testExecute_customizedNumberOfRecipient_shouldUpdateSuccessfully() {
        InstructorAttributes instructor1ofCourse1 = typicalBundle.instructors.get("instructor1OfCourse1");
        FeedbackSessionAttributes session = typicalBundle.feedbackSessions.get("session1InCourse1");
        FeedbackQuestionAttributes typicalQuestion =
                logic.getFeedbackQuestion(session.getFeedbackSessionName(), session.getCourseId(), 1);

        loginAsInstructor(instructor1ofCourse1.getGoogleId());

        String[] param = new String[] {
                Const.ParamsNames.FEEDBACK_QUESTION_ID, typicalQuestion.getFeedbackQuestionId(),
        };
        FeedbackQuestionUpdateRequest updateRequest = getTypicalTextQuestionUpdateRequest();
        updateRequest.setNumberOfEntitiesToGiveFeedbackToSetting(NumberOfEntitiesToGiveFeedbackToSetting.CUSTOM);
        updateRequest.setCustomNumberOfEntitiesToGiveFeedbackTo(10);

        UpdateFeedbackQuestionAction a = getAction(updateRequest, param);
        JsonResult r = getJsonResult(a);

        assertEquals(HttpStatus.SC_OK, r.getStatusCode());
        typicalQuestion = logic.getFeedbackQuestion(typicalQuestion.getId());

        assertEquals(10, typicalQuestion.getNumberOfEntitiesToGiveFeedbackTo());
    }

    @Test
    public void testExecute_anonymousTeamSession_shouldUpdateSuccessfully() {
        InstructorAttributes instructor1ofCourse1 = typicalBundle.instructors.get("instructor1OfCourse1");
        FeedbackSessionAttributes session = typicalBundle.feedbackSessions.get("session1InCourse1");
        FeedbackQuestionAttributes typicalQuestion =
                logic.getFeedbackQuestion(session.getFeedbackSessionName(), session.getCourseId(), 1);

        loginAsInstructor(instructor1ofCourse1.getGoogleId());

        String[] param = new String[] {
                Const.ParamsNames.FEEDBACK_QUESTION_ID, typicalQuestion.getFeedbackQuestionId(),
        };
        FeedbackQuestionUpdateRequest updateRequest = getTypicalTextQuestionUpdateRequest();
        updateRequest.setGiverType(FeedbackParticipantType.STUDENTS);
        updateRequest.setRecipientType(FeedbackParticipantType.TEAMS);
        updateRequest.setShowResponsesTo(Arrays.asList(FeedbackVisibilityType.RECIPIENT));
        updateRequest.setShowGiverNameTo(Arrays.asList());
        updateRequest.setShowRecipientNameTo(Arrays.asList(FeedbackVisibilityType.RECIPIENT));

        UpdateFeedbackQuestionAction a = getAction(updateRequest, param);
        JsonResult r = getJsonResult(a);

        assertEquals(HttpStatus.SC_OK, r.getStatusCode());
        typicalQuestion = logic.getFeedbackQuestion(typicalQuestion.getId());

        assertEquals(FeedbackParticipantType.STUDENTS, typicalQuestion.getGiverType());
        assertEquals(FeedbackParticipantType.TEAMS, typicalQuestion.getRecipientType());
        assertEquals(Arrays.asList(FeedbackParticipantType.RECEIVER), typicalQuestion.getShowResponsesTo());
        assertTrue(typicalQuestion.getShowGiverNameTo().isEmpty());
        assertEquals(Arrays.asList(FeedbackParticipantType.RECEIVER), typicalQuestion.getShowRecipientNameTo());
    }

    @Test
    public void testExecute_selfFeedback_shouldUpdateSuccessfully() {
        InstructorAttributes instructor1ofCourse1 = typicalBundle.instructors.get("instructor1OfCourse1");
        FeedbackSessionAttributes session = typicalBundle.feedbackSessions.get("session1InCourse1");
        FeedbackQuestionAttributes typicalQuestion =
                logic.getFeedbackQuestion(session.getFeedbackSessionName(), session.getCourseId(), 1);

        loginAsInstructor(instructor1ofCourse1.getGoogleId());

        String[] param = new String[] {
                Const.ParamsNames.FEEDBACK_QUESTION_ID, typicalQuestion.getFeedbackQuestionId(),
        };
        FeedbackQuestionUpdateRequest updateRequest = getTypicalTextQuestionUpdateRequest();
        updateRequest.setGiverType(FeedbackParticipantType.STUDENTS);
        updateRequest.setRecipientType(FeedbackParticipantType.SELF);
        updateRequest.setShowResponsesTo(Arrays.asList(FeedbackVisibilityType.RECIPIENT));
        updateRequest.setShowGiverNameTo(Arrays.asList());
        updateRequest.setShowRecipientNameTo(Arrays.asList(FeedbackVisibilityType.RECIPIENT));

        UpdateFeedbackQuestionAction a = getAction(updateRequest, param);
        JsonResult r = getJsonResult(a);

        assertEquals(HttpStatus.SC_OK, r.getStatusCode());
        typicalQuestion = logic.getFeedbackQuestion(typicalQuestion.getId());

        assertEquals(FeedbackParticipantType.STUDENTS, typicalQuestion.getGiverType());
        assertEquals(FeedbackParticipantType.SELF, typicalQuestion.getRecipientType());
        assertEquals(Arrays.asList(FeedbackParticipantType.RECEIVER), typicalQuestion.getShowResponsesTo());
        assertTrue(typicalQuestion.getShowGiverNameTo().isEmpty());
        assertEquals(Arrays.asList(FeedbackParticipantType.RECEIVER), typicalQuestion.getShowRecipientNameTo());
    }

    @Test
    public void testExecute_editingContributionTypeQuestion_shouldUpdateSuccessfully() {
        DataBundle dataBundle = loadDataBundle("/FeedbackSessionQuestionTypeTest.json");
        removeAndRestoreDataBundle(dataBundle);

        InstructorAttributes instructor1ofCourse1 = dataBundle.instructors.get("instructor1OfCourse1");

        loginAsInstructor(instructor1ofCourse1.googleId);

        FeedbackSessionAttributes fs = dataBundle.feedbackSessions.get("contribSession");
        FeedbackQuestionAttributes fq =
                logic.getFeedbackQuestion(fs.getFeedbackSessionName(), fs.getCourseId(), 1);
        FeedbackResponsesDb frDb = new FeedbackResponsesDb();

        ______TS("Edit text won't delete response");

        // There are already responses for this question
        assertFalse(frDb.getFeedbackResponsesForQuestion(fq.getId()).isEmpty());

        FeedbackQuestionUpdateRequest updateRequest = getTypicalContributionQuestionUpdateRequest();
        updateRequest.setQuestionNumber(fq.getQuestionNumber());
        updateRequest.setGiverType(fq.getGiverType());
        updateRequest.setRecipientType(fq.getRecipientType());
        updateRequest.setQuestionDetails(fq.getQuestionDetails());

        String[] param = new String[] {
                Const.ParamsNames.FEEDBACK_QUESTION_ID, fq.getFeedbackQuestionId(),
        };
        UpdateFeedbackQuestionAction a = getAction(updateRequest, param);
        JsonResult r = getJsonResult(a);

        assertEquals(HttpStatus.SC_OK, r.getStatusCode());

        // All existing responses should remain
        assertFalse(frDb.getFeedbackResponsesForQuestion(fq.getId()).isEmpty());

        ______TS("Edit: Invalid recipient type");

        assertThrows(InvalidHttpRequestBodyException.class, () -> {
            FeedbackQuestionUpdateRequest request = getTypicalContributionQuestionUpdateRequest();
            request.setQuestionNumber(fq.getQuestionNumber());
            request.setRecipientType(FeedbackParticipantType.STUDENTS);
            getJsonResult(getAction(request, param));
        });
    }

    @Test
    public void testExecute_invalidQuestionNumber_shouldThrowException() {
        InstructorAttributes instructor1ofCourse1 = typicalBundle.instructors.get("instructor1OfCourse1");
        FeedbackSessionAttributes session = typicalBundle.feedbackSessions.get("session1InCourse1");
        FeedbackQuestionAttributes typicalQuestion =
                logic.getFeedbackQuestion(session.getFeedbackSessionName(), session.getCourseId(), 1);

        loginAsInstructor(instructor1ofCourse1.getGoogleId());

        String[] param = new String[] {
                Const.ParamsNames.FEEDBACK_QUESTION_ID, typicalQuestion.getFeedbackQuestionId(),
        };
        FeedbackQuestionUpdateRequest updateRequest = getTypicalTextQuestionUpdateRequest();
        updateRequest.setQuestionNumber(-1);

        UpdateFeedbackQuestionAction a = getAction(updateRequest, param);

        assertThrows(InvalidHttpRequestBodyException.class, () -> {
            getJsonResult(a);
        });

        // question is not updated
        assertEquals(typicalQuestion.getQuestionDescription(),
                logic.getFeedbackQuestion(typicalQuestion.getId()).getQuestionDescription());
    }

    // TODO: ADD this test case in FeedbackTextQuestionDetailsTest
    @Test
    public void testExecute_invalidRecommendedLength_shouldThrowException() {
        InstructorAttributes instructor1ofCourse1 = typicalBundle.instructors.get("instructor1OfCourse1");
        FeedbackSessionAttributes session = typicalBundle.feedbackSessions.get("session1InCourse1");
        FeedbackQuestionAttributes typicalQuestion =
                logic.getFeedbackQuestion(session.getFeedbackSessionName(), session.getCourseId(), 1);

        loginAsInstructor(instructor1ofCourse1.getGoogleId());

        String[] param = new String[] {
                Const.ParamsNames.FEEDBACK_QUESTION_ID, typicalQuestion.getFeedbackQuestionId(),
        };

        FeedbackQuestionUpdateRequest updateRequest = getTypicalTextQuestionUpdateRequest();
        FeedbackTextQuestionDetails textQuestionDetails = new FeedbackTextQuestionDetails();
        // set recommended length as a negative integer
        textQuestionDetails.setRecommendedLength(-1);
        updateRequest.setQuestionDetails(textQuestionDetails);
        UpdateFeedbackQuestionAction a = getAction(updateRequest, param);

        assertThrows(InvalidHttpRequestBodyException.class, () -> getJsonResult(a));

        // question is not updated
        assertEquals(typicalQuestion.getQuestionDescription(),
                logic.getFeedbackQuestion(typicalQuestion.getId()).getQuestionDescription());

        // recommended length does not change
        assertEquals(0, ((FeedbackTextQuestionDetails) typicalQuestion.getQuestionDetails())
                .getRecommendedLength());
    }

    @Test
    public void testExecute_invalidGiverRecipientType_shouldThrowException() {
        InstructorAttributes instructor1ofCourse1 = typicalBundle.instructors.get("instructor1OfCourse1");
        FeedbackSessionAttributes session = typicalBundle.feedbackSessions.get("session1InCourse1");
        FeedbackQuestionAttributes typicalQuestion =
                logic.getFeedbackQuestion(session.getFeedbackSessionName(), session.getCourseId(), 1);

        loginAsInstructor(instructor1ofCourse1.getGoogleId());

        String[] param = new String[] {
                Const.ParamsNames.FEEDBACK_QUESTION_ID, typicalQuestion.getFeedbackQuestionId(),
        };
        FeedbackQuestionUpdateRequest updateRequest = getTypicalTextQuestionUpdateRequest();
        updateRequest.setGiverType(FeedbackParticipantType.TEAMS);
        updateRequest.setRecipientType(FeedbackParticipantType.OWN_TEAM_MEMBERS);

        UpdateFeedbackQuestionAction a = getAction(updateRequest, param);

        assertThrows(InvalidHttpRequestBodyException.class, () -> {
            getJsonResult(a);
        });

        // question is not updated
        assertEquals(typicalQuestion.getQuestionDescription(),
                logic.getFeedbackQuestion(typicalQuestion.getId()).getQuestionDescription());
    }

    private FeedbackQuestionUpdateRequest getTypicalTextQuestionUpdateRequest() {
        FeedbackQuestionUpdateRequest updateRequest = new FeedbackQuestionUpdateRequest();
        updateRequest.setQuestionNumber(2);
        updateRequest.setQuestionBrief("this is the brief");
        updateRequest.setQuestionDescription("this is the description");
        FeedbackTextQuestionDetails textQuestionDetails = new FeedbackTextQuestionDetails();
        textQuestionDetails.setRecommendedLength(800);
        updateRequest.setQuestionDetails(textQuestionDetails);
        updateRequest.setQuestionType(FeedbackQuestionType.TEXT);
        updateRequest.setGiverType(FeedbackParticipantType.STUDENTS);
        updateRequest.setRecipientType(FeedbackParticipantType.INSTRUCTORS);
        updateRequest.setNumberOfEntitiesToGiveFeedbackToSetting(NumberOfEntitiesToGiveFeedbackToSetting.UNLIMITED);

        updateRequest.setShowResponsesTo(new ArrayList<>());
        updateRequest.setShowGiverNameTo(new ArrayList<>());
        updateRequest.setShowRecipientNameTo(new ArrayList<>());

        return updateRequest;
    }

    private FeedbackQuestionUpdateRequest getTypicalContributionQuestionUpdateRequest() {
        FeedbackQuestionUpdateRequest updateRequest = new FeedbackQuestionUpdateRequest();
        updateRequest.setQuestionNumber(1);
        updateRequest.setQuestionBrief("this is the brief for contribution question");
        updateRequest.setQuestionDescription("this is the description for contribution question");
        FeedbackContributionQuestionDetails textQuestionDetails = new FeedbackContributionQuestionDetails();
        textQuestionDetails.setNotSureAllowed(false);
        updateRequest.setQuestionDetails(textQuestionDetails);
        updateRequest.setQuestionType(FeedbackQuestionType.CONTRIB);
        updateRequest.setGiverType(FeedbackParticipantType.STUDENTS);
        updateRequest.setRecipientType(FeedbackParticipantType.OWN_TEAM_MEMBERS_INCLUDING_SELF);
        updateRequest.setNumberOfEntitiesToGiveFeedbackToSetting(NumberOfEntitiesToGiveFeedbackToSetting.UNLIMITED);

        updateRequest.setShowResponsesTo(Arrays.asList(FeedbackVisibilityType.INSTRUCTORS));
        updateRequest.setShowGiverNameTo(Arrays.asList(FeedbackVisibilityType.INSTRUCTORS));
        updateRequest.setShowRecipientNameTo(Arrays.asList(FeedbackVisibilityType.INSTRUCTORS));

        return updateRequest;
    }

    @Override
    @Test
    protected void testAccessControl() throws Exception {
        InstructorAttributes instructor1OfCourse1 = typicalBundle.instructors.get("instructor1OfCourse1");
        FeedbackSessionAttributes fs = typicalBundle.feedbackSessions.get("session1InCourse1");
        FeedbackQuestionAttributes typicalQuestion =
                logic.getFeedbackQuestion(fs.getFeedbackSessionName(), fs.getCourseId(), 1);

        ______TS("non-existent feedback question");

        loginAsInstructor(instructor1OfCourse1.googleId);

        assertThrows(EntityNotFoundException.class, () -> {
            getAction(new String[] {Const.ParamsNames.FEEDBACK_QUESTION_ID, "random"}).checkSpecificAccessControl();
        });

        ______TS("inaccessible without ModifySessionPrivilege");

        String[] submissionParams = new String[] {
                Const.ParamsNames.FEEDBACK_QUESTION_ID, typicalQuestion.getFeedbackQuestionId(),
        };

        verifyInaccessibleWithoutModifySessionPrivilege(submissionParams);

        ______TS("only instructors of the same course can access");

        verifyOnlyInstructorsOfTheSameCourseCanAccess(submissionParams);
    }

}
