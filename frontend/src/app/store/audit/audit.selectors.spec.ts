import * as fromAudit from './audit.selectors';
import { AuditState } from './audit.reducer';
import { AuditEvent, AuditEventType, AuditEventCategory, AuditStatistics } from '../../core/models/audit.model';

describe('Audit Selectors', () => {
  const mockEvent: AuditEvent = {
    id: '1',
    eventType: AuditEventType.USER_CREATED,
    eventCategory: 'USER',
    userId: 'user-1',
    username: 'testuser',
    targetEntityType: 'USER',
    targetEntityId: 'entity-1',
    targetEntityName: 'Test User',
    details: 'User created',
    ipAddress: '192.168.1.1',
    userAgent: 'Mozilla/5.0',
    success: true,
    errorMessage: undefined,
    timestamp: '2024-01-01T10:00:00',
    eventHour: 10
  };

  const mockStatistics: AuditStatistics = {
    totalEvents: 1000,
    successfulEvents: 950,
    failedEvents: 50,
    eventsByType: {
      [AuditEventType.USER_CREATED]: 100,
      [AuditEventType.LOGIN_SUCCESS]: 500
    },
    eventsByCategory: {
      ['USER']: 300,
      ['AUTH']: 600
    },
    topUsers: {
      'user1': 50,
      'user2': 30
    },
    topTargetEntities: {
      'USER': 200,
      'BATCH': 100
    }
  };

  const mockState: { audit: AuditState } = {
    audit: {
      events: [mockEvent],
      statistics: mockStatistics,
      currentQuery: { eventTypes: [AuditEventType.USER_CREATED] },
      totalElements: 100,
      totalPages: 5,
      currentPage: 2,
      loading: false,
      loadingStatistics: false,
      error: null
    }
  };

  describe('selectAuditEvents', () => {
    it('should select audit events', () => {
      const result = fromAudit.selectAuditEvents(mockState);
      expect(result).toEqual([mockEvent]);
      expect(result.length).toBe(1);
    });

    it('should return empty array when no events', () => {
      const emptyState = { audit: { ...mockState.audit, events: [] } };
      const result = fromAudit.selectAuditEvents(emptyState);
      expect(result).toEqual([]);
    });
  });

  describe('selectAuditStatistics', () => {
    it('should select audit statistics', () => {
      const result = fromAudit.selectAuditStatistics(mockState);
      expect(result).toEqual(mockStatistics);
      expect(result?.totalEvents).toBe(1000);
      expect(result?.successfulEvents).toBe(950);
    });

    it('should return null when no statistics', () => {
      const stateWithoutStats = { audit: { ...mockState.audit, statistics: null } };
      const result = fromAudit.selectAuditStatistics(stateWithoutStats);
      expect(result).toBeNull();
    });
  });

  describe('selectAuditCurrentQuery', () => {
    it('should select current query', () => {
      const result = fromAudit.selectAuditCurrentQuery(mockState);
      expect(result).toEqual({ eventTypes: [AuditEventType.USER_CREATED] });
    });

    it('should return empty query when not set', () => {
      const stateWithoutQuery = { audit: { ...mockState.audit, currentQuery: {} } };
      const result = fromAudit.selectAuditCurrentQuery(stateWithoutQuery);
      expect(result).toEqual({});
    });
  });

  describe('selectAuditLoading', () => {
    it('should select loading state as false', () => {
      const result = fromAudit.selectAuditLoading(mockState);
      expect(result).toBe(false);
    });

    it('should select loading state as true', () => {
      const loadingState = { audit: { ...mockState.audit, loading: true } };
      const result = fromAudit.selectAuditLoading(loadingState);
      expect(result).toBe(true);
    });
  });

  describe('selectAuditLoadingStatistics', () => {
    it('should select statistics loading state as false', () => {
      const result = fromAudit.selectAuditLoadingStatistics(mockState);
      expect(result).toBe(false);
    });

    it('should select statistics loading state as true', () => {
      const loadingState = { audit: { ...mockState.audit, loadingStatistics: true } };
      const result = fromAudit.selectAuditLoadingStatistics(loadingState);
      expect(result).toBe(true);
    });
  });

  describe('selectAuditError', () => {
    it('should select error as null', () => {
      const result = fromAudit.selectAuditError(mockState);
      expect(result).toBeNull();
    });

    it('should select error message', () => {
      const errorState = { audit: { ...mockState.audit, error: 'Something went wrong' } };
      const result = fromAudit.selectAuditError(errorState);
      expect(result).toBe('Something went wrong');
    });
  });

  describe('selectAuditPagination', () => {
    it('should select pagination info', () => {
      const result = fromAudit.selectAuditPagination(mockState);
      expect(result).toEqual({
        totalElements: 100,
        totalPages: 5,
        currentPage: 2
      });
    });

    it('should handle zero pagination', () => {
      const emptyPaginationState = {
        audit: {
          ...mockState.audit,
          totalElements: 0,
          totalPages: 0,
          currentPage: 0
        }
      };
      const result = fromAudit.selectAuditPagination(emptyPaginationState);
      expect(result).toEqual({
        totalElements: 0,
        totalPages: 0,
        currentPage: 0
      });
    });
  });

  describe('Selector Memoization', () => {
    it('should return same reference when state unchanged', () => {
      const result1 = fromAudit.selectAuditEvents(mockState);
      const result2 = fromAudit.selectAuditEvents(mockState);
      expect(result1).toBe(result2);
    });

    it('should return different reference when state changes', () => {
      const result1 = fromAudit.selectAuditEvents(mockState);
      const newState = {
        audit: {
          ...mockState.audit,
          events: [{ ...mockEvent, id: '2' }]
        }
      };
      const result2 = fromAudit.selectAuditEvents(newState);
      expect(result1).not.toBe(result2);
    });

    it('should memoize computed selectors', () => {
      const result1 = fromAudit.selectAuditPagination(mockState);
      const result2 = fromAudit.selectAuditPagination(mockState);
      expect(result1).toBe(result2);
    });
  });
});
