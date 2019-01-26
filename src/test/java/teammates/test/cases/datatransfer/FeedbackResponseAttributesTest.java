package teammates.test.cases.datatransfer;

import java.time.Instant;

import org.testng.annotations.Test;

import teammates.common.datatransfer.attributes.FeedbackResponseAttributes;
import teammates.common.datatransfer.questions.FeedbackQuestionType;
import teammates.common.datatransfer.questions.FeedbackTextResponseDetails;
import teammates.common.util.Const;
import teammates.test.cases.BaseTestCase;

/**
 * SUT: {@link FeedbackResponseAttributes}.
 */
public class FeedbackResponseAttributesTest extends BaseTestCase {

    private static class FeedbackResponseAttributesWithModifiableTimestamp extends FeedbackResponseAttributes {

        void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }

        void setUpdatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
        }

    }

    @Test
    public void testDefaultTimestamp() {
        FeedbackResponseAttributesWithModifiableTimestamp fra =
                new FeedbackResponseAttributesWithModifiableTimestamp();

        fra.setCreatedAt(null);
        fra.setUpdatedAt(null);

        Instant defaultTimeStamp = Const.TIME_REPRESENTS_DEFAULT_TIMESTAMP;

        ______TS("success : defaultTimeStamp for createdAt date");

        assertEquals(defaultTimeStamp, fra.getCreatedAt());

        ______TS("success : defaultTimeStamp for updatedAt date");

        assertEquals(defaultTimeStamp, fra.getUpdatedAt());
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

        FeedbackResponseAttributes feedbackResponseAttributes =
                new FeedbackResponseAttributes("session", "course", "questionId",
                        FeedbackQuestionType.TEXT, "giver2", "section3", "recipient2", "section4", "Test 2");

        feedbackResponseAttributes.update(updateOptions);

        assertEquals("session", feedbackResponseAttributes.feedbackSessionName);
        assertEquals("course", feedbackResponseAttributes.courseId);
        assertEquals("questionId", feedbackResponseAttributes.feedbackQuestionId);
        assertEquals(FeedbackQuestionType.TEXT, feedbackResponseAttributes.feedbackQuestionType);
        assertEquals("giver1", feedbackResponseAttributes.giver);
        assertEquals("section1", feedbackResponseAttributes.giverSection);
        assertEquals("recipient1", feedbackResponseAttributes.recipient);
        assertEquals("section2", feedbackResponseAttributes.recipientSection);
        assertEquals("Test 1", feedbackResponseAttributes.responseMetaData);
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
