import { ResponseVisibleSetting, SessionVisibleSetting } from '../../feedback-session';

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
export enum SessionEditFromMode {
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
 * The basic input data model for session edit form.
 */
export interface SessionEditFormBasicInputModel {
  courseId: string;
  timeZone: string;
  courseName: string;
  feedbackSessionName: string;
  instructions: string;

  submissionStartTimestamp: number;
  submissionEndTimestamp: number;
  gracePeriod: number;

  sessionVisibleSetting: SessionVisibleSetting;
  customSessionVisibleTimestamp?: number;

  responseVisibleSetting: ResponseVisibleSetting;
  customResponseVisibleTimestamp?: number;

  isClosingEmailEnabled: boolean;
  isPublishedEmailEnabled: boolean;
}

/**
 * The basic output data model for session edit form.
 */
export interface SessionEditFormBasicOutputModel {
  instructions: string;

  submissionStartTimestamp: number;
  submissionEndTimestamp: number;
  gracePeriod: number;

  sessionVisibleSetting: SessionVisibleSetting;
  customSessionVisibleTimestamp?: number;

  responseVisibleSetting: ResponseVisibleSetting;
  customResponseVisibleTimestamp?: number;

  isClosingEmailEnabled: boolean;
  isPublishedEmailEnabled: boolean;
}

/**
 * The input data model for session edit form in EDIT mode.
 */
export interface SessionEditFormEditInputModel extends SessionEditFormBasicInputModel {
  submissionStatus: string;
  publishStatus: string;
}

/**
 * The output data model for session edit form in EDIT mode.
 */
// tslint:disable-next-line:no-empty-interface
export interface SessionEditFormEditOutputModel extends SessionEditFormBasicOutputModel {

}

/**
 * The input data model for session edit form in ADD mode.
 */
// tslint:disable-next-line:no-empty-interface
export interface SessionEditFormAddInputModel extends SessionEditFormBasicInputModel {

}

/**
 * The output data model for session edit form in ADD mode.
 */
export interface SessionEditFormAddOutputModel extends SessionEditFormBasicOutputModel {
  courseId: string;
  feedbackSessionName: string;
  sessionTemplateName: string;
}
