import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Store } from '@ngrx/store';
import { TranslateModule } from '@ngx-translate/core';
import { uploadCsvFile } from '../../../../store/batch-import/batch-import.actions';
import {
  selectBatchImportUploadingFile,
  selectBatchImportError
} from '../../../../store/batch-import/batch-import.selectors';

/**
 * Component for uploading CSV files for batch import.
 * Uses DSFR file upload component.
 */
@Component({
  selector: 'app-batch-import-upload',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './batch-import-upload.component.html',
  styleUrls: ['./batch-import-upload.component.scss']
})
export class BatchImportUploadComponent {
  uploadingFile$ = this.store.select(selectBatchImportUploadingFile);
  error$ = this.store.select(selectBatchImportError);

  selectedFile: File | null = null;
  dragOver = false;

  constructor(private store: Store) {}

  /**
   * Handle file selection from input.
   */
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
      this.validateAndUpload();
    }
  }

  /**
   * Handle drag over event.
   */
  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver = true;
  }

  /**
   * Handle drag leave event.
   */
  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver = false;
  }

  /**
   * Handle file drop.
   */
  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver = false;

    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
      this.selectedFile = event.dataTransfer.files[0];
      this.validateAndUpload();
    }
  }

  /**
   * Validate file and upload if valid.
   */
  private validateAndUpload(): void {
    if (!this.selectedFile) {
      return;
    }

    // Validate file extension
    if (!this.selectedFile.name.endsWith('.csv')) {
      alert('Please select a CSV file');
      this.selectedFile = null;
      return;
    }

    // Validate file size (max 10MB)
    const maxSize = 10 * 1024 * 1024; // 10MB
    if (this.selectedFile.size > maxSize) {
      alert('File size must not exceed 10MB');
      this.selectedFile = null;
      return;
    }

    // Upload file
    this.store.dispatch(uploadCsvFile({ file: this.selectedFile }));
    this.selectedFile = null;
  }

  /**
   * Get file size in human-readable format.
   */
  getFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
  }
}
