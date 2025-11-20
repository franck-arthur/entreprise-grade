/**
 * Batch import models
 */

export enum BatchImportStatus {
  PENDING = 'PENDING',
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED',
  COMPLETED_WITH_ERRORS = 'COMPLETED_WITH_ERRORS',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED'
}

export interface BatchImport {
  id: string;
  fileName: string;
  fileSize: number;
  status: BatchImportStatus;
  totalLines: number;
  processedLines: number;
  successLines: number;
  failedLines: number;
  progressPercentage: number;
  errorMessage?: string;
  initiatedByUserId: string;
  initiatedByUsername: string;
  createdAt: string;
  updatedAt: string;
  startedAt?: string;
  completedAt?: string;
}

export interface BatchImportLine {
  id: string;
  lineNumber: number;
  rawData: string;
  success: boolean;
  errorMessage?: string;
  createdUserId?: string;
  createdUsername?: string;
}

export interface BatchImportDetail extends BatchImport {
  lines: BatchImportLine[];
}

export interface BatchImportPage {
  content: BatchImport[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
