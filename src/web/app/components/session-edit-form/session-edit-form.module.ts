import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { AjaxLoadingModule } from '../ajax-loading/ajax-loading.module';
import { SessionEditFormComponent } from './session-edit-form.component';
import { SubmissionStatusNamePipe } from './submission-status-name.pipe';
import { TimePickerComponent } from './time-picker/time-picker.component';

/**
 * Module for instructor session edit/create form.
 */
@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    NgbModule,
    AjaxLoadingModule,
  ],
  declarations: [
    SessionEditFormComponent,
    TimePickerComponent,
    SubmissionStatusNamePipe,
  ],
  exports: [
    SessionEditFormComponent,
  ],
})
export class SessionEditFormModule { }
