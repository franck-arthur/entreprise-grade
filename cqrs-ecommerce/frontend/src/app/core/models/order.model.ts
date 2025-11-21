// ==================== READ MODELS (Projections) ====================

export interface OrderView {
  orderId: string;
  customerId: string;
  customerEmail: string;
  status: OrderStatus;
  items: OrderItemView[];
  totalAmount: number;
  shippingAddress?: string;
  paymentMethod?: string;
  createdAt: string;
  confirmedAt?: string;
}

export interface OrderItemView {
  id: number;
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export enum OrderStatus {
  CREATED = 'CREATED',
  CONFIRMED = 'CONFIRMED',
  SHIPPED = 'SHIPPED',
  DELIVERED = 'DELIVERED',
  CANCELLED = 'CANCELLED'
}

// ==================== COMMAND MODELS ====================

export interface CreateOrderCommand {
  customerId: string;
  customerEmail: string;
}

export interface AddItemToOrderCommand {
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
}

export interface RemoveItemFromOrderCommand {
  productId: string;
}

export interface ConfirmOrderCommand {
  shippingAddress: string;
  paymentMethod: string;
}

// ==================== API RESPONSES ====================

export interface CommandResponse {
  id: string;
  message: string;
}
