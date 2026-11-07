import React, { useState } from 'react';
import type { Product } from '../types';
import { api } from '../services/api';
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
        });
      } else {
        // Stock Out deficit
        await api.stockOut({
          productId: p.id,
          quantity: Math.abs(diff),
          reason: `Cycle count deficit adjustment (${diff})`,
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
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center gap-2">
            <ClipboardCheck className="w-6 h-6 text-indigo-500" />
            <span>Guided Floor Cycle Count</span>
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            Physical stock verification, live variance computation, and 1-click ledger reconciliation.
          </p>
        </div>

        <button
          onClick={() => {
            setCounts({});
            setSuccessNote(null);
          }}
          className="glass-button-secondary text-xs py-2 px-3"
        >
          <RotateCw className="w-3.5 h-3.5" />
          <span>Reset Floor Form</span>
        </button>
      </div>

      {/* Success Notification */}
      {successNote && (
        <div className="p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-700 dark:text-emerald-400 text-xs flex items-center justify-between">
          <div className="flex items-center gap-2">
            <CheckCircle className="w-4 h-4 text-emerald-500" />
            <span>{successNote}</span>
          </div>
          <button onClick={() => setSuccessNote(null)} className="text-xs font-semibold hover:underline">
            Dismiss
          </button>
        </div>
      )}

      {/* Cycle Count Table */}
      <GlassCard className="p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-slate-200/60 dark:border-slate-800 text-slate-400 font-semibold uppercase tracking-wider bg-slate-500/5">
                <th className="py-3 px-4">Bin Location</th>
                <th className="py-3 px-4">SKU / Product</th>
                <th className="py-3 px-4">System Recorded</th>
                <th className="py-3 px-4 w-36">Physical Floor Count</th>
                <th className="py-3 px-4">Variance</th>
                <th className="py-3 px-4 text-right">Reconciliation</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200/40 dark:divide-slate-800/60">
              {products.slice(0, 20).map((p) => {
                const physicalCount = counts[p.id] !== undefined ? counts[p.id] : p.quantity;
                const variance = physicalCount - p.quantity;

                return (
                  <tr key={p.id} className="hover:bg-white/40 dark:hover:bg-slate-800/40 transition-colors">
                    <td className="py-3 px-4 font-mono font-bold text-slate-800 dark:text-slate-200">
                      {p.binLocation}
                    </td>
                    <td className="py-3 px-4">
                      <div className="font-semibold text-slate-900 dark:text-white">
                        {p.name}
                      </div>
                      <div className="text-[10px] font-mono text-indigo-500">
                        {p.sku} &bull; {p.category}
                      </div>
                    </td>
                    <td className="py-3 px-4 font-mono font-bold text-slate-700 dark:text-slate-300">
                      {p.quantity} units
                    </td>
                    <td className="py-3 px-4">
                      <input
                        type="number"
                        min="0"
                        value={physicalCount}
                        onChange={(e) => handleCountChange(p.id, parseInt(e.target.value) || 0)}
                        className="glass-input py-1 px-2 font-mono text-xs w-28"
                      />
                    </td>
                    <td className="py-3 px-4">
                      {variance === 0 ? (
                        <span className="text-emerald-600 dark:text-emerald-400 font-bold font-mono text-xs flex items-center gap-1">
                          <Check className="w-3.5 h-3.5" />
                          <span>Exact Match (0)</span>
                        </span>
                      ) : variance > 0 ? (
                        <span className="text-blue-600 dark:text-blue-400 font-bold font-mono text-xs">
                          +{variance} (Surplus)
                        </span>
                      ) : (
                        <span className="text-rose-600 dark:text-rose-400 font-bold font-mono text-xs flex items-center gap-1">
                          <AlertTriangle className="w-3.5 h-3.5" />
                          <span>{variance} (Deficit)</span>
                        </span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-right">
                      {variance !== 0 ? (
                        <button
                          onClick={() => handleReconcile(p)}
                          disabled={reconcilingId === p.id}
                          className="glass-button-primary text-xs py-1 px-2.5 bg-gradient-to-r from-amber-500 to-orange-600 hover:from-amber-600 hover:to-orange-700"
                        >
                          {reconcilingId === p.id ? (
                            <span className="inline-block w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin" />
                          ) : (
                            <>
                              <Save className="w-3 h-3" />
                              <span>Reconcile</span>
                            </>
                          )}
                        </button>
                      ) : (
                        <span className="text-slate-400 text-[11px]">Synced</span>
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
