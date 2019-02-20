package teammates.ui.webapi.action;

import org.apache.http.HttpStatus;

import teammates.common.datatransfer.attributes.FeedbackSessionAttributes;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidHttpRequestBodyException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.Const;
import teammates.ui.webapi.output.FeedbackSessionData;
import teammates.ui.webapi.request.FeedbackSessionSaveRequest;

/**
 * Save a feedback session.
 */
public class SaveFeedbackSessionAction extends Action {

    @Override
    protected AuthType getMinAuthLevel() {
        return AuthType.LOGGED_IN;
    }

    @Override
    public void checkSpecificAccessControl() {
        String courseId = getNonNullRequestParamValue(Const.ParamsNames.COURSE_ID);
        String feedbackSessionName = getNonNullRequestParamValue(Const.ParamsNames.FEEDBACK_SESSION_NAME);
        FeedbackSessionAttributes feedbackSession = logic.getFeedbackSession(feedbackSessionName, courseId);

        gateKeeper.verifyAccessible(
                logic.getInstructorForGoogleId(courseId, userInfo.getId()),
                feedbackSession,
                Const.ParamsNames.INSTRUCTOR_PERMISSION_MODIFY_SESSION);
    }

    @Override
    public ActionResult execute() {
        String courseId = getNonNullRequestParamValue(Const.ParamsNames.COURSE_ID);
        String feedbackSessionName = getNonNullRequestParamValue(Const.ParamsNames.FEEDBACK_SESSION_NAME);

        FeedbackSessionSaveRequest saveRequest =
                getAndValidateRequestBody(FeedbackSessionSaveRequest.class);

        try {
            FeedbackSessionAttributes updateFeedbackSession = logic.updateFeedbackSession(
                    FeedbackSessionAttributes.updateOptionsBuilder(feedbackSessionName, courseId)
                            .withInstructions(saveRequest.getInstructions())
                            .withStartTime(saveRequest.getSubmissionStartTime())
                            .withEndTime(saveRequest.getSubmissionEndTime())
                            .withGracePeriod(saveRequest.getGracePeriod())
                            .withSessionVisibleFromTime(saveRequest.getSessionVisibleFromTime())
                            .withResultsVisibleFromTime(saveRequest.getResultsVisibleFromTime())
                            .withIsClosingEmailEnabled(saveRequest.isClosingEmailEnabled())
                            .withIsPublishedEmailEnabled(saveRequest.isPublishedEmailEnabled())
                            .build());

            return new JsonResult(new FeedbackSessionData(updateFeedbackSession));
        } catch (InvalidParametersException ipe) {
            throw new InvalidHttpRequestBodyException(ipe.getMessage(), ipe);
        } catch (EntityDoesNotExistException ednee) {
            return new JsonResult(ednee.getMessage(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

}
