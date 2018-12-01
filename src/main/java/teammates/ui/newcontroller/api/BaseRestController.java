package teammates.ui.newcontroller.api;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import teammates.common.datatransfer.UserInfo;
import teammates.common.exception.UnauthorizedAccessException;
import teammates.common.util.Config;
import teammates.common.util.Const;
import teammates.logic.api.GateKeeper;
import teammates.ui.newcontroller.AuthType;

@RequestMapping(value = "/webapi", produces = MediaType.APPLICATION_JSON_VALUE)
public abstract class BaseRestController {

    protected GateKeeper gateKeeper;

    public BaseRestController(GateKeeper gateKeeper) {
        this.gateKeeper = gateKeeper;
    }

    @ModelAttribute
    protected void addAuthenticationInfo(HttpServletRequest req, Model model) {
        AuthenticationVerifier authVerifier = new AuthenticationVerifier(req);

        model.addAttribute("authVerifier", authVerifier);
    }

    public class AuthenticationVerifier {

        private AuthType authType;
        private UserInfo userInfo;

        private AuthenticationVerifier(HttpServletRequest req) {
            if (Config.BACKDOOR_KEY.equals(req.getHeader("Backdoor-Key"))) {
                authType = AuthType.ALL_ACCESS;
                return;
            }

            userInfo = gateKeeper.getCurrentUser();
            authType = userInfo == null ? AuthType.PUBLIC : AuthType.LOGGED_IN;

            String userParam = req.getParameter(Const.ParamsNames.USER_ID);
            if (userInfo != null && userInfo.isAdmin && userParam != null) {
                userInfo = gateKeeper.getMasqueradeUser(userParam);
                authType = AuthType.MASQUERADE;
            }
        }

        public AuthenticationVerifier requireMinAuthLevel(AuthType minAuthType) throws UnauthorizedAccessException {
            if (authType.getLevel() < minAuthType.getLevel()) {
                // Access control level lower than required
                throw new UnauthorizedAccessException("Not authorized to access this resource.");
            }

            return this;
        }

        public AuthenticationVerifier checkSpecificAccessControl(Runnable specificAccessControlCheck) throws UnauthorizedAccessException {
            if (authType == AuthType.ALL_ACCESS) {
                // All-access pass granted
                return this;
            }

            specificAccessControlCheck.run();

            return this;
        }

        public AuthType getAuthType() {
            return authType;
        }

        public UserInfo getUserInfo() {
            return userInfo;
        }
    }
}
