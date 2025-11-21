import { auditReducer, initialState, AuditState } from './audit.reducer';
import * as AuditActions from './audit.actions';
import { AuditEvent, AuditEventType, AuditEventCategory, AuditStatistics } from '../../core/models/audit.model';

describe('Audit Reducer', () => {
  describe('unknown action', () => {
    it('should return the initial state', () => {
      const action = { type: 'Unknown' } as any;
      const result = auditReducer(initialState, action);

      expect(result).toBe(initialState);
    });
  });

  describe('loadAuditEvents', () => {
    it('should set loading to true and store query', () => {
      const query = { eventTypes: [AuditEventType.USER_CREATED] };
      const action = AuditActions.loadAuditEvents({ query, page: 0, size: 50 });
      const result = auditReducer(initialState, action);

      expect(result.loading).toBe(true);
      expect(result.currentQuery).toEqual(query);
      expect(result.currentPage).toBe(0);
      expect(result.error).toBeNull();
    });

    it('should preserve existing events while loading', () => {
      const existingEvents: AuditEvent[] = [{
        id: '1',
        eventType: AuditEventType.USER_CREATED,
        eventCategory: 'USER',
        userId: 'user-1',
        username: 'test',
        targetEntityType: 'USER',
        targetEntityId: 'entity-1',
        targetEntityName: 'Test',
        description: 'Test',
        ipAddress: '127.0.0.1',
        userAgent: 'test',
        success: true,
        errorMessage: undefined,
        metadata: {},
        timestamp: '2024-01-01T10:00:00',
        eventDate: '2024-01-01',
        eventHour: 10
      }];
      const stateWithEvents: AuditState = { ...initialState, events: existingEvents };
      const action = AuditActions.loadAuditEvents({ query: {}, page: 1, size: 50 });
      const result = auditReducer(stateWithEvents, action);

      expect(result.events).toEqual(existingEvents);
      expect(result.loading).toBe(true);
    });
  });

  describe('loadAuditEventsSuccess', () => {
    it('should update events and pagination', () => {
      const events: AuditEvent[] = [
        {
          id: '1',
          eventType: AuditEventType.LOGIN_SUCCESS,
          eventCategory: 'AUTH',
          userId: 'user-1',
          username: 'testuser',
          targetEntityType: 'USER',
          targetEntityId: 'user-1',
          targetEntityName: 'Test User',
          description: 'Login successful',
          ipAddress: '192.168.1.1',
          userAgent: 'Mozilla/5.0',
          success: true,
          errorMessage: undefined,
          metadata: {},
          timestamp: '2024-01-01T10:00:00',
          eventDate: '2024-01-01',
          eventHour: 10
        }
      ];
      const action = AuditActions.loadAuditEventsSuccess({
        events,
        totalElements: 100,
        totalPages: 2
      });
      const result = auditReducer(initialState, action);

      expect(result.events).toEqual(events);
      expect(result.totalElements).toBe(100);
      expect(result.totalPages).toBe(2);
      expect(result.loading).toBe(false);
      expect(result.error).toBeNull();
    });

    it('should clear loading state', () => {
      const loadingState: AuditState = { ...initialState, loading: true };
      const action = AuditActions.loadAuditEventsSuccess({
        events: [],
        totalElements: 0,
        totalPages: 0
      });
      const result = auditReducer(loadingState, action);

      expect(result.loading).toBe(false);
    });
  });

  describe('loadAuditEventsFailure', () => {
    it('should set error and clear loading', () => {
      const loadingState: AuditState = { ...initialState, loading: true };
      const errorMessage = 'Failed to load audit events';
      const action = AuditActions.loadAuditEventsFailure({ error: errorMessage });
      const result = auditReducer(loadingState, action);

      expect(result.loading).toBe(false);
      expect(result.error).toBe(errorMessage);
    });
  });

  describe('loadAuditEventsByUser', () => {
    it('should set loading and page', () => {
      const action = AuditActions.loadAuditEventsByUser({ userId: 'user-123', page: 1, size: 20 });
      const result = auditReducer(initialState, action);

      expect(result.loading).toBe(true);
      expect(result.currentPage).toBe(1);
      expect(result.error).toBeNull();
    });
  });

  describe('loadAuditEventsByUserSuccess', () => {
    it('should update state with user events', () => {
      const events: AuditEvent[] = [{
        id: '1',
        eventType: AuditEventType.USER_UPDATED,
        eventCategory: 'USER',
        userId: 'user-123',
        username: 'testuser',
        targetEntityType: 'USER',
        targetEntityId: 'user-123',
        targetEntityName: 'Test User',
        description: 'User updated',
        ipAddress: '192.168.1.1',
        userAgent: 'Mozilla/5.0',
        success: true,
        errorMessage: undefined,
        metadata: {},
        timestamp: '2024-01-01T10:00:00',
        eventDate: '2024-01-01',
        eventHour: 10
      }];
      const action = AuditActions.loadAuditEventsByUserSuccess({
        events,
        totalElements: 10,
        totalPages: 1
      });
      const result = auditReducer(initialState, action);

      expect(result.events).toEqual(events);
      expect(result.totalElements).toBe(10);
      expect(result.loading).toBe(false);
    });
  });

  describe('loadAuditStatistics', () => {
    it('should set loadingStatistics to true', () => {
      const action = AuditActions.loadAuditStatistics();
      const result = auditReducer(initialState, action);

      expect(result.loadingStatistics).toBe(true);
      expect(result.error).toBeNull();
    });
  });

  describe('loadAuditStatisticsSuccess', () => {
    it('should update statistics', () => {
      const statistics: AuditStatistics = {
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
      const action = AuditActions.loadAuditStatisticsSuccess({ statistics });
      const result = auditReducer(initialState, action);

      expect(result.statistics).toEqual(statistics);
      expect(result.loadingStatistics).toBe(false);
      expect(result.error).toBeNull();
    });
  });

  describe('loadAuditStatisticsFailure', () => {
    it('should set error for statistics loading', () => {
      const loadingState: AuditState = { ...initialState, loadingStatistics: true };
      const errorMessage = 'Failed to load statistics';
      const action = AuditActions.loadAuditStatisticsFailure({ error: errorMessage });
      const result = auditReducer(loadingState, action);

      expect(result.loadingStatistics).toBe(false);
      expect(result.error).toBe(errorMessage);
    });
  });

  describe('loadAuditStatisticsForDateRange', () => {
    it('should set loadingStatistics to true', () => {
      const action = AuditActions.loadAuditStatisticsForDateRange({
        fromDate: '2024-01-01',
        toDate: '2024-01-31'
      });
      const result = auditReducer(initialState, action);

      expect(result.loadingStatistics).toBe(true);
      expect(result.error).toBeNull();
    });
  });

  describe('loadAuditStatisticsForDateRangeSuccess', () => {
    it('should update statistics with date range data', () => {
      const statistics: AuditStatistics = {
        totalEvents: 500,
        successfulEvents: 480,
        failedEvents: 20,
        eventsByType: {},
        eventsByCategory: {},
        topUsers: {},
        topTargetEntities: {}
      };
      const action = AuditActions.loadAuditStatisticsForDateRangeSuccess({ statistics });
      const result = auditReducer(initialState, action);

      expect(result.statistics).toEqual(statistics);
      expect(result.loadingStatistics).toBe(false);
    });
  });

  describe('setAuditFilter', () => {
    it('should update current query', () => {
      const query = {
        eventTypes: [AuditEventType.LOGIN_FAILED],
        eventCategory: 'SECURITY',
        success: false
      };
      const action = AuditActions.setAuditFilter({ query });
      const result = auditReducer(initialState, action);

      expect(result.currentQuery).toEqual(query);
    });

    it('should preserve other state properties', () => {
      const stateWithData: AuditState = {
        ...initialState,
        events: [{
          id: '1',
          eventType: AuditEventType.USER_CREATED,
          eventCategory: 'USER',
          userId: 'user-1',
          username: 'test',
          targetEntityType: 'USER',
          targetEntityId: 'entity-1',
          targetEntityName: 'Test',
          description: 'Test',
          ipAddress: '127.0.0.1',
          userAgent: 'test',
          success: true,
          errorMessage: undefined,
          metadata: {},
          timestamp: '2024-01-01T10:00:00',
          eventDate: '2024-01-01',
          eventHour: 10
        }],
        totalElements: 1
      };
      const action = AuditActions.setAuditFilter({ query: {} });
      const result = auditReducer(stateWithData, action);

      expect(result.events).toEqual(stateWithData.events);
      expect(result.totalElements).toBe(1);
    });
  });

  describe('clearAuditFilter', () => {
    it('should reset query and page', () => {
      const filteredState: AuditState = {
        ...initialState,
        currentQuery: { eventTypes: [AuditEventType.USER_CREATED] },
        currentPage: 2
      };
      const action = AuditActions.clearAuditFilter();
      const result = auditReducer(filteredState, action);

      expect(result.currentQuery).toEqual({});
      expect(result.currentPage).toBe(0);
    });
  });

  describe('clearAuditError', () => {
    it('should clear error message', () => {
      const errorState: AuditState = {
        ...initialState,
        error: 'Some error occurred'
      };
      const action = AuditActions.clearAuditError();
      const result = auditReducer(errorState, action);

      expect(result.error).toBeNull();
    });
  });

  describe('State Immutability', () => {
    it('should not mutate original state', () => {
      const originalState = { ...initialState };
      const action = AuditActions.loadAuditEvents({ query: {}, page: 0, size: 50 });
      const result = auditReducer(originalState, action);

      expect(result).not.toBe(originalState);
      expect(originalState.loading).toBe(false); // Original unchanged
      expect(result.loading).toBe(true);
    });

    it('should create new object references for nested properties', () => {
      const events: AuditEvent[] = [{
        id: '1',
        eventType: AuditEventType.USER_CREATED,
        eventCategory: 'USER',
        userId: 'user-1',
        username: 'test',
        targetEntityType: 'USER',
        targetEntityId: 'entity-1',
        targetEntityName: 'Test',
        description: 'Test',
        ipAddress: '127.0.0.1',
        userAgent: 'test',
        success: true,
        errorMessage: undefined,
        metadata: {},
        timestamp: '2024-01-01T10:00:00',
        eventDate: '2024-01-01',
        eventHour: 10
      }];
      const action = AuditActions.loadAuditEventsSuccess({
        events,
        totalElements: 1,
        totalPages: 1
      });
      const result = auditReducer(initialState, action);

      expect(result.events).not.toBe(initialState.events);
      expect(result.currentQuery).not.toBe(initialState.currentQuery);
    });
  });

  describe('Complex State Transitions', () => {
    it('should handle multiple state transitions correctly', () => {
      let state = initialState;

      // Start loading
      state = auditReducer(state, AuditActions.loadAuditEvents({ query: {}, page: 0, size: 50 }));
      expect(state.loading).toBe(true);

      // Load success
      const events: AuditEvent[] = [{
        id: '1',
        eventType: AuditEventType.USER_CREATED,
        eventCategory: 'USER',
        userId: 'user-1',
        username: 'test',
        targetEntityType: 'USER',
        targetEntityId: 'entity-1',
        targetEntityName: 'Test',
        description: 'Test',
        ipAddress: '127.0.0.1',
        userAgent: 'test',
        success: true,
        errorMessage: undefined,
        metadata: {},
        timestamp: '2024-01-01T10:00:00',
        eventDate: '2024-01-01',
        eventHour: 10
      }];
      state = auditReducer(state, AuditActions.loadAuditEventsSuccess({
        events,
        totalElements: 1,
        totalPages: 1
      }));
      expect(state.loading).toBe(false);
      expect(state.events.length).toBe(1);

      // Load statistics
      state = auditReducer(state, AuditActions.loadAuditStatistics());
      expect(state.loadingStatistics).toBe(true);
      expect(state.events.length).toBe(1); // Events preserved

      // Statistics success
      const statistics: AuditStatistics = {
        totalEvents: 1,
        successfulEvents: 1,
        failedEvents: 0,
        eventsByType: {},
        eventsByCategory: {},
        topUsers: {},
        topTargetEntities: {}
      };
      state = auditReducer(state, AuditActions.loadAuditStatisticsSuccess({ statistics }));
      expect(state.loadingStatistics).toBe(false);
      expect(state.statistics).toEqual(statistics);
      expect(state.events.length).toBe(1); // Events still preserved
    });
  });
});
