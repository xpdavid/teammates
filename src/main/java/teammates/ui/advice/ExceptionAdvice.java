package teammates.ui.advice;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.google.appengine.api.datastore.DatastoreTimeoutException;
import com.google.apphosting.api.DeadlineExceededException;

import teammates.common.exception.InvalidHttpParameterException;
import teammates.common.exception.TeammatesException;
import teammates.common.exception.UnauthorizedAccessException;
import teammates.common.util.Config;
import teammates.common.util.Logger;
import teammates.ui.newcontroller.data.MessageResponse;

@ControllerAdvice
public class ExceptionAdvice {

    private static final Logger log = Logger.getLogger();

    @Order(1)
    @ResponseBody
    @ExceptionHandler(InvalidHttpParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public MessageResponse invalidHttpParameterExceptionHandler(InvalidHttpParameterException ihpe) {
        return generateMessageResponseWithException(ihpe);
    }

    @Order(2)
    @ResponseBody
    @ExceptionHandler(UnauthorizedAccessException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public MessageResponse unauthorizedAccessExceptionHandler(UnauthorizedAccessException uae) {
        return generateMessageResponseWithException(uae);
    }

    @Order(3)
    @ResponseBody
    @ExceptionHandler({ DeadlineExceededException.class, DatastoreTimeoutException.class})
    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    public MessageResponse timeoutHandler(RuntimeException e) {

        // This exception may not be caught because GAE kills the request soon after throwing it
        // In that case, the error message in the log will be emailed to the admin by a separate cron job

        log.severe(e.getClass().getSimpleName() + " caught by WebApiServlet: "
                + TeammatesException.toStringWithStackTrace(e));

        return generateMessageResponseWithException(e);
    }

    @Order(4)
    @ResponseBody
    @ExceptionHandler({ Throwable.class })
    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    public MessageResponse genericExceptionHandler(Throwable t) {
        log.severe(t.getClass().getSimpleName() + " caught by WebApiServlet: "
                + TeammatesException.toStringWithStackTrace(t));

        return generateMessageResponseWithException(t);
    }

    private MessageResponse generateMessageResponseWithException(Throwable t) {
        return new MessageResponse(t.getMessage());
    }
}
