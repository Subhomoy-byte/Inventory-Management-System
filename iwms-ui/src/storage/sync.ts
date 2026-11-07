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
    return db.getAll('cached_products');
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
    return db.getAll('cached_warehouses');
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
