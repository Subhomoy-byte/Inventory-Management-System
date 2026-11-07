import type { Product, Warehouse, StockMovement, SyncQueueItem, SyncConflict } from '../types';

const API_BASE = '/api';

function getAuthHeader(): Record<string, string> {
  const token = localStorage.getItem('iwms_token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    if (res.status === 401) {
      localStorage.removeItem('iwms_token');
      localStorage.removeItem('iwms_user');
      window.dispatchEvent(new Event('iwms:unauthorized'));
      throw new Error('Session expired or unauthorized');
    }
    const errText = await res.text().catch(() => '');
    throw new Error(errText || `API error: ${res.status} ${res.statusText}`);
  }
  return res.json();
}

export const api = {
  // Products
  async getProducts(): Promise<Product[]> {
    const res = await fetch(`${API_BASE}/products`, {
      headers: { ...getAuthHeader() },
    });
    return handleResponse<Product[]>(res);
  },

  async getProduct(id: string): Promise<Product> {
    const res = await fetch(`${API_BASE}/products/${id}`, {
      headers: { ...getAuthHeader() },
    });
    return handleResponse<Product>(res);
  },

  async searchProducts(query: string): Promise<Product[]> {
    const res = await fetch(`${API_BASE}/products/search?query=${encodeURIComponent(query)}`, {
      headers: { ...getAuthHeader() },
    });
    return handleResponse<Product[]>(res);
  },

  // Warehouses
  async getWarehouses(): Promise<Warehouse[]> {
    const res = await fetch(`${API_BASE}/warehouses`, {
      headers: { ...getAuthHeader() },
    });
    return handleResponse<Warehouse[]>(res);
  },

  // Stock operations
  async stockIn(data: { productId: string; quantity: number; unitPrice?: number; targetBin?: string; reason?: string }): Promise<StockMovement> {
    const res = await fetch(`${API_BASE}/stock/in`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeader(),
      },
      body: JSON.stringify(data),
    });
    return handleResponse<StockMovement>(res);
  },

  async stockOut(data: { productId: string; quantity: number; sourceBin?: string; reason?: string }): Promise<StockMovement> {
    const res = await fetch(`${API_BASE}/stock/out`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeader(),
      },
      body: JSON.stringify(data),
    });
    return handleResponse<StockMovement>(res);
  },

  async stockTransfer(data: { productId: string; sourceBin: string; targetBin: string; quantity: number }): Promise<StockMovement> {
    const res = await fetch(`${API_BASE}/stock/transfer`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeader(),
      },
      body: JSON.stringify(data),
    });
    return handleResponse<StockMovement>(res);
  },

  async undoLastMovement(): Promise<StockMovement> {
    const res = await fetch(`${API_BASE}/stock/undo`, {
      method: 'POST',
      headers: { ...getAuthHeader() },
    });
    return handleResponse<StockMovement>(res);
  },

  async getStockMovements(): Promise<StockMovement[]> {
    const res = await fetch(`${API_BASE}/stock/movements`, {
      headers: { ...getAuthHeader() },
    });
    return handleResponse<StockMovement[]>(res);
  },

  async getStockAsOf(timestamp: string): Promise<Record<string, number>> {
    const res = await fetch(`${API_BASE}/stock/asof?timestamp=${encodeURIComponent(timestamp)}`, {
      headers: { ...getAuthHeader() },
    });
    return handleResponse<Record<string, number>>(res);
  },

  // Sync operations
  async syncBatch(items: SyncQueueItem[]): Promise<{ processed: number; duplicatesIgnored: number; conflicts: number }> {
    const res = await fetch(`${API_BASE}/sync/batch`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeader(),
      },
      body: JSON.stringify(items),
    });
    return handleResponse(res);
  },

  async getConflicts(): Promise<SyncConflict[]> {
    const res = await fetch(`${API_BASE}/sync/conflicts`, {
      headers: { ...getAuthHeader() },
    });
    return handleResponse<SyncConflict[]>(res);
  },

  // Reports
  getPdfReportUrl(category?: string): string {
    const token = localStorage.getItem('iwms_token') || '';
    const catParam = category ? `&category=${encodeURIComponent(category)}` : '';
    return `${API_BASE}/reports/summary.pdf?token=${encodeURIComponent(token)}${catParam}`;
  },

  async getCsvCatalog(): Promise<string> {
    const res = await fetch(`${API_BASE}/reports/catalog.csv`, {
      headers: { ...getAuthHeader() },
    });
    if (!res.ok) throw new Error('Failed to download CSV catalog');
    return res.text();
  },

  // Backups
  async createBackup(): Promise<{ message: string; backupPath: string }> {
    const res = await fetch(`${API_BASE}/backups/create`, {
      method: 'POST',
      headers: { ...getAuthHeader() },
    });
    return handleResponse(res);
  },
};
