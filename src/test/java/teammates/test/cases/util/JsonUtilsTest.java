package teammates.test.cases.util;

import org.testng.annotations.Test;

import com.google.gson.JsonParseException;

import teammates.common.datatransfer.attributes.FeedbackResponseAttributes;
import teammates.common.datatransfer.questions.FeedbackTextResponseDetails;
import teammates.common.util.JsonUtils;
import teammates.test.cases.BaseTestCase;

/**
 * SUT: {@link JsonUtils}.
 */
public class JsonUtilsTest extends BaseTestCase {

    @Test
    public void testFeedbackResponseDetailsAdaptor_withComposedResponseDetails_shouldSerializeToConcreteClass() {
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

        try {
            String serializeString = JsonUtils.toJson(fra);
            assertEquals("{\n"
                    + "  \"feedbackSessionName\": \"Session1\",\n"
                    + "  \"courseId\": \"CS3281\",\n"
                    + "  \"feedbackQuestionId\": \"questionId\",\n"
                    + "  \"giver\": \"giver@email.com\",\n"
                    + "  \"recipient\": \"recipient@email.com\",\n"
                    + "  \"responseDetails\": {\n"
                    + "    \"answer\": \"My answer\",\n"
                    + "    \"questionType\": \"TEXT\"\n"
                    + "  },\n"
                    + "  \"giverSection\": \"giverSection\",\n"
                    + "  \"recipientSection\": \"recipientSection\"\n"
                    + "}", serializeString);
        } catch (JsonParseException e) {
            fail("error detected during serializing");
        }
    }
}
