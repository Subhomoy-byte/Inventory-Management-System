import React, { useState, useEffect } from 'react';
import type { StockMovement, Product } from '../types';
import { api } from '../services/api';
import { OfflineSyncManager } from '../storage/sync';
import { GlassCard } from '../components/common/GlassCard';
import { StatusChip } from '../components/common/StatusChip';
import {
  History,
  RotateCcw,
  Clock,
  RotateCw,
  Sliders,
  Calendar,
  Boxes,
} from 'lucide-react';

export const MovementsPage: React.FC = () => {
  const [movements, setMovements] = useState<StockMovement[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [selectedType, setSelectedType] = useState<string>('ALL');
  const [undoLoading, setUndoLoading] = useState<boolean>(false);
  const [undoResult, setUndoResult] = useState<string | null>(null);

  // Time-Travel state
  const [isTimeTraveling, setIsTimeTraveling] = useState<boolean>(false);
  const [timeTravelMinutesAgo, setTimeTravelMinutesAgo] = useState<number>(0);
  const [historicalStock, setHistoricalStock] = useState<Record<string, number> | null>(null);
  const [historyLoading, setHistoryLoading] = useState<boolean>(false);

  const loadMovements = async () => {
    try {
      const [mList, pList] = await Promise.all([
        api.getStockMovements().catch(() => []),
        api.getProducts().catch(() => OfflineSyncManager.getCachedProducts()),
      ]);
      setMovements(mList.reverse());
      setProducts(pList);
    } catch (err) {
      console.warn('Failed to load movements:', err);
    }
  };

  useEffect(() => {
    loadMovements();
  }, []);

  const handleUndo = async () => {
    setUndoLoading(true);
    setUndoResult(null);
    try {
      const undone = await api.undoLastMovement();
      setUndoResult(`Undone movement: [${undone.movementType}] ${undone.quantity} units of ${undone.productId}`);
      await loadMovements();
    } catch (err: any) {
      setUndoResult(`Undo Error: ${err.message}`);
    } finally {
      setUndoLoading(false);
    }
  };

  // Execute time-travel query
  const handleQueryAsOf = async (minutesAgo: number) => {
    setTimeTravelMinutesAgo(minutesAgo);
    if (minutesAgo === 0) {
      setIsTimeTraveling(false);
      setHistoricalStock(null);
      return;
    }

    setIsTimeTraveling(true);
    setHistoryLoading(true);
    try {
      const targetTime = new Date(Date.now() - minutesAgo * 60 * 1000).toISOString();
      const stockMap = await api.getStockAsOf(targetTime);
      setHistoricalStock(stockMap);
    } catch (err: any) {
      console.warn('Time-travel query error:', err);
    } finally {
      setHistoryLoading(false);
    }
  };

  const filteredMovements = movements.filter((m) => {
    if (selectedType === 'ALL') return true;
    return m.movementType === selectedType;
  });

  const timeTravelDate = new Date(Date.now() - timeTravelMinutesAgo * 60 * 1000);

  return (
    <div className="space-y-8">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight text-slate-900 dark:text-white flex items-center gap-3">
            <History className="w-8 h-8 text-indigo-500" />
            <span>Inventory Transaction History & Time Travel</span>
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Complete transaction ledger with 1-click transaction undo and historical state replay.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={handleUndo}
            disabled={undoLoading || movements.length === 0}
            className="glass-button-secondary text-sm py-2.5 px-4 hover:text-amber-600 disabled:opacity-50"
            title="Reverses the most recent inventory transaction"
          >
            <RotateCcw className={`w-4 h-4 ${undoLoading ? 'animate-spin' : ''}`} />
            <span>Undo Last Transaction</span>
          </button>
        </div>
      </div>

      {/* Undo Alert Notification */}
      {undoResult && (
        <div className="p-4 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-700 dark:text-amber-400 text-sm font-semibold flex items-center justify-between">
          <span>{undoResult}</span>
          <button onClick={() => setUndoResult(null)} className="text-sm font-bold hover:underline">
            Dismiss
          </button>
        </div>
      )}

      {/* Time-Travel Interactive Slider Card */}
      <GlassCard className="p-5 border-indigo-500/30 bg-gradient-to-r from-indigo-500/5 via-purple-500/5 to-transparent">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 flex items-center justify-center">
              <Sliders className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-slate-900 dark:text-white flex items-center gap-2">
                <span>Time-Travel Inventory Replay</span>
                {isTimeTraveling && (
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-mono bg-purple-500/10 text-purple-600 dark:text-purple-400 border border-purple-500/30 animate-pulse">
                    Historical View Active
                  </span>
                )}
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Scrub through the immutable TreeMap ledger timeline to inspect inventory levels at any historical instant.
              </p>
            </div>
          </div>

          {isTimeTraveling && (
            <button
              onClick={() => handleQueryAsOf(0)}
              className="glass-button-secondary text-xs py-1.5 px-3 self-start md:self-auto"
            >
              <RotateCw className="w-3.5 h-3.5" />
              <span>Reset to Present</span>
            </button>
          )}
        </div>

        {/* Slider Controls */}
        <div className="space-y-3 pt-2">
          <div className="flex items-center justify-between text-xs text-slate-500 dark:text-slate-400">
            <span className="flex items-center gap-1 font-mono">
              <Calendar className="w-3.5 h-3.5" />
              {timeTravelMinutesAgo === 0 ? 'Present (Live Stock)' : timeTravelDate.toLocaleString()}
            </span>
            <span className="font-mono font-bold text-indigo-600 dark:text-indigo-400">
              {timeTravelMinutesAgo === 0 ? 'Live 0m' : `-${timeTravelMinutesAgo} mins ago`}
            </span>
          </div>

          <input
            type="range"
            min="0"
            max="1440"
            step="15"
            value={timeTravelMinutesAgo}
            onChange={(e) => handleQueryAsOf(parseInt(e.target.value) || 0)}
            className="w-full h-2 bg-slate-200 dark:bg-slate-700 rounded-lg appearance-none cursor-pointer accent-indigo-600"
          />

          <div className="flex justify-between text-[10px] text-slate-400 font-mono">
            <span>Now (0m)</span>
            <span>-1 Hour</span>
            <span>-6 Hours</span>
            <span>-12 Hours</span>
            <span>-24 Hours (1 Day)</span>
          </div>
        </div>

        {/* Historical Stock Results Preview */}
        {isTimeTraveling && (
          <div className="mt-5 pt-4 border-t border-slate-200/60 dark:border-slate-800">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-3 flex items-center gap-1.5">
              <Clock className="w-3.5 h-3.5" />
              <span>Reconstructed Stock Snapshot at {timeTravelDate.toLocaleTimeString()}</span>
            </h4>

            {historyLoading ? (
              <div className="py-6 text-center text-xs text-slate-400">
                Reconstructing state via TreeMap.headMap()...
              </div>
            ) : historicalStock ? (
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-2 max-h-48 overflow-y-auto pr-1">
                {products.slice(0, 18).map((p) => {
                  const historicQty = historicalStock[p.id] ?? 0;
                  const delta = p.quantity - historicQty;

                  return (
                    <div
                      key={p.id}
                      className="p-2.5 rounded-xl bg-white/50 dark:bg-slate-800/50 border border-slate-200/60 dark:border-slate-700/60 text-sm"
                    >
                      <div className="font-mono text-xs font-semibold text-indigo-500 truncate">{p.sku}</div>
                      <div className="font-bold text-slate-900 dark:text-white truncate">{p.name}</div>
                      <div className="mt-1 flex items-center justify-between text-xs font-mono">
                        <span className="font-bold">{historicQty} units</span>
                        {delta !== 0 && (
                          <span className={delta > 0 ? 'text-emerald-500 font-bold' : 'text-rose-500 font-bold'}>
                            {delta > 0 ? `+${delta}` : delta}
                          </span>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : null}
          </div>
        )}
      </GlassCard>

      {/* Movement Ledger Table */}
      <GlassCard className="p-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-5">
          <div>
            <h3 className="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2">
              <History className="w-5 h-5 text-purple-500" />
              <span>Verified Transaction Log</span>
            </h3>
            <p className="text-sm text-slate-500 dark:text-slate-400 mt-0.5">
              Total {movements.length} recorded inventory mutations in local transaction ledger
            </p>
          </div>

          {/* Filter Pills */}
          <div className="flex items-center gap-2 p-1.5 rounded-2xl bg-slate-200/50 dark:bg-slate-800/50 text-sm font-semibold">
            {[
              { id: 'ALL', label: 'All Transactions' },
              { id: 'IN', label: 'Stock In' },
              { id: 'OUT', label: 'Stock Out' },
              { id: 'TRANSFER', label: 'Shelf Transfer' },
              { id: 'ADJUST', label: 'Cycle Adjustment' },
            ].map((t) => (
              <button
                key={t.id}
                onClick={() => setSelectedType(t.id)}
                className={`px-3 py-1.5 rounded-xl transition-all ${
                  selectedType === t.id
                    ? 'bg-white dark:bg-slate-900 text-indigo-600 dark:text-indigo-400 shadow-sm font-bold'
                    : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'
                }`}
              >
                {t.label}
              </button>
            ))}
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200/60 dark:border-slate-800 text-slate-500 dark:text-slate-400 font-bold uppercase tracking-wider bg-slate-500/5 text-xs">
                <th className="py-4 px-4">Transaction ID</th>
                <th className="py-4 px-4">Action</th>
                <th className="py-4 px-4">Product Code & Name</th>
                <th className="py-4 px-4">Quantity</th>
                <th className="py-4 px-4">Shelf Route</th>
                <th className="py-4 px-4">Reason / Reference</th>
                <th className="py-4 px-4">Staff Member</th>
                <th className="py-4 px-4">Date & Time</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200/40 dark:divide-slate-800/60">
              {filteredMovements.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-16 text-center text-slate-400">
                    <Boxes className="w-10 h-10 mx-auto mb-3 opacity-40" />
                    <span className="text-base font-medium">No inventory transactions match the filter criteria.</span>
                  </td>
                </tr>
              ) : (
                filteredMovements.map((m) => (
                  <tr key={m.movementId} className="hover:bg-white/40 dark:hover:bg-slate-800/40 transition-colors">
                    <td className="py-4 px-4 font-mono text-xs text-slate-500 dark:text-slate-400">
                      {m.movementId.slice(0, 8)}
                    </td>
                    <td className="py-4 px-4">
                      <StatusChip status={m.movementType} />
                    </td>
                    <td className="py-4 px-4 font-bold text-slate-900 dark:text-white">
                      {m.productId}
                    </td>
                    <td className="py-4 px-4 font-mono font-extrabold text-slate-900 dark:text-white text-base">
                      {m.quantity} units
                    </td>
                    <td className="py-4 px-4 font-mono text-slate-600 dark:text-slate-300 text-xs">
                      {m.sourceBin || 'Receiving Dock'} &rarr; {m.targetBin || 'Shipping Dock'}
                    </td>
                    <td className="py-4 px-4 text-slate-600 dark:text-slate-300 max-w-xs truncate text-xs">
                      {m.reason || '-'}
                    </td>
                    <td className="py-4 px-4 text-slate-800 dark:text-slate-200 font-semibold">
                      {m.operatorId}
                    </td>
                    <td className="py-4 px-4 text-slate-400 font-mono text-xs">
                      {new Date(m.timestamp).toLocaleString()}
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
