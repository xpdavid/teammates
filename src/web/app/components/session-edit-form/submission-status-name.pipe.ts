import { Pipe, PipeTransform } from '@angular/core';
import { FeedbackSessionSubmissionStatus } from '../../feedback-session';

/**
 * Pipe to handle the display of {@code FeedbackSessionSubmissionStatus}.
 */
@Pipe({
  name: 'submissionStatusName',
})
export class SubmissionStatusNamePipe implements PipeTransform {

  /**
   * Transforms {@link FeedbackQuestionType} to a simple description.
   */
  transform(status: FeedbackSessionSubmissionStatus): string {
    switch (status) {
      case FeedbackSessionSubmissionStatus.NOT_VISIBLE:
      case FeedbackSessionSubmissionStatus.VISIBLE_NOT_OPEN:
        return 'Awaiting';
      case FeedbackSessionSubmissionStatus.OPEN:
      case FeedbackSessionSubmissionStatus.GRACE_PERIOD:
        return 'Open';
      case FeedbackSessionSubmissionStatus.CLOSED:
        return 'Closed';
      default:
        return 'Unknown';
    }
  }

}
