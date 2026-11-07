export type UserRole = 'ADMIN' | 'MANAGER' | 'EMPLOYEE';

export interface User {
  id: string;
  username: string;
  role: UserRole;
  active: boolean;
  failedAttempts?: number;
  lockedUntil?: string | null;
}

export type ProductStatus = 'OPTIMAL' | 'LOW_STOCK' | 'CRITICAL' | 'OVERSTOCKED';

export interface Product {
  id: string;
  sku: string;
  name: string;
  description: string;
  category: string;
  quantity: number;
  reorderPoint: number;
  costPrice?: number; // Only present for ADMIN/MANAGER, undefined/null for EMPLOYEE
  sellingPrice: number;
  binLocation: string;
  productType: 'PERISHABLE' | 'ELECTRONIC' | 'FRAGILE' | 'BULK' | 'STANDARD';
  expiryDate?: string;
  warrantyMonths?: number;
  fragile?: boolean;
  unitOfMeasure?: string;
  status: ProductStatus;
}

export type MovementType = 'IN' | 'OUT' | 'TRANSFER' | 'ADJUST';

export interface StockMovement {
  movementId: string;
  syncId?: string;
  productId: string;
  movementType: MovementType;
  quantity: number;
  sourceBin?: string;
  targetBin?: string;
  timestamp: string;
  operatorId: string;
  reason?: string;
  unitPrice?: number;
}

export interface Bin {
  id: string;
  code: string;
  capacityQuantity: number;
  maxWeightKg: number;
  currentQuantity: number;
  currentWeightKg: number;
  productIds: string[];
}

export interface Zone {
  id: string;
  name: string;
  categoryPreference: string;
  bins: Bin[];
}

export interface Warehouse {
  id: string;
  name: string;
  location: string;
  zones: Zone[];
}

export interface PurchaseOrder {
  orderId: string;
  supplierId: string;
  orderDate: string;
  expectedDeliveryDate: string;
  status: 'DRAFT' | 'ORDERED' | 'RECEIVED' | 'CANCELLED';
  items: Record<string, number>;
}

export interface SyncQueueItem {
  id?: number;
  syncId: string;
  productId: string;
  movementType: MovementType;
  quantity: number;
  sourceBin?: string;
  targetBin?: string;
  operatorId: string;
  reason?: string;
  timestamp: string;
  status: 'PENDING' | 'SYNCED' | 'CONFLICT';
  errorMessage?: string;
}

export interface SyncConflict {
  syncId: string;
  productId: string;
  message: string;
  deficitQuantity?: number;
  timestamp: string;
}
