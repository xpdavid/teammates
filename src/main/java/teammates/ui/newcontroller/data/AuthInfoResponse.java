package teammates.ui.newcontroller.data;

import teammates.common.datatransfer.UserInfo;

public class AuthInfoResponse extends BasicResponse {
    private final String studentLoginUrl;
    private final String instructorLoginUrl;
    private final String adminLoginUrl;

    private final UserInfo user;
    private final boolean isMasquerade;
    private final String logoutUrl;

    public AuthInfoResponse(String studentLoginUrl, String instructorLoginUrl, String adminLoginUrl) {
        this.studentLoginUrl = studentLoginUrl;
        this.instructorLoginUrl = instructorLoginUrl;
        this.adminLoginUrl = adminLoginUrl;

        this.user = null;
        this.isMasquerade = false;
        this.logoutUrl = null;
    }

    public AuthInfoResponse(UserInfo user, boolean isMasquerade, String logoutUrl) {
        this.studentLoginUrl = null;
        this.instructorLoginUrl = null;
        this.adminLoginUrl = null;

        this.user = user;
        this.isMasquerade = isMasquerade;
        this.logoutUrl = logoutUrl;
    }

    public String getStudentLoginUrl() {
        return studentLoginUrl;
    }

    public String getInstructorLoginUrl() {
        return instructorLoginUrl;
    }

    public String getAdminLoginUrl() {
        return adminLoginUrl;
    }

    public UserInfo getUser() {
        return user;
    }

    public boolean isMasquerade() {
        return isMasquerade;
    }

    public String getLogoutUrl() {
        return logoutUrl;
    }
}
