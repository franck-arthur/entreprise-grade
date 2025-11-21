import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, interval } from 'rxjs';
import { switchMap, startWith, tap } from 'rxjs/operators';
import { OrderView, OrderStatus } from '../models/order.model';

/**
 * SERVICE QUERY - Lit les projections (READ SIDE).
 *
 * Ce service gère toutes les opérations de lecture via les vues matérialisées.
 * Les données lues sont optimisées pour l'affichage (dénormalisées).
 */
@Injectable({
  providedIn: 'root'
})
export class OrderQueryService {

  private readonly apiUrl = '/api/orders';

  // Cache local avec BehaviorSubject pour les mises à jour réactives
  private ordersCache$ = new BehaviorSubject<OrderView[]>([]);

  constructor(private http: HttpClient) {}

  /**
   * Récupérer toutes les commandes.
   */
  getAllOrders(): Observable<OrderView[]> {
    return this.http.get<OrderView[]>(this.apiUrl);
  }

  /**
   * Récupérer une commande par ID.
   */
  getOrderById(orderId: string): Observable<OrderView> {
    return this.http.get<OrderView>(`${this.apiUrl}/${orderId}`);
  }

  /**
   * Récupérer les commandes d'un client.
   */
  getOrdersByCustomer(customerId: string): Observable<OrderView[]> {
    return this.http.get<OrderView[]>(`${this.apiUrl}/customer/${customerId}`);
  }

  /**
   * Récupérer les commandes par statut.
   */
  getOrdersByStatus(status: OrderStatus): Observable<OrderView[]> {
    return this.http.get<OrderView[]>(`${this.apiUrl}/status/${status}`);
  }

  /**
   * Polling automatique pour les mises à jour (éventuelle cohérence).
   * En production, préférer WebSockets ou SSE.
   */
  pollOrders(intervalMs: number = 2000): Observable<OrderView[]> {
    return interval(intervalMs).pipe(
      startWith(0),
      switchMap(() => this.getAllOrders()),
      tap(orders => this.ordersCache$.next(orders))
    );
  }

  /**
   * Accès au cache local.
   */
  get cachedOrders$(): Observable<OrderView[]> {
    return this.ordersCache$.asObservable();
  }
}
