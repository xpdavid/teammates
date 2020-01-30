import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { SingleResponseModule } from '../question-responses/single-response/single-response.module';
import { RichTextEditorModule } from '../rich-text-editor/rich-text-editor.module';
import { TeammatesCommonModule } from '../teammates-common/teammates-common.module';
import { CommentEditFormComponent } from './comment-edit-form/comment-edit-form.component';
import {
  CommentVisibilityControlNamePipe,
  CommentVisibilityTypeDescriptionPipe, CommentVisibilityTypeNamePipe,
} from './comment-edit-form/comment-visibility-setting.pipe';
import { CommentRowComponent } from './comment-row/comment-row.component';
import { CommentTableModalComponent } from './comment-table-modal/comment-table-modal.component';
import { CommentTableComponent } from './comment-table/comment-table.component';
import {
  ConfirmDeleteCommentModalComponent,
} from './confirm-delete-comment-modal/confirm-delete-comment-modal.component';

/**
 * Module for comments table
 */
@NgModule({
  declarations: [
    CommentEditFormComponent,
    CommentRowComponent,
    ConfirmDeleteCommentModalComponent,
    CommentTableModalComponent,
    CommentTableComponent,
    CommentVisibilityControlNamePipe,
    CommentVisibilityTypeDescriptionPipe,
    CommentVisibilityTypeNamePipe,
  ],
  imports: [
    TeammatesCommonModule,
    CommonModule,
    SingleResponseModule,
    RichTextEditorModule,
    NgbModule,
    FormsModule,
  ],
  exports: [
    CommentRowComponent,
    CommentTableComponent,
  ],
  entryComponents: [
    ConfirmDeleteCommentModalComponent,
    CommentTableModalComponent,
  ],
})
export class CommentBoxModule { }
