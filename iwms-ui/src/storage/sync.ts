import { getDb } from './db';
import { api } from '../services/api';
import type { SyncQueueItem, Product, Warehouse, SyncConflict } from '../types';

export class OfflineSyncManager {
  private static isSyncing = false;

  public static init() {
    window.addEventListener('online', () => {
      console.log('[OfflineSyncManager] Network online detected. Triggering sync...');
      this.drainQueue();
    });
  }

  // Cache products for offline browsing
  public static async cacheProducts(products: Product[]): Promise<void> {
    const db = await getDb();
    const tx = db.transaction('cached_products', 'readwrite');
    for (const p of products) {
      await tx.store.put(p);
    }
    await tx.done;
  }

  public static async getCachedProducts(): Promise<Product[]> {
    const db = await getDb();
    const products = await db.getAll('cached_products');
    if (products.length > 0) return products;

    // Default demonstration seed for standalone Vercel preview
    const demoSeed: Product[] = [
      { id: 'prod-001', sku: 'PROD-PERISH-001', name: 'Organic Fresh Milk 1L', description: 'Grade A Pasteurized Milk', category: 'PERISHABLE', quantity: 45, reorderPoint: 20, costPrice: 1.80, sellingPrice: 3.50, binLocation: 'BIN-B1-01', productType: 'PERISHABLE', status: 'OPTIMAL' },
      { id: 'prod-002', sku: 'PROD-PERISH-002', name: 'Fresh Strawberries 500g', description: 'Hydroponic strawberries', category: 'PERISHABLE', quantity: 12, reorderPoint: 15, costPrice: 2.50, sellingPrice: 5.00, binLocation: 'BIN-B1-02', productType: 'PERISHABLE', status: 'LOW_STOCK' },
      { id: 'prod-003', sku: 'PROD-ELEC-001', name: 'Wireless Ergonomic Mouse', description: 'Bluetooth 5.0 rechargeable', category: 'ELECTRONICS', quantity: 85, reorderPoint: 25, costPrice: 18.00, sellingPrice: 39.99, binLocation: 'BIN-C1-01', productType: 'ELECTRONIC', status: 'OPTIMAL' },
      { id: 'prod-004', sku: 'PROD-ELEC-002', name: 'Mechanical Keyboard RGB', description: 'Hot-swappable tactile switches', category: 'ELECTRONICS', quantity: 8, reorderPoint: 20, costPrice: 42.00, sellingPrice: 89.99, binLocation: 'BIN-C1-02', productType: 'ELECTRONIC', status: 'CRITICAL' },
      { id: 'prod-005', sku: 'PROD-FRAG-001', name: 'Ceramic Dinnerware Set 16pc', description: 'Porcelain dinner set', category: 'FRAGILE', quantity: 34, reorderPoint: 10, costPrice: 28.00, sellingPrice: 65.00, binLocation: 'BIN-A1-01', productType: 'FRAGILE', status: 'OPTIMAL' },
      { id: 'prod-006', sku: 'PROD-BULK-001', name: 'Industrial Steel Fasteners 10kg', description: 'Galvanized zinc hex bolts', category: 'BULK', quantity: 150, reorderPoint: 50, costPrice: 12.00, sellingPrice: 28.50, binLocation: 'BIN-A1-02', productType: 'BULK', status: 'OPTIMAL' },
    ];
    await this.cacheProducts(demoSeed);
    return demoSeed;
  }

  // Cache warehouses for offline topology
  public static async cacheWarehouses(warehouses: Warehouse[]): Promise<void> {
    const db = await getDb();
    const tx = db.transaction('cached_warehouses', 'readwrite');
    for (const w of warehouses) {
      await tx.store.put(w);
    }
    await tx.done;
  }

  public static async getCachedWarehouses(): Promise<Warehouse[]> {
    const db = await getDb();
    const warehouses = await db.getAll('cached_warehouses');
    if (warehouses.length > 0) return warehouses;

    const demoWarehouse: Warehouse = {
      id: 'WH-MAIN',
      name: 'Central Logistics Fulfillment Hub',
      location: '100 Industrial Parkway',
      zones: [
        {
          id: 'ZONE-A',
          name: 'Ambient Storage Zone',
          categoryPreference: 'GENERAL / BULK',
          bins: [
            { id: 'BIN-A1-01', code: 'BIN-A1-01', capacityQuantity: 200, maxWeightKg: 500, currentQuantity: 34, currentWeightKg: 80, productIds: ['prod-005'] },
            { id: 'BIN-A1-02', code: 'BIN-A1-02', capacityQuantity: 200, maxWeightKg: 1000, currentQuantity: 150, currentWeightKg: 750, productIds: ['prod-006'] },
          ],
        },
        {
          id: 'ZONE-B',
          name: 'Cold Storage Refrigeration Zone',
          categoryPreference: 'PERISHABLE',
          bins: [
            { id: 'BIN-B1-01', code: 'BIN-B1-01', capacityQuantity: 100, maxWeightKg: 250, currentQuantity: 45, currentWeightKg: 45, productIds: ['prod-001'] },
            { id: 'BIN-B1-02', code: 'BIN-B1-02', capacityQuantity: 100, maxWeightKg: 250, currentQuantity: 12, currentWeightKg: 10, productIds: ['prod-002'] },
          ],
        },
        {
          id: 'ZONE-C',
          name: 'High-Security Electronics Vault',
          categoryPreference: 'ELECTRONICS',
          bins: [
            { id: 'BIN-C1-01', code: 'BIN-C1-01', capacityQuantity: 150, maxWeightKg: 300, currentQuantity: 85, currentWeightKg: 40, productIds: ['prod-003'] },
            { id: 'BIN-C1-02', code: 'BIN-C1-02', capacityQuantity: 150, maxWeightKg: 300, currentQuantity: 8, currentWeightKg: 12, productIds: ['prod-004'] },
          ],
        },
      ],
    };
    await this.cacheWarehouses([demoWarehouse]);
    return [demoWarehouse];
  }

  // Enqueue an offline movement
  public static async queueMovement(item: Omit<SyncQueueItem, 'status' | 'syncId'>): Promise<SyncQueueItem> {
    const db = await getDb();
    const syncId = 'sync_' + Date.now() + '_' + Math.random().toString(36).substring(2, 8);
    const queueItem: SyncQueueItem = {
      ...item,
      syncId,
      status: 'PENDING',
    };

    const id = await db.add('outbox', queueItem);
    queueItem.id = id;

    // Optimistically adjust cached product stock
    const cached = await db.get('cached_products', item.productId);
    if (cached) {
      if (item.movementType === 'IN') {
        cached.quantity += item.quantity;
      } else if (item.movementType === 'OUT') {
        cached.quantity = Math.max(0, cached.quantity - item.quantity);
      }
      await db.put('cached_products', cached);
    }

    window.dispatchEvent(new CustomEvent('iwms:outbox-updated'));

    // Attempt immediate drain if online
    if (navigator.onLine) {
      this.drainQueue();
    }

    return queueItem;
  }

  // Drain outbox queue to server
  public static async drainQueue(): Promise<{ processed: number; conflicts: number }> {
    if (this.isSyncing) return { processed: 0, conflicts: 0 };
    if (!navigator.onLine) return { processed: 0, conflicts: 0 };

    this.isSyncing = true;
    try {
      const db = await getDb();
      const pending = await db.getAllFromIndex('outbox', 'by-status', 'PENDING');

      if (pending.length === 0) {
        return { processed: 0, conflicts: 0 };
      }

      console.log(`[OfflineSyncManager] Flushing ${pending.length} queued offline actions...`);
      const result = await api.syncBatch(pending);

      // Remove successfully processed items from outbox
      const tx = db.transaction('outbox', 'readwrite');
      for (const item of pending) {
        if (item.id !== undefined) {
          await tx.store.delete(item.id);
        }
      }
      await tx.done;

      // Update conflicts in db if any
      const serverConflicts = await api.getConflicts().catch(() => []);
      if (serverConflicts.length > 0) {
        const cTx = db.transaction('conflicts', 'readwrite');
        for (const c of serverConflicts) {
          await cTx.store.put(c);
        }
        await cTx.done;
      }

      window.dispatchEvent(new CustomEvent('iwms:outbox-updated'));
      window.dispatchEvent(new CustomEvent('iwms:sync-complete', { detail: result }));

      return { processed: result.processed, conflicts: result.conflicts };
    } catch (err) {
      console.warn('[OfflineSyncManager] Sync batch failed, items remain queued in outbox:', err);
      return { processed: 0, conflicts: 0 };
    } finally {
      this.isSyncing = false;
    }
  }

  public static async getPendingCount(): Promise<number> {
    const db = await getDb();
    const pending = await db.getAllFromIndex('outbox', 'by-status', 'PENDING');
    return pending.length;
  }

  public static async getPendingItems(): Promise<SyncQueueItem[]> {
    const db = await getDb();
    return db.getAllFromIndex('outbox', 'by-status', 'PENDING');
  }

  public static async getConflicts(): Promise<SyncConflict[]> {
    const db = await getDb();
    return db.getAll('conflicts');
  }

  public static async clearConflict(syncId: string): Promise<void> {
    const db = await getDb();
    await db.delete('conflicts', syncId);
    window.dispatchEvent(new CustomEvent('iwms:outbox-updated'));
  }
}
