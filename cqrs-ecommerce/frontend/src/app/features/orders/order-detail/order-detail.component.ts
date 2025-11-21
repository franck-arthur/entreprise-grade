import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { OrderQueryService } from '../../../core/services/order-query.service';
import { OrderCommandService } from '../../../core/services/order-command.service';
import { OrderView, OrderStatus, AddItemToOrderCommand, ConfirmOrderCommand } from '../../../core/models/order.model';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="order-detail" *ngIf="order">
      <header>
        <h2>Commande #{{ order.orderId | slice:0:8 }}</h2>
        <span class="status" [class]="'status-' + order.status.toLowerCase()">
          {{ order.status }}
        </span>
      </header>

      <section class="info">
        <div><strong>Client:</strong> {{ order.customerEmail }}</div>
        <div><strong>Créée le:</strong> {{ order.createdAt | date:'medium' }}</div>
        <div *ngIf="order.confirmedAt">
          <strong>Confirmée le:</strong> {{ order.confirmedAt | date:'medium' }}
        </div>
      </section>

      <!-- Articles -->
      <section class="items">
        <h3>Articles</h3>
        <table>
          <thead>
            <tr>
              <th>Produit</th>
              <th>Qté</th>
              <th>Prix unitaire</th>
              <th>Total</th>
              <th *ngIf="order.status === OrderStatus.CREATED">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let item of order.items">
              <td>{{ item.productName }}</td>
              <td>{{ item.quantity }}</td>
              <td>{{ item.unitPrice | currency:'EUR' }}</td>
              <td>{{ item.lineTotal | currency:'EUR' }}</td>
              <td *ngIf="order.status === OrderStatus.CREATED">
                <button (click)="removeItem(item.productId)" class="btn-remove">
                  Supprimer
                </button>
              </td>
            </tr>
          </tbody>
          <tfoot>
            <tr>
              <td colspan="3"><strong>Total</strong></td>
              <td><strong>{{ order.totalAmount | currency:'EUR' }}</strong></td>
              <td *ngIf="order.status === OrderStatus.CREATED"></td>
            </tr>
          </tfoot>
        </table>
      </section>

      <!-- Ajouter un article (si non confirmée) -->
      <section class="add-item" *ngIf="order.status === OrderStatus.CREATED">
        <h3>Ajouter un article</h3>
        <form (ngSubmit)="addItem()">
          <input [(ngModel)]="newItem.productId" name="productId" placeholder="ID Produit" required>
          <input [(ngModel)]="newItem.productName" name="productName" placeholder="Nom" required>
          <input [(ngModel)]="newItem.quantity" name="quantity" type="number" placeholder="Qté" min="1" required>
          <input [(ngModel)]="newItem.unitPrice" name="unitPrice" type="number" placeholder="Prix" step="0.01" required>
          <button type="submit">Ajouter</button>
        </form>
      </section>

      <!-- Confirmer la commande -->
      <section class="confirm" *ngIf="order.status === OrderStatus.CREATED && order.items.length > 0">
        <h3>Confirmer la commande</h3>
        <form (ngSubmit)="confirmOrder()">
          <textarea [(ngModel)]="confirmData.shippingAddress" name="address"
                    placeholder="Adresse de livraison" required></textarea>
          <select [(ngModel)]="confirmData.paymentMethod" name="payment" required>
            <option value="">Mode de paiement</option>
            <option value="CARD">Carte bancaire</option>
            <option value="PAYPAL">PayPal</option>
            <option value="TRANSFER">Virement</option>
          </select>
          <button type="submit" class="btn-confirm">Confirmer la commande</button>
        </form>
      </section>
    </div>
  `,
  styles: [`
    .order-detail { padding: 20px; max-width: 800px; margin: 0 auto; }
    header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
    .status { padding: 8px 16px; border-radius: 4px; }
    .status-created { background: #ffc107; }
    .status-confirmed { background: #28a745; color: white; }
    .info { background: #f8f9fa; padding: 16px; border-radius: 8px; margin-bottom: 20px; }
    table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
    th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }
    .btn-remove { background: #dc3545; color: white; border: none; padding: 4px 8px; cursor: pointer; }
    .add-item form, .confirm form { display: flex; flex-wrap: wrap; gap: 10px; }
    input, textarea, select { padding: 8px; border: 1px solid #ddd; border-radius: 4px; }
    textarea { width: 100%; min-height: 80px; }
    button { padding: 10px 20px; background: #007bff; color: white; border: none; cursor: pointer; border-radius: 4px; }
    .btn-confirm { background: #28a745; width: 100%; margin-top: 10px; }
  `]
})
export class OrderDetailComponent implements OnInit {
  order: OrderView | null = null;
  OrderStatus = OrderStatus;

  newItem: AddItemToOrderCommand = {
    productId: '',
    productName: '',
    quantity: 1,
    unitPrice: 0
  };

  confirmData: ConfirmOrderCommand = {
    shippingAddress: '',
    paymentMethod: ''
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private queryService: OrderQueryService,
    private commandService: OrderCommandService
  ) {}

  ngOnInit(): void {
    const orderId = this.route.snapshot.paramMap.get('orderId');
    if (orderId) {
      this.loadOrder(orderId);
    }
  }

  loadOrder(orderId: string): void {
    this.queryService.getOrderById(orderId).subscribe(order => {
      this.order = order;
    });
  }

  addItem(): void {
    if (!this.order) return;

    this.commandService.addItem(this.order.orderId, this.newItem).subscribe({
      next: () => {
        this.loadOrder(this.order!.orderId);
        this.newItem = { productId: '', productName: '', quantity: 1, unitPrice: 0 };
      },
      error: (err) => alert('Erreur: ' + err.message)
    });
  }

  removeItem(productId: string): void {
    if (!this.order) return;

    this.commandService.removeItem(this.order.orderId, productId).subscribe({
      next: () => this.loadOrder(this.order!.orderId),
      error: (err) => alert('Erreur: ' + err.message)
    });
  }

  confirmOrder(): void {
    if (!this.order) return;

    this.commandService.confirmOrder(this.order.orderId, this.confirmData).subscribe({
      next: () => {
        this.loadOrder(this.order!.orderId);
        alert('Commande confirmée !');
      },
      error: (err) => alert('Erreur: ' + err.message)
    });
  }
}
