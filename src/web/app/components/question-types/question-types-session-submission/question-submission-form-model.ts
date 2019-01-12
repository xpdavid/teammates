import { FeedbackParticipantType } from '../../../feedback-participant-type';
import {
  FeedbackQuestionDetails,
  FeedbackQuestionType,
  NumberOfEntitiesToGiveFeedbackToSetting,
} from '../../../feedback-question';
import { FeedbackResponseDetails } from '../../../feedback-response';
import { FeedbackVisibilityType } from '../../../feedback-visibility';

/**
 * The mode of operation for question submission form.
 */
export enum QuestionSubmissionFormMode {
  /**
   * User cannot select recipient to give feedback to.
   */
  FIXED_RECIPIENT,

  /**
   * User can select recipient to give feedback to.
   */
  FLEXIBLE_RECIPIENT,
}

/**
 * The form model of question submission form.
 */
export interface QuestionSubmissionFormModel {
  feedbackQuestionId: string;

  questionNumber: number;
  questionBrief: string;
  questionDescription: string;

  questionType: FeedbackQuestionType;
  questionDetails: FeedbackQuestionDetails;

  giverType: FeedbackParticipantType;
  recipientType: FeedbackParticipantType;
  recipientList: FeedbackResponseRecipient[];
  recipientSubmissionForms: FeedbackResponseRecipientSubmissionFormModel[];

  numberOfEntitiesToGiveFeedbackToSetting: NumberOfEntitiesToGiveFeedbackToSetting;
  customNumberOfEntitiesToGiveFeedbackTo: number;

  showResponsesTo: FeedbackVisibilityType[];
  showGiverNameTo: FeedbackVisibilityType[];
  showRecipientNameTo: FeedbackVisibilityType[];
}

/**
 * A recipient of a feedback question.
 */
export interface FeedbackResponseRecipient {
  recipientIdentifier: string;
  recipientName: string;
}

/**
 * The form modal of recipient submission form.
 */
export interface FeedbackResponseRecipientSubmissionFormModel {
  responseId: string;
  recipientIdentifier: string;
  responseDetails: FeedbackResponseDetails;
}
