package teammates.ui.pagedata;

import java.util.List;

import teammates.common.datatransfer.attributes.AccountAttributes;
import teammates.common.util.StatusMessage;

/**
 * Page data for a page with created image URL
 */
public class FeedbackSubmissionEditSaveAjaxPageData extends PageData {
    public String redirectTo;
    public boolean isError;

    public FeedbackSubmissionEditSaveAjaxPageData(AccountAttributes account, List<StatusMessage> statusToUser,
                                                  boolean hasError) {
        super(account);
        setStatusMessagesToUser(statusToUser);
        isError = hasError;
    }

    public void setRedirectTo(String redirect) {
        redirectTo = redirect;
    }

    public void setIsError(boolean error) {
        isError = error;
    }
}
