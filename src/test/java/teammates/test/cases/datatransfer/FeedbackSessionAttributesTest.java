package teammates.test.cases.datatransfer;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.Sets;

import teammates.common.datatransfer.attributes.FeedbackSessionAttributes;
import teammates.common.util.Const;
import teammates.common.util.TimeHelper;
import teammates.storage.entity.FeedbackSession;
import teammates.test.cases.BaseTestCase;
import teammates.test.driver.TimeHelperExtension;

/**
 * SUT: {@link FeedbackSessionAttributes}.
 */
public class FeedbackSessionAttributesTest extends BaseTestCase {

    @Test
    public void testSort() {
        List<FeedbackSessionAttributes> testList = new ArrayList<>();
        List<FeedbackSessionAttributes> expected = new ArrayList<>();

        Instant time1 = TimeHelper.parseInstant("2014-01-01 12:00 AM +0000");
        Instant time2 = TimeHelper.parseInstant("2014-02-01 12:00 AM +0000");
        Instant time3 = TimeHelper.parseInstant("2014-03-01 12:00 AM +0000");

        FeedbackSessionAttributes s1 =
                FeedbackSessionAttributes.builder("Session 1", "", "")
                        .withStartTime(time1)
                        .withEndTime(time2)
                        .build();
        FeedbackSessionAttributes s2 =
                FeedbackSessionAttributes.builder("Session 2", "", "")
                        .withStartTime(time2)
                        .withEndTime(time3)
                        .build();
        FeedbackSessionAttributes s3 =
                FeedbackSessionAttributes.builder("Session 3", "", "")
                        .withStartTime(time1)
                        .withEndTime(time2)
                        .build();
        FeedbackSessionAttributes s4 =
                FeedbackSessionAttributes.builder("Session 4", "", "")
                        .withStartTime(time1)
                        .withEndTime(time3)
                        .build();
        FeedbackSessionAttributes s5 =
                FeedbackSessionAttributes.builder("Session 5", "", "")
                        .withStartTime(time2)
                        .withEndTime(time3)
                        .build();

        testList.add(s1);
        testList.add(s2);
        testList.add(s3);
        testList.add(s4);
        testList.add(s5);

        expected.add(s2);
        expected.add(s5);
        expected.add(s4);
        expected.add(s1);
        expected.add(s3);

        testList.sort(FeedbackSessionAttributes.DESCENDING_ORDER);
        for (int i = 0; i < testList.size(); i++) {
            assertEquals(expected.get(i), testList.get(i));
        }
    }

    @Test
    public void testBuilder_buildNothing_shouldUseDefaultValues() {
        FeedbackSessionAttributes fsa = FeedbackSessionAttributes
                .builder("name", "course", "email@email.com")
                .build();

        assertEquals("name", fsa.getFeedbackSessionName());
        assertEquals("course", fsa.getCourseId());
        assertEquals("email@email.com", fsa.getCreatorEmail());

        // Default values
        assertNotNull(fsa.getCreatedTime());
        assertEquals("", fsa.getInstructions());
        assertNull(fsa.getDeletedTime());
        assertNull(fsa.getStartTime());
        assertNull(fsa.getEndTime());
        assertNull(fsa.getSessionVisibleFromTime());
        assertNull(fsa.getResultsVisibleFromTime());
        assertEquals(Const.DEFAULT_TIME_ZONE, fsa.getTimeZone());
        assertEquals(0, fsa.getGracePeriodMinutes());

        assertFalse(fsa.isSentOpenEmail());
        assertFalse(fsa.isSentClosingEmail());
        assertFalse(fsa.isSentClosedEmail());
        assertFalse(fsa.isSentPublishedEmail());

        assertTrue(fsa.isOpeningEmailEnabled());
        assertTrue(fsa.isClosingEmailEnabled());
        assertTrue(fsa.isPublishedEmailEnabled());

        assertTrue(fsa.getRespondingInstructorList().isEmpty());
        assertTrue(fsa.getRespondingStudentList().isEmpty());
    }

    @Test
    public void testBuilder_withNullInput_shouldFailWithAssertionError() {
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.builder(null, "course", "email@email.com"));
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.builder("session name", null, "email@email.com"));
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.builder("session name", "course", null));
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.builder("session name", "course", "email@email.com")
                        .withInstructions(null));
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.builder("session name", "course", "email@email.com")
                        .withStartTime(null));
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.builder("session name", "course", "email@email.com")
                        .withEndTime(null));
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.builder("session name", "course", "email@email.com")
                        .withSessionVisibleFromTime(null));
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.builder("session name", "course", "email@email.com")
                        .withResultsVisibleFromTime(null));
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.builder("session name", "course", "email@email.com")
                        .withTimeZone(null));
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.builder("session name", "course", "email@email.com")
                        .withGracePeriod(null));
    }

    @Test
    public void testValueOf_withAllFieldPopulatedFeedbackSession_shouldGenerateAttributesCorrectly() {
        FeedbackSession feedbackSession = new FeedbackSession(
                "testName", "testCourse", "email@email.com", "text",
                Instant.now(), null,
                Instant.now().minusSeconds(10), Instant.now().plusSeconds(10),
                Instant.now().minusSeconds(20), Instant.now().plusSeconds(20),
                "UTC", 10,
                false, false, false, false,
                true, true, true,
                new HashSet<>(), new HashSet<>());

        FeedbackSessionAttributes feedbackSessionAttributes = FeedbackSessionAttributes.valueOf(feedbackSession);

        assertEquals(feedbackSession.getFeedbackSessionName(), feedbackSessionAttributes.getFeedbackSessionName());
        assertEquals(feedbackSession.getCourseId(), feedbackSessionAttributes.getCourseId());
        assertEquals(feedbackSession.getCreatorEmail(), feedbackSessionAttributes.getCreatorEmail());
        assertEquals(feedbackSession.getInstructions(), feedbackSessionAttributes.getInstructions());
        assertEquals(feedbackSession.getCreatedTime(), feedbackSessionAttributes.getCreatedTime());
        assertEquals(feedbackSession.getDeletedTime(), feedbackSessionAttributes.getDeletedTime());
        assertEquals(feedbackSession.getSessionVisibleFromTime(), feedbackSessionAttributes.getSessionVisibleFromTime());
        assertEquals(feedbackSession.getStartTime(), feedbackSessionAttributes.getStartTime());
        assertEquals(feedbackSession.getEndTime(), feedbackSessionAttributes.getEndTime());
        assertEquals(feedbackSession.getResultsVisibleFromTime(), feedbackSessionAttributes.getResultsVisibleFromTime());
        assertEquals(feedbackSession.isSentOpenEmail(), feedbackSessionAttributes.isSentOpenEmail());
        assertEquals(feedbackSession.isSentClosingEmail(), feedbackSessionAttributes.isSentClosingEmail());
        assertEquals(feedbackSession.isSentClosedEmail(), feedbackSessionAttributes.isSentClosedEmail());
        assertEquals(feedbackSession.isSentPublishedEmail(), feedbackSessionAttributes.isSentPublishedEmail());
        assertEquals(feedbackSession.isOpeningEmailEnabled(), feedbackSessionAttributes.isOpeningEmailEnabled());
        assertEquals(feedbackSession.isClosingEmailEnabled(), feedbackSessionAttributes.isClosingEmailEnabled());
        assertEquals(feedbackSession.isPublishedEmailEnabled(), feedbackSessionAttributes.isPublishedEmailEnabled());
        assertEquals(feedbackSession.getRespondingStudentList(), feedbackSessionAttributes.getRespondingStudentList());
        assertEquals(feedbackSession.getRespondingInstructorList(), feedbackSessionAttributes.getRespondingInstructorList());
    }

    @Test
    public void testValueOf_withSomeFieldsPopulatedAsNull_shouldUseDefaultValues() {
        FeedbackSession feedbackSession = new FeedbackSession(
                "testName", "testCourse", "email@email.com", null,
                Instant.now(), null,
                Instant.now().minusSeconds(10), Instant.now().plusSeconds(10),
                Instant.now().minusSeconds(20), Instant.now().plusSeconds(20),
                "UTC", 10,
                false, false, false, false,
                true, true, true,
                null, null);
        feedbackSession.setRespondingInstructorList(null);
        feedbackSession.setRespondingStudentList(null);
        assertNull(feedbackSession.getRespondingStudentList());
        assertNull(feedbackSession.getRespondingInstructorList());
        assertNull(feedbackSession.getInstructions());

        FeedbackSessionAttributes feedbackSessionAttributes = FeedbackSessionAttributes.valueOf(feedbackSession);

        assertEquals(feedbackSession.getFeedbackSessionName(), feedbackSessionAttributes.getFeedbackSessionName());
        assertEquals(feedbackSession.getCourseId(), feedbackSessionAttributes.getCourseId());
        assertEquals(feedbackSession.getCreatorEmail(), feedbackSessionAttributes.getCreatorEmail());
        assertEquals("", feedbackSessionAttributes.getInstructions());
        assertEquals(feedbackSession.getCreatedTime(), feedbackSessionAttributes.getCreatedTime());
        assertEquals(feedbackSession.getDeletedTime(), feedbackSessionAttributes.getDeletedTime());
        assertEquals(feedbackSession.getSessionVisibleFromTime(), feedbackSessionAttributes.getSessionVisibleFromTime());
        assertEquals(feedbackSession.getStartTime(), feedbackSessionAttributes.getStartTime());
        assertEquals(feedbackSession.getEndTime(), feedbackSessionAttributes.getEndTime());
        assertEquals(feedbackSession.getResultsVisibleFromTime(), feedbackSessionAttributes.getResultsVisibleFromTime());
        assertEquals(feedbackSession.isSentOpenEmail(), feedbackSessionAttributes.isSentOpenEmail());
        assertEquals(feedbackSession.isSentClosingEmail(), feedbackSessionAttributes.isSentClosingEmail());
        assertEquals(feedbackSession.isSentClosedEmail(), feedbackSessionAttributes.isSentClosedEmail());
        assertEquals(feedbackSession.isSentPublishedEmail(), feedbackSessionAttributes.isSentPublishedEmail());
        assertEquals(feedbackSession.isOpeningEmailEnabled(), feedbackSessionAttributes.isOpeningEmailEnabled());
        assertEquals(feedbackSession.isClosingEmailEnabled(), feedbackSessionAttributes.isClosingEmailEnabled());
        assertEquals(feedbackSession.isPublishedEmailEnabled(), feedbackSessionAttributes.isPublishedEmailEnabled());
        assertEquals(new HashSet<>(), feedbackSessionAttributes.getRespondingStudentList());
        assertEquals(new HashSet<>(), feedbackSessionAttributes.getRespondingInstructorList());
    }

    @Test
    public void testBuilder_withTypicalData_shouldBuildCorrectly() {

        ZoneId timeZone = ZoneId.of("Asia/Singapore");
        Instant startTime = TimeHelper.convertLocalDateTimeToInstant(
                TimeHelper.parseDateTimeFromSessionsForm("Mon, 09 May, 2016", "10", "0"), timeZone);
        Instant endTime = TimeHelper.convertLocalDateTimeToInstant(
                TimeHelper.parseDateTimeFromSessionsForm("Tue, 09 May, 2017", "10", "0"), timeZone);

        FeedbackSessionAttributes fsa = FeedbackSessionAttributes
                .builder("sessionName", "courseId", "email@email.com")
                .withInstructions("instructor")
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withSessionVisibleFromTime(startTime.minusSeconds(60))
                .withResultsVisibleFromTime(endTime.plusSeconds(60))
                .withTimeZone(timeZone)
                .withGracePeriod(Duration.ofMinutes(15))
                .withIsClosingEmailEnabled(false)
                .withIsPublishedEmailEnabled(false)
                .build();

        assertEquals("sessionName", fsa.getFeedbackSessionName());
        assertEquals("courseId", fsa.getCourseId());
        assertEquals("email@email.com", fsa.getCreatorEmail());
        assertEquals(startTime, fsa.getStartTime());
        assertEquals(endTime, fsa.getEndTime());
        assertEquals(startTime.minusSeconds(60), fsa.getSessionVisibleFromTime());
        assertEquals(endTime.plusSeconds(60), fsa.getResultsVisibleFromTime());
        assertEquals(timeZone, fsa.getTimeZone());
        assertEquals(15, fsa.getGracePeriodMinutes());
        assertTrue(fsa.isOpeningEmailEnabled());
        assertFalse(fsa.isClosingEmailEnabled());
        assertFalse(fsa.isPublishedEmailEnabled());

        assertFalse(fsa.isSentOpenEmail());
        assertFalse(fsa.isSentClosingEmail());
        assertFalse(fsa.isSentClosedEmail());
        assertFalse(fsa.isSentPublishedEmail());

    }

    @Test
    public void testGetCopy() {
        FeedbackSessionAttributes original = FeedbackSessionAttributes
                .builder("newFeedbackSessionName", "course", "email")
                .withInstructions("default instructions")
                .withStartTime(TimeHelperExtension.getInstantHoursOffsetFromNow(2))
                .withEndTime(TimeHelperExtension.getInstantHoursOffsetFromNow(5))
                .withSessionVisibleFromTime(TimeHelperExtension.getInstantHoursOffsetFromNow(1))
                .withResultsVisibleFromTime(TimeHelperExtension.getInstantHoursOffsetFromNow(6))
                .withTimeZone(ZoneId.of("Asia/Singapore"))
                .withGracePeriod(Duration.ZERO)
                .withIsClosingEmailEnabled(false)
                .withIsPublishedEmailEnabled(false)
                .build();

        FeedbackSessionAttributes copy = original.getCopy();

        assertEquals(original.getFeedbackSessionName(), copy.getFeedbackSessionName());
        assertEquals(original.getCourseId(), copy.getCourseId());
        assertEquals(original.getCreatorEmail(), copy.getCreatorEmail());
        assertEquals(original.getInstructions(), copy.getInstructions());
        assertEquals(original.getCreatedTime(), copy.getCreatedTime());
        assertEquals(original.getDeletedTime(), copy.getDeletedTime());
        assertEquals(original.getStartTime(), copy.getStartTime());
        assertEquals(original.getEndTime(), copy.getEndTime());
        assertEquals(original.getSessionVisibleFromTime(), copy.getSessionVisibleFromTime());
        assertEquals(original.getResultsVisibleFromTime(), copy.getResultsVisibleFromTime());
        assertEquals(original.getTimeZone(), copy.getTimeZone());
        assertEquals(original.getGracePeriodMinutes(), copy.getGracePeriodMinutes());
        assertEquals(original.isOpeningEmailEnabled(), copy.isOpeningEmailEnabled());
        assertEquals(original.isClosingEmailEnabled(), copy.isClosingEmailEnabled());
        assertEquals(original.isPublishedEmailEnabled(), copy.isPublishedEmailEnabled());
        assertEquals(original.isSentClosedEmail(), copy.isSentClosedEmail());
        assertEquals(original.isSentClosingEmail(), copy.isSentClosingEmail());
        assertEquals(original.isSentOpenEmail(), copy.isSentOpenEmail());
        assertEquals(original.isSentPublishedEmail(), copy.isSentPublishedEmail());
        assertEquals(original.getRespondingInstructorList(), copy.getRespondingInstructorList());
        assertEquals(original.getRespondingStudentList(), copy.getRespondingStudentList());
    }

    @Test
    public void testValidate() {
        ______TS("invalid parameter error messages");

        FeedbackSessionAttributes feedbackSessionAttributes = FeedbackSessionAttributes
                .builder("", "", "")
                .withStartTime(Instant.now())
                .withEndTime(Instant.now())
                .withResultsVisibleFromTime(Instant.now())
                .withSessionVisibleFromTime(Instant.now())
                .withGracePeriod(Duration.ofMinutes(-100L))
                .build();

        String feedbackSessionNameError = "The field 'feedback session name' should not be empty. The value of 'feedback "
                + "session name' field should be no longer than 38 characters.";
        String courseIdError = "The field 'course ID' is empty. A course ID can contain letters, numbers, fullstops, "
                + "hyphens, underscores, and dollar signs. It cannot be longer than 40 characters, cannot be empty and "
                + "cannot contain spaces.";
        String creatorEmailError = "The field 'email' is empty. An email address contains some text followed "
                + "by one '@' sign followed by some more text. It cannot be longer than 254 characters, cannot be empty"
                + " and cannot contain spaces.";
        String gracePeriodError = "Grace period should not be negative." + " "
                + "The value must be one of the options in the grace period dropdown selector.";

        assertEquals(Arrays.asList(feedbackSessionNameError, courseIdError, creatorEmailError, gracePeriodError),
                feedbackSessionAttributes.getInvalidityInfo());
    }

    @Test
    public void testGetBackUpIdentifier() {
        FeedbackSessionAttributes sessionAttributes = FeedbackSessionAttributes
                .builder("newFeedbackSessionName", "course", "email")
                .withInstructions("default instructions")
                .withStartTime(TimeHelperExtension.getInstantHoursOffsetFromNow(2))
                .withEndTime(TimeHelperExtension.getInstantHoursOffsetFromNow(5))
                .withSessionVisibleFromTime(TimeHelperExtension.getInstantHoursOffsetFromNow(1))
                .withResultsVisibleFromTime(TimeHelperExtension.getInstantHoursOffsetFromNow(6))
                .withTimeZone(ZoneId.of("Asia/Singapore"))
                .withGracePeriod(Duration.ZERO)
                .withIsClosingEmailEnabled(false)
                .withIsPublishedEmailEnabled(false)
                .build();

        String expectedBackUpIdentifierMessage = "Recently modified feedback session::"
                + sessionAttributes.getCourseId() + "::" + sessionAttributes.getFeedbackSessionName();
        assertEquals(expectedBackUpIdentifierMessage, sessionAttributes.getBackupIdentifier());
    }

    @Test
    public void testUpdateOptions_withTypicalUpdateOptions_shouldUpdateAttributeCorrectly() {
        Instant sessionVisibleTime = TimeHelper.getInstantDaysOffsetFromNow(-3);
        Instant startTime = TimeHelper.getInstantDaysOffsetFromNow(-2);
        Instant endTime = TimeHelper.getInstantDaysOffsetFromNow(-1);
        Instant resultVisibleTime = TimeHelper.getInstantDaysOffsetFromNow(1);
        FeedbackSessionAttributes.UpdateOptions updateOptions =
                FeedbackSessionAttributes.updateOptionsBuilder("sessionName", "courseId")
                        .withInstructions("instruction 1")
                        .withStartTime(startTime)
                        .withEndTime(endTime)
                        .withSessionVisibleFromTime(sessionVisibleTime)
                        .withResultsVisibleFromTime(resultVisibleTime)
                        .withTimeZone(ZoneId.of("Asia/Singapore"))
                        .withGracePeriod(Duration.ofMinutes(5))
                        .withSentOpenEmail(true)
                        .withSentClosingEmail(true)
                        .withSentClosedEmail(true)
                        .withSentPublishedEmail(true)
                        .withIsClosingEmailEnabled(true)
                        .withIsPublishedEmailEnabled(true)
                        .withAddingInstructorRespondent("instructor@email.com")
                        .withAddingStudentRespondent("student@email.com")
                        .withUpdatingStudentRespondent("studentA@email.com", "studentB@email.com")
                        .withUpdatingInstructorRespondent("insturctorA@email.com", "insturctorB@email.com")
                        .withRemovingStudentRespondent("studentF@email.com")
                        .withRemovingInstructorRespondent("instructorF@email.com")
                        .build();

        assertEquals("sessionName", updateOptions.getFeedbackSessionName());
        assertEquals("courseId", updateOptions.getCourseId());

        FeedbackSessionAttributes feedbackSessionAttributes =
                FeedbackSessionAttributes.builder("sessionName", "courseId", "i@email.com")
                        .withInstructions("instruction")
                        .withStartTime(TimeHelper.getInstantDaysOffsetFromNow(1))
                        .withEndTime(TimeHelper.getInstantDaysOffsetFromNow(2))
                        .withSessionVisibleFromTime(sessionVisibleTime.minusSeconds(60))
                        .withResultsVisibleFromTime(Instant.now().minusSeconds(60))
                        .withTimeZone(ZoneId.of("UTC"))
                        .withGracePeriod(Duration.ofMinutes(20))
                        .withIsClosingEmailEnabled(false)
                        .withIsPublishedEmailEnabled(false)
                        .build();
        feedbackSessionAttributes.getRespondingInstructorList().add("insturctorA@email.com");
        feedbackSessionAttributes
                .getRespondingStudentList().addAll(Arrays.asList("studentA@email.com", "studentF@email.com"));

        feedbackSessionAttributes.update(updateOptions);

        assertEquals("instruction 1", feedbackSessionAttributes.getInstructions());
        assertEquals(startTime, feedbackSessionAttributes.getStartTime());
        assertEquals(endTime, feedbackSessionAttributes.getEndTime());
        assertEquals(sessionVisibleTime, feedbackSessionAttributes.getSessionVisibleFromTime());
        assertEquals(resultVisibleTime, feedbackSessionAttributes.getResultsVisibleFromTime());
        assertEquals(ZoneId.of("Asia/Singapore"), feedbackSessionAttributes.getTimeZone());
        assertEquals(5, feedbackSessionAttributes.getGracePeriodMinutes());
        assertTrue(feedbackSessionAttributes.isSentOpenEmail());
        assertTrue(feedbackSessionAttributes.isSentClosingEmail());
        assertTrue(feedbackSessionAttributes.isSentClosedEmail());
        assertTrue(feedbackSessionAttributes.isSentPublishedEmail());
        assertTrue(feedbackSessionAttributes.isOpeningEmailEnabled());
        assertTrue(feedbackSessionAttributes.isClosingEmailEnabled());
        assertTrue(feedbackSessionAttributes.isPublishedEmailEnabled());
        assertEquals(Sets.newHashSet("student@email.com", "studentB@email.com"),
                feedbackSessionAttributes.getRespondingStudentList());
        assertEquals(Sets.newHashSet("instructor@email.com", "insturctorB@email.com"),
                feedbackSessionAttributes.getRespondingInstructorList());

        // constructor update option based on existing update option
        FeedbackSessionAttributes.UpdateOptions newUpdateOptions =
                FeedbackSessionAttributes.updateOptionsBuilder(updateOptions)
                        .withInstructions("instruction")
                        .build();
        feedbackSessionAttributes.update(newUpdateOptions);
        assertEquals("instruction", feedbackSessionAttributes.getInstructions());
    }

    @Test
    public void testUpdateOptionsBuilder_withNullInput_shouldFailWithAssertionError() {
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.updateOptionsBuilder(null));
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.updateOptionsBuilder(null, "courseId"));
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.updateOptionsBuilder("session", null));
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.updateOptionsBuilder("session", "courseId")
                        .withInstructions(null));
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.updateOptionsBuilder("session", "courseId")
                        .withStartTime(null));
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.updateOptionsBuilder("session", "courseId")
                        .withEndTime(null));
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.updateOptionsBuilder("session", "courseId")
                        .withSessionVisibleFromTime(null));
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.updateOptionsBuilder("session", "courseId")
                        .withResultsVisibleFromTime(null));
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.updateOptionsBuilder("session", "courseId")
                        .withTimeZone(null));
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.updateOptionsBuilder("session", "courseId")
                        .withGracePeriod(null));
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.updateOptionsBuilder("session", "courseId")
                        .withAddingInstructorRespondent(null));
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.updateOptionsBuilder("session", "courseId")
                        .withAddingStudentRespondent(null));
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.updateOptionsBuilder("session", "courseId")
                        .withUpdatingStudentRespondent(null, "email@email.com"));
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.updateOptionsBuilder("session", "courseId")
                        .withUpdatingStudentRespondent("email@email.com", null));
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.updateOptionsBuilder("session", "courseId")
                        .withUpdatingInstructorRespondent(null, "email@email.com"));
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.updateOptionsBuilder("session", "courseId")
                        .withUpdatingInstructorRespondent("email@email.com", null));
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.updateOptionsBuilder("session", "courseId")
                        .withRemovingStudentRespondent(null));
        assertThrows(AssertionError.class, () ->
                FeedbackSessionAttributes.updateOptionsBuilder("session", "courseId")
                        .withRemovingInstructorRespondent(null));
    }

}
