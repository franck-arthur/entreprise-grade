import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuditService } from './audit.service';
import {
  AuditEvent,
  AuditEventPage,
  AuditEventQuery,
  AuditEventType,
  AuditEventCategory,
  AuditStatistics
} from '../models/audit.model';
import { environment } from '../../../environments/environment';

describe('AuditService', () => {
  let service: AuditService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.apiUrl}/audit`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuditService]
    });
    service = TestBed.inject(AuditService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getAuditEvents', () => {
    it('should retrieve audit events with filters', () => {
      const mockQuery: AuditEventQuery = {
        eventTypes: [AuditEventType.USER_CREATED, AuditEventType.LOGIN_SUCCESS],
        eventCategory: 'USER',
        username: 'testuser',
        success: true
      };
      const mockResponse: AuditEventPage = {
        content: [
          {
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
            metadata: {},
            timestamp: '2024-01-01T10:00:00',
            eventDate: '2024-01-01',
            eventHour: 10
          }
        ],
        totalElements: 1,
        totalPages: 1,
        size: 50,
        number: 0
      };

      service.getAuditEvents(mockQuery, 0, 50).subscribe(response => {
        expect(response).toEqual(mockResponse);
        expect(response.content.length).toBe(1);
        expect(response.content[0].eventType).toBe(AuditEventType.USER_CREATED);
      });

      const req = httpMock.expectOne(request => {
        return request.url === apiUrl &&
               request.params.has('eventTypes') &&
               request.params.get('eventCategory') === 'USER' &&
               request.params.get('username') === 'testuser' &&
               request.params.get('success') === 'true';
      });
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should handle empty query', () => {
      const mockQuery: AuditEventQuery = {};
      const mockResponse: AuditEventPage = {
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 50,
        number: 0
      };

      service.getAuditEvents(mockQuery).subscribe(response => {
        expect(response).toEqual(mockResponse);
        expect(response.content.length).toBe(0);
      });

      const req = httpMock.expectOne(request => request.url === apiUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should handle multiple event types', () => {
      const mockQuery: AuditEventQuery = {
        eventTypes: [
          AuditEventType.USER_CREATED,
          AuditEventType.USER_UPDATED,
          AuditEventType.USER_DELETED
        ]
      };

      service.getAuditEvents(mockQuery).subscribe();

      const req = httpMock.expectOne(request => {
        const eventTypesParams = request.params.getAll('eventTypes');
        return request.url === apiUrl && eventTypesParams?.length === 3;
      });
      expect(req.request.method).toBe('GET');
      req.flush({ content: [], totalElements: 0, totalPages: 0, size: 50, number: 0 });
    });
  });

  describe('getAuditEventsByUser', () => {
    it('should retrieve audit events for a specific user', () => {
      const userId = 'user-123';
      const mockResponse: AuditEventPage = {
        content: [],
        totalElements: 5,
        totalPages: 1,
        size: 50,
        number: 0
      };

      service.getAuditEventsByUser(userId, 0, 50).subscribe(response => {
        expect(response).toEqual(mockResponse);
        expect(response.totalElements).toBe(5);
      });

      const req = httpMock.expectOne(request =>
        request.url === `${apiUrl}/user/${userId}`
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('50');
      req.flush(mockResponse);
    });
  });

  describe('getAuditEventsByType', () => {
    it('should retrieve audit events by type', () => {
      const eventType = AuditEventType.LOGIN_SUCCESS;
      const mockResponse: AuditEventPage = {
        content: [],
        totalElements: 10,
        totalPages: 1,
        size: 50,
        number: 0
      };

      service.getAuditEventsByType(eventType).subscribe(response => {
        expect(response.totalElements).toBe(10);
      });

      const req = httpMock.expectOne(`${apiUrl}/type/${eventType}?page=0&size=50&sort=timestamp%2Cdesc`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('getAuditEventsByEntity', () => {
    it('should retrieve audit events for a specific entity', () => {
      const entityType = 'USER';
      const entityId = 'entity-456';
      const mockResponse: AuditEventPage = {
        content: [],
        totalElements: 3,
        totalPages: 1,
        size: 50,
        number: 0
      };

      service.getAuditEventsByEntity(entityType, entityId).subscribe(response => {
        expect(response.totalElements).toBe(3);
      });

      const req = httpMock.expectOne(request =>
        request.url === `${apiUrl}/entity/${entityType}/${entityId}`
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('getStatistics', () => {
    it('should retrieve audit statistics', () => {
      const mockStatistics: AuditStatistics = {
        totalEvents: 1000,
        successfulEvents: 950,
        failedEvents: 50,
        eventsByType: {
          [AuditEventType.USER_CREATED]: 100,
          [AuditEventType.LOGIN_SUCCESS]: 500
        },
        eventsByCategory: {
          ['USER']: 200,
          ['AUTH']: 600
        },
        topUsers: {
          'user1': 50,
          'user2': 30
        },
        topTargetEntities: {
          'USER': 150,
          'BATCH': 80
        }
      };

      service.getStatistics().subscribe(stats => {
        expect(stats).toEqual(mockStatistics);
        expect(stats.totalEvents).toBe(1000);
        expect(stats.successfulEvents).toBe(950);
        expect(stats.failedEvents).toBe(50);
      });

      const req = httpMock.expectOne(`${apiUrl}/statistics`);
      expect(req.request.method).toBe('GET');
      req.flush(mockStatistics);
    });
  });

  describe('getStatisticsForDateRange', () => {
    it('should retrieve statistics for a date range', () => {
      const fromDate = '2024-01-01';
      const toDate = '2024-01-31';
      const mockStatistics: AuditStatistics = {
        totalEvents: 500,
        successfulEvents: 480,
        failedEvents: 20,
        eventsByType: {},
        eventsByCategory: {},
        topUsers: {},
        topTargetEntities: {}
      };

      service.getStatisticsForDateRange(fromDate, toDate).subscribe(stats => {
        expect(stats.totalEvents).toBe(500);
      });

      const req = httpMock.expectOne(request =>
        request.url === `${apiUrl}/statistics/range` &&
        request.params.get('fromDate') === fromDate &&
        request.params.get('toDate') === toDate
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockStatistics);
    });
  });

  describe('getHourlyStatistics', () => {
    it('should retrieve hourly statistics for a date', () => {
      const date = '2024-01-15';
      const mockHourlyStats: Record<string, number> = {
        '0': 10,
        '1': 5,
        '2': 3,
        '8': 50,
        '9': 100,
        '10': 120
      };

      service.getHourlyStatistics(date).subscribe(stats => {
        expect(stats).toEqual(mockHourlyStats);
        expect(stats['9']).toBe(100);
      });

      const req = httpMock.expectOne(request =>
        request.url === `${apiUrl}/statistics/hourly` &&
        request.params.get('date') === date
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockHourlyStats);
    });
  });

  describe('Error Handling', () => {
    it('should handle HTTP errors', () => {
      const mockQuery: AuditEventQuery = {};

      service.getAuditEvents(mockQuery).subscribe(
        () => fail('should have failed'),
        error => {
          expect(error.status).toBe(500);
          expect(error.error).toBe('Server error');
        }
      );

      const req = httpMock.expectOne(request => request.url === apiUrl);
      req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    });
  });
});
