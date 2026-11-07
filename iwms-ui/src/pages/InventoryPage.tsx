import React, { useState, useEffect } from 'react';
import type { Product } from '../types';
import { api } from '../services/api';
import { OfflineSyncManager } from '../storage/sync';
import { useAuth } from '../context/AuthContext';
import { GlassCard } from '../components/common/GlassCard';
import { StatusChip } from '../components/common/StatusChip';
import {
  Search,
  ArrowDownRight,
  ArrowUpRight,
  ArrowLeftRight,
  FileSpreadsheet,
  ChevronLeft,
  ChevronRight,
  Boxes,
  Lock,
} from 'lucide-react';

interface InventoryPageProps {
  onOpenMovementModal: (type: 'IN' | 'OUT' | 'TRANSFER', product?: Product) => void;
}

export const InventoryPage: React.FC<InventoryPageProps> = ({ onOpenMovementModal }) => {
  const { isAdmin, isManager } = useAuth();
  const [products, setProducts] = useState<Product[]>([]);
  const [search, setSearch] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 12;

  const loadProducts = async () => {
    try {
      if (navigator.onLine) {
        const data = await api.getProducts().catch(() => OfflineSyncManager.getCachedProducts());
        setProducts(data);
        await OfflineSyncManager.cacheProducts(data);
      } else {
        const cached = await OfflineSyncManager.getCachedProducts();
        setProducts(cached);
      }
    } catch {
      const cached = await OfflineSyncManager.getCachedProducts();
      setProducts(cached);
    }
  };

  useEffect(() => {
    loadProducts();
    const handleUpdate = () => loadProducts();
    window.addEventListener('iwms:outbox-updated', handleUpdate);
    window.addEventListener('iwms:sync-complete', handleUpdate);
    return () => {
      window.removeEventListener('iwms:outbox-updated', handleUpdate);
      window.removeEventListener('iwms:sync-complete', handleUpdate);
    };
  }, []);

  // Filter products
  const filteredProducts = products.filter((p) => {
    const matchesSearch =
      search === '' ||
      p.sku.toLowerCase().includes(search.toLowerCase()) ||
      p.name.toLowerCase().includes(search.toLowerCase()) ||
      p.description.toLowerCase().includes(search.toLowerCase());

    const matchesCategory = categoryFilter === 'ALL' || p.category === categoryFilter;
    const matchesStatus = statusFilter === 'ALL' || p.status === statusFilter;
    const matchesType = typeFilter === 'ALL' || p.productType === typeFilter;

    return matchesSearch && matchesCategory && matchesStatus && matchesType;
  });

  // Unique categories for filter dropdown
  const categories = Array.from(new Set(products.map((p) => p.category))).filter(Boolean);

  // Pagination
  const totalPages = Math.ceil(filteredProducts.length / pageSize) || 1;
  const paginatedProducts = filteredProducts.slice(
    (currentPage - 1) * pageSize,
    currentPage * pageSize
  );

  const handleExportCsv = async () => {
    try {
      const csvData = await api.getCsvCatalog();
      const blob = new Blob([csvData], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.setAttribute('href', url);
      link.setAttribute('download', `iwms_catalog_${new Date().toISOString().slice(0, 10)}.csv`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    } catch (err: any) {
      alert('CSV Export Error: ' + err.message);
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
            Inventory Catalog
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            Real-time stock levels, multi-category taxonomy, and role-enforced price masking.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={handleExportCsv}
            className="glass-button-secondary text-xs py-2 px-3"
            title="Export CSV"
          >
            <FileSpreadsheet className="w-3.5 h-3.5 text-emerald-500" />
            <span>Export CSV</span>
          </button>
          <button
            onClick={() => onOpenMovementModal('IN')}
            className="glass-button-primary text-xs py-2 px-3"
          >
            <ArrowDownRight className="w-3.5 h-3.5" />
            <span>New Movement</span>
          </button>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <GlassCard className="p-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          {/* Search Box */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
            <input
              type="text"
              placeholder="Search by SKU, product name..."
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setCurrentPage(1);
              }}
              className="glass-input pl-9 text-xs"
            />
          </div>

          {/* Category Filter */}
          <div className="relative">
            <select
              value={categoryFilter}
              onChange={(e) => {
                setCategoryFilter(e.target.value);
                setCurrentPage(1);
              }}
              className="glass-input text-xs"
            >
              <option value="ALL" className="bg-slate-900 text-white">All Categories</option>
              {categories.map((c) => (
                <option key={c} value={c} className="bg-slate-900 text-white">
                  {c}
                </option>
              ))}
            </select>
          </div>

          {/* Status Filter */}
          <div className="relative">
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setCurrentPage(1);
              }}
              className="glass-input text-xs"
            >
              <option value="ALL" className="bg-slate-900 text-white">All Stock Statuses</option>
              <option value="OPTIMAL" className="bg-slate-900 text-white">Optimal</option>
              <option value="LOW_STOCK" className="bg-slate-900 text-white">Low Stock</option>
              <option value="CRITICAL" className="bg-slate-900 text-white">Critical</option>
              <option value="OVERSTOCKED" className="bg-slate-900 text-white">Overstocked</option>
            </select>
          </div>

          {/* Type Filter */}
          <div className="relative">
            <select
              value={typeFilter}
              onChange={(e) => {
                setTypeFilter(e.target.value);
                setCurrentPage(1);
              }}
              className="glass-input text-xs"
            >
              <option value="ALL" className="bg-slate-900 text-white">All Product Types</option>
              <option value="PERISHABLE" className="bg-slate-900 text-white">Perishable</option>
              <option value="ELECTRONIC" className="bg-slate-900 text-white">Electronic</option>
              <option value="FRAGILE" className="bg-slate-900 text-white">Fragile</option>
              <option value="BULK" className="bg-slate-900 text-white">Bulk</option>
              <option value="STANDARD" className="bg-slate-900 text-white">Standard</option>
            </select>
          </div>
        </div>
      </GlassCard>

      {/* Products Glass Table */}
      <GlassCard className="p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-slate-200/60 dark:border-slate-800 text-slate-400 font-semibold uppercase tracking-wider bg-slate-500/5">
                <th className="py-3 px-4">SKU / Product</th>
                <th className="py-3 px-4">Category & Type</th>
                <th className="py-3 px-4">On Hand / Reorder</th>
                <th className="py-3 px-4">Bin Location</th>
                <th className="py-3 px-4">
                  {isAdmin || isManager ? 'Cost / Sell Price' : 'Sell Price'}
                </th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200/40 dark:divide-slate-800/60">
              {paginatedProducts.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-slate-400">
                    <Boxes className="w-8 h-8 mx-auto mb-2 opacity-50" />
                    <span>No products found matching the active filters.</span>
                  </td>
                </tr>
              ) : (
                paginatedProducts.map((p) => {
                  const percent = Math.min(100, Math.round((p.quantity / (p.reorderPoint * 2 || 50)) * 100));

                  return (
                    <tr
                      key={p.id}
                      className="hover:bg-white/40 dark:hover:bg-slate-800/40 transition-colors"
                    >
                      {/* SKU / Name */}
                      <td className="py-3.5 px-4">
                        <div className="font-bold text-slate-900 dark:text-white">
                          {p.name}
                        </div>
                        <div className="text-[11px] font-mono text-indigo-600 dark:text-indigo-400">
                          {p.sku}
                        </div>
                      </td>

                      {/* Category & Type */}
                      <td className="py-3.5 px-4">
                        <div className="font-medium text-slate-700 dark:text-slate-300">
                          {p.category}
                        </div>
                        <span className="inline-block text-[10px] px-1.5 py-0.2 rounded bg-slate-200/60 dark:bg-slate-800/60 text-slate-500 dark:text-slate-400 font-mono mt-0.5">
                          {p.productType}
                        </span>
                      </td>

                      {/* Quantity & Level Bar */}
                      <td className="py-3.5 px-4">
                        <div className="flex items-center gap-2 mb-1">
                          <span className="font-mono font-bold text-slate-900 dark:text-white">
                            {p.quantity}
                          </span>
                          <span className="text-slate-400 text-[10px]">/ min {p.reorderPoint}</span>
                        </div>
                        <div className="w-24 h-1.5 rounded-full bg-slate-200 dark:bg-slate-800 overflow-hidden">
                          <div
                            className={`h-full rounded-full ${
                              p.quantity <= p.reorderPoint
                                ? 'bg-rose-500'
                                : p.quantity <= p.reorderPoint * 1.3
                                ? 'bg-amber-500'
                                : 'bg-emerald-500'
                            }`}
                            style={{ width: `${percent}%` }}
                          />
                        </div>
                      </td>

                      {/* Bin Location */}
                      <td className="py-3.5 px-4 font-mono font-semibold text-slate-600 dark:text-slate-300">
                        {p.binLocation}
                      </td>

                      {/* Price / RBAC Masking */}
                      <td className="py-3.5 px-4 font-mono">
                        <div>
                          <span className="font-bold text-slate-900 dark:text-white">
                            ${p.sellingPrice.toFixed(2)}
                          </span>
                        </div>
                        {isAdmin || isManager ? (
                          <div className="text-[10px] text-slate-500 dark:text-slate-400">
                            Cost: ${p.costPrice !== undefined ? p.costPrice.toFixed(2) : 'N/A'}
                          </div>
                        ) : (
                          <div className="text-[10px] text-slate-400 flex items-center gap-1">
                            <Lock className="w-2.5 h-2.5" />
                            <span>Cost Masked</span>
                          </div>
                        )}
                      </td>

                      {/* Status */}
                      <td className="py-3.5 px-4">
                        <StatusChip status={p.status} />
                      </td>

                      {/* Actions */}
                      <td className="py-3.5 px-4 text-right">
                        <div className="flex items-center justify-end gap-1">
                          <button
                            onClick={() => onOpenMovementModal('IN', p)}
                            className="p-1.5 rounded-lg bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 transition-all"
                            title="Stock In"
                          >
                            <ArrowDownRight className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => onOpenMovementModal('OUT', p)}
                            className="p-1.5 rounded-lg bg-rose-500/10 hover:bg-rose-500/20 text-rose-600 dark:text-rose-400 transition-all"
                            title="Stock Out"
                          >
                            <ArrowUpRight className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => onOpenMovementModal('TRANSFER', p)}
                            className="p-1.5 rounded-lg bg-indigo-500/10 hover:bg-indigo-500/20 text-indigo-600 dark:text-indigo-400 transition-all"
                            title="Transfer Bin"
                          >
                            <ArrowLeftRight className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Footer */}
        <div className="p-4 border-t border-slate-200/60 dark:border-slate-800 flex items-center justify-between text-xs text-slate-500 dark:text-slate-400">
          <div>
            Showing <span className="font-semibold text-slate-800 dark:text-slate-200">
              {filteredProducts.length === 0 ? 0 : (currentPage - 1) * pageSize + 1}
            </span> to <span className="font-semibold text-slate-800 dark:text-slate-200">
              {Math.min(currentPage * pageSize, filteredProducts.length)}
            </span> of <span className="font-semibold text-slate-800 dark:text-slate-200">
              {filteredProducts.length}
            </span> products
          </div>

          <div className="flex items-center gap-2">
            <button
              disabled={currentPage <= 1}
              onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
              className="p-1.5 rounded-lg glass-card disabled:opacity-40 disabled:cursor-not-allowed hover:bg-white/80 dark:hover:bg-slate-800"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <span className="font-mono text-xs">
              {currentPage} / {totalPages}
            </span>
            <button
              disabled={currentPage >= totalPages}
              onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
              className="p-1.5 rounded-lg glass-card disabled:opacity-40 disabled:cursor-not-allowed hover:bg-white/80 dark:hover:bg-slate-800"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </GlassCard>
    </div>
  );
};
