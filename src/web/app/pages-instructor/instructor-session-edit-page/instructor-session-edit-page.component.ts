import { Component, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, switchMap } from 'rxjs/operators';
import { HttpRequestService } from '../../../services/http-request.service';
import { NavigationService } from '../../../services/navigation.service';
import { StatusMessageService } from '../../../services/status-message.service';
import {
  SessionEditFormEditOutputModel,
  SessionEditFromMode,
} from '../../components/session-edit-form/session-edit-form-model';
import {
  SessionEditFormComponent,
} from '../../components/session-edit-form/session-edit-form.component';
import { FeedbackSession } from '../../feedback-session';
import { ErrorMessageOutput } from '../../message-output';

/**
 * Instructor feedback session edit page.
 */
@Component({
  selector: 'tm-instructor-session-edit-page',
  templateUrl: './instructor-session-edit-page.component.html',
  styleUrls: ['./instructor-session-edit-page.component.scss'],
})
export class InstructorSessionEditPageComponent implements OnInit {

  // enum
  SessionEditFromMode: typeof SessionEditFromMode = SessionEditFromMode;

  // url param
  user: string = '';
  courseId: string = '';
  feedbackSessionName: string = '';

  // session form
  @ViewChild(SessionEditFormComponent)
  private sessionEditForm!: SessionEditFormComponent;

  constructor(private route: ActivatedRoute, private router: Router, private httpRequestService: HttpRequestService,
              private statusMessageService: StatusMessageService, private navigationService: NavigationService) { }

  ngOnInit(): void {
    this.route.queryParams.subscribe((queryParams: any) => {
      this.user = queryParams.user;
      this.courseId = queryParams.courseid;
      this.feedbackSessionName = queryParams.fsname;

      this.loadFeedbackSession();
    });
  }

  /**
   * Loads a feedback session.
   */
  loadFeedbackSession(): void {
    this.sessionEditForm.setIsSaving(true);
    const paramMap: { [key: string]: string } = { courseid: this.courseId, fsname: this.feedbackSessionName };
    this.httpRequestService.get('/session', paramMap)
        .pipe(
            finalize(() => { this.sessionEditForm.setIsSaving(false); }),
        )
        .subscribe((feedbackSession: FeedbackSession) => {
          this.sessionEditForm.setEditModel(Object.assign({ courseName: 'TODO: USE COURSE API' }, feedbackSession));
          this.sessionEditForm.setIsEditable(false);
        }, (resp: ErrorMessageOutput) => {
          this.statusMessageService.showErrorMessage(resp.error.message);
        });
  }

  /**
   * Handles editing existing session event.
   */
  editExistingSessionHandler(model$: Observable<SessionEditFormEditOutputModel>): void {
    this.sessionEditForm.setIsSaving(true);
    const paramMap: { [key: string]: string } = { courseid: this.courseId, fsname: this.feedbackSessionName };
    model$.pipe(
        switchMap((val: SessionEditFormEditOutputModel) => {
          return this.httpRequestService.put('/session', paramMap, val);
        }),
        finalize(() => { this.sessionEditForm.setIsSaving(false); }),
    ).subscribe((val: FeedbackSession) => {
      this.sessionEditForm.setEditModel(Object.assign({ courseName: 'TODO: USE COURSE API' }, val));
      this.sessionEditForm.setIsEditable(false);
      this.statusMessageService.showSuccessMessage('The feedback session has been updated.');
    }, (resp: ErrorMessageOutput) => {
      this.statusMessageService.showErrorMessage(resp.error.message);
    });
  }

  /**
   * Handles deleting current feedback session.
   */
  deleteExistingSessionHandler(): void {
    const paramMap: { [key: string]: string } = { courseid: this.courseId, fsname: this.feedbackSessionName };
    this.httpRequestService.put('/bin/session', paramMap).subscribe(() => {
      this.navigationService.navigateWithSuccessMessage(this.router, '/web/instructor/sessions',
          'The feedback session has been deleted. You can restore it from the deleted sessions table below.');
    }, (resp: ErrorMessageOutput) => {
      this.statusMessageService.showErrorMessage(resp.error.message);
    });
  }
}
