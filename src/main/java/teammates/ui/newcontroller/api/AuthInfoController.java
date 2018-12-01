package teammates.ui.newcontroller.api;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import teammates.common.datatransfer.UserInfo;
import teammates.common.util.Config;
import teammates.common.util.Const;
import teammates.common.util.HttpRequestHelper;
import teammates.common.util.StringHelper;
import teammates.logic.api.GateKeeper;
import teammates.ui.newcontroller.data.AuthInfoResponse;

@RestController
public class AuthInfoController extends BaseRestController {

    public AuthInfoController(GateKeeper gateKeeper) {
        super(gateKeeper);
    }

    @GetMapping("/auth")
    public AuthInfoResponse getAuthInfo(
            HttpServletRequest req,
            HttpServletResponse res,
            @ModelAttribute("authVerifier") AuthenticationVerifier authVerifier,
            @RequestParam(value = "frontendUrl", required = false, defaultValue = "") String frontendUrl) {

        AuthInfoResponse response = null;
        if (authVerifier.getUserInfo() == null) {

            response = new AuthInfoResponse(
                    gateKeeper.getLoginUrl(frontendUrl + Const.WebPageURIs.STUDENT_HOME_PAGE),
                    gateKeeper.getLoginUrl(frontendUrl + Const.WebPageURIs.INSTRUCTOR_HOME_PAGE),
                    gateKeeper.getLoginUrl(frontendUrl + Const.WebPageURIs.ADMIN_HOME_PAGE)
            );
        } else {
            boolean isMasquerade = !authVerifier.getUserInfo().getId().equals(gateKeeper.getCurrentUser().getId());
            response = new AuthInfoResponse(authVerifier.getUserInfo(), isMasquerade,
                    gateKeeper.getLogoutUrl(frontendUrl + "/web"));
        }

        String csrfToken = StringHelper.encrypt(req.getSession().getId());
        String existingCsrfToken = HttpRequestHelper.getCookieValueFromRequest(req, Const.CsrfConfig.TOKEN_COOKIE_NAME);
        if (!csrfToken.equals(existingCsrfToken)) {
            Cookie csrfTokenCookie = new Cookie(Const.CsrfConfig.TOKEN_COOKIE_NAME, csrfToken);
            csrfTokenCookie.setSecure(!Config.isDevServer());
            csrfTokenCookie.setPath("/");
            res.addCookie(csrfTokenCookie);
        }

        return response;
    }
}
