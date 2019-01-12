import { FeedbackSessionSubmissionStatus, ResponseVisibleSetting, SessionVisibleSetting } from '../../feedback-session';

/**
 * The format of a session template.
 */
export interface SessionTemplate {
  name: string;
  description: string;
}

/**
 * The mode of operation for session edit form.
 */
export enum SessionEditFormMode {
  /**
   * Adding a new feedback session.
   */
  ADD,

  /**
   * Editing the existing feedback session.
   */
  EDIT,
}

/**
 * The form model of session edit form.
 */
export interface SessionEditFormModel {
  courseId: string;
  timeZone: string;
  courseName: string;
  feedbackSessionName: string;
  instructions: string;

  submissionStartTime: TimeFormat;
  submissionStartDate: DateFormat;
  submissionEndTime: TimeFormat;
  submissionEndDate: DateFormat;
  gracePeriod: number;

  sessionVisibleSetting: SessionVisibleSetting;
  customSessionVisibleTime: TimeFormat;
  customSessionVisibleDate: DateFormat;

  responseVisibleSetting: ResponseVisibleSetting;
  customResponseVisibleTime: TimeFormat;
  customResponseVisibleDate: DateFormat;

  hasVisibleSettingsPanelExpanded: boolean;
  hasEmailSettingsPanelExpanded: boolean;

  submissionStatus: FeedbackSessionSubmissionStatus;
  publishStatus: string;

  isClosingEmailEnabled: boolean;
  isPublishedEmailEnabled: boolean;

  isSaving: boolean;
  isEditable: boolean;
}

/**
 * The time format.
 */
export interface TimeFormat {
  hour: number;
  minute: number;
}

/**
 * The date format.
 */
export interface DateFormat {
  year: number;
  month: number;
  day: number;
}
