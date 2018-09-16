package teammates.test.cases.storage;

import static teammates.common.util.FieldValidator.SESSION_END_TIME_FIELD_NAME;
import static teammates.common.util.FieldValidator.SESSION_START_TIME_FIELD_NAME;
import static teammates.common.util.FieldValidator.TIME_FRAME_ERROR_MESSAGE;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import teammates.common.datatransfer.DataBundle;
import teammates.common.datatransfer.attributes.FeedbackSessionAttributes;
import teammates.common.exception.EntityAlreadyExistsException;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.Const;
import teammates.common.util.JsonUtils;
import teammates.storage.api.FeedbackSessionsDb;
import teammates.test.cases.BaseComponentTestCase;
import teammates.test.driver.AssertHelper;

/**
 * SUT: {@link FeedbackSessionsDb}.
 */
public class FeedbackSessionsDbTest extends BaseComponentTestCase {

    private static final FeedbackSessionsDb fsDb = new FeedbackSessionsDb();
    private DataBundle dataBundle = getTypicalDataBundle();

    @BeforeMethod
    public void addSessionsToDb() throws Exception {
        Set<String> keys = dataBundle.feedbackSessions.keySet();
        for (String i : keys) {
            try {
                fsDb.createEntity(dataBundle.feedbackSessions.get(i));
            } catch (EntityAlreadyExistsException e) {
                fsDb.updateFeedbackSession(dataBundle.feedbackSessions.get(i));
            }
        }
    }

    @AfterMethod
    public void deleteSessionsFromDb() {
        Set<String> keys = dataBundle.feedbackSessions.keySet();
        for (String i : keys) {
            fsDb.deleteEntity(dataBundle.feedbackSessions.get(i));
        }
        fsDb.deleteEntity(getNewFeedbackSession());
    }

    @Test
    public void testGetAllOpenFeedbackSessions_typicalCase_shouldQuerySuccessfullyWithoutDuplication() {
        Instant rangeStart = Instant.parse("2000-12-03T10:15:30.00Z");
        Instant rangeEnd = Instant.parse("2050-04-30T21:59:00Z");
        List<FeedbackSessionAttributes> actualAttributesList = fsDb.getAllOpenFeedbackSessions(rangeStart, rangeEnd);
        assertEquals("should not return more than 13 sessions as there are only 13 distinct sessions in the range",
                13, actualAttributesList.size());
    }

    @Test
    public void testCreateDeleteFeedbackSession()
            throws Exception {

        ______TS("standard success case");

        FeedbackSessionAttributes fsa = getNewFeedbackSession();
        fsDb.createEntity(fsa);
        verifyPresentInDatastore(fsa);

        ______TS("duplicate");
        try {
            fsDb.createEntity(fsa);
            signalFailureToDetectException();
        } catch (EntityAlreadyExistsException e) {
            AssertHelper.assertContains(String.format(FeedbackSessionsDb.ERROR_CREATE_ENTITY_ALREADY_EXISTS,
                                                      fsa.getEntityTypeAsString())
                                            + fsa.getIdentificationString(),
                                        e.getMessage());
        }

        fsDb.deleteEntity(fsa);
        verifyAbsentInDatastore(fsa);

        ______TS("null params");

        try {
            fsDb.createEntity(null);
            signalFailureToDetectException();
        } catch (AssertionError e) {
            AssertHelper.assertContains(Const.StatusCodes.DBLEVEL_NULL_INPUT, e.getLocalizedMessage());
        }

        ______TS("invalid params");

        try {
            fsa.setStartTime(Instant.now());
            fsDb.createEntity(fsa);
            signalFailureToDetectException();
        } catch (InvalidParametersException e) {
            // start time is now after end time
            AssertHelper.assertContains("start time", e.getLocalizedMessage());
        }

    }

    @Test
    public void testGetSoftDeletedFeedbackSession_typicalCase_shouldGetDeletedSession() {
        assertNotNull(fsDb.getSoftDeletedFeedbackSession("idOfTypicalCourse4",
                "First feedback session"));
    }

    @Test
    public void testGetSoftDeletedFeedbackSession_sessionIsNotDeleted_shouldReturnNull() {
        assertNotNull(fsDb.getFeedbackSession("idOfTypicalCourse2", "Instructor feedback session"));
        assertNull(fsDb.getSoftDeletedFeedbackSession("idOfTypicalCourse2", "Instructor feedback session"));
    }

    @Test
    public void testAllGetFeedbackSessions() {

        testGetFeedbackSessions();
        testGetFeedbackSessionsForCourse();
        testGetSoftDeletedFeedbackSessionsForCourse();
    }

    private void testGetFeedbackSessions() {

        ______TS("standard success case");

        FeedbackSessionAttributes expected =
                dataBundle.feedbackSessions.get("session1InCourse2");
        FeedbackSessionAttributes actual =
                fsDb.getFeedbackSession("idOfTypicalCourse2", "Instructor feedback session");

        assertEquals(expected.toString(), actual.toString());

        ______TS("non-existant session");

        assertNull(fsDb.getFeedbackSession("non-course", "Non-existant feedback session"));

        ______TS("soft-deleted session");

        assertNotNull(fsDb.getSoftDeletedFeedbackSession("idOfTypicalCourse4", "First feedback session"));
        assertNull(fsDb.getFeedbackSession("idOfTypicalCourse4", "First feedback session"));

        ______TS("null fsName");

        try {
            fsDb.getFeedbackSession("idOfTypicalCourse1", null);
            signalFailureToDetectException();
        } catch (AssertionError e) {
            AssertHelper.assertContains(Const.StatusCodes.DBLEVEL_NULL_INPUT, e.getLocalizedMessage());
        }

        ______TS("null courseId");

        try {
            fsDb.getFeedbackSession(null, "First feedback session");
            signalFailureToDetectException();
        } catch (AssertionError e) {
            AssertHelper.assertContains(Const.StatusCodes.DBLEVEL_NULL_INPUT, e.getLocalizedMessage());
        }

    }

    private void testGetFeedbackSessionsForCourse() {

        ______TS("standard success case");

        List<FeedbackSessionAttributes> sessions = fsDb.getFeedbackSessionsForCourse("idOfTypicalCourse1");

        String expected =
                dataBundle.feedbackSessions.get("session1InCourse1").toString() + System.lineSeparator()
                + dataBundle.feedbackSessions.get("session2InCourse1").toString() + System.lineSeparator()
                + dataBundle.feedbackSessions.get("empty.session").toString() + System.lineSeparator()
                + dataBundle.feedbackSessions.get("awaiting.session").toString() + System.lineSeparator()
                + dataBundle.feedbackSessions.get("closedSession").toString() + System.lineSeparator()
                + dataBundle.feedbackSessions.get("gracePeriodSession").toString() + System.lineSeparator();

        for (FeedbackSessionAttributes session : sessions) {
            AssertHelper.assertContains(session.toString(), expected);
        }
        assertEquals(6, sessions.size());

        ______TS("null params");

        try {
            fsDb.getFeedbackSessionsForCourse(null);
            signalFailureToDetectException();
        } catch (AssertionError e) {
            AssertHelper.assertContains(Const.StatusCodes.DBLEVEL_NULL_INPUT, e.getLocalizedMessage());
        }

        ______TS("non-existant course");

        assertTrue(fsDb.getFeedbackSessionsForCourse("non-existant course").isEmpty());

        ______TS("no sessions in course");

        assertTrue(fsDb.getFeedbackSessionsForCourse("idOfCourseNoEvals").isEmpty());
    }

    private void testGetSoftDeletedFeedbackSessionsForCourse() {

        ______TS("standard success case");

        List<FeedbackSessionAttributes> softDeletedSessions = fsDb
                .getSoftDeletedFeedbackSessionsForCourse("idOfTypicalCourse3");

        String expected =
                dataBundle.feedbackSessions.get("session2InCourse3").toString() + System.lineSeparator();

        for (FeedbackSessionAttributes session : softDeletedSessions) {
            AssertHelper.assertContains(session.toString(), expected);
        }
        assertEquals(1, softDeletedSessions.size());

        ______TS("null params");

        try {
            fsDb.getSoftDeletedFeedbackSessionsForCourse(null);
            signalFailureToDetectException();
        } catch (AssertionError e) {
            AssertHelper.assertContains(Const.StatusCodes.DBLEVEL_NULL_INPUT, e.getLocalizedMessage());
        }

        ______TS("non-existant course");

        assertTrue(fsDb.getSoftDeletedFeedbackSessionsForCourse("non-existant course").isEmpty());

        ______TS("no sessions in course");

        assertTrue(fsDb.getSoftDeletedFeedbackSessionsForCourse("idOfCourseNoEvals").isEmpty());
    }

    @Test
    public void testGetFeedbackSessionsPossiblyNeedingOpenEmail() throws Exception {

        ______TS("standard success case");

        List<FeedbackSessionAttributes> fsaList = fsDb.getFeedbackSessionsPossiblyNeedingOpenEmail();

        assertEquals(1, fsaList.size());
        for (FeedbackSessionAttributes fsa : fsaList) {
            assertFalse(fsa.isSentOpenEmail());
            assertFalse(fsa.isSessionDeleted());
        }

        ______TS("soft-deleted session should not appear");

        // soft delete a feedback session now
        FeedbackSessionAttributes feedbackSession = fsaList.get(0);
        feedbackSession.setDeletedTime();
        fsDb.updateFeedbackSession(feedbackSession);

        fsaList = fsDb.getFeedbackSessionsPossiblyNeedingOpenEmail();
        assertEquals(0, fsaList.size());
    }

    @Test
    public void testGetFeedbackSessionsPossiblyNeedingClosingEmail() throws Exception {

        ______TS("standard success case");

        List<FeedbackSessionAttributes> fsaList = fsDb.getFeedbackSessionsPossiblyNeedingClosingEmail();

        assertEquals(9, fsaList.size());
        for (FeedbackSessionAttributes fsa : fsaList) {
            assertFalse(fsa.isSentClosingEmail());
            assertTrue(fsa.isClosingEmailEnabled());
            assertFalse(fsa.isSessionDeleted());
        }

        ______TS("soft-deleted session should not appear");

        // soft delete a feedback session now
        FeedbackSessionAttributes feedbackSession = fsaList.get(0);
        feedbackSession.setDeletedTime();
        fsDb.updateFeedbackSession(feedbackSession);

        fsaList = fsDb.getFeedbackSessionsPossiblyNeedingClosingEmail();
        assertEquals(8, fsaList.size());
    }

    @Test
    public void testGetFeedbackSessionsPossiblyNeedingClosedEmail() throws Exception {

        ______TS("standard success case");

        List<FeedbackSessionAttributes> fsaList = fsDb.getFeedbackSessionsPossiblyNeedingClosedEmail();

        assertEquals(9, fsaList.size());
        for (FeedbackSessionAttributes fsa : fsaList) {
            assertFalse(fsa.isSentClosedEmail());
            assertTrue(fsa.isClosingEmailEnabled());
            assertFalse(fsa.isSessionDeleted());
        }

        ______TS("soft-deleted session should not appear");

        // soft delete a feedback session now
        FeedbackSessionAttributes feedbackSession = fsaList.get(0);
        feedbackSession.setDeletedTime();
        fsDb.updateFeedbackSession(feedbackSession);

        fsaList = fsDb.getFeedbackSessionsPossiblyNeedingClosedEmail();
        assertEquals(8, fsaList.size());
    }

    @Test
    public void testGetFeedbackSessionsPossiblyNeedingPublishedEmail() throws Exception {

        ______TS("standard success case");

        List<FeedbackSessionAttributes> fsaList = fsDb.getFeedbackSessionsPossiblyNeedingPublishedEmail();

        assertEquals(11, fsaList.size());
        for (FeedbackSessionAttributes fsa : fsaList) {
            assertFalse(fsa.isSentPublishedEmail());
            assertTrue(fsa.isPublishedEmailEnabled());
            assertFalse(fsa.isSessionDeleted());
        }

        ______TS("soft-deleted session should not appear");

        // soft delete a feedback session now
        FeedbackSessionAttributes feedbackSession = fsaList.get(0);
        feedbackSession.setDeletedTime();
        fsDb.updateFeedbackSession(feedbackSession);

        fsaList = fsDb.getFeedbackSessionsPossiblyNeedingPublishedEmail();
        assertEquals(10, fsaList.size());
    }

    @Test
    public void testUpdateFeedbackSession() throws Exception {

        ______TS("null params");
        try {
            fsDb.updateFeedbackSession(null);
            signalFailureToDetectException();
        } catch (AssertionError e) {
            AssertHelper.assertContains(Const.StatusCodes.DBLEVEL_NULL_INPUT, e.getLocalizedMessage());
        }
        ______TS("invalid feedback sesion attributes");
        FeedbackSessionAttributes invalidFs = getNewFeedbackSession();
        fsDb.deleteEntity(invalidFs);
        fsDb.createEntity(invalidFs);
        Instant afterEndTime = invalidFs.getEndTime().plus(Duration.ofDays(30));
        invalidFs.setStartTime(afterEndTime);
        invalidFs.setResultsVisibleFromTime(afterEndTime);
        try {
            fsDb.updateFeedbackSession(invalidFs);
            signalFailureToDetectException();
        } catch (InvalidParametersException e) {
            assertEquals(
                    String.format(TIME_FRAME_ERROR_MESSAGE, SESSION_END_TIME_FIELD_NAME,
                                  SESSION_START_TIME_FIELD_NAME),
                    e.getLocalizedMessage());
        }
        ______TS("feedback session does not exist");
        FeedbackSessionAttributes nonexistantFs = getNewFeedbackSession();
        nonexistantFs.setFeedbackSessionName("non existant fs");
        nonexistantFs.setCourseId("non.existant.course");
        try {
            fsDb.updateFeedbackSession(nonexistantFs);
            signalFailureToDetectException();
        } catch (EntityDoesNotExistException e) {
            AssertHelper.assertContains(FeedbackSessionsDb.ERROR_UPDATE_NON_EXISTENT, e.getLocalizedMessage());
        }
        ______TS("standard success case");
        FeedbackSessionAttributes modifiedSession = getNewFeedbackSession();
        fsDb.deleteEntity(modifiedSession);
        fsDb.createEntity(modifiedSession);
        verifyPresentInDatastore(modifiedSession);
        modifiedSession.setInstructions("new instructions");
        modifiedSession.setGracePeriodMinutes(0);
        modifiedSession.setSentOpenEmail(false);
        modifiedSession.setDeletedTime(Instant.now());
        fsDb.updateFeedbackSession(modifiedSession);
        FeedbackSessionAttributes actualFs =
                fsDb.getSoftDeletedFeedbackSession(modifiedSession.getCourseId(), modifiedSession.getFeedbackSessionName());
        assertEquals(JsonUtils.toJson(modifiedSession), JsonUtils.toJson(actualFs));
        assertTrue(fsDb.getFeedbackSessionsForCourse("testCourse").isEmpty());
        assertFalse(fsDb.getSoftDeletedFeedbackSessionsForCourse("testCourse").isEmpty());
    }

    private FeedbackSessionAttributes getNewFeedbackSession() {
        return FeedbackSessionAttributes.builder("fsTest1", "testCourse", "valid@email.com")
                .withCreatedTime(Instant.now())
                .withStartTime(Instant.now())
                .withEndTime(Instant.now())
                .withSessionVisibleFromTime(Instant.now())
                .withResultsVisibleFromTime(Instant.now())
                .withGracePeriodMinutes(5)
                .withSentOpenEmail(true)
                .withSentPublishedEmail(true)
                .withInstructions("Give feedback.")
                .build();
    }

}
