package teammates.test.cases.datatransfer;

import org.testng.annotations.Test;

import teammates.common.datatransfer.attributes.FeedbackResponseAttributes;
import teammates.common.datatransfer.questions.FeedbackQuestionType;
import teammates.common.datatransfer.questions.FeedbackResponseDetails;
import teammates.common.datatransfer.questions.FeedbackTextResponseDetails;
import teammates.common.util.Const;
import teammates.storage.entity.FeedbackResponse;
import teammates.test.cases.BaseTestCase;

/**
 * SUT: {@link FeedbackResponseAttributes}.
 */
public class FeedbackResponseAttributesTest extends BaseTestCase {

    @Test
    public void testValueOf_withAllFieldPopulatedFeedbackResponse_shouldGenerateAttributesCorrectly() {
        FeedbackResponse response = new FeedbackResponse("session", "course", "id",
                FeedbackQuestionType.TEXT, "giver@email.com", "section1",
                "recipient@email.com", "section2", "answer");

        FeedbackResponseAttributes fra = FeedbackResponseAttributes.valueOf(response);

        assertEquals(response.getFeedbackSessionName(), fra.getFeedbackSessionName());
        assertEquals(response.getCourseId(), fra.getCourseId());
        assertEquals(response.getFeedbackQuestionId(), fra.getFeedbackQuestionId());
        assertEquals(response.getFeedbackQuestionType(), fra.getFeedbackQuestionType());
        assertEquals(response.getGiverEmail(), fra.getGiver());
        assertEquals(response.getGiverSection(), fra.getGiverSection());
        assertEquals(response.getRecipientEmail(), fra.getRecipient());
        assertEquals(response.getRecipientSection(), fra.getRecipientSection());
        assertEquals(response.getResponseMetaData(), fra.getResponseDetails().getAnswerString());
        assertEquals(response.getCreatedAt(), fra.getCreatedAt());
        assertEquals(response.getUpdatedAt(), fra.getUpdatedAt());
    }

    @Test
    public void testValueOf_withSomeFieldsPopulatedAsNull_shouldUseDefaultValues() {
        FeedbackResponse response = new FeedbackResponse("session", "course", "id",
                FeedbackQuestionType.TEXT, "giver@email.com", null,
                "recipient@email.com", null, "answer");
        response.setLastUpdate(null);
        response.setCreatedAt(null);
        assertNull(response.getGiverSection());
        assertNull(response.getRecipientSection());

        FeedbackResponseAttributes fra = FeedbackResponseAttributes.valueOf(response);

        assertEquals(response.getFeedbackSessionName(), fra.getFeedbackSessionName());
        assertEquals(response.getCourseId(), fra.getCourseId());
        assertEquals(response.getFeedbackQuestionId(), fra.getFeedbackQuestionId());
        assertEquals(response.getFeedbackQuestionType(), fra.getFeedbackQuestionType());
        assertEquals(response.getGiverEmail(), fra.getGiver());
        assertEquals(Const.DEFAULT_SECTION, fra.getGiverSection());
        assertEquals(response.getRecipientEmail(), fra.getRecipient());
        assertEquals(Const.DEFAULT_SECTION, fra.getRecipientSection());
        assertEquals(response.getResponseMetaData(), fra.getResponseDetails().getAnswerString());
        assertEquals(Const.TIME_REPRESENTS_DEFAULT_TIMESTAMP, fra.getCreatedAt());
        assertEquals(Const.TIME_REPRESENTS_DEFAULT_TIMESTAMP, fra.getUpdatedAt());
    }

    @Test
    public void testBuilder_buildNothing_shouldUseDefaultValue() {
        FeedbackResponseAttributes fra = FeedbackResponseAttributes.builder().build();

        assertNull(fra.getId());
        assertNull(fra.getCourseId());
        assertNull(fra.getFeedbackSessionName());
        assertNull(fra.getFeedbackQuestionId());
        assertNull(fra.getGiver());
        assertEquals(Const.DEFAULT_SECTION, fra.getGiverSection());
        assertNull(fra.getRecipient());
        assertEquals(Const.DEFAULT_SECTION, fra.getRecipientSection());
        assertNull(fra.getCreatedAt());
        assertNull(fra.getUpdatedAt());
    }

    @Test
    public void testBuilder_withNullArguments_shouldThrowException() {
        assertThrows(AssertionError.class, () -> {
            FeedbackResponseAttributes
                    .builder()
                    .withCourseId(null)
                    .build();
        });

        assertThrows(AssertionError.class, () -> {
            FeedbackResponseAttributes
                    .builder()
                    .withFeedbackSessionName(null)
                    .build();
        });

        assertThrows(AssertionError.class, () -> {
            FeedbackResponseAttributes
                    .builder()
                    .withFeedbackQuestionId(null)
                    .build();
        });

        assertThrows(AssertionError.class, () -> {
            FeedbackResponseAttributes
                    .builder()
                    .withGiver(null)
                    .build();
        });

        assertThrows(AssertionError.class, () -> {
            FeedbackResponseAttributes
                    .builder()
                    .withGiverSection(null)
                    .build();
        });

        assertThrows(AssertionError.class, () -> {
            FeedbackResponseAttributes
                    .builder()
                    .withRecipient(null)
                    .build();
        });

        assertThrows(AssertionError.class, () -> {
            FeedbackResponseAttributes
                    .builder()
                    .withRecipientSection(null)
                    .build();
        });

        assertThrows(AssertionError.class, () -> {
            FeedbackResponseAttributes
                    .builder()
                    .withResponseDetails(null)
                    .build();
        });
    }

    @Test
    public void testBuilder_withTypicalData_shouldBuildCorrectAttributes() {
        FeedbackResponseAttributes fra = FeedbackResponseAttributes.builder()
                .withFeedbackSessionName("Session1")
                .withCourseId("CS3281")
                .withFeedbackQuestionId("questionId")
                .withGiver("giver@email.com")
                .withGiverSection("giverSection")
                .withRecipient("recipient@email.com")
                .withRecipientSection("recipientSection")
                .withResponseDetails(new FeedbackTextResponseDetails("My answer"))
                .build();

        assertEquals("Session1", fra.getFeedbackSessionName());
        assertEquals("CS3281", fra.getCourseId());
        assertEquals("questionId", fra.getFeedbackQuestionId());
        assertEquals("giver@email.com", fra.getGiver());
        assertEquals("giverSection", fra.getGiverSection());
        assertEquals("recipient@email.com", fra.getRecipient());
        assertEquals("recipientSection", fra.getRecipientSection());
        assertEquals("My answer", fra.getResponseDetails().getAnswerString());
    }

    @Test
    public void testCopyConstructor_shouldDoDeepCopyOfResponseDetails() {
        FeedbackResponseAttributes fra1 = FeedbackResponseAttributes.builder()
                .withResponseDetails(new FeedbackTextResponseDetails("My original answer"))
                .build();
        FeedbackResponseAttributes fra2 = new FeedbackResponseAttributes(fra1);

        ((FeedbackTextResponseDetails) fra2.responseDetails).answer = "My second answer";
        assertEquals(fra1.responseDetails.getAnswerString(), "My original answer");
        assertEquals(fra2.responseDetails.getAnswerString(), "My second answer");

    }

    @Test
    public void testGetResponseDetails_shouldDoDeepCopy() {
        FeedbackResponseAttributes fra = FeedbackResponseAttributes.builder()
                .withResponseDetails(new FeedbackTextResponseDetails("My original answer"))
                .build();
        FeedbackResponseDetails frdDeep = fra.getResponseDetails();

        ((FeedbackTextResponseDetails) fra.responseDetails).answer = "My second answer";
        assertEquals(frdDeep.getAnswerString(), "My original answer");
    }

    @Test
    public void testSetResponseDetails_shouldDoDeepCopy() {
        FeedbackResponseAttributes fra = FeedbackResponseAttributes.builder()
                .withResponseDetails(new FeedbackTextResponseDetails("My original answer"))
                .build();
        FeedbackTextResponseDetails updatedDetails = new FeedbackTextResponseDetails("Updated answer");
        fra.setResponseDetails(updatedDetails);
        updatedDetails.answer = "Modified deep copy answer";

        assertEquals(updatedDetails.getAnswerString(), "Modified deep copy answer");
        assertEquals(fra.responseDetails.getAnswerString(), "Updated answer");

    }

    @Test
    public void testGetBackUpIdentifier() {
        FeedbackResponse response = new FeedbackResponse("session", "course", "id",
                FeedbackQuestionType.TEXT, "giver@email.com", "section1",
                "recipient@email.com", "section2", "answer");
        FeedbackResponseAttributes responseAttributes = FeedbackResponseAttributes.valueOf(response);

        String expectedBackUpIdentifierMessage = "Recently modified feedback response::" + responseAttributes.getId();
        assertEquals(expectedBackUpIdentifierMessage, responseAttributes.getBackupIdentifier());
    }

    @Test
    public void testUpdateOptions_withTypicalUpdateOptions_shouldUpdateAttributeCorrectly() {
        FeedbackResponseAttributes.UpdateOptions updateOptions =
                FeedbackResponseAttributes.updateOptionsBuilder("responseId")
                        .withGiver("giver1")
                        .withGiverSection("section1")
                        .withRecipient("recipient1")
                        .withRecipientSection("section2")
                        .withResponseDetails(new FeedbackTextResponseDetails("Test 1"))
                        .build();

        assertEquals("responseId", updateOptions.getFeedbackResponseId());

        FeedbackResponseAttributes feedbackResponseAttributes = FeedbackResponseAttributes.builder()
                .withCourseId("course")
                .withFeedbackSessionName("session")
                .withFeedbackQuestionId("questionId")
                .withGiver("giver2")
                .withGiverSection("section3")
                .withRecipient("recipient2")
                .withRecipientSection("section4")
                .withResponseDetails(new FeedbackTextResponseDetails("Test 2"))
                .build();

        feedbackResponseAttributes.update(updateOptions);

        assertEquals("session", feedbackResponseAttributes.feedbackSessionName);
        assertEquals("course", feedbackResponseAttributes.courseId);
        assertEquals("questionId", feedbackResponseAttributes.feedbackQuestionId);
        assertEquals(FeedbackQuestionType.TEXT, feedbackResponseAttributes.getFeedbackQuestionType());
        assertEquals("giver1", feedbackResponseAttributes.giver);
        assertEquals("section1", feedbackResponseAttributes.giverSection);
        assertEquals("recipient1", feedbackResponseAttributes.recipient);
        assertEquals("section2", feedbackResponseAttributes.recipientSection);
        assertEquals("Test 1", feedbackResponseAttributes.getResponseDetails().getAnswerString());
    }

    @Test
    public void testUpdateOptionsBuilder_withNullInput_shouldFailWithAssertionError() {
        assertThrows(AssertionError.class, () ->
                FeedbackResponseAttributes.updateOptionsBuilder(null));
        assertThrows(AssertionError.class, () ->
                FeedbackResponseAttributes.updateOptionsBuilder("id")
                        .withGiver(null));
        assertThrows(AssertionError.class, () ->
                FeedbackResponseAttributes.updateOptionsBuilder("id")
                        .withGiverSection(null));
        assertThrows(AssertionError.class, () ->
                FeedbackResponseAttributes.updateOptionsBuilder("id")
                        .withRecipient(null));
        assertThrows(AssertionError.class, () ->
                FeedbackResponseAttributes.updateOptionsBuilder("id")
                        .withRecipientSection(null));
        assertThrows(AssertionError.class, () ->
                FeedbackResponseAttributes.updateOptionsBuilder("id")
                        .withResponseDetails(null));
    }
}
