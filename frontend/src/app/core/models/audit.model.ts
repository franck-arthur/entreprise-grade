/**
 * Audit event models and types
 */

export enum AuditEventType {
  // User management
  USER_CREATED = 'USER_CREATED',
  USER_UPDATED = 'USER_UPDATED',
  USER_DELETED = 'USER_DELETED',
  USER_ACTIVATED = 'USER_ACTIVATED',
  USER_DEACTIVATED = 'USER_DEACTIVATED',

  // Authentication
  LOGIN_SUCCESS = 'LOGIN_SUCCESS',
  LOGIN_FAILED = 'LOGIN_FAILED',
  LOGOUT = 'LOGOUT',
  PASSWORD_CHANGED = 'PASSWORD_CHANGED',

  // Batch import
  BATCH_IMPORT_STARTED = 'BATCH_IMPORT_STARTED',
  BATCH_IMPORT_COMPLETED = 'BATCH_IMPORT_COMPLETED',
  BATCH_IMPORT_FAILED = 'BATCH_IMPORT_FAILED',

  // Security
  UNAUTHORIZED_ACCESS = 'UNAUTHORIZED_ACCESS',
  FORBIDDEN_ACCESS = 'FORBIDDEN_ACCESS',

  // System
  SYSTEM_ERROR = 'SYSTEM_ERROR',
  CONFIGURATION_CHANGED = 'CONFIGURATION_CHANGED'
}

export type AuditEventCategory = 'USER' | 'AUTH' | 'BATCH' | 'SECURITY' | 'SYSTEM';

export interface AuditEvent {
  id: string;
  eventType: AuditEventType;
  eventCategory: AuditEventCategory;
  userId?: string;
  username?: string;
  targetEntityType?: string;
  targetEntityId?: string;
  targetEntityName?: string;
  ipAddress?: string;
  userAgent?: string;
  details?: string;
  success: boolean;
  errorMessage?: string;
  timestamp: string;
}

export interface AuditEventQuery {
  eventTypes?: AuditEventType[];
  eventCategory?: AuditEventCategory;
  userId?: string;
  username?: string;
  targetEntityType?: string;
  targetEntityId?: string;
  success?: boolean;
  fromDate?: string;
  toDate?: string;
  ipAddress?: string;
}

export interface AuditStatistics {
  totalEvents: number;
  successfulEvents: number;
  failedEvents: number;
  eventsByType: Record<string, number>;
  eventsByCategory: Record<string, number>;
  eventsByHour?: Record<string, number>;
  eventsByDate?: Record<string, number>;
  topUsers: Record<string, number>;
  topTargetEntities: Record<string, number>;
}

export interface AuditEventPage {
  content: AuditEvent[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
