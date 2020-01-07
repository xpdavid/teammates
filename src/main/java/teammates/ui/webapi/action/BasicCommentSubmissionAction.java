package teammates.ui.webapi.action;

import teammates.common.datatransfer.FeedbackParticipantType;
import teammates.common.datatransfer.attributes.FeedbackQuestionAttributes;
import teammates.common.datatransfer.attributes.FeedbackResponseAttributes;
import teammates.common.datatransfer.attributes.FeedbackResponseCommentAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.datatransfer.questions.FeedbackQuestionType;
import teammates.common.exception.InvalidHttpParameterException;
import teammates.common.exception.UnauthorizedAccessException;

public abstract class BasicCommentSubmissionAction extends BasicFeedbackSubmissionAction {
    /**
     * Validates the questionType of the corresponding question.
     */
    protected void validQuestionTypeForCommentInSubmission(FeedbackQuestionType questionType) {
        if (questionType != FeedbackQuestionType.MCQ && questionType != FeedbackQuestionType.MSQ) {
            throw new InvalidHttpParameterException("Invalid question type for comment in submission");
        }
    }

    /**
     * Validates comment doesn't exist of corresponding response.
     */
    protected void verifyCommentNotExist(String feedbackResponseId) {
        FeedbackResponseCommentAttributes comment =
                logic.getFeedbackResponseCommentForResponseFromParticipant(feedbackResponseId);

        if (comment != null) {
            throw new InvalidHttpParameterException("Comment has been created for the response in submission");
        }

    }

    /**
     * Verify response ownership for student.
     */
    protected void verifyResponseOwnerShipForStudent(StudentAttributes student, FeedbackResponseAttributes response,
                                                     FeedbackQuestionAttributes question) {

        if (question.getGiverType() == FeedbackParticipantType.TEAMS
                && !response.getGiver().equals(student.getTeam())) {
            throw new UnauthorizedAccessException("Response [" + response.getId() + "] is not accessible to "
                    + student.getTeam());
        } else if (question.getGiverType() == FeedbackParticipantType.STUDENTS
                && !response.getGiver().equals(student.getEmail())) {
            throw new UnauthorizedAccessException("Response [" + response.getId() + "] is not accessible to "
                    + student.getName());
        }
    }

    /**
     * Verify response ownership for instructor.
     */
    protected void verifyResponseOwnerShipForInstructor(InstructorAttributes instructor,
                                                        FeedbackResponseAttributes response) {
        if (!response.getGiver().equals(instructor.getEmail())) {
            throw new UnauthorizedAccessException("Response [" + response.getId() + "] is not accessible to "
                    + instructor.getName());
        }
    }
}
