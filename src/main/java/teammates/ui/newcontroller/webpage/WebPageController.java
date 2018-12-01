package teammates.ui.newcontroller.webpage;

import java.util.Arrays;

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller that handles the single web page.
 */
@Controller
public class WebPageController {

    private static final String CSP_POLICY = String.join("; ", Arrays.asList(
            "default-src 'none'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",
            "frame-src 'self' docs.google.com",
            "img-src 'self' data:",
            "connect-src 'self'",
            "form-action 'none'",
            "frame-ancestors 'self'",
            "base-uri 'self'"
    ));

    @RequestMapping("/web/**")
    public String handleFrontEndRequest(HttpServletResponse resp) {
        resp.setHeader("Content-Security-Policy", CSP_POLICY);
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "SAMEORIGIN");
        resp.setHeader("X-XSS-Protection", "1; mode=block");
        resp.setHeader("Strict-Transport-Security", "max-age=31536000");
        return "/dist/index.html";
    }

    @RequestMapping("/")
    public String welcomeStranger() {
        return "redirect:/web/front/home";
    }

}