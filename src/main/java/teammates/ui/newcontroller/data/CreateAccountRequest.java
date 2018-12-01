package teammates.ui.newcontroller.data;

import javax.validation.constraints.NotEmpty;

public class CreateAccountRequest {

    @NotEmpty
    private String instructorName;

    @NotEmpty
    private String instructorEmail;

    @NotEmpty
    private String institute;

    public String getInstructorName() {
        return instructorName;
    }

    public void setInstructorName(String instructorName) {
        this.instructorName = instructorName;
    }

    public String getInstructorEmail() {
        return instructorEmail;
    }

    public void setInstructorEmail(String instructorEmail) {
        this.instructorEmail = instructorEmail;
    }

    public String getInstitute() {
        return institute;
    }

    public void setInstitute(String institute) {
        this.institute = institute;
    }
}
