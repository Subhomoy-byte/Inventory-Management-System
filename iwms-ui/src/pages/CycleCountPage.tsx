import React, { useState } from 'react';
import type { Product } from '../types';
import { api } from '../services/api';
import { OfflineSyncManager } from '../storage/sync';
import { GlassCard } from '../components/common/GlassCard';
import {
  ClipboardCheck,
  CheckCircle,
  AlertTriangle,
  RotateCw,
  Save,
  Check,
} from 'lucide-react';

interface CycleCountPageProps {
  products: Product[];
  onRefreshCatalog: () => void;
}

export const CycleCountPage: React.FC<CycleCountPageProps> = ({
  products,
  onRefreshCatalog,
}) => {
  // Store physical counts keyed by productId
  const [counts, setCounts] = useState<Record<string, number>>({});
  const [reconcilingId, setReconcilingId] = useState<string | null>(null);
  const [successNote, setSuccessNote] = useState<string | null>(null);

  const handleCountChange = (productId: string, val: number) => {
    setCounts((prev) => ({
      ...prev,
      [productId]: Math.max(0, val),
    }));
  };

  const handleReconcile = async (p: Product) => {
    const countedVal = counts[p.id];
    if (countedVal === undefined) return;

    const diff = countedVal - p.quantity;
    if (diff === 0) return;

    setReconcilingId(p.id);
    setSuccessNote(null);
    try {
      if (diff > 0) {
        // Stock In surplus
        await api.stockIn({
          productId: p.id,
          quantity: diff,
          reason: `Cycle count surplus adjustment (+${diff})`,
        }).catch(async () => {
          await OfflineSyncManager.queueMovement({
            productId: p.id,
            movementType: 'IN',
            quantity: diff,
            reason: `Cycle count surplus adjustment (+${diff})`,
            operatorId: 'auditor',
            timestamp: new Date().toISOString(),
          });
        });
      } else {
        // Stock Out deficit
        await api.stockOut({
          productId: p.id,
          quantity: Math.abs(diff),
          reason: `Cycle count deficit adjustment (${diff})`,
        }).catch(async () => {
          await OfflineSyncManager.queueMovement({
            productId: p.id,
            movementType: 'OUT',
            quantity: Math.abs(diff),
            reason: `Cycle count deficit adjustment (${diff})`,
            operatorId: 'auditor',
            timestamp: new Date().toISOString(),
          });
        });
      }

      setSuccessNote(`Reconciled ${p.sku} to ${countedVal} units`);
      onRefreshCatalog();
    } catch (err: any) {
      alert('Adjustment failed: ' + err.message);
    } finally {
      setReconcilingId(null);
    }
  };

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight text-slate-900 dark:text-white flex items-center gap-3">
            <ClipboardCheck className="w-8 h-8 text-indigo-500" />
            <span>Floor Stock Verification & Cycle Count</span>
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Physical stock count audits, discrepancy calculations, and 1-click inventory balance reconciliation.
          </p>
        </div>

        <button
          onClick={() => {
            setCounts({});
            setSuccessNote(null);
          }}
          className="glass-button-secondary text-sm py-2.5 px-4"
        >
          <RotateCw className="w-4 h-4" />
          <span>Reset Count Form</span>
        </button>
      </div>

      {/* Success Notification */}
      {successNote && (
        <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-700 dark:text-emerald-400 text-sm font-semibold flex items-center justify-between">
          <div className="flex items-center gap-2">
            <CheckCircle className="w-5 h-5 text-emerald-500" />
            <span>{successNote}</span>
          </div>
          <button onClick={() => setSuccessNote(null)} className="text-sm font-bold hover:underline">
            Dismiss
          </button>
        </div>
      )}

      {/* Cycle Count Table */}
      <GlassCard className="p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200/60 dark:border-slate-800 text-slate-500 dark:text-slate-400 font-bold uppercase tracking-wider bg-slate-500/5 text-xs">
                <th className="py-4 px-5">Storage Shelf</th>
                <th className="py-4 px-5">Product Code & Name</th>
                <th className="py-4 px-5">Recorded Stock</th>
                <th className="py-4 px-5 w-44">Physical Count</th>
                <th className="py-4 px-5">Discrepancy</th>
                <th className="py-4 px-5 text-right">Adjustment Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200/40 dark:divide-slate-800/60">
              {products.slice(0, 20).map((p) => {
                const physicalCount = counts[p.id] !== undefined ? counts[p.id] : p.quantity;
                const variance = physicalCount - p.quantity;

                return (
                  <tr key={p.id} className="hover:bg-white/40 dark:hover:bg-slate-800/40 transition-colors">
                    <td className="py-4 px-5 font-mono font-bold text-slate-800 dark:text-slate-200 text-sm">
                      {p.binLocation}
                    </td>
                    <td className="py-4 px-5">
                      <div className="font-bold text-base text-slate-900 dark:text-white">
                        {p.name}
                      </div>
                      <div className="text-xs font-mono font-semibold text-indigo-500 mt-0.5">
                        Code: {p.sku} &bull; {p.category}
                      </div>
                    </td>
                    <td className="py-4 px-5 font-mono font-extrabold text-slate-800 dark:text-slate-200 text-base">
                      {p.quantity} units
                    </td>
                    <td className="py-4 px-5">
                      <input
                        type="number"
                        min="0"
                        value={physicalCount}
                        onChange={(e) => handleCountChange(p.id, parseInt(e.target.value) || 0)}
                        className="glass-input py-1.5 px-3 font-mono text-sm font-bold w-32"
                      />
                    </td>
                    <td className="py-4 px-5">
                      {variance === 0 ? (
                        <span className="text-emerald-600 dark:text-emerald-400 font-bold font-mono text-sm flex items-center gap-1.5">
                          <Check className="w-4 h-4" />
                          <span>Exact Match (0)</span>
                        </span>
                      ) : variance > 0 ? (
                        <span className="text-blue-600 dark:text-blue-400 font-bold font-mono text-sm">
                          +{variance} (Surplus)
                        </span>
                      ) : (
                        <span className="text-rose-600 dark:text-rose-400 font-bold font-mono text-sm flex items-center gap-1.5">
                          <AlertTriangle className="w-4 h-4" />
                          <span>{variance} (Deficit)</span>
                        </span>
                      )}
                    </td>
                    <td className="py-4 px-5 text-right">
                      {variance !== 0 ? (
                        <button
                          onClick={() => handleReconcile(p)}
                          disabled={reconcilingId === p.id}
                          className="glass-button-primary text-xs py-2 px-3.5 bg-gradient-to-r from-amber-500 to-orange-600 hover:from-amber-600 hover:to-orange-700"
                        >
                          {reconcilingId === p.id ? (
                            <span className="inline-block w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                          ) : (
                            <>
                              <Save className="w-3.5 h-3.5" />
                              <span>Reconcile Ledger</span>
                            </>
                          )}
                        </button>
                      ) : (
                        <span className="text-emerald-500 font-bold text-xs px-2.5 py-1 rounded-lg bg-emerald-500/10">Balanced</span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </GlassCard>
    </div>
  );
};
