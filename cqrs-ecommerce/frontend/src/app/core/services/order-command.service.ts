import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateOrderCommand,
  AddItemToOrderCommand,
  ConfirmOrderCommand,
  CommandResponse
} from '../models/order.model';

/**
 * SERVICE COMMAND - Envoie les commandes (WRITE SIDE).
 *
 * Ce service gère toutes les opérations d'écriture via des commandes.
 * Les commandes sont des intentions de modifier l'état du système.
 */
@Injectable({
  providedIn: 'root'
})
export class OrderCommandService {

  private readonly apiUrl = '/api/orders';

  constructor(private http: HttpClient) {}

  /**
   * Créer une nouvelle commande.
   * Envoie CreateOrderCommand -> OrderCreatedEvent
   */
  createOrder(command: CreateOrderCommand): Observable<CommandResponse> {
    return this.http.post<CommandResponse>(this.apiUrl, command);
  }

  /**
   * Ajouter un article à une commande.
   * Envoie AddItemToOrderCommand -> ItemAddedToOrderEvent
   */
  addItem(orderId: string, command: AddItemToOrderCommand): Observable<CommandResponse> {
    return this.http.post<CommandResponse>(
      `${this.apiUrl}/${orderId}/items`,
      command
    );
  }

  /**
   * Supprimer un article d'une commande.
   * Envoie RemoveItemFromOrderCommand -> ItemRemovedFromOrderEvent
   */
  removeItem(orderId: string, productId: string): Observable<CommandResponse> {
    return this.http.delete<CommandResponse>(
      `${this.apiUrl}/${orderId}/items/${productId}`
    );
  }

  /**
   * Confirmer une commande.
   * Envoie ConfirmOrderCommand -> OrderConfirmedEvent
   */
  confirmOrder(orderId: string, command: ConfirmOrderCommand): Observable<CommandResponse> {
    return this.http.post<CommandResponse>(
      `${this.apiUrl}/${orderId}/confirm`,
      command
    );
  }
}
