import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Intent } from '../Intent';
import { PageNotFoundComponent } from '../page-not-found/page-not-found.component';
import { PageNotFoundModule } from '../page-not-found/page-not-found.module';
import { StudentHelpPageComponent } from '../pages-help/student-help-page/student-help-page.component';
import { StudentHelpPageModule } from '../pages-help/student-help-page/student-help-page.module';
import { SessionResultPageComponent } from '../pages-session/session-result-page/session-result-page.component';
import { SessionResultPageModule } from '../pages-session/session-result-page/session-result-page.module';
import {
  SessionSubmissionPageComponent,
} from '../pages-session/session-submission-page/session-submission-page.component';
import { SessionSubmissionPageModule } from '../pages-session/session-submission-page/session-submission-page.module';
import { StudentCourseDetailsPageComponent } from './student-course-details-page/student-course-details-page.component';
import { StudentHomePageComponent } from './student-home-page/student-home-page.component';
import { StudentProfilePageComponent } from './student-profile-page/student-profile-page.component';

const routes: Routes = [
  {
    path: 'home',
    component: StudentHomePageComponent,
  },
  {
    path: 'profile',
    component: StudentProfilePageComponent,
  },
  {
    path: 'course',
    component: StudentCourseDetailsPageComponent,
  },
  {
    path: 'sessions',
    children: [
      {
        path: 'result',
        component: SessionResultPageComponent,
      },
      {
        path: 'submission',
        component: SessionSubmissionPageComponent,
        data: {
          pageTitle: 'Submit Feedback',
          intent: Intent.STUDENT_SUBMISSION,
        },
      },
    ],
  },
  {
    path: 'help',
    component: StudentHelpPageComponent,
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'home',
  },
  {
    path: '**',
    pathMatch: 'full',
    component: PageNotFoundComponent,
  },
];

/**
 * Module for student pages.
 */
@NgModule({
  imports: [
    CommonModule,
    PageNotFoundModule,
    StudentHelpPageModule,
    SessionResultPageModule,
    SessionSubmissionPageModule,
    RouterModule.forChild(routes),
  ],
  declarations: [
    StudentHomePageComponent,
    StudentProfilePageComponent,
    StudentCourseDetailsPageComponent,
  ],
})
export class StudentPagesModule {}
