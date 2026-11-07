import React, { useState, useEffect } from 'react';
import type { SyncQueueItem, SyncConflict } from '../../types';
import { OfflineSyncManager } from '../../storage/sync';
import { GlassCard } from '../common/GlassCard';
import { StatusChip } from '../common/StatusChip';
import {
  RefreshCw,
  AlertOctagon,
  CheckCircle2,
  Clock,
  Wifi,
  WifiOff,
} from 'lucide-react';

export const ConflictReviewPanel: React.FC = () => {
  const [pendingItems, setPendingItems] = useState<SyncQueueItem[]>([]);
  const [conflicts, setConflicts] = useState<SyncConflict[]>([]);
  const [syncing, setSyncing] = useState(false);
  const [isOnline, setIsOnline] = useState(navigator.onLine);

  const loadQueue = async () => {
    const items = await OfflineSyncManager.getPendingItems();
    const confs = await OfflineSyncManager.getConflicts();
    setPendingItems(items);
    setConflicts(confs);
  };

  useEffect(() => {
    loadQueue();

    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);
    const handleUpdate = () => loadQueue();

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    window.addEventListener('iwms:outbox-updated', handleUpdate);
    window.addEventListener('iwms:sync-complete', handleUpdate);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
      window.removeEventListener('iwms:outbox-updated', handleUpdate);
      window.removeEventListener('iwms:sync-complete', handleUpdate);
    };
  }, []);

  const handleForceSync = async () => {
    setSyncing(true);
    try {
      await OfflineSyncManager.drainQueue();
      await loadQueue();
    } finally {
      setSyncing(false);
    }
  };

  const handleDismissConflict = async (syncId: string) => {
    await OfflineSyncManager.clearConflict(syncId);
    await loadQueue();
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center gap-2">
            <RefreshCw className="w-6 h-6 text-indigo-500" />
            <span>PWA Offline Outbox & Conflict Manager</span>
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            Local IndexedDB queue, idempotent transaction replay, and deficit reconciliation.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold border ${
              isOnline
                ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/30'
                : 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/30 animate-pulse'
            }`}
          >
            {isOnline ? <Wifi className="w-3.5 h-3.5" /> : <WifiOff className="w-3.5 h-3.5" />}
            <span>{isOnline ? 'Network Online' : 'Network Offline'}</span>
          </div>

          <button
            onClick={handleForceSync}
            disabled={syncing || !isOnline || pendingItems.length === 0}
            className="glass-button-primary text-xs py-2 px-3"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${syncing ? 'animate-spin' : ''}`} />
            <span>{syncing ? 'Syncing...' : 'Force Sync Now'}</span>
          </button>
        </div>
      </div>

      {/* Conflicts Alert Card */}
      {conflicts.length > 0 && (
        <GlassCard className="border-rose-500/30 bg-rose-500/5">
          <div className="flex items-center gap-2 mb-3 text-rose-600 dark:text-rose-400 font-bold text-sm">
            <AlertOctagon className="w-4 h-4" />
            <span>Unresolved Inventory Deficit Conflicts ({conflicts.length})</span>
          </div>

          <p className="text-xs text-slate-600 dark:text-slate-300 mb-4">
            The following offline transactions resulted in stock deficits when synced against the master ledger. Review and dismiss after physical count adjustment:
          </p>

          <div className="space-y-2">
            {conflicts.map((c) => (
              <div
                key={c.syncId}
                className="p-3 rounded-xl bg-white/60 dark:bg-slate-800/60 border border-rose-500/20 flex items-center justify-between text-xs"
              >
                <div>
                  <div className="font-semibold text-slate-900 dark:text-white">
                    {c.message}
                  </div>
                  <div className="text-[10px] font-mono text-slate-400">
                    Product: {c.productId} &bull; Deficit: {c.deficitQuantity ?? 'N/A'} units &bull; ID: {c.syncId}
                  </div>
                </div>

                <button
                  onClick={() => handleDismissConflict(c.syncId)}
                  className="px-2.5 py-1 rounded-lg bg-rose-500/10 hover:bg-rose-500/20 text-rose-600 dark:text-rose-400 text-xs font-medium transition-all"
                >
                  Dismiss
                </button>
              </div>
            ))}
          </div>
        </GlassCard>
      )}

      {/* Outbox Items List */}
      <GlassCard>
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <Clock className="w-4 h-4 text-indigo-500" />
            <h3 className="text-sm font-bold text-slate-900 dark:text-white">
              Queued Offline Mutations ({pendingItems.length})
            </h3>
          </div>
          <span className="text-xs font-mono text-slate-400">Stored in IndexedDB</span>
        </div>

        {pendingItems.length === 0 ? (
          <div className="py-12 text-center text-slate-400 text-xs space-y-2">
            <CheckCircle2 className="w-8 h-8 mx-auto text-emerald-500" />
            <div>Offline outbox is clean. All movements are synchronized with server!</div>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-slate-200/60 dark:border-slate-800 text-slate-400 font-semibold uppercase tracking-wider">
                  <th className="py-2.5 px-3">Sync ID</th>
                  <th className="py-2.5 px-3">Operation</th>
                  <th className="py-2.5 px-3">Product</th>
                  <th className="py-2.5 px-3">Quantity</th>
                  <th className="py-2.5 px-3">Bins</th>
                  <th className="py-2.5 px-3">Operator</th>
                  <th className="py-2.5 px-3">Queued At</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200/40 dark:divide-slate-800/60">
                {pendingItems.map((item) => (
                  <tr key={item.syncId} className="hover:bg-white/40 dark:hover:bg-slate-800/40">
                    <td className="py-2.5 px-3 font-mono text-[11px] text-indigo-500">
                      {item.syncId.slice(0, 16)}...
                    </td>
                    <td className="py-2.5 px-3">
                      <StatusChip status={item.movementType} />
                    </td>
                    <td className="py-2.5 px-3 font-medium text-slate-900 dark:text-white">
                      {item.productId}
                    </td>
                    <td className="py-2.5 px-3 font-mono font-bold">
                      {item.quantity}
                    </td>
                    <td className="py-2.5 px-3 font-mono text-slate-400 text-[11px]">
                      {item.sourceBin || '-'} &rarr; {item.targetBin || '-'}
                    </td>
                    <td className="py-2.5 px-3 text-slate-600 dark:text-slate-300">
                      {item.operatorId}
                    </td>
                    <td className="py-2.5 px-3 text-slate-400 font-mono text-[10px]">
                      {new Date(item.timestamp).toLocaleTimeString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </GlassCard>
    </div>
  );
};
