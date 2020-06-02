import { Component, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { saveAs } from 'file-saver';
import moment from 'moment-timezone';
import { Observable } from 'rxjs';
import { CourseService } from '../../../services/course.service';
import { FeedbackQuestionsService } from '../../../services/feedback-questions.service';
import { FeedbackResponseCommentService } from '../../../services/feedback-response-comment.service';
import { FeedbackSessionsService } from '../../../services/feedback-sessions.service';
import { InstructorService } from '../../../services/instructor.service';
import { StatusMessageService } from '../../../services/status-message.service';
import { StudentService } from '../../../services/student.service';
import { TimezoneService } from '../../../services/timezone.service';
import {
  CourseSectionNames,
  FeedbackQuestion,
  FeedbackQuestions,
  FeedbackSession,
  FeedbackSessionPublishStatus,
  FeedbackSessionSubmittedGiverSet,
  Instructor,
  QuestionOutput,
  ResponseOutput,
  SessionResults,
  Student,
  Students,
} from '../../../types/api-output';
import { Intent } from '../../../types/api-request';
import { CommentToCommentRowModelPipe } from '../../components/comment-box/comment-to-comment-row-model.pipe';
import { CommentsToCommentTableModelPipe } from '../../components/comment-box/comments-to-comment-table-model.pipe';
import {
  ConfirmPublishingSessionModalComponent,
} from '../../components/sessions-table/confirm-publishing-session-modal/confirm-publishing-session-modal.component';
import {
    ConfirmUnpublishingSessionModalComponent,
// tslint:disable-next-line:max-line-length
} from '../../components/sessions-table/confirm-unpublishing-session-modal/confirm-unpublishing-session-modal.component';
import {
  StudentListInfoTableRowModel,
} from '../../components/sessions-table/student-list-info-table/student-list-info-table-model';
import { ErrorMessageOutput } from '../../error-message-output';
import { InstructorCommentsComponent } from '../instructor-comments.component';
import { InstructorSessionNoResponsePanelComponent } from './instructor-session-no-response-panel.component';
import { InstructorSessionResultSectionType } from './instructor-session-result-section-type.enum';
import { InstructorSessionResultViewType } from './instructor-session-result-view-type.enum';

/**
 * Per section view tab model.
 */
export interface SectionTabModel {
  questions: QuestionOutput[];

  hasPopulated: boolean;
  isTabExpanded: boolean;
}

/**
 * Per question view tab model.
 */
export interface QuestionTabModel {
  question: FeedbackQuestion;
  responses: ResponseOutput[];
  statistics: string; // TODO will define types later
  hasPopulated: boolean;
  isTabExpanded: boolean;
}

/**
 * Instructor feedback session result page.
 */
@Component({
  selector: 'tm-instructor-session-result-page',
  templateUrl: './instructor-session-result-page.component.html',
  styleUrls: ['./instructor-session-result-page.component.scss'],
})
export class InstructorSessionResultPageComponent extends InstructorCommentsComponent implements OnInit {

  // enum
  InstructorSessionResultSectionType: typeof InstructorSessionResultSectionType = InstructorSessionResultSectionType;
  InstructorSessionResultViewType: typeof InstructorSessionResultViewType = InstructorSessionResultViewType;

  formattedSessionOpeningTime: string = '';
  formattedSessionClosingTime: string = '';

  viewType: string = InstructorSessionResultViewType.QUESTION;
  section: string = '';
  sectionType: InstructorSessionResultSectionType = InstructorSessionResultSectionType.EITHER;
  groupByTeam: boolean = true;
  showStatistics: boolean = true;
  indicateMissingResponses: boolean = true;

  // below are two models contain similar and duplicate data
  // they are for different views
  sectionsModel: Record<string, SectionTabModel> = {};
  isSectionsLoaded: boolean = false;
  questionsModel: Record<string, QuestionTabModel> = {};
  isQuestionsLoaded: boolean = false;

  noResponseStudents: Student[] = [];
  isNoResponsePanelLoaded: boolean = false;

  allStudentsInCourse: Student[] = [];

  FeedbackSessionPublishStatus: typeof FeedbackSessionPublishStatus = FeedbackSessionPublishStatus;
  isExpandAll: boolean = false;

  @ViewChild(InstructorSessionNoResponsePanelComponent, { static: false }) noResponsePanel?:
    InstructorSessionNoResponsePanelComponent;

  constructor(private feedbackSessionsService: FeedbackSessionsService,
              private feedbackQuestionsService: FeedbackQuestionsService,
              private courseService: CourseService,
              private studentService: StudentService,
              private instructorService: InstructorService,
              private route: ActivatedRoute,
              private timezoneService: TimezoneService,
              private modalService: NgbModal,
              private router: Router,
              private commentsToCommentTableModel: CommentsToCommentTableModelPipe,
              statusMessageService: StatusMessageService,
              commentService: FeedbackResponseCommentService,
              commentToCommentRowModel: CommentToCommentRowModelPipe) {
    super(commentToCommentRowModel, commentService, statusMessageService);
    this.timezoneService.getTzVersion(); // import timezone service to load timezone data
  }

  ngOnInit(): void {

    this.route.queryParams.subscribe((queryParams: any) => {
      this.feedbackSessionsService.getFeedbackSession({
        courseId: queryParams.courseid,
        feedbackSessionName: queryParams.fsname,
        intent: Intent.INSTRUCTOR_RESULT,
      }).subscribe((feedbackSession: FeedbackSession) => {
        const TIME_FORMAT: string = 'ddd, DD MMM, YYYY, hh:mm A zz';
        this.session = feedbackSession;
        this.formattedSessionOpeningTime =
            moment(this.session.submissionStartTimestamp).tz(this.session.timeZone).format(TIME_FORMAT);
        this.formattedSessionClosingTime =
            moment(this.session.submissionEndTimestamp).tz(this.session.timeZone).format(TIME_FORMAT);

        // load section tabs
        this.courseService.getCourseSectionNames(queryParams.courseid)
            .subscribe((courseSectionNames: CourseSectionNames) => {
              for (const sectionName of courseSectionNames.sectionNames) {
                this.sectionsModel[sectionName] = {
                  questions: [],
                  hasPopulated: false,
                  isTabExpanded: false,
                };
              }
              this.isSectionsLoaded = true;
            }, (resp: ErrorMessageOutput) => {
              this.statusMessageService.showErrorMessage(resp.error.message);
            });

        // load question tabs
        this.feedbackQuestionsService.getFeedbackQuestions({
          courseId: queryParams.courseid,
          feedbackSessionName: queryParams.fsname,
          intent: Intent.INSTRUCTOR_RESULT,
        }).subscribe((feedbackQuestions: FeedbackQuestions) => {
          for (const question of feedbackQuestions.questions) {
            this.questionsModel[question.feedbackQuestionId] = {
              question,
              responses: [],
              statistics: '',
              hasPopulated: false,
              isTabExpanded: false,
            };
          }
          this.isQuestionsLoaded = true;
        }, (resp: ErrorMessageOutput) => {
          this.statusMessageService.showErrorMessage(resp.error.message);
        });

        // load all students in course
        this.studentService.getStudentsFromCourse({
          courseId: queryParams.courseid,
        }).subscribe((allStudents: Students) => {
          this.allStudentsInCourse = allStudents.students;

          // load no response students
          this.feedbackSessionsService.getFeedbackSessionSubmittedGiverSet({
            courseId: queryParams.courseid,
            feedbackSessionName: queryParams.fsname,
          }).subscribe((feedbackSessionSubmittedGiverSet: FeedbackSessionSubmittedGiverSet) => {
            // TODO team is missing
            this.noResponseStudents = this.allStudentsInCourse.filter((student: Student) =>
                                        !feedbackSessionSubmittedGiverSet.giverIdentifiers.includes(student.email));
          }, (resp: ErrorMessageOutput) => {
            this.statusMessageService.showErrorMessage(resp.error.message);
          });

          this.isNoResponsePanelLoaded = true;

        }, (resp: ErrorMessageOutput) => {
          this.statusMessageService.showErrorMessage(resp.error.message);
        });

        // load current instructor name
        this.instructorService.getInstructor({
          courseId: queryParams.courseid,
          intent: Intent.FULL_DETAIL,
        }).subscribe((instructor: Instructor) => {
          this.currInstructorName = instructor.name;
        });
      }, (resp: ErrorMessageOutput) => {
        this.statusMessageService.showErrorMessage(resp.error.message);
      });
    });
  }

  /**
   * Toggles the question tab in per question view.
   */
  toggleQuestionTab(questionId: string): void {
    this.questionsModel[questionId].isTabExpanded = !this.questionsModel[questionId].isTabExpanded;
    if (this.questionsModel[questionId].isTabExpanded) {
      this.loadQuestionTab(questionId);
    }
  }

  /**
   * Loads all the responses and response statistics for the specified question.
   */
  loadQuestionTab(questionId: string): void {
    if (this.questionsModel[questionId].hasPopulated) {
      // Do not re-fetch data
      return;
    }
    this.feedbackSessionsService.getFeedbackSessionResults({
      questionId,
      courseId: this.session.courseId,
      feedbackSessionName: this.session.feedbackSessionName,
      intent: Intent.INSTRUCTOR_RESULT,
    })
    .subscribe((resp: SessionResults) => {
      if (resp.questions.length) {
        const responses: QuestionOutput = resp.questions[0];
        this.questionsModel[questionId].responses = responses.allResponses;
        this.questionsModel[questionId].statistics = responses.questionStatistics;
        this.questionsModel[questionId].hasPopulated = true;

        this.preprocessComments(responses.allResponses);
      }
    }, (resp: ErrorMessageOutput) => {
      this.statusMessageService.showErrorMessage(resp.error.message);
    });
  }

  /**
   * Toggles the section tab in per section view.
   */
  toggleSectionTab(sectionName: string): void {
    this.sectionsModel[sectionName].isTabExpanded = !this.sectionsModel[sectionName].isTabExpanded;
    if (this.sectionsModel[sectionName].isTabExpanded) {
      this.loadSectionTab(sectionName);
    }
  }

  /**
   * Loads all the responses and response statistics for the specified section.
   */
  loadSectionTab(sectionName: string): void {
    if (this.sectionsModel[sectionName].hasPopulated) {
      // Do not re-fetch data
      return;
    }
    this.feedbackSessionsService.getFeedbackSessionResults({
      courseId: this.session.courseId,
      feedbackSessionName: this.session.feedbackSessionName,
      intent: Intent.INSTRUCTOR_RESULT,
      groupBySection: sectionName,
    })
    .subscribe((resp: SessionResults) => {
      this.sectionsModel[sectionName].questions = resp.questions;
      this.sectionsModel[sectionName].hasPopulated = true;

      // sort questions by question number
      resp.questions.sort((a: QuestionOutput, b: QuestionOutput) =>
          a.feedbackQuestion.questionNumber - b.feedbackQuestion.questionNumber);
      resp.questions.forEach((question: QuestionOutput) => {
        this.preprocessComments(question.allResponses);
      });
    }, (resp: ErrorMessageOutput) => {
      this.statusMessageService.showErrorMessage(resp.error.message);
    });
  }

  /**
   * Preprocesses the comments from instructor.
   *
   * <p>The instructor comment will be moved to map {@code instructorCommentTableModel}. The original
   * instructor comments associated with the response will be deleted.
   */
  preprocessComments(responses: ResponseOutput[]): void {
    responses.forEach((response: ResponseOutput) => {
      this.instructorCommentTableModel[response.responseId] =
          this.commentsToCommentTableModel.transform(response.instructorComments, false, this.session.timeZone);

      // clear the original comments for safe as instructorCommentTableModel will become the single point of truth
      response.instructorComments = [];
    });
  }

  /**
   * Handle publish result button event.
   */
  publishResultHandler(): void {
    const isPublished: boolean = this.session.publishStatus === FeedbackSessionPublishStatus.PUBLISHED;
    const modalRef: NgbModalRef = this.modalService.open(isPublished ? ConfirmUnpublishingSessionModalComponent :
        ConfirmPublishingSessionModalComponent);
    modalRef.componentInstance.feedbackSessionName = this.session.feedbackSessionName;

    modalRef.result.then(() => {
      const response: Observable<any> = isPublished ?
          this.feedbackSessionsService.unpublishFeedbackSession(
            this.session.courseId, this.session.feedbackSessionName,
          ) :
          this.feedbackSessionsService.publishFeedbackSession(
            this.session.courseId, this.session.feedbackSessionName,
          )
      ;

      response.subscribe(() => {
        this.router.navigateByUrl('/web/instructor/sessions');
      }, (resp: ErrorMessageOutput) => {
        this.statusMessageService.showErrorMessage(resp.error.message);
      });
    }, () => {});
  }

  /**
   * Handle print view button event.
   */
  printViewHandler(): void {
    this.expandAllTabs();
    setTimeout(() => {
      // TODO the timeout is brittle
      window.print();
    }, 1000);
  }

  /**
   * Handle download results button event.
   */
  downloadResultHandler(): void {
    const filename: string = `${this.session.feedbackSessionName.concat('_result')}.csv`;
    let blob: any;

    this.feedbackSessionsService.downloadSessionResults(
      this.session.courseId,
      this.session.feedbackSessionName,
      Intent.INSTRUCTOR_RESULT,
      this.indicateMissingResponses,
      this.showStatistics,
    ).subscribe((resp: string) => {
      blob = new Blob([resp], { type: 'text/csv' });
      saveAs(blob, filename);
    }, (resp: ErrorMessageOutput) => {
      this.statusMessageService.showErrorMessage(resp.error.message);
    });
  }

  /**
   * Handle expand all questions button event.
   */
  toggleExpandAllHandler(): void {
    if (!this.isExpandAll) {
      this.expandAllTabs();
    } else {
      this.collapseAllTabs();
    }
  }

  /**
   * Expands the tab of all sections.
   */
  expandAllTabs(): void {
    this.isExpandAll = true;

    if (this.noResponsePanel !== undefined) {
      this.noResponsePanel.expandTab();
    }

    if (this.viewType === InstructorSessionResultViewType.QUESTION) {
      for (const questionId of Object.keys(this.questionsModel)) {
        this.questionsModel[questionId].isTabExpanded = true;
        this.loadQuestionTab(questionId);
      }
      return;
    }

    for (const sectionName of Object.keys(this.sectionsModel)) {
      this.sectionsModel[sectionName].isTabExpanded = true;
      this.loadSectionTab(sectionName);
    }
  }

  /**
   * Collapses the tab of all sections.
   */
  collapseAllTabs(): void {
    this.isExpandAll = false;

    if (this.noResponsePanel !== undefined) {
      this.noResponsePanel.collapseTab();
    }

    if (this.viewType === InstructorSessionResultViewType.QUESTION) {
      for (const questionId of Object.keys(this.questionsModel)) {
        this.questionsModel[questionId].isTabExpanded = false;
      }
      return;
    }

    for (const sectionName of Object.keys(this.sectionsModel)) {
      this.sectionsModel[sectionName].isTabExpanded = false;
    }
  }

  /**
   * Handles the sending of reminders to students.
   */
  sendReminderToStudents(studentsToRemindData: StudentListInfoTableRowModel[]): void {
    this.feedbackSessionsService
      .remindFeedbackSessionSubmissionForStudent(this.session.courseId, this.session.feedbackSessionName, {
        usersToRemind: studentsToRemindData.map((m: StudentListInfoTableRowModel) => m.email),
      }).subscribe(() => {
        this.statusMessageService.showSuccessMessage(
          'Reminder e-mails have been sent out to those students and instructors. '
          + 'Please allow up to 1 hour for all the notification emails to be sent out.');

      }, (resp: ErrorMessageOutput) => {
        this.statusMessageService.showErrorMessage(resp.error.message);
      });
  }

  /**
   * Handles view type changes.
   */
  handleViewTypeChange(newViewType: InstructorSessionResultViewType): void {
    if (this.viewType === newViewType) {
      // do nothing
      return;
    }
    this.viewType = newViewType;
    // the expand all will be reset if the view type changed
    this.collapseAllTabs();
  }
}
