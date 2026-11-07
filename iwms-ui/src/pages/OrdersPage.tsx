import React, { useState } from 'react';
import type { PurchaseOrder, Product } from '../types';
import { api } from '../services/api';
import { GlassCard } from '../components/common/GlassCard';
import {
  ShoppingCart,
  Plus,
  Truck,
  CheckCircle,
  Calendar,
  X,
  FileCheck,
} from 'lucide-react';

interface OrdersPageProps {
  products: Product[];
  onRefreshCatalog: () => void;
}

export const OrdersPage: React.FC<OrdersPageProps> = ({ products, onRefreshCatalog }) => {
  // Demo initial orders
  const [orders, setOrders] = useState<PurchaseOrder[]>([
    {
      orderId: 'PO-2026-001',
      supplierId: 'Global Tech Distribution LLC',
      orderDate: '2026-11-01',
      expectedDeliveryDate: '2026-11-10',
      status: 'ORDERED',
      items: {
        'prod-001': 50,
        'prod-002': 30,
      },
    },
    {
      orderId: 'PO-2026-002',
      supplierId: 'Fresh Harvest Farms Co.',
      orderDate: '2026-11-05',
      expectedDeliveryDate: '2026-11-08',
      status: 'RECEIVED',
      items: {
        'prod-003': 100,
      },
    },
    {
      orderId: 'PO-2026-003',
      supplierId: 'Apex Industrial Supply',
      orderDate: '2026-11-07',
      expectedDeliveryDate: '2026-11-14',
      status: 'ORDERED',
      items: {
        'prod-004': 75,
        'prod-005': 25,
      },
    },
  ]);

  const [isNewOrderModalOpen, setIsNewOrderModalOpen] = useState(false);
  const [supplierName, setSupplierName] = useState('Apex Industrial Supply');
  const [deliveryDate, setDeliveryDate] = useState('2026-11-20');
  const [selectedProductId, setSelectedProductId] = useState(products[0]?.id || '');
  const [orderQuantity, setOrderQuantity] = useState(50);
  const [receivingId, setReceivingId] = useState<string | null>(null);

  const handleCreateOrder = (e: React.FormEvent) => {
    e.preventDefault();
    const newPO: PurchaseOrder = {
      orderId: `PO-2026-${String(orders.length + 1).padStart(3, '0')}`,
      supplierId: supplierName,
      orderDate: new Date().toISOString().slice(0, 10),
      expectedDeliveryDate: deliveryDate,
      status: 'ORDERED',
      items: {
        [selectedProductId]: orderQuantity,
      },
    };

    setOrders([newPO, ...orders]);
    setIsNewOrderModalOpen(false);
  };

  const handleReceiveOrder = async (po: PurchaseOrder) => {
    setReceivingId(po.orderId);
    try {
      // Execute stock in for each item in the PO
      for (const [prodId, qty] of Object.entries(po.items)) {
        await api.stockIn({
          productId: prodId,
          quantity: qty,
          reason: `Received Purchase Order ${po.orderId}`,
        }).catch(() => null);
      }

      // Update PO status
      setOrders(orders.map((o) => (o.orderId === po.orderId ? { ...o, status: 'RECEIVED' } : o)));
      onRefreshCatalog();
    } finally {
      setReceivingId(null);
    }
  };

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight text-slate-900 dark:text-white flex items-center gap-3">
            <ShoppingCart className="w-8 h-8 text-indigo-500" />
            <span>Purchase Order Management</span>
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Supplier procurement, order dispatch tracking, and 1-click receiving with automated inventory stock-in.
          </p>
        </div>

        <button
          onClick={() => setIsNewOrderModalOpen(true)}
          className="glass-button-primary text-sm py-2.5 px-4"
        >
          <Plus className="w-4 h-4" />
          <span>Create Purchase Order</span>
        </button>
      </div>

      {/* Orders List */}
      <div className="space-y-4">
        {orders.map((po) => {
          const totalItemsCount = Object.values(po.items).reduce((a, b) => a + b, 0);

          return (
            <GlassCard key={po.orderId} hoverEffect className="p-6">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div className="space-y-1.5">
                  <div className="flex items-center gap-3">
                    <span className="font-mono font-extrabold text-base text-slate-900 dark:text-white">
                      Order #{po.orderId}
                    </span>
                    <span
                      className={`px-3 py-1 rounded-full text-xs font-bold border ${
                        po.status === 'RECEIVED'
                          ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/30'
                          : 'bg-blue-500/10 text-blue-600 dark:text-blue-400 border-blue-500/30'
                      }`}
                    >
                      {po.status === 'RECEIVED' ? 'Received & Stored' : 'Pending Delivery'}
                    </span>
                  </div>

                  <div className="flex items-center gap-3 text-sm text-slate-500 dark:text-slate-400">
                    <span className="flex items-center gap-1.5 font-semibold text-slate-700 dark:text-slate-300">
                      <Truck className="w-4 h-4 text-indigo-500" />
                      Supplier: {po.supplierId}
                    </span>
                    <span>&bull;</span>
                    <span className="flex items-center gap-1.5 font-mono text-xs">
                      <Calendar className="w-4 h-4" />
                      Order Date: {po.orderDate}
                    </span>
                    <span>&bull;</span>
                    <span className="font-mono text-xs">
                      Expected Arrival: {po.expectedDeliveryDate}
                    </span>
                  </div>
                </div>

                <div className="flex items-center justify-between md:justify-end gap-5 border-t md:border-t-0 pt-3 md:pt-0 border-slate-200/50 dark:border-slate-800">
                  <div className="text-right">
                    <div className="text-base font-extrabold text-slate-900 dark:text-white font-mono">
                      {totalItemsCount} units
                    </div>
                    <div className="text-xs text-slate-400 font-medium">
                      {Object.keys(po.items).length} product items
                    </div>
                  </div>

                  {po.status === 'ORDERED' && (
                    <button
                      onClick={() => handleReceiveOrder(po)}
                      disabled={receivingId === po.orderId}
                      className="glass-button-primary text-sm py-2 px-4 bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-600 hover:to-teal-700"
                    >
                      {receivingId === po.orderId ? (
                        <span className="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                      ) : (
                        <>
                          <CheckCircle className="w-4 h-4" />
                          <span>Receive at Loading Dock</span>
                        </>
                      )}
                    </button>
                  )}

                  {po.status === 'RECEIVED' && (
                    <div className="flex items-center gap-1.5 text-sm text-emerald-600 dark:text-emerald-400 font-bold px-3 py-1.5 rounded-xl bg-emerald-500/10 border border-emerald-500/20">
                      <FileCheck className="w-4 h-4" />
                      <span>Stock In Complete</span>
                    </div>
                  )}
                </div>
              </div>
            </GlassCard>
          );
        })}
      </div>

      {/* New Order Modal */}
      {isNewOrderModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="w-full max-w-lg glass-panel p-6 shadow-2xl ring-1 ring-white/20 relative">
            <button
              onClick={() => setIsNewOrderModalOpen(false)}
              className="absolute top-5 right-5 p-2 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-white/40 dark:hover:bg-slate-800/40"
            >
              <X className="w-5 h-5" />
            </button>

            <h3 className="text-xl font-bold text-slate-900 dark:text-white mb-5">
              Create New Purchase Order
            </h3>

            <form onSubmit={handleCreateOrder} className="space-y-4">
              <div>
                <label className="block text-sm font-semibold text-slate-700 dark:text-slate-200 mb-1.5">
                  Supplier Name / Company
                </label>
                <input
                  type="text"
                  required
                  value={supplierName}
                  onChange={(e) => setSupplierName(e.target.value)}
                  className="glass-input text-sm"
                  placeholder="e.g. Apex Global Distributors"
                />
              </div>

              <div>
                <label className="block text-sm font-semibold text-slate-700 dark:text-slate-200 mb-1.5">
                  Select Product Item
                </label>
                <select
                  value={selectedProductId}
                  onChange={(e) => setSelectedProductId(e.target.value)}
                  className="glass-input text-sm"
                >
                  {products.map((p) => (
                    <option key={p.id} value={p.id} className="bg-slate-900 text-white">
                      [Code: {p.sku}] {p.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1.5">
                    Order Quantity
                  </label>
                  <input
                    type="number"
                    min="1"
                    required
                    value={orderQuantity}
                    onChange={(e) => setOrderQuantity(parseInt(e.target.value) || 1)}
                    className="glass-input text-xs font-mono"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1.5">
                    Expected Delivery
                  </label>
                  <input
                    type="date"
                    required
                    value={deliveryDate}
                    onChange={(e) => setDeliveryDate(e.target.value)}
                    className="glass-input text-xs font-mono"
                  />
                </div>
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-200/60 dark:border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsNewOrderModalOpen(false)}
                  className="glass-button-secondary text-sm"
                >
                  Cancel
                </button>
                <button type="submit" className="glass-button-primary text-sm">
                  Create Purchase Order
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
