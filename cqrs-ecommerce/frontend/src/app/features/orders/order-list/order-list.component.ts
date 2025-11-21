import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { OrderQueryService } from '../../../core/services/order-query.service';
import { OrderView, OrderStatus } from '../../../core/models/order.model';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="order-list-container">
      <h2>Commandes</h2>

      <div class="filters">
        <button (click)="filterByStatus(null)" [class.active]="!currentFilter">
          Toutes
        </button>
        <button (click)="filterByStatus(OrderStatus.CREATED)"
                [class.active]="currentFilter === OrderStatus.CREATED">
          En cours
        </button>
        <button (click)="filterByStatus(OrderStatus.CONFIRMED)"
                [class.active]="currentFilter === OrderStatus.CONFIRMED">
          Confirmées
        </button>
      </div>

      <div class="orders-grid">
        @for (order of orders; track order.orderId) {
          <div class="order-card" [class]="'status-' + order.status.toLowerCase()">
            <div class="order-header">
              <span class="order-id">{{ order.orderId | slice:0:8 }}...</span>
              <span class="order-status">{{ order.status }}</span>
            </div>

            <div class="order-body">
              <p><strong>Client:</strong> {{ order.customerEmail }}</p>
              <p><strong>Articles:</strong> {{ order.items.length }}</p>
              <p class="total"><strong>Total:</strong> {{ order.totalAmount | currency:'EUR' }}</p>
            </div>

            <div class="order-footer">
              <span class="date">{{ order.createdAt | date:'short' }}</span>
              <a [routerLink]="['/orders', order.orderId]" class="btn-detail">
                Voir détails
              </a>
            </div>
          </div>
        } @empty {
          <p class="no-orders">Aucune commande trouvée</p>
        }
      </div>
    </div>
  `,
  styles: [`
    .order-list-container { padding: 20px; }
    .filters { margin-bottom: 20px; display: flex; gap: 10px; }
    .filters button {
      padding: 8px 16px;
      border: 1px solid #ddd;
      background: white;
      cursor: pointer;
      border-radius: 4px;
    }
    .filters button.active { background: #007bff; color: white; }
    .orders-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 20px;
    }
    .order-card {
      border: 1px solid #ddd;
      border-radius: 8px;
      padding: 16px;
      background: white;
    }
    .order-card.status-created { border-left: 4px solid #ffc107; }
    .order-card.status-confirmed { border-left: 4px solid #28a745; }
    .order-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 12px;
    }
    .order-status {
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 12px;
      background: #e9ecef;
    }
    .total { font-size: 18px; color: #28a745; }
    .order-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 12px;
      padding-top: 12px;
      border-top: 1px solid #eee;
    }
    .btn-detail {
      padding: 6px 12px;
      background: #007bff;
      color: white;
      text-decoration: none;
      border-radius: 4px;
    }
  `]
})
export class OrderListComponent implements OnInit, OnDestroy {
  orders: OrderView[] = [];
  currentFilter: OrderStatus | null = null;
  OrderStatus = OrderStatus;

  private destroy$ = new Subject<void>();

  constructor(private orderQueryService: OrderQueryService) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadOrders(): void {
    const orders$ = this.currentFilter
      ? this.orderQueryService.getOrdersByStatus(this.currentFilter)
      : this.orderQueryService.getAllOrders();

    orders$.pipe(takeUntil(this.destroy$))
      .subscribe(orders => this.orders = orders);
  }

  filterByStatus(status: OrderStatus | null): void {
    this.currentFilter = status;
    this.loadOrders();
  }
}
