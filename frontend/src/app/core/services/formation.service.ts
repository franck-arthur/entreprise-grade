import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@environments/environment';
import {
  Formation,
  CreateFormationRequest,
  UpdateFormationRequest,
  FormationParticipation,
  FormationFilters,
  PagedResponse,
  ModaliteFormation,
  FormationStatut,
} from '@app/core/models/formation.model';

/**
 * Formation API service.
 *
 * Handles all HTTP requests related to formation management.
 */
@Injectable({
  providedIn: 'root',
})
export class FormationService {
  private readonly apiUrl = `${environment.apiUrl}/formations`;

  constructor(private http: HttpClient) {}

  /**
   * Get all formations with pagination and filters.
   */
  getFormations(
    page: number = 0,
    size: number = 20,
    filters?: FormationFilters
  ): Observable<PagedResponse<Formation>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (filters?.secteurId) {
      params = params.set('secteurId', filters.secteurId.toString());
    }
    if (filters?.regionId) {
      params = params.set('regionId', filters.regionId.toString());
    }
    if (filters?.modalite) {
      params = params.set('modalite', filters.modalite);
    }
    if (filters?.statut) {
      params = params.set('statut', filters.statut);
    }

    return this.http.get<PagedResponse<Formation>>(this.apiUrl, { params });
  }

  /**
   * Get formation by ID.
   */
  getFormationById(id: number): Observable<Formation> {
    return this.http.get<Formation>(`${this.apiUrl}/${id}`);
  }

  /**
   * Create new formation.
   */
  createFormation(request: CreateFormationRequest): Observable<Formation> {
    return this.http.post<Formation>(this.apiUrl, request);
  }

  /**
   * Update formation.
   */
  updateFormation(id: number, request: UpdateFormationRequest): Observable<Formation> {
    return this.http.put<Formation>(`${this.apiUrl}/${id}`, request);
  }

  /**
   * Delete formation.
   */
  deleteFormation(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Get formation participants.
   */
  getFormationParticipants(id: number): Observable<FormationParticipation[]> {
    return this.http.get<FormationParticipation[]>(`${this.apiUrl}/${id}/participants`);
  }

  /**
   * Get all available modalites.
   */
  getModalites(): ModaliteFormation[] {
    return Object.values(ModaliteFormation);
  }

  /**
   * Get all available statuts.
   */
  getStatuts(): FormationStatut[] {
    return Object.values(FormationStatut);
  }
}