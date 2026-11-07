import { openDB, type DBSchema, type IDBPDatabase } from 'idb';
import type { Product, Warehouse, SyncQueueItem, SyncConflict } from '../types';

interface IwmsDbSchema extends DBSchema {
  outbox: {
    key: number;
    value: SyncQueueItem;
    indexes: { 'by-status': string; 'by-syncId': string };
  };
  cached_products: {
    key: string;
    value: Product;
  };
  cached_warehouses: {
    key: string;
    value: Warehouse;
  };
  conflicts: {
    key: string;
    value: SyncConflict;
  };
}

let dbPromise: Promise<IDBPDatabase<IwmsDbSchema>> | null = null;

export function getDb(): Promise<IDBPDatabase<IwmsDbSchema>> {
  if (!dbPromise) {
    dbPromise = openDB<IwmsDbSchema>('iwms_offline_db', 1, {
      upgrade(db) {
        // Outbox queue for offline movements
        const outboxStore = db.createObjectStore('outbox', {
          keyPath: 'id',
          autoIncrement: true,
        });
        outboxStore.createIndex('by-status', 'status');
        outboxStore.createIndex('by-syncId', 'syncId', { unique: true });

        // Offline cached products
        db.createObjectStore('cached_products', { keyPath: 'id' });

        // Offline cached warehouse topology
        db.createObjectStore('cached_warehouses', { keyPath: 'id' });

        // Offline recorded conflicts
        db.createObjectStore('conflicts', { keyPath: 'syncId' });
      },
    });
  }
  return dbPromise;
}
