import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { NgbDateParserFormatter, NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';
import moment from 'moment-timezone';
import { forkJoin, Observable, of } from 'rxjs';
import { map, switchMap, tap } from 'rxjs/operators';
import { StatusMessageService } from '../../../services/status-message.service';
import {
  LOCAL_DATE_TIME_FORMAT,
  LocalDateTimeAmbiguityStatus,
  LocalDateTimeInfo,
  TimezoneService,
} from '../../../services/timezone.service';
import { ResponseVisibleSetting, SessionVisibleSetting } from '../../feedback-session';
import { SessionEditFormDatePickerFormatter } from './session-edit-form-datepicker-formatter';
import {
  SessionEditFormAddInputModel,
  SessionEditFormAddOutputModel,
  SessionEditFormBasicInputModel,
  SessionEditFormBasicOutputModel,
  SessionEditFormEditInputModel,
  SessionEditFormEditOutputModel,
  SessionEditFromMode,
  SessionTemplate,
} from './session-edit-form-model';
import { TimeFormat } from './time-picker/time-picker.component';

/**
 * Form to Add/Edit feedback sessions.
 */
@Component({
  selector: 'tm-session-edit-form',
  templateUrl: './session-edit-form.component.html',
  styleUrls: ['./session-edit-form.component.scss'],
  providers: [{ provide: NgbDateParserFormatter, useClass: SessionEditFormDatePickerFormatter }],
})
export class SessionEditFormComponent implements OnInit {

  // enum
  SessionEditFromMode: typeof SessionEditFromMode = SessionEditFromMode;
  SessionVisibleSetting: typeof SessionVisibleSetting = SessionVisibleSetting;
  ResponseVisibleSetting: typeof ResponseVisibleSetting = ResponseVisibleSetting;

  // UI status tracking
  hasVisibleSettingsPanelExpanded: boolean = false;
  hasEmailSettingsPanelExpanded: boolean = false;

  @Input()
  formMode: SessionEditFromMode = SessionEditFromMode.ADD;

  isSaving: boolean = false;

  // model
  courseId: string = '';
  timeZone: string = '';
  courseName: string = '';
  feedbackSessionName: string = '';
  instructions: string = '';

  submissionStartDate: NgbDateStruct = { year: 0, month: 0, day: 0 };
  submissionStartTime: TimeFormat = { hour: 0, minute: 0 };
  submissionEndDate: NgbDateStruct = { year: 0, month: 0, day: 0 };
  submissionEndTime: TimeFormat = { hour: 0, minute: 0 };
  gracePeriod: number = 0;

  sessionVisibleSetting: SessionVisibleSetting = SessionVisibleSetting.AT_OPEN;
  customSessionVisibleDate: NgbDateStruct = { year: 0, month: 0, day: 0 };
  customSessionVisibleTime: TimeFormat = { hour: 0, minute: 0 };

  responseVisibleSetting: ResponseVisibleSetting = ResponseVisibleSetting.LATER;
  customResponseVisibleDate: NgbDateStruct = { year: 0, month: 0, day: 0 };
  customResponseVisibleTime: TimeFormat = { hour: 0, minute: 0 };

  isClosingEmailEnabled: boolean = true;
  isPublishedEmailEnabled: boolean = true;

  // edit mode specific
  submissionStatus: string = '';
  publishStatus: string = '';
  // changeable in edit mode
  isEditable: boolean = true;

  // add mode specific
  @Input()
  coursesIdCandidates: string[] = [];
  @Input()
  sessionTemplates: SessionTemplate[] = [];
  sessionTemplateName: string = '';

  // event emission
  @Output()
  addNewSessionEvent: EventEmitter<Observable<SessionEditFormAddOutputModel>> =
      new EventEmitter<Observable<SessionEditFormAddOutputModel>>();

  @Output()
  editExistingSessionEvent: EventEmitter<Observable<SessionEditFormEditOutputModel>> =
      new EventEmitter<Observable<SessionEditFormEditOutputModel>>();

  @Output()
  deleteExistingSessionEvent: EventEmitter<void> = new EventEmitter<void>();

  @Output()
  copyCurrentSessionEvent: EventEmitter<void> = new EventEmitter<void>();

  @Output()
  copyOtherSessionsEvent: EventEmitter<void> = new EventEmitter<void>();

  constructor(private timezoneService: TimezoneService, private statusMessageService: StatusMessageService) { }

  ngOnInit(): void {
  }

  /**
   * Sets data model in Edit mode.
   */
  setEditModel(model: SessionEditFormEditInputModel): void {
    this.setBasicModel(model);
    this.submissionStatus = model.submissionStatus;
    this.publishStatus = model.publishStatus;
  }

  /**
   * Sets the form to be editable in Edit mode.
   */
  setIsEditable(isEditable: boolean): void {
    this.isEditable = isEditable;
  }

  /**
   * Sets the form to be in saving process.
   */
  setIsSaving(isSaving: boolean): void {
    this.isSaving = isSaving;
  }

  /**
   * Sets data model in ADD mode.
   */
  setAddModel(model: SessionEditFormAddInputModel): void {
    this.setBasicModel(model);
  }

  private setBasicModel(model: SessionEditFormBasicInputModel): void {
    this.courseId = model.courseId;
    if (this.timezoneService.getTzOffsets().hasOwnProperty(model.timeZone)) {
      // valid timezone
      this.timeZone = model.timeZone;
    }
    this.courseName = model.courseName;
    this.feedbackSessionName = model.feedbackSessionName;
    this.instructions = model.instructions;

    let { date, time }: {date: NgbDateStruct; time: TimeFormat} = this.getDateTime(model.submissionStartTimestamp);
    this.submissionStartDate = date;
    this.submissionStartTime = time;

    ({ date, time } = this.getDateTime(model.submissionEndTimestamp));
    this.submissionEndDate = date;
    this.submissionEndTime = time;

    this.gracePeriod = model.gracePeriod;

    this.sessionVisibleSetting = model.sessionVisibleSetting;
    if (model.customSessionVisibleTimestamp) {
      ({ date, time } = this.getDateTime(model.customSessionVisibleTimestamp));
      this.customSessionVisibleDate = date;
      this.customSessionVisibleTime = time;
    }

    this.responseVisibleSetting = model.responseVisibleSetting;
    if (model.customResponseVisibleTimestamp) {
      ({ date, time } = this.getDateTime(model.customResponseVisibleTimestamp));
      this.customResponseVisibleDate = date;
      this.customResponseVisibleTime = time;
    }

    this.isClosingEmailEnabled = model.isClosingEmailEnabled;
    this.isPublishedEmailEnabled = model.isPublishedEmailEnabled;

    // display uncommon setting if the value is not default value
    this.hasVisibleSettingsPanelExpanded =
        this.sessionVisibleSetting !== SessionVisibleSetting.AT_OPEN
        || this.responseVisibleSetting !== ResponseVisibleSetting.LATER;
    this.hasEmailSettingsPanelExpanded =
        !this.isClosingEmailEnabled || !this.isPublishedEmailEnabled;
  }

  private getDateTime(timestamp: number): {date: NgbDateStruct; time: TimeFormat} {
    const momentInstance: any = moment(timestamp).tz(this.timeZone);
    const date: NgbDateStruct = {
      year: momentInstance.year(),
      month: momentInstance.month() + 1, // moment return 0-11 for month
      day: momentInstance.date(),
    };
    const time: TimeFormat = {
      minute: momentInstance.minute(),
      hour: momentInstance.hour(),
    };
    return {
      date,
      time,
    };
  }

  /**
   * Handles submit button click event.
   */
  submitFormHandler(): void {
    // resolve local date time to timestamp
    const basic: Observable<SessionEditFormBasicOutputModel> = forkJoin(
        this.getTimestamp(this.submissionStartDate, this.submissionStartTime, 'Submission opening time'),
        this.getTimestamp(this.submissionEndDate, this.submissionEndTime, 'Submission closing time'),
        this.sessionVisibleSetting === SessionVisibleSetting.CUSTOM ?
            this.getTimestamp(this.customSessionVisibleDate, this.customSessionVisibleTime, 'Session visible time')
            : of(0),
        this.responseVisibleSetting === ResponseVisibleSetting.CUSTOM ?
            this.getTimestamp(this.customResponseVisibleDate, this.customResponseVisibleTime, 'Response visible time')
            : of(0),
    ).pipe(
        switchMap((vals: number[]) => {
          return of({
            instructions: this.instructions,
            gracePeriod: this.gracePeriod,
            isClosingEmailEnabled: this.isClosingEmailEnabled,
            isPublishedEmailEnabled: this.isPublishedEmailEnabled,

            submissionStartTimestamp: vals[0],
            submissionEndTimestamp: vals[1],

            sessionVisibleSetting: this.sessionVisibleSetting,
            customSessionVisibleTimestamp: vals[2],
            responseVisibleSetting: this.responseVisibleSetting,
            customResponseVisibleTimestamp: vals[3],
          });
        }),
    );

    if (this.formMode === SessionEditFromMode.ADD) {
      this.addNewSessionEvent.emit(basic.pipe(map((val: SessionEditFormBasicOutputModel) => Object.assign({
        courseId: this.courseId,
        feedbackSessionName: this.feedbackSessionName,
        sessionTemplateName: this.sessionTemplateName,
      }, val))));
    }

    if (this.formMode === SessionEditFromMode.EDIT) {
      this.editExistingSessionEvent.emit(basic);
    }
  }

  /**
   * Resolves the local date time to an UNIX timestamp.
   */
  private getTimestamp(date: NgbDateStruct, time: TimeFormat, fieldName: string): Observable<number> {
    const inst: any = moment();
    inst.set('year', date.year);
    inst.set('month', date.month - 1); // moment month is from 0-11
    inst.set('date', date.day);
    inst.set('hour', time.hour);
    inst.set('minute', time.minute);

    const localDateTime: string = inst.format(LOCAL_DATE_TIME_FORMAT);
    return this.timezoneService.getResolveLocalDateTime(localDateTime, this.timeZone).pipe(
        tap((info: LocalDateTimeInfo) => {
          const DATE_FORMAT_WITHOUT_ZONE_INFO: any = 'ddd, DD MMM, YYYY hh:mm A';
          const DATE_FORMAT_WITH_ZONE_INFO: any = "ddd, DD MMM, YYYY hh:mm A z ('UTC'Z)";

          switch (info.resolvedStatus) {
            case LocalDateTimeAmbiguityStatus.UNAMBIGUOUS:
              break;
            case LocalDateTimeAmbiguityStatus.GAP:
              this.statusMessageService.showWarningMessage(
                  `The ${fieldName}, ${moment.format(DATE_FORMAT_WITHOUT_ZONE_INFO)},
                  falls within the gap period when clocks spring forward at the start of DST.
                  It was resolved to ${moment(info.resolvedTimestamp).format(DATE_FORMAT_WITH_ZONE_INFO)}.`);
              break;
            case LocalDateTimeAmbiguityStatus.OVERLAP:
              this.statusMessageService.showWarningMessage(
                  `The ${fieldName}, ${moment.format(DATE_FORMAT_WITHOUT_ZONE_INFO)},
                  falls within the overlap period when clocks fall back at the end of DST.
                  It can refer to ${moment(info.earlierInterpretationTimestamp).format(DATE_FORMAT_WITH_ZONE_INFO)}
                  or ${moment(info.laterInterpretationTimestamp).format(DATE_FORMAT_WITH_ZONE_INFO)} .
                  It was resolved to %s.`,
              );
              break;
            default:
          }
        }),
        map((info: LocalDateTimeInfo) => info.resolvedTimestamp));
  }

  /**
   * Handles delete current feedback session button click event.
   */
  deleteHandler(): void {
    this.deleteExistingSessionEvent.emit();
  }

  /**
   * Handles copy current feedback session button click event.
   */
  copyHandler(): void {
    this.copyCurrentSessionEvent.emit();
  }

  /**
   * Handles copy from other feedback sessions button click event.
   */
  copyOthersHandler(): void {
    this.copyOtherSessionsEvent.emit();
  }
}
