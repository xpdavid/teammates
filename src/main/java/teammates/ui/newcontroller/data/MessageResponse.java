package teammates.ui.newcontroller.data;

public class MessageResponse extends BasicResponse {

    private final String message;

    public MessageResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
