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
          <h1 className="text-3xl font-extrabold tracking-tight text-slate-900 dark:text-white flex items-center gap-3">
            <RefreshCw className="w-7 h-7 text-indigo-500" />
            <span>Offline Synchronization Queue & Reconciliation</span>
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Local browser database queue, duplicate-safe transaction sync, and stock discrepancy reconciliation.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div
            className={`flex items-center gap-2 px-3.5 py-2 rounded-full text-sm font-semibold border ${
              isOnline
                ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/30'
                : 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/30 animate-pulse'
            }`}
          >
            {isOnline ? <Wifi className="w-4 h-4" /> : <WifiOff className="w-4 h-4" />}
            <span>{isOnline ? 'Network Online' : 'Network Offline'}</span>
          </div>

          <button
            onClick={handleForceSync}
            disabled={syncing || !isOnline || pendingItems.length === 0}
            className="glass-button-primary text-sm py-2.5 px-4"
          >
            <RefreshCw className={`w-4 h-4 ${syncing ? 'animate-spin' : ''}`} />
            <span>{syncing ? 'Syncing...' : 'Force Sync Now'}</span>
          </button>
        </div>
      </div>

      {/* Conflicts Alert Card */}
      {conflicts.length > 0 && (
        <GlassCard className="border-rose-500/30 bg-rose-500/5 p-6">
          <div className="flex items-center gap-2.5 mb-3 text-rose-600 dark:text-rose-400 font-bold text-base">
            <AlertOctagon className="w-5 h-5" />
            <span>Unresolved Inventory Deficit Conflicts ({conflicts.length})</span>
          </div>

          <p className="text-sm text-slate-600 dark:text-slate-300 mb-4">
            The following offline transactions resulted in stock deficits when synced against the master ledger. Review and dismiss after physical count adjustment:
          </p>

          <div className="space-y-3">
            {conflicts.map((c) => (
              <div
                key={c.syncId}
                className="p-4 rounded-xl bg-white/60 dark:bg-slate-800/60 border border-rose-500/20 flex items-center justify-between text-sm"
              >
                <div>
                  <div className="font-semibold text-slate-900 dark:text-white">
                    {c.message}
                  </div>
                  <div className="text-xs font-mono text-slate-400 mt-1">
                    Product: {c.productId} &bull; Deficit: {c.deficitQuantity ?? 'N/A'} units &bull; Sync Reference: {c.syncId}
                  </div>
                </div>

                <button
                  onClick={() => handleDismissConflict(c.syncId)}
                  className="px-3.5 py-1.5 rounded-lg bg-rose-500/10 hover:bg-rose-500/20 text-rose-600 dark:text-rose-400 text-sm font-semibold transition-all"
                >
                  Dismiss
                </button>
              </div>
            ))}
          </div>
        </GlassCard>
      )}

      {/* Outbox Items List */}
      <GlassCard className="p-6">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2.5">
            <Clock className="w-5 h-5 text-indigo-500" />
            <h3 className="text-base font-bold text-slate-900 dark:text-white">
              Queued Offline Transactions ({pendingItems.length})
            </h3>
          </div>
          <span className="text-sm font-medium text-slate-400">Stored in Local Browser Database</span>
        </div>

        {pendingItems.length === 0 ? (
          <div className="py-12 text-center text-slate-400 text-sm space-y-2">
            <CheckCircle2 className="w-10 h-10 mx-auto text-emerald-500" />
            <div>Offline outbox is clean. All movements are synchronized with server!</div>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200/60 dark:border-slate-800 text-slate-400 font-bold uppercase tracking-wider text-xs">
                  <th className="py-3 px-3">Sync ID</th>
                  <th className="py-3 px-3">Operation</th>
                  <th className="py-3 px-3">Product</th>
                  <th className="py-3 px-3">Quantity</th>
                  <th className="py-3 px-3">Shelf Locations</th>
                  <th className="py-3 px-3">Operator</th>
                  <th className="py-3 px-3">Queued At</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200/40 dark:divide-slate-800/60">
                {pendingItems.map((item) => (
                  <tr key={item.syncId} className="hover:bg-white/40 dark:hover:bg-slate-800/40 font-medium">
                    <td className="py-3 px-3 font-mono text-xs text-indigo-500">
                      {item.syncId.slice(0, 16)}...
                    </td>
                    <td className="py-3 px-3">
                      <StatusChip status={item.movementType} />
                    </td>
                    <td className="py-3 px-3 font-semibold text-slate-900 dark:text-white">
                      {item.productId}
                    </td>
                    <td className="py-3 px-3 font-mono font-bold text-base">
                      {item.quantity}
                    </td>
                    <td className="py-3 px-3 font-mono text-slate-400 text-xs">
                      {item.sourceBin || '-'} &rarr; {item.targetBin || '-'}
                    </td>
                    <td className="py-3 px-3 text-slate-600 dark:text-slate-300">
                      {item.operatorId}
                    </td>
                    <td className="py-3 px-3 text-slate-400 font-mono text-xs">
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
