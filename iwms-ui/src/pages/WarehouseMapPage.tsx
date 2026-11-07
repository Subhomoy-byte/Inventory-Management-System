import React, { useState, useEffect } from 'react';
import type { Warehouse, Bin, Product } from '../types';
import { api } from '../services/api';
import { OfflineSyncManager } from '../storage/sync';
import { GlassCard } from '../components/common/GlassCard';
import { StatusChip } from '../components/common/StatusChip';
import {
  MapPin,
  Box,
  X,
  ArrowRight,
} from 'lucide-react';

interface WarehouseMapPageProps {
  onOpenMovementModal: (type: 'IN' | 'OUT' | 'TRANSFER', product?: Product) => void;
}

export const WarehouseMapPage: React.FC<WarehouseMapPageProps> = ({ onOpenMovementModal }) => {
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [selectedWarehouseId, setSelectedWarehouseId] = useState<string>('');
  const [selectedBin, setSelectedBin] = useState<Bin | null>(null);

  const loadData = async () => {
    try {
      if (navigator.onLine) {
        const [wList, pList] = await Promise.all([
          api.getWarehouses().catch(() => OfflineSyncManager.getCachedWarehouses()),
          api.getProducts().catch(() => OfflineSyncManager.getCachedProducts()),
        ]);
        setWarehouses(wList);
        setProducts(pList);
        if (wList.length > 0 && !selectedWarehouseId) {
          setSelectedWarehouseId(wList[0].id);
        }
        await OfflineSyncManager.cacheWarehouses(wList);
        await OfflineSyncManager.cacheProducts(pList);
      } else {
        const cachedW = await OfflineSyncManager.getCachedWarehouses();
        const cachedP = await OfflineSyncManager.getCachedProducts();
        setWarehouses(cachedW);
        setProducts(cachedP);
        if (cachedW.length > 0 && !selectedWarehouseId) {
          setSelectedWarehouseId(cachedW[0].id);
        }
      }
    } catch {
      const cachedW = await OfflineSyncManager.getCachedWarehouses();
      const cachedP = await OfflineSyncManager.getCachedProducts();
      setWarehouses(cachedW);
      setProducts(cachedP);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const currentWarehouse = warehouses.find((w) => w.id === selectedWarehouseId) || warehouses[0];

  // Helper to find products stored in a bin
  const getProductsInBin = (binCode: string) => {
    return products.filter((p) => p.binLocation === binCode);
  };

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center gap-2">
            <MapPin className="w-6 h-6 text-indigo-500" />
            <span>2D Warehouse Floor Map</span>
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            Topology hierarchy (Warehouse &rarr; Zones &rarr; Bins) with capacity utilization.
          </p>
        </div>

        {/* Warehouse Selector */}
        {warehouses.length > 1 && (
          <div className="flex items-center gap-2">
            <span className="text-xs text-slate-400 font-medium">Facility:</span>
            <select
              value={selectedWarehouseId}
              onChange={(e) => setSelectedWarehouseId(e.target.value)}
              className="glass-input text-xs py-1.5"
            >
              {warehouses.map((w) => (
                <option key={w.id} value={w.id} className="bg-slate-900 text-white">
                  {w.name} ({w.location})
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      {/* Main Floor Grid */}
      {currentWarehouse ? (
        <div className="space-y-6">
          {currentWarehouse.zones.map((zone) => (
            <GlassCard key={zone.id} className="p-5">
              {/* Zone Header */}
              <div className="flex items-center justify-between pb-3 mb-4 border-b border-slate-200/50 dark:border-slate-800">
                <div className="flex items-center gap-2.5">
                  <div className="w-8 h-8 rounded-xl bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 flex items-center justify-center font-bold text-xs">
                    {zone.name.charAt(0)}
                  </div>
                  <div>
                    <h3 className="text-sm font-bold text-slate-900 dark:text-white">
                      {zone.name}
                    </h3>
                    <p className="text-[11px] text-slate-500 dark:text-slate-400">
                      Category Preference: <span className="font-medium text-indigo-600 dark:text-indigo-400">{zone.categoryPreference}</span>
                    </p>
                  </div>
                </div>

                <span className="text-xs font-mono text-slate-400">
                  {zone.bins.length} Storage Bins
                </span>
              </div>

              {/* Bins Grid */}
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
                {zone.bins.map((bin) => {
                  const binProducts = getProductsInBin(bin.code);
                  const currentUnits = binProducts.reduce((acc, p) => acc + p.quantity, 0);
                  const fillPercent = Math.min(
                    100,
                    Math.round((currentUnits / (bin.capacityQuantity || 100)) * 100)
                  );

                  let fillBg = 'bg-emerald-500';
                  let ringColor = 'hover:ring-emerald-500/50';
                  if (fillPercent > 85) {
                    fillBg = 'bg-rose-500';
                    ringColor = 'hover:ring-rose-500/50';
                  } else if (fillPercent > 60) {
                    fillBg = 'bg-amber-500';
                    ringColor = 'hover:ring-amber-500/50';
                  }

                  const isSelected = selectedBin?.id === bin.id;

                  return (
                    <button
                      key={bin.id}
                      onClick={() => setSelectedBin(bin)}
                      className={`p-3 rounded-xl border text-left transition-all hover:scale-[1.02] hover:shadow-lg hover:ring-2 ${ringColor} ${
                        isSelected
                          ? 'bg-indigo-500/15 border-indigo-500 ring-2 ring-indigo-500/50'
                          : 'bg-white/40 dark:bg-slate-800/40 border-slate-200/60 dark:border-slate-700/60'
                      }`}
                    >
                      <div className="flex items-center justify-between mb-1.5">
                        <span className="font-mono font-bold text-xs text-slate-900 dark:text-white">
                          {bin.code}
                        </span>
                        <span className="text-[10px] font-mono text-slate-400">
                          {fillPercent}%
                        </span>
                      </div>

                      {/* Fill Progress Bar */}
                      <div className="w-full h-1.5 rounded-full bg-slate-200/70 dark:bg-slate-700/70 overflow-hidden mb-2">
                        <div
                          className={`h-full rounded-full transition-all duration-300 ${fillBg}`}
                          style={{ width: `${fillPercent}%` }}
                        />
                      </div>

                      <div className="flex items-center justify-between text-[10px] text-slate-500 dark:text-slate-400">
                        <span className="font-mono">{currentUnits} / {bin.capacityQuantity} u</span>
                        <span>{binProducts.length} skus</span>
                      </div>
                    </button>
                  );
                })}
              </div>
            </GlassCard>
          ))}
        </div>
      ) : (
        <GlassCard className="py-12 text-center text-slate-400">
          Loading warehouse zones...
        </GlassCard>
      )}

      {/* Bin Detail Inspection Modal / Drawer */}
      {selectedBin && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="w-full max-w-lg glass-panel p-6 shadow-2xl ring-1 ring-white/20 relative">
            <button
              onClick={() => setSelectedBin(null)}
              className="absolute top-5 right-5 p-2 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-white/40 dark:hover:bg-slate-800/40 transition-all"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="flex items-center gap-3 mb-4">
              <div className="w-12 h-12 rounded-2xl bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 flex items-center justify-center">
                <Box className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-lg font-bold text-slate-900 dark:text-white font-mono">
                  Bin Inspection: {selectedBin.code}
                </h3>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  Capacity: {selectedBin.capacityQuantity} units &bull; Max Weight: {selectedBin.maxWeightKg} kg
                </p>
              </div>
            </div>

            <div className="mt-4">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">
                Stored Items In This Bin:
              </h4>

              <div className="space-y-2 max-h-60 overflow-y-auto pr-1">
                {getProductsInBin(selectedBin.code).length === 0 ? (
                  <div className="p-6 text-center text-xs text-slate-400 bg-white/30 dark:bg-slate-800/30 rounded-xl">
                    Bin is currently empty.
                  </div>
                ) : (
                  getProductsInBin(selectedBin.code).map((p) => (
                    <div
                      key={p.id}
                      className="p-3 rounded-xl bg-white/40 dark:bg-slate-800/40 border border-slate-200/50 dark:border-slate-700/50 flex items-center justify-between text-xs"
                    >
                      <div>
                        <div className="font-semibold text-slate-900 dark:text-white">
                          {p.name}
                        </div>
                        <div className="text-[10px] font-mono text-indigo-500">
                          {p.sku} &bull; {p.category}
                        </div>
                      </div>
                      <div className="text-right">
                        <span className="font-mono font-bold text-slate-900 dark:text-white">
                          {p.quantity} units
                        </span>
                        <div>
                          <StatusChip status={p.status} />
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

            <div className="mt-6 flex items-center justify-end gap-2 pt-3 border-t border-slate-200/60 dark:border-slate-800">
              <button
                onClick={() => setSelectedBin(null)}
                className="glass-button-secondary text-xs"
              >
                Close
              </button>
              <button
                onClick={() => {
                  setSelectedBin(null);
                  onOpenMovementModal('IN');
                }}
                className="glass-button-primary text-xs"
              >
                <span>Stock In To Bin</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
