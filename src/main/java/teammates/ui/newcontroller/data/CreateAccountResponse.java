package teammates.ui.newcontroller.data;

public class CreateAccountResponse extends BasicResponse {

    private final String joinLink;

    public CreateAccountResponse(String joinLink) {
        this.joinLink = joinLink;
    }

    public String getJoinLink() {
        return joinLink;
    }

}
