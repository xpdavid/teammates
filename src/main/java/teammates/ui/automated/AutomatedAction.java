package teammates.ui.automated;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;

import teammates.common.exception.NullHttpParameterException;
import teammates.common.util.Const;
import teammates.logic.api.EmailSender;
import teammates.logic.api.Logic;
import teammates.logic.api.TaskQueuer;

/**
 * An automated "action" to be performed by the system, triggered by cron jobs or task queues.
 * <p>
 * This class of action is different from the non-automated ones in the following manner:
 * <ul>
 *     <li>Non-administrators are barred from performing it.</li>
 *     <li>The limit for request is 10 minutes instead of 1 minute.</li>
 * </ul>
 * </p>
 */
public abstract class AutomatedAction {

    protected Logic logic;
    protected TaskQueuer taskQueuer;
    protected EmailSender emailSender;

    protected HttpServletRequest request;
    protected HttpServletResponse response;

    protected void initialiseAttributes(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
        this.logic = new Logic();
        setTaskQueuer(new TaskQueuer());
        setEmailSender(new EmailSender());
    }

    public TaskQueuer getTaskQueuer() {
        return taskQueuer;
    }

    public void setTaskQueuer(TaskQueuer taskQueuer) {
        this.taskQueuer = taskQueuer;
    }

    public EmailSender getEmailSender() {
        return emailSender;
    }

    public void setEmailSender(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    /**
     * Returns the first value for the specified parameter in the HTTP request, or null if such parameter is not found.
     */
    protected String getRequestParamValue(String paramName) {
        return request.getParameter(paramName);
    }

    /**
     * Returns the first value for the specified parameter expected to be present in the HTTP request.
     */
    protected String getNonNullRequestParamValue(String paramName) {
        return getNonNullRequestParamValues(paramName)[0];
    }

    /**
     * Returns the values for the specified parameter expected to be present in the HTTP request.
     */
    protected String[] getNonNullRequestParamValues(String paramName) {
        String[] values = request.getParameterValues(paramName);
        if (values == null || values.length == 0) {
            throw new NullHttpParameterException(String.format(Const.StatusCodes.NULL_HTTP_PARAMETER, paramName));
        }
        return values;
    }

    protected void setForRetry() {
        // Sets an arbitrary retry code outside of the range 200-299 so GAE will automatically retry upon failure
        response.setStatus(HttpStatus.SC_CONTINUE);
    }

    protected abstract String getActionMessage();

    /** Executes the action. */
    public abstract void execute();

}
