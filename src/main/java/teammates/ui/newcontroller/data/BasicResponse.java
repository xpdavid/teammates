package teammates.ui.newcontroller.data;

import teammates.common.util.Config;

public class BasicResponse {

    private String requestId = Config.getRequestId();

    public String getRequestId() {
        return requestId;
    }
}
