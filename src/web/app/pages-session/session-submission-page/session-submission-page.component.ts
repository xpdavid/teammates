import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import moment from 'moment-timezone';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, finalize, switchMap, tap } from 'rxjs/operators';
import { FeedbackResponsesService } from '../../../services/feedback-responses.service';
import { HttpRequestService } from '../../../services/http-request.service';
import { StatusMessageService } from '../../../services/status-message.service';
import { TimezoneService } from '../../../services/timezone.service';
import {
  FeedbackResponseRecipient,
  FeedbackResponseRecipientSubmissionFormModel,
  QuestionSubmissionFormMode,
  QuestionSubmissionFormModel,
} from '../../components/question-types/question-types-session-submission/question-submission-form-model';
import { FeedbackParticipantType } from '../../feedback-participant-type';
import { FeedbackQuestion, NumberOfEntitiesToGiveFeedbackToSetting } from '../../feedback-question';
import { FeedbackResponse } from '../../feedback-response';
import { FeedbackSession, FeedbackSessionSubmissionStatus } from '../../feedback-session';
import { Intent } from '../../Intent';
import { ErrorMessageOutput } from '../../message-output';
import {
  FeedbackSessionClosedModalComponent,
  FeedbackSessionClosingSoonModalComponent,
  FeedbackSessionDeletedModalComponent,
  FeedbackSessionNotOpenModalComponent,
  SavingCompleteModalComponent,
} from './session-submission-modals.component';

interface FeedbackQuestionsResponse {
  questions: FeedbackQuestion[];
}

interface FeedbackQuestionParticipants {
  recipients: {
    name: string,
    identifier: string,
  }[];
}

interface FeedbackResponsesResponse {
  responses: FeedbackResponse[];
}

/**
 * Feedback session submission page.
 */
@Component({
  selector: 'tm-session-submission-page',
  templateUrl: './session-submission-page.component.html',
  styleUrls: ['./session-submission-page.component.scss'],
})
export class SessionSubmissionPageComponent implements OnInit {

  // enum
  FeedbackSessionSubmissionStatus: typeof FeedbackSessionSubmissionStatus = FeedbackSessionSubmissionStatus;
  Intent: typeof Intent = Intent;

  courseId: string = '';
  feedbackSessionName: string = '';
  regKey: string = '';

  moderatedPerson: string = '';
  previewAsPerson: string = '';

  formattedSessionOpeningTime: string = '';
  formattedSessionClosingTime: string = '';
  feedbackSessionInstructions: string = '';
  feedbackSessionSubmissionStatus: FeedbackSessionSubmissionStatus = FeedbackSessionSubmissionStatus.OPEN;

  intent: Intent = Intent.STUDENT_SUBMISSION;

  questionSubmissionForms: QuestionSubmissionFormModel[] = [];

  shouldSendConfirmationEmail: boolean = true;

  isSavingResponses: boolean = false;
  isSubmissionFormsDisabled: boolean = false;

  isModerationHintExpanded: boolean = false;

  constructor(private route: ActivatedRoute, private statusMessageService: StatusMessageService,
              private httpRequestService: HttpRequestService, private timezoneService: TimezoneService,
              private feedbackResponsesService: FeedbackResponsesService, private modalService: NgbModal) {
    this.timezoneService.getTzVersion(); // import timezone service to load timezone data
  }

  ngOnInit(): void {
    this.route.data.pipe(
        tap((data: any) => {
          this.intent = data.intent;
        }),
        switchMap(() => this.route.queryParams),
    ).subscribe((queryParams: any) => {
      this.courseId = queryParams.courseid;
      this.feedbackSessionName = queryParams.fsname;
      this.regKey = queryParams.key ? queryParams.key : '';
      this.moderatedPerson = queryParams.moderatedperson ? queryParams.moderatedperson : '';
      this.previewAsPerson = queryParams.previewas ? queryParams.previewas : '';

      if (this.previewAsPerson) {
        // disable submission in the preview mode
        this.isSubmissionFormsDisabled = true;
      }
      this.loadFeedbackSession();
    });
  }

  /**
   * Loads the feedback session information.
   */
  loadFeedbackSession(): void {
    const paramMap: { [key: string]: string } = {
      courseid: this.courseId,
      fsname: this.feedbackSessionName,
      intent: this.intent,
      key: this.regKey,
      moderatedperson: this.moderatedPerson,
      previewas: this.previewAsPerson,
    };
    const TIME_FORMAT: string = 'ddd, DD MMM, YYYY, hh:mm A zz';
    this.httpRequestService.get('/session', paramMap)
        .subscribe((feedbackSession: FeedbackSession) => {
          this.feedbackSessionInstructions = feedbackSession.instructions;
          this.formattedSessionOpeningTime =
              moment(feedbackSession.submissionStartTimestamp)
                  .tz(feedbackSession.timeZone).format(TIME_FORMAT);

          const submissionEndTime: any = moment(feedbackSession.submissionEndTimestamp);
          this.formattedSessionClosingTime = submissionEndTime
                  .tz(feedbackSession.timeZone).format(TIME_FORMAT);

          this.feedbackSessionSubmissionStatus = feedbackSession.submissionStatus;

          // don't show alert modal in moderation
          if (!this.moderatedPerson) {
            switch (feedbackSession.submissionStatus) {
              case FeedbackSessionSubmissionStatus.VISIBLE_NOT_OPEN:
                this.isSubmissionFormsDisabled = true;
                this.modalService.open(FeedbackSessionNotOpenModalComponent);
                break;
              case FeedbackSessionSubmissionStatus.OPEN:
                // closing in 15 minutes
                if (moment.utc().add(15, 'minutes').isAfter(submissionEndTime)) {
                  this.modalService.open(FeedbackSessionClosingSoonModalComponent);
                }
                break;
              case FeedbackSessionSubmissionStatus.CLOSED:
                this.isSubmissionFormsDisabled = true;
                this.modalService.open(FeedbackSessionClosedModalComponent);
                break;
              case FeedbackSessionSubmissionStatus.GRACE_PERIOD:
              default:
            }
          }

          this.loadFeedbackQuestions();
        }, (resp: ErrorMessageOutput) => {
          if (resp.status === 404) {
            this.modalService.open(FeedbackSessionDeletedModalComponent);
          }
          this.statusMessageService.showErrorMessage(resp.error.message);
        });
  }

  /**
   * Loads feedback questions to submit.
   */
  loadFeedbackQuestions(): void {
    const paramMap: { [key: string]: string } = {
      courseid: this.courseId,
      fsname: this.feedbackSessionName,
      intent: this.intent,
      key: this.regKey,
      moderatedperson: this.moderatedPerson,
      previewas: this.previewAsPerson,
    };
    this.httpRequestService.get('/questions', paramMap)
        .subscribe((response: FeedbackQuestionsResponse) => {
          response.questions.forEach((feedbackQuestion: FeedbackQuestion) => {
            const model: QuestionSubmissionFormModel = {
              feedbackQuestionId: feedbackQuestion.feedbackQuestionId,

              questionNumber: feedbackQuestion.questionNumber,
              questionBrief: feedbackQuestion.questionBrief,
              questionDescription: feedbackQuestion.questionDescription,

              giverType: feedbackQuestion.giverType,
              recipientType: feedbackQuestion.recipientType,
              recipientList: [],
              recipientSubmissionForms: [],

              questionType: feedbackQuestion.questionType,
              questionDetails: feedbackQuestion.questionDetails,

              numberOfEntitiesToGiveFeedbackToSetting: feedbackQuestion.numberOfEntitiesToGiveFeedbackToSetting,
              customNumberOfEntitiesToGiveFeedbackTo: feedbackQuestion.customNumberOfEntitiesToGiveFeedbackTo
                  ? feedbackQuestion.customNumberOfEntitiesToGiveFeedbackTo : 0,

              showGiverNameTo: feedbackQuestion.showGiverNameTo,
              showRecipientNameTo: feedbackQuestion.showRecipientNameTo,
              showResponsesTo: feedbackQuestion.showResponsesTo,
            };
            this.questionSubmissionForms.push(model);
            this.loadFeedbackQuestionParticipantsForQuestion(model);
          });
        }, (resp: ErrorMessageOutput) => this.statusMessageService.showErrorMessage(resp.error.message));
  }

  /**
   * Tracks the question submission form by feedback question id.
   *
   * @see https://angular.io/api/common/NgForOf#properties
   */
  trackQuestionSubmissionFormByFn(_: any, item: QuestionSubmissionFormModel): any {
    return item.feedbackQuestionId;
  }

  /**
   * Loads the feedback question participants for the question.
   */
  loadFeedbackQuestionParticipantsForQuestion(model: QuestionSubmissionFormModel): void {
    const paramMap: { [key: string]: string } = {
      questionid: model.feedbackQuestionId,
      intent: this.intent,
      key: this.regKey,
      moderatedperson: this.moderatedPerson,
      previewas: this.previewAsPerson,
    };
    this.httpRequestService.get('/question/participants', paramMap)
        .subscribe((response: FeedbackQuestionParticipants) => {
          response.recipients.forEach((recipient: { name: string, identifier: string }) => {
            model.recipientList.push({
              recipientIdentifier: recipient.identifier,
              recipientName: recipient.name,
            });
          });

          if (this.previewAsPerson) {
            // don't load responses in preview mode
            // generate a list of empty response box
            model.recipientList.forEach((recipient: FeedbackResponseRecipient) => {
              model.recipientSubmissionForms.push({
                recipientIdentifier:
                this.getQuestionSubmissionFormMode(model) === QuestionSubmissionFormMode.FLEXIBLE_RECIPIENT
                    ? '' : recipient.recipientIdentifier,
                responseDetails: this.feedbackResponsesService.getDefaultFeedbackResponseDetails(model.questionType),
                responseId: '',
              });
            });
          } else {
            this.loadFeedbackResponses(model);
          }
        }, (resp: ErrorMessageOutput) => this.statusMessageService.showErrorMessage(resp.error.message));
  }

  /**
   * Gets the form mode of the question submission form.
   */
  getQuestionSubmissionFormMode(model: QuestionSubmissionFormModel): QuestionSubmissionFormMode {
    const isNumberOfEntitiesToGiveFeedbackToSettingLimited: boolean
        = (model.recipientType === FeedbackParticipantType.STUDENTS
        || model.recipientType === FeedbackParticipantType.TEAMS
        || model.recipientType === FeedbackParticipantType.INSTRUCTORS)
        && model.numberOfEntitiesToGiveFeedbackToSetting === NumberOfEntitiesToGiveFeedbackToSetting.CUSTOM
        && model.recipientList.length > model.customNumberOfEntitiesToGiveFeedbackTo;

    return isNumberOfEntitiesToGiveFeedbackToSettingLimited
        ? QuestionSubmissionFormMode.FLEXIBLE_RECIPIENT : QuestionSubmissionFormMode.FIXED_RECIPIENT;
  }

  /**
   * Loads the responses of the feedback question to {@responseToRecipientList} in the model.
   */
  loadFeedbackResponses(model: QuestionSubmissionFormModel): void {
    const paramMap: { [key: string]: string } = {
      questionid: model.feedbackQuestionId,
      intent: this.intent,
      key: this.regKey,
      moderatedperson: this.moderatedPerson,
    };
    this.httpRequestService.get('/responses', paramMap).subscribe((existingResponses: FeedbackResponsesResponse) => {
      // if student does not have any responses (i.e. first time answering), then enable sending of confirmation email
      this.shouldSendConfirmationEmail = this.shouldSendConfirmationEmail && existingResponses.responses.length === 0;

      if (this.getQuestionSubmissionFormMode(model) === QuestionSubmissionFormMode.FIXED_RECIPIENT) {
        // need to generate a full list of submission forms
        model.recipientList.forEach((recipient: FeedbackResponseRecipient) => {
          const matchedExistingResponse: FeedbackResponse | undefined =
              existingResponses.responses.find(
                  (response: FeedbackResponse) => response.recipientIdentifier === recipient.recipientIdentifier);
          model.recipientSubmissionForms.push({
            recipientIdentifier: recipient.recipientIdentifier,
            responseDetails: matchedExistingResponse
                ? matchedExistingResponse.responseDetails
                : this.feedbackResponsesService.getDefaultFeedbackResponseDetails(model.questionType),
            responseId: matchedExistingResponse ? matchedExistingResponse.feedbackResponseId : '',
          });
        });
      }

      if (this.getQuestionSubmissionFormMode(model) === QuestionSubmissionFormMode.FLEXIBLE_RECIPIENT) {
        // need to generate limited number of submission forms
        let numberOfRecipientSubmissionFormsNeeded: number =
            model.customNumberOfEntitiesToGiveFeedbackTo - existingResponses.responses.length;

        existingResponses.responses.forEach((response: FeedbackResponse) => {
          model.recipientSubmissionForms.push({
            recipientIdentifier: response.recipientIdentifier,
            responseDetails: response.responseDetails,
            responseId: response.feedbackResponseId,
          });
        });

        // generate empty submission forms
        while (numberOfRecipientSubmissionFormsNeeded > 0) {
          model.recipientSubmissionForms.push({
            recipientIdentifier: '',
            responseDetails: this.feedbackResponsesService.getDefaultFeedbackResponseDetails(model.questionType),
            responseId: '',
          });
          numberOfRecipientSubmissionFormsNeeded -= 1;
        }
      }
    }, (resp: ErrorMessageOutput) => this.statusMessageService.showErrorMessage(resp.error.message));
  }

  /**
   * Checks whether there is any submission forms in the current page.
   */
  get hasAnyResponseToSubmit(): boolean {
    return this.questionSubmissionForms
        .some((model: QuestionSubmissionFormModel) => model.recipientSubmissionForms.length !== 0);
  }

  /**
   * Saves all feedback response.
   *
   * <p>All empty feedback response will be deleted; For non-empty responses, update/create them if necessary.
   */
  saveFeedbackResponses(): void {
    const notYetAnsweredQuestions: Set<number> = new Set();
    const failToSaveQuestions: Set<number> = new Set();
    const savingRequests: Observable<any>[] = [];

    this.questionSubmissionForms.forEach((questionSubmissionFormModel: QuestionSubmissionFormModel) => {
      let isQuestionFullyAnswered: boolean = true;

      questionSubmissionFormModel.recipientSubmissionForms
          .forEach((recipientSubmissionFormModel: FeedbackResponseRecipientSubmissionFormModel) => {
            const isFeedbackResponseDetailsEmpty: boolean =
                this.feedbackResponsesService.isFeedbackResponseDetailsEmpty(
            questionSubmissionFormModel.questionType, recipientSubmissionFormModel.responseDetails);
            isQuestionFullyAnswered = isQuestionFullyAnswered && !isFeedbackResponseDetailsEmpty;

            if (recipientSubmissionFormModel.responseId !== '' && isFeedbackResponseDetailsEmpty) {
              // existing response but empty details -> delete response
              savingRequests.push(this.httpRequestService.delete('/response', {
                responseid: recipientSubmissionFormModel.responseId,
                intent: this.intent,
                key: this.regKey,
                moderatedperson: this.moderatedPerson,
              }).pipe(
                  tap(() => {
                    recipientSubmissionFormModel.responseId = '';
                  }),
                  catchError((error: any) => {
                    this.statusMessageService.showErrorMessage((error as ErrorMessageOutput).error.message);
                    failToSaveQuestions.add(questionSubmissionFormModel.questionNumber);
                    return of(error);
                  }),
              ));
            }

            if (recipientSubmissionFormModel.responseId !== '' && !isFeedbackResponseDetailsEmpty) {
              // existing response and details is not empty -> update response
              savingRequests.push(this.httpRequestService.put('/response', {
                responseid: recipientSubmissionFormModel.responseId,
                intent: this.intent,
                key: this.regKey,
                moderatedperson: this.moderatedPerson,
              }, {
                recipientIdentifier: recipientSubmissionFormModel.recipientIdentifier,
                questionType: questionSubmissionFormModel.questionType,
                responseDetails: recipientSubmissionFormModel.responseDetails,
              }).pipe(
                  tap((resp: FeedbackResponse) => {
                    recipientSubmissionFormModel.responseId = resp.feedbackResponseId;
                    recipientSubmissionFormModel.responseDetails = resp.responseDetails;
                    recipientSubmissionFormModel.recipientIdentifier = resp.recipientIdentifier;
                  }),
                  catchError((error: any) => {
                    this.statusMessageService.showErrorMessage((error as ErrorMessageOutput).error.message);
                    failToSaveQuestions.add(questionSubmissionFormModel.questionNumber);
                    return of(error);
                  }),
              ));
            }

            if (recipientSubmissionFormModel.responseId === '' && !isFeedbackResponseDetailsEmpty) {
              // new response and the details is not empty -> create response
              savingRequests.push(this.httpRequestService.post('/response', {
                questionid: questionSubmissionFormModel.feedbackQuestionId,
                intent: this.intent,
                key: this.regKey,
                moderatedperson: this.moderatedPerson,
              }, {
                recipientIdentifier: recipientSubmissionFormModel.recipientIdentifier,
                questionType: questionSubmissionFormModel.questionType,
                responseDetails: recipientSubmissionFormModel.responseDetails,
              }).pipe(
                  tap((resp: FeedbackResponse) => {
                    recipientSubmissionFormModel.responseId = resp.feedbackResponseId;
                    recipientSubmissionFormModel.responseDetails = resp.responseDetails;
                    recipientSubmissionFormModel.recipientIdentifier = resp.recipientIdentifier;
                  }),
                  catchError((error: any) => {
                    this.statusMessageService.showErrorMessage((error as ErrorMessageOutput).error.message);
                    failToSaveQuestions.add(questionSubmissionFormModel.questionNumber);
                    return of(error);
                  }),
              ));
            }
          });

      if (!isQuestionFullyAnswered) {
        notYetAnsweredQuestions.add(questionSubmissionFormModel.questionNumber);
      }
    });

    this.isSavingResponses = true;
    let hasSubmissionConfirmationError: boolean = false;
    forkJoin(savingRequests).pipe(
        switchMap(() => {
          if (failToSaveQuestions.size === 0) {
            this.statusMessageService.showSuccessMessage('All responses submitted successfully!');
          } else {
            this.statusMessageService.showErrorMessage('Some responses are not saved successfully');
          }

          if (notYetAnsweredQuestions.size !== 0) {
            // TODO use showInfoMessage
            this.statusMessageService.showSuccessMessage(
                `Note that some questions are yet to be answered. They are:
                ${ Array.from(notYetAnsweredQuestions.values()) }.`);
          }

          return this.httpRequestService.post('/submission/confirmation', {
            courseid: this.courseId,
            fsname: this.feedbackSessionName,
            sendsubmissionemail: String(this.shouldSendConfirmationEmail),
            intent: this.intent,
            key: this.regKey,
            moderatedperson: this.moderatedPerson,
          });
        }),
    ).pipe(
        finalize(() => {
          this.isSavingResponses = false;

          const modalRef: NgbModalRef = this.modalService.open(SavingCompleteModalComponent);
          modalRef.componentInstance.notYetAnsweredQuestions = Array.from(notYetAnsweredQuestions.values()).join(', ');
          modalRef.componentInstance.failToSaveQuestions = Array.from(failToSaveQuestions.values()).join(', ');
          modalRef.componentInstance.hasSubmissionConfirmationError = hasSubmissionConfirmationError;
        }),
    ).subscribe(() => {
      hasSubmissionConfirmationError = false;
    }, (resp: ErrorMessageOutput) => {
      hasSubmissionConfirmationError = true;
      this.statusMessageService.showErrorMessage(resp.error.message);
    });
  }
}
