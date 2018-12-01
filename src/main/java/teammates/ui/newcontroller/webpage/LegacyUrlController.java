package teammates.ui.newcontroller.webpage;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import teammates.common.util.Config;
import teammates.common.util.Const;

@Controller
public class LegacyUrlController {

    @GetMapping("/page/**")
    public String legacyUrlRedirect(HttpServletRequest req) {
        String uri = req.getRequestURI();
        if (uri.contains(";")) {
            uri = uri.split(";")[0];
        }
        String redirectUrl;

        switch (uri) {
            case Const.LegacyURIs.INSTRUCTOR_COURSE_JOIN:
                String key = req.getParameter(Const.ParamsNames.REGKEY);
                redirectUrl = Config.getFrontEndAppUrl(Const.WebPageURIs.JOIN_PAGE)
                        .withRegistrationKey(key)
                        .withParam(Const.ParamsNames.ENTITY_TYPE, Const.EntityType.INSTRUCTOR)
                        .toString();
                break;
            case Const.LegacyURIs.STUDENT_COURSE_JOIN:
            case Const.LegacyURIs.STUDENT_COURSE_JOIN_NEW:
                String key0 = req.getParameter(Const.ParamsNames.REGKEY);
                redirectUrl = Config.getFrontEndAppUrl(Const.WebPageURIs.JOIN_PAGE)
                        .withRegistrationKey(key0)
                        .withParam(Const.ParamsNames.ENTITY_TYPE, Const.EntityType.STUDENT)
                        .toString();
                break;
            case Const.LegacyURIs.STUDENT_FEEDBACK_SUBMISSION_EDIT_PAGE:
            case Const.LegacyURIs.INSTRUCTOR_FEEDBACK_SUBMISSION_EDIT_PAGE:
            case Const.LegacyURIs.STUDENT_FEEDBACK_RESULTS_PAGE:
            case Const.LegacyURIs.INSTRUCTOR_FEEDBACK_RESULTS_PAGE:
                // TODO
                redirectUrl = "/";
                break;
            default:
                redirectUrl = "/";
                break;
        }

        return "redirect:" + redirectUrl;
    }
}
