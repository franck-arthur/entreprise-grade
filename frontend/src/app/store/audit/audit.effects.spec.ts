import { TestBed } from '@angular/core/testing';
import { provideMockActions } from '@ngrx/effects/testing';
import { Observable, of, throwError } from 'rxjs';
import { AuditEffects } from './audit.effects';
import { AuditService } from '../../core/services/audit.service';
import * as AuditActions from './audit.actions';
import {
  AuditEvent,
  AuditEventPage,
  AuditEventType,
  AuditEventCategory,
  AuditStatistics
} from '../../core/models/audit.model';

describe('AuditEffects', () => {
  let actions$: Observable<any>;
  let effects: AuditEffects;
  let auditService: jest.Mocked<AuditService>;

  const mockEvent: AuditEvent = {
    id: '1',
    eventType: AuditEventType.USER_CREATED,
    eventCategory: AuditEventCategory.USER,
    userId: 'user-1',
    username: 'testuser',
    targetEntityType: 'USER',
    targetEntityId: 'entity-1',
    targetEntityName: 'Test User',
    description: 'User created',
    ipAddress: '192.168.1.1',
    userAgent: 'Mozilla/5.0',
    success: true,
    errorMessage: null,
    metadata: {},
    timestamp: '2024-01-01T10:00:00',
    eventDate: '2024-01-01',
    eventHour: 10
  };

  const mockPage: AuditEventPage = {
    content: [mockEvent],
    totalElements: 1,
    totalPages: 1,
    size: 50,
    number: 0
  };

  beforeEach(() => {
    const auditServiceMock = {
      getAuditEvents: jest.fn(),
      getAuditEventsByUser: jest.fn(),
      getAuditEventsByType: jest.fn(),
      getAuditEventsByEntity: jest.fn(),
      getStatistics: jest.fn(),
      getStatisticsForDateRange: jest.fn()
    };

    TestBed.configureTestingModule({
      providers: [
        AuditEffects,
        provideMockActions(() => actions$),
        { provide: AuditService, useValue: auditServiceMock }
      ]
    });

    effects = TestBed.inject(AuditEffects);
    auditService = TestBed.inject(AuditService) as jest.Mocked<AuditService>;
  });

  describe('loadAuditEvents$', () => {
    it('should return loadAuditEventsSuccess on success', (done) => {
      const query = { eventTypes: [AuditEventType.USER_CREATED] };
      const action = AuditActions.loadAuditEvents({ query, page: 0, size: 50 });
      const outcome = AuditActions.loadAuditEventsSuccess({
        events: mockPage.content,
        totalElements: mockPage.totalElements,
        totalPages: mockPage.totalPages
      });

      auditService.getAuditEvents.mockReturnValue(of(mockPage));
      actions$ = of(action);

      effects.loadAuditEvents$.subscribe(result => {
        expect(result).toEqual(outcome);
        expect(auditService.getAuditEvents).toHaveBeenCalledWith(query, 0, 50);
        done();
      });
    });

    it('should return loadAuditEventsFailure on error', (done) => {
      const query = {};
      const action = AuditActions.loadAuditEvents({ query, page: 0, size: 50 });
      const error = { error: { message: 'Server error' } };
      const outcome = AuditActions.loadAuditEventsFailure({ error: 'Server error' });

      auditService.getAuditEvents.mockReturnValue(throwError(() => error));
      actions$ = of(action);

      effects.loadAuditEvents$.subscribe(result => {
        expect(result).toEqual(outcome);
        done();
      });
    });

    it('should use default error message when error.error.message is missing', (done) => {
      const query = {};
      const action = AuditActions.loadAuditEvents({ query, page: 0, size: 50 });
      const error = {};
      const outcome = AuditActions.loadAuditEventsFailure({ error: 'Failed to load audit events' });

      auditService.getAuditEvents.mockReturnValue(throwError(() => error));
      actions$ = of(action);

      effects.loadAuditEvents$.subscribe(result => {
        expect(result).toEqual(outcome);
        done();
      });
    });
  });

  describe('loadAuditEventsByUser$', () => {
    it('should return loadAuditEventsByUserSuccess on success', (done) => {
      const action = AuditActions.loadAuditEventsByUser({ userId: 'user-1', page: 0, size: 50 });
      const outcome = AuditActions.loadAuditEventsByUserSuccess({
        events: mockPage.content,
        totalElements: mockPage.totalElements,
        totalPages: mockPage.totalPages
      });

      auditService.getAuditEventsByUser.mockReturnValue(of(mockPage));
      actions$ = of(action);

      effects.loadAuditEventsByUser$.subscribe(result => {
        expect(result).toEqual(outcome);
        expect(auditService.getAuditEventsByUser).toHaveBeenCalledWith('user-1', 0, 50);
        done();
      });
    });

    it('should return loadAuditEventsByUserFailure on error', (done) => {
      const action = AuditActions.loadAuditEventsByUser({ userId: 'user-1', page: 0, size: 50 });
      const error = { error: { message: 'User not found' } };
      const outcome = AuditActions.loadAuditEventsByUserFailure({ error: 'User not found' });

      auditService.getAuditEventsByUser.mockReturnValue(throwError(() => error));
      actions$ = of(action);

      effects.loadAuditEventsByUser$.subscribe(result => {
        expect(result).toEqual(outcome);
        done();
      });
    });
  });

  describe('loadAuditEventsByType$', () => {
    it('should return loadAuditEventsByTypeSuccess on success', (done) => {
      const action = AuditActions.loadAuditEventsByType({
        eventType: AuditEventType.LOGIN_SUCCESS,
        page: 0,
        size: 50
      });
      const outcome = AuditActions.loadAuditEventsByTypeSuccess({
        events: mockPage.content,
        totalElements: mockPage.totalElements,
        totalPages: mockPage.totalPages
      });

      auditService.getAuditEventsByType.mockReturnValue(of(mockPage));
      actions$ = of(action);

      effects.loadAuditEventsByType$.subscribe(result => {
        expect(result).toEqual(outcome);
        expect(auditService.getAuditEventsByType).toHaveBeenCalledWith(
          AuditEventType.LOGIN_SUCCESS,
          0,
          50
        );
        done();
      });
    });
  });

  describe('loadAuditEventsByEntity$', () => {
    it('should return loadAuditEventsByEntitySuccess on success', (done) => {
      const action = AuditActions.loadAuditEventsByEntity({
        entityType: 'USER',
        entityId: 'entity-1',
        page: 0,
        size: 50
      });
      const outcome = AuditActions.loadAuditEventsByEntitySuccess({
        events: mockPage.content,
        totalElements: mockPage.totalElements,
        totalPages: mockPage.totalPages
      });

      auditService.getAuditEventsByEntity.mockReturnValue(of(mockPage));
      actions$ = of(action);

      effects.loadAuditEventsByEntity$.subscribe(result => {
        expect(result).toEqual(outcome);
        expect(auditService.getAuditEventsByEntity).toHaveBeenCalledWith('USER', 'entity-1', 0, 50);
        done();
      });
    });
  });

  describe('loadAuditStatistics$', () => {
    it('should return loadAuditStatisticsSuccess on success', (done) => {
      const mockStatistics: AuditStatistics = {
        totalEvents: 1000,
        successfulEvents: 950,
        failedEvents: 50,
        eventsByType: {
          [AuditEventType.USER_CREATED]: 100
        },
        eventsByCategory: {
          [AuditEventCategory.USER]: 300
        },
        topUsers: { 'user1': 50 },
        topEntities: { 'USER': 200 }
      };

      const action = AuditActions.loadAuditStatistics();
      const outcome = AuditActions.loadAuditStatisticsSuccess({ statistics: mockStatistics });

      auditService.getStatistics.mockReturnValue(of(mockStatistics));
      actions$ = of(action);

      effects.loadAuditStatistics$.subscribe(result => {
        expect(result).toEqual(outcome);
        expect(auditService.getStatistics).toHaveBeenCalled();
        done();
      });
    });

    it('should return loadAuditStatisticsFailure on error', (done) => {
      const action = AuditActions.loadAuditStatistics();
      const error = { error: { message: 'Failed to load statistics' } };
      const outcome = AuditActions.loadAuditStatisticsFailure({ error: 'Failed to load statistics' });

      auditService.getStatistics.mockReturnValue(throwError(() => error));
      actions$ = of(action);

      effects.loadAuditStatistics$.subscribe(result => {
        expect(result).toEqual(outcome);
        done();
      });
    });
  });

  describe('loadAuditStatisticsForDateRange$', () => {
    it('should return loadAuditStatisticsForDateRangeSuccess on success', (done) => {
      const mockStatistics: AuditStatistics = {
        totalEvents: 500,
        successfulEvents: 480,
        failedEvents: 20,
        eventsByType: {},
        eventsByCategory: {},
        topUsers: {},
        topEntities: {}
      };

      const action = AuditActions.loadAuditStatisticsForDateRange({
        fromDate: '2024-01-01',
        toDate: '2024-01-31'
      });
      const outcome = AuditActions.loadAuditStatisticsForDateRangeSuccess({
        statistics: mockStatistics
      });

      auditService.getStatisticsForDateRange.mockReturnValue(of(mockStatistics));
      actions$ = of(action);

      effects.loadAuditStatisticsForDateRange$.subscribe(result => {
        expect(result).toEqual(outcome);
        expect(auditService.getStatisticsForDateRange).toHaveBeenCalledWith('2024-01-01', '2024-01-31');
        done();
      });
    });
  });
});
