package teammates.ui.newcontroller;

import java.lang.reflect.Type;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import teammates.common.datatransfer.UserInfo;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.exception.InvalidHttpParameterException;
import teammates.common.exception.InvalidHttpRequestBodyException;
import teammates.common.exception.NullHttpParameterException;
import teammates.common.exception.UnauthorizedAccessException;
import teammates.common.util.Config;
import teammates.common.util.Const;
import teammates.common.util.HttpRequestHelper;
import teammates.common.util.JsonUtils;
import teammates.logic.api.EmailGenerator;
import teammates.logic.api.EmailSender;
import teammates.logic.api.GateKeeper;
import teammates.logic.api.Logic;
import teammates.logic.api.TaskQueuer;

/**
 * An "action" to be performed by the system.
 * If the requesting user is allowed to perform the requested action,
 * this object can talk to the back end to perform that action.
 */
public abstract class Action {

    protected Logic logic = new Logic();
    protected GateKeeper gateKeeper = new GateKeeper();
    protected EmailGenerator emailGenerator = new EmailGenerator();
    protected TaskQueuer taskQueuer = new TaskQueuer();
    protected EmailSender emailSender = new EmailSender();

    protected HttpServletRequest req;
    protected HttpServletResponse resp;
    protected UserInfo userInfo;
    protected AuthType authType;

    // buffer to store the request body
    private String requestBody;

    /**
     * Initializes the action object based on the HTTP request.
     */
    protected void init(HttpServletRequest req, HttpServletResponse resp) {
        this.req = req;
        this.resp = resp;
        initAuthInfo();
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
     * Checks if the requesting user has sufficient authority to access the resource.
     */
    public void checkAccessControl() {
        if (authType.getLevel() < getMinAuthLevel().getLevel()) {
            // Access control level lower than required
            throw new UnauthorizedAccessException("Not authorized to access this resource.");
        }

        if (authType == AuthType.ALL_ACCESS) {
            // All-access pass granted
            return;
        }

        // All other cases: to be dealt in case-by-case basis
        checkSpecificAccessControl();
    }

    private void initAuthInfo() {
        if (Config.BACKDOOR_KEY.equals(req.getHeader("Backdoor-Key"))) {
            authType = AuthType.ALL_ACCESS;
            return;
        }

        userInfo = gateKeeper.getCurrentUser();
        authType = userInfo == null ? AuthType.PUBLIC : AuthType.LOGGED_IN;

        String userParam = getRequestParamValue(Const.ParamsNames.USER_ID);
        if (userInfo != null && userInfo.isAdmin && userParam != null) {
            userInfo = gateKeeper.getMasqueradeUser(userParam);
            authType = AuthType.MASQUERADE;
        }
    }

    /**
     * Returns the first value for the specified parameter in the HTTP request, or null if such parameter is not found.
     */
    protected String getRequestParamValue(String paramName) {
        return req.getParameter(paramName);
    }

    /**
     * Returns the first value for the specified parameter expected to be present in the HTTP request.
     */
    protected String getNonNullRequestParamValue(String paramName) {
        return getNonNullRequestParamValues(paramName)[0];
    }

    /**
     * Returns the first value for the specified parameter expected to be present in the HTTP request as boolean.
     */
    @SuppressWarnings("PMD.PreserveStackTrace")
    protected boolean getBooleanRequestParamValue(String paramName) {
        String value = getNonNullRequestParamValue(paramName);
        try {
            return Boolean.parseBoolean(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidHttpParameterException(
                    "Expected boolean value for " + paramName + " parameter, but found: [" + value + "]");
        }
    }

    /**
     * Returns the values for the specified parameter in the HTTP request, or null if such parameter is not found.
     */
    protected String[] getRequestParamValues(String paramName) {
        return req.getParameterValues(paramName);
    }

    /**
     * Returns the values for the specified parameter expected to be present in the HTTP request.
     */
    protected String[] getNonNullRequestParamValues(String paramName) {
        String[] values = getRequestParamValues(paramName);
        if (values == null || values.length == 0) {
            throw new NullHttpParameterException(String.format(Const.StatusCodes.NULL_HTTP_PARAMETER, paramName));
        }
        return values;
    }

    /**
     * Returns the request body payload.
     */
    protected String getRequestBody() {
        if (requestBody == null) {
            requestBody = HttpRequestHelper.getRequestBody(req);
        }
        return requestBody;
    }

    /**
     * Deserializes and validates the request body payload.
     */
    protected <T extends RequestBody> T getAndValidateRequestBody(Type typeOfBody) {
        T requestBody = JsonUtils.fromJson(getRequestBody(), typeOfBody);
        requestBody.validate();
        return requestBody;
    }

    /**
     * Gets the unregistered student by the HTTP param.
     *
     * @throws UnauthorizedAccessException if HTTP param is provided but student cannot be found
     */
    protected Optional<StudentAttributes> getUnregisteredStudent() {
        String key = getRequestParamValue(Const.ParamsNames.REGKEY);
        if (key != null) {
            StudentAttributes studentAttributes = logic.getStudentForRegistrationKey(key);
            if (studentAttributes == null) {
                throw new UnauthorizedAccessException("RegKey is not valid.");
            } else {
                return Optional.of(studentAttributes);
            }
        }
        return Optional.empty();
    }

    /**
     * Gets the minimum access control level required to access the resource.
     */
    protected abstract AuthType getMinAuthLevel();

    /**
     * Checks the specific access control needs for the resource.
     */
    public abstract void checkSpecificAccessControl();

    /**
     * Executes the action.
     */
    public abstract ActionResult execute();

    /**
     * The request body of a HTTP request.
     */
    public abstract static class RequestBody {

        /**
         * Validate the request.
         */
        public abstract void validate();

        /**
         * Asserts a condition or throws {@link InvalidHttpRequestBodyException}.
         */
        public void assertTrue(boolean condition, String message) {
            if (!condition) {
                throw new InvalidHttpRequestBodyException(message);
            }
        }
    }

}
