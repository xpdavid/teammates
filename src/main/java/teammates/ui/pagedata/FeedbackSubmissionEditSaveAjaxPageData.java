package teammates.ui.pagedata;

import teammates.common.datatransfer.attributes.AccountAttributes;

/**
 * Page data for a page with created image URL
 */
public class FeedbackSubmissionEditSaveAjaxPageData extends PageData {
    public String redirectTo;

    public FeedbackSubmissionEditSaveAjaxPageData(AccountAttributes account) {
        super(account);
    }

    public void setRedirectTo(String redirect) {
        redirectTo = redirect;
    }
}
