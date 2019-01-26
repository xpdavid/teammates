package teammates.ui.automated;

import java.util.List;

import teammates.common.datatransfer.attributes.FeedbackSessionAttributes;
import teammates.common.exception.TeammatesException;
import teammates.common.util.Const.ParamsNames;
import teammates.common.util.EmailWrapper;
import teammates.common.util.Logger;

/**
 * Task queue worker action: prepares session published reminder for a particular session to be sent.
 */
public class FeedbackSessionPublishedEmailWorkerAction extends AutomatedAction {

    private static final Logger log = Logger.getLogger();

    @Override
    public void execute() {
        String feedbackSessionName = getNonNullRequestParamValue(ParamsNames.EMAIL_FEEDBACK);
        String courseId = getNonNullRequestParamValue(ParamsNames.EMAIL_COURSE);

        FeedbackSessionAttributes session = logic.getFeedbackSession(feedbackSessionName, courseId);
        if (session == null) {
            log.severe("Feedback session object for feedback session name: " + feedbackSessionName
                       + " for course: " + courseId + " could not be fetched.");
            return;
        }
        List<EmailWrapper> emailsToBeSent = emailGenerator.generateFeedbackSessionPublishedEmails(session);
        try {
            taskQueuer.scheduleEmailsForSending(emailsToBeSent);
            logic.updateFeedbackSession(
                    FeedbackSessionAttributes
                            .updateOptionsBuilder(session.getFeedbackSessionName(), session.getCourseId())
                            .withSentPublishedEmail(true)
                            .build());
        } catch (Exception e) {
            log.severe("Unexpected error: " + TeammatesException.toStringWithStackTrace(e));
        }
    }

}
