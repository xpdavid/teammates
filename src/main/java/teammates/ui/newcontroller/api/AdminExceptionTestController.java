package teammates.ui.newcontroller.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.google.appengine.api.datastore.DatastoreTimeoutException;
import com.google.apphosting.api.DeadlineExceededException;

import teammates.common.exception.InvalidHttpParameterException;
import teammates.common.exception.UnauthorizedAccessException;
import teammates.common.util.Config;
import teammates.logic.api.GateKeeper;
import teammates.ui.newcontroller.data.MessageResponse;

@RestController
public class AdminExceptionTestController extends BaseRestController {

    public AdminExceptionTestController(GateKeeper gateKeeper) {
        super(gateKeeper);
    }

    @SuppressWarnings("PMD.AvoidThrowingNullPointerException") // deliberately done for testing
    @GetMapping("/exception/{exceptionName}")
    public MessageResponse execute(@PathVariable("exceptionName") String exceptionName,
                                   @ModelAttribute("authVerifier") AuthenticationVerifier authVerifier) {
        authVerifier.checkSpecificAccessControl(() -> {
            if (!Config.isDevServer()) {
                throw new UnauthorizedAccessException("Admin privilege is required to access this resource.");
            }
        });

        if (exceptionName.equals(AssertionError.class.getSimpleName())) {
            throw new AssertionError("AssertionError testing");
        }
        if (exceptionName.equals(NullPointerException.class.getSimpleName())) {
            throw new NullPointerException("NullPointerException testing");
        }
        if (exceptionName.equals(DeadlineExceededException.class.getSimpleName())) {
            throw new DeadlineExceededException("DeadlineExceededException testing");
        }
        if (exceptionName.equals(DatastoreTimeoutException.class.getSimpleName())) {
            throw new DatastoreTimeoutException("DatastoreTimeoutException testing");
        }
        if (exceptionName.equals(InvalidHttpParameterException.class.getSimpleName())) {
            throw new InvalidHttpParameterException("InvalidHttpParameterException testing");
        }
        if (exceptionName.equals(UnauthorizedAccessException.class.getSimpleName())) {
            throw new UnauthorizedAccessException("UnauthorizedAccessException testing");
        }
        return new MessageResponse("Test output");
    }
}
