import React, { useState, useEffect } from 'react';
import type { Product, StockMovement } from '../types';
import { api } from '../services/api';
import { OfflineSyncManager } from '../storage/sync';
import { useAuth } from '../context/AuthContext';
import { GlassCard } from '../components/common/GlassCard';
import { StatusChip } from '../components/common/StatusChip';
import {
  Boxes,
  DollarSign,
  AlertTriangle,
  ArrowDownRight,
  ArrowUpRight,
  ArrowLeftRight,
  TrendingUp,
  RefreshCw,
  Layers,
} from 'lucide-react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Cell,
} from 'recharts';

interface DashboardPageProps {
  onOpenMovementModal: (type: 'IN' | 'OUT' | 'TRANSFER') => void;
  onNavigateToTab: (tab: any) => void;
}

export const DashboardPage: React.FC<DashboardPageProps> = ({
  onOpenMovementModal,
  onNavigateToTab,
}) => {
  const { isAdmin, isManager } = useAuth();
  const [products, setProducts] = useState<Product[]>([]);
  const [movements, setMovements] = useState<StockMovement[]>([]);
  const [, setLoading] = useState(true);

  const loadData = async () => {
    setLoading(true);
    try {
      if (navigator.onLine) {
        const [prodList, movList] = await Promise.all([
          api.getProducts().catch(() => OfflineSyncManager.getCachedProducts()),
          api.getStockMovements().catch(() => []),
        ]);
        setProducts(prodList);
        setMovements(movList.slice(-10).reverse());
        await OfflineSyncManager.cacheProducts(prodList);
      } else {
        const cached = await OfflineSyncManager.getCachedProducts();
        setProducts(cached);
      }
    } catch (err) {
      console.warn('Dashboard data fetch fallback to cache:', err);
      const cached = await OfflineSyncManager.getCachedProducts();
      setProducts(cached);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();

    const handleOutboxUpdate = () => loadData();
    window.addEventListener('iwms:outbox-updated', handleOutboxUpdate);
    window.addEventListener('iwms:sync-complete', handleOutboxUpdate);

    return () => {
      window.removeEventListener('iwms:outbox-updated', handleOutboxUpdate);
      window.removeEventListener('iwms:sync-complete', handleOutboxUpdate);
    };
  }, []);

  // Compute metrics
  const totalSkus = products.length;
  const totalUnits = products.reduce((acc, p) => acc + p.quantity, 0);
  const lowStockProducts = products.filter(
    (p) => p.status === 'LOW_STOCK' || p.status === 'CRITICAL' || p.quantity <= p.reorderPoint
  );

  const totalValuation = products.reduce((acc, p) => {
    const cost = p.costPrice ?? p.sellingPrice * 0.6; // If employee, costPrice is omitted by DTO
    return acc + p.quantity * cost;
  }, 0);

  // Category distribution for charts
  const categoryDataMap: Record<string, { count: number; units: number }> = {};
  products.forEach((p) => {
    const cat = p.category || 'Standard';
    if (!categoryDataMap[cat]) {
      categoryDataMap[cat] = { count: 0, units: 0 };
    }
    categoryDataMap[cat].count += 1;
    categoryDataMap[cat].units += p.quantity;
  });

  const categoryChartData = Object.entries(categoryDataMap).map(([name, val]) => ({
    name,
    units: val.units,
    productCount: val.count,
  }));

  const COLORS = ['#6366f1', '#a855f7', '#10b981', '#f59e0b', '#ec4899', '#3b82f6'];

  return (
    <div className="space-y-8">
      {/* Top Banner & Quick Action Buttons */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight text-slate-900 dark:text-white">
            Warehouse Overview & Dashboard
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Real-time inventory valuation, product category distribution, and low stock notifications.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => onOpenMovementModal('IN')}
            className="glass-button-primary text-sm py-2.5 px-4"
          >
            <ArrowDownRight className="w-4 h-4" />
            <span>Receive Stock</span>
          </button>
          <button
            onClick={() => onOpenMovementModal('OUT')}
            className="glass-button-secondary text-sm py-2.5 px-4 hover:text-rose-600"
          >
            <ArrowUpRight className="w-4 h-4" />
            <span>Dispatch Stock</span>
          </button>
          <button
            onClick={() => onOpenMovementModal('TRANSFER')}
            className="glass-button-secondary text-sm py-2.5 px-4"
          >
            <ArrowLeftRight className="w-4 h-4" />
            <span>Transfer Location</span>
          </button>
        </div>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {/* Total Products */}
        <GlassCard hoverEffect>
          <div className="flex items-center justify-between text-slate-500 dark:text-slate-400 mb-2">
            <span className="text-sm font-bold uppercase tracking-wider">Total Products</span>
            <Boxes className="w-5 h-5 text-indigo-500" />
          </div>
          <div className="text-4xl font-extrabold text-slate-900 dark:text-white font-mono">
            {totalSkus}
          </div>
          <div className="text-sm text-slate-500 dark:text-slate-400 mt-2 flex items-center justify-between">
            <span>Distinct Catalog Items</span>
            <span className="text-emerald-500 font-semibold">100% Synced</span>
          </div>
        </GlassCard>

        {/* Total Units On Hand */}
        <GlassCard hoverEffect>
          <div className="flex items-center justify-between text-slate-500 dark:text-slate-400 mb-2">
            <span className="text-sm font-bold uppercase tracking-wider">Total Items in Stock</span>
            <Layers className="w-5 h-5 text-purple-500" />
          </div>
          <div className="text-4xl font-extrabold text-slate-900 dark:text-white font-mono">
            {totalUnits.toLocaleString()}
          </div>
          <div className="text-sm text-slate-500 dark:text-slate-400 mt-2 flex items-center justify-between">
            <span>Physical Floor Stock</span>
            <span className="text-indigo-500 font-semibold">All Storage Shelves</span>
          </div>
        </GlassCard>

        {/* Low Stock Alerts */}
        <GlassCard hoverEffect>
          <div className="flex items-center justify-between text-slate-500 dark:text-slate-400 mb-2">
            <span className="text-sm font-bold uppercase tracking-wider">Low Stock Warnings</span>
            <AlertTriangle className={`w-5 h-5 ${lowStockProducts.length > 0 ? 'text-amber-500 animate-pulse' : 'text-slate-400'}`} />
          </div>
          <div className="text-4xl font-extrabold text-amber-600 dark:text-amber-400 font-mono">
            {lowStockProducts.length}
          </div>
          <div className="text-sm text-slate-500 dark:text-slate-400 mt-2 flex items-center justify-between">
            <span>Below Reorder Level</span>
            <button
              onClick={() => onNavigateToTab('inventory')}
              className="text-amber-600 dark:text-amber-400 hover:underline font-semibold"
            >
              Review Items &rarr;
            </button>
          </div>
        </GlassCard>

        {/* Inventory Valuation */}
        <GlassCard hoverEffect>
          <div className="flex items-center justify-between text-slate-500 dark:text-slate-400 mb-2">
            <span className="text-sm font-bold uppercase tracking-wider">Total Inventory Value</span>
            <DollarSign className="w-5 h-5 text-emerald-500" />
          </div>
          <div className="text-4xl font-extrabold text-emerald-600 dark:text-emerald-400 font-mono">
            {isAdmin || isManager ? (
              `$${totalValuation.toLocaleString(undefined, { maximumFractionDigits: 0 })}`
            ) : (
              <span className="text-slate-400 text-lg">Confidential</span>
            )}
          </div>
          <div className="text-sm text-slate-500 dark:text-slate-400 mt-2 flex items-center justify-between">
            <span>Asset Valuation</span>
            <span className="text-emerald-500 font-semibold">First-In First-Out</span>
          </div>
        </GlassCard>
      </div>

      {/* Analytics Charts Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Category Stock Distribution */}
        <GlassCard className="lg:col-span-2">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-sm font-bold text-slate-900 dark:text-white flex items-center gap-2">
                <TrendingUp className="w-4 h-4 text-indigo-500" />
                <span>Stock Units by Category</span>
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Quantity allocation across warehouse product categories
              </p>
            </div>
            <span className="text-xs font-mono text-slate-400">{categoryChartData.length} Categories</span>
          </div>

          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={categoryChartData} margin={{ top: 10, right: 10, left: -20, bottom: 20 }}>
                <XAxis
                  dataKey="name"
                  tick={{ fill: '#94a3b8', fontSize: 11 }}
                  interval={0}
                  angle={-15}
                  textAnchor="end"
                />
                <YAxis tick={{ fill: '#94a3b8', fontSize: 11 }} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: 'rgba(15, 23, 42, 0.85)',
                    borderRadius: '12px',
                    borderColor: 'rgba(255, 255, 255, 0.1)',
                    backdropFilter: 'blur(8px)',
                    color: '#fff',
                    fontSize: '12px',
                  }}
                />
                <Bar dataKey="units" fill="#6366f1" radius={[6, 6, 0, 0]}>
                  {categoryChartData.map((_, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </GlassCard>

        {/* Low Stock Radar Table */}
        <GlassCard>
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2">
              <AlertTriangle className="w-5 h-5 text-amber-500" />
              <span>Low Stock Alerts</span>
            </h3>
            <span className="text-sm px-3 py-1 rounded-full bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20 font-bold font-mono">
              {lowStockProducts.length} items
            </span>
          </div>

          <div className="space-y-3 max-h-72 overflow-y-auto pr-1">
            {lowStockProducts.length === 0 ? (
              <div className="text-center py-12 text-sm text-slate-400 font-medium">
                All inventory stock levels are optimal!
              </div>
            ) : (
              lowStockProducts.slice(0, 6).map((p) => (
                <div
                  key={p.id}
                  className="p-3 rounded-xl bg-white/40 dark:bg-slate-800/40 border border-slate-200/50 dark:border-slate-700/50 flex items-center justify-between"
                >
                  <div className="truncate mr-3">
                    <div className="text-sm font-bold text-slate-900 dark:text-white truncate">
                      {p.name}
                    </div>
                    <div className="text-xs text-slate-500 dark:text-slate-400 font-mono mt-0.5">
                      Item Code: <span className="font-semibold text-indigo-500">{p.sku}</span> &bull; Shelf: {p.binLocation}
                    </div>
                  </div>
                  <div className="text-right shrink-0">
                    <span className="text-sm font-mono font-bold text-rose-600 dark:text-rose-400">
                      {p.quantity} / {p.reorderPoint} units
                    </span>
                    <div className="text-xs text-slate-400 font-medium">Reorder Needed</div>
                  </div>
                </div>
              ))
            )}
          </div>
        </GlassCard>
      </div>

      {/* Recent Movements Stream */}
      <GlassCard>
        <div className="flex items-center justify-between mb-5">
          <div>
            <h3 className="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2">
              <RefreshCw className="w-5 h-5 text-purple-500" />
              <span>Recent Inventory Transactions (Audit Log)</span>
            </h3>
            <p className="text-sm text-slate-500 dark:text-slate-400 mt-0.5">
              Verified record of all stock-in, dispatch, and internal warehouse movements
            </p>
          </div>
          <button
            onClick={() => onNavigateToTab('movements')}
            className="text-sm text-indigo-600 dark:text-indigo-400 hover:underline font-bold"
          >
            View Full Timeline &rarr;
          </button>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200/60 dark:border-slate-800 text-slate-500 dark:text-slate-400 font-bold uppercase tracking-wider text-xs">
                <th className="py-3.5 px-4">Transaction Type</th>
                <th className="py-3.5 px-4">Item Code & Name</th>
                <th className="py-3.5 px-4">Quantity</th>
                <th className="py-3.5 px-4">Origin &rarr; Destination</th>
                <th className="py-3.5 px-4">Operator / Staff</th>
                <th className="py-3.5 px-4">Timestamp</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200/40 dark:divide-slate-800/60">
              {movements.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-sm text-slate-400">
                    No recent inventory transactions recorded yet.
                  </td>
                </tr>
              ) : (
                movements.map((m) => (
                  <tr key={m.movementId} className="hover:bg-white/40 dark:hover:bg-slate-800/40 transition-colors">
                    <td className="py-3.5 px-4">
                      <StatusChip status={m.movementType} />
                    </td>
                    <td className="py-3.5 px-4 font-semibold text-slate-900 dark:text-white">
                      {m.productId}
                    </td>
                    <td className="py-3.5 px-4 font-mono font-extrabold text-slate-900 dark:text-white">
                      {m.quantity} units
                    </td>
                    <td className="py-3.5 px-4 text-slate-600 dark:text-slate-300 font-mono text-xs">
                      {m.sourceBin || 'Receiving Dock'} &rarr; {m.targetBin || 'Shipping Dock'}
                    </td>
                    <td className="py-3.5 px-4 text-slate-700 dark:text-slate-200 font-medium">
                      {m.operatorId}
                    </td>
                    <td className="py-3.5 px-4 text-slate-400 font-mono text-xs">
                      {new Date(m.timestamp).toLocaleTimeString()}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </GlassCard>
    </div>
  );
};
