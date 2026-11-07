import React, { useState } from 'react';
import type { Product, MovementType } from '../../types';
import { api } from '../../services/api';
import { OfflineSyncManager } from '../../storage/sync';
import { useAuth } from '../../context/AuthContext';
import { X, ArrowDownRight, ArrowUpRight, ArrowLeftRight, Check, AlertTriangle } from 'lucide-react';

interface StockMovementModalProps {
  isOpen: boolean;
  onClose: () => void;
  product?: Product | null;
  allProducts: Product[];
  initialType?: MovementType;
  onSuccess: () => void;
}

export const StockMovementModal: React.FC<StockMovementModalProps> = ({
  isOpen,
  onClose,
  product,
  allProducts,
  initialType = 'IN',
  onSuccess,
}) => {
  const { user } = useAuth();
  const [movementType, setMovementType] = useState<MovementType>(initialType);
  const [selectedProductId, setSelectedProductId] = useState<string>(product?.id || allProducts[0]?.id || '');
  const [quantity, setQuantity] = useState<number>(10);
  const [unitPrice, setUnitPrice] = useState<number>(product?.costPrice || 25);
  const [sourceBin, setSourceBin] = useState<string>(product?.binLocation || 'BIN-A1-01');
  const [targetBin, setTargetBin] = useState<string>('BIN-B1-02');
  const [reason, setReason] = useState<string>('Routine operation');
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const currentSelectedProduct = allProducts.find(p => p.id === selectedProductId) || product;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      if (quantity <= 0) {
        throw new Error('Quantity must be greater than zero');
      }

      const operatorId = user?.username || 'operator';

      if (!navigator.onLine) {
        // Queue to IndexedDB Outbox
        await OfflineSyncManager.queueMovement({
          productId: selectedProductId,
          movementType,
          quantity,
          sourceBin: movementType === 'IN' ? undefined : sourceBin,
          targetBin: movementType === 'OUT' ? undefined : targetBin,
          reason,
          operatorId,
          timestamp: new Date().toISOString(),
        });
      } else {
        // Send directly via API with fallback to IndexedDB Outbox
        try {
          if (movementType === 'IN') {
            await api.stockIn({
              productId: selectedProductId,
              quantity,
              unitPrice,
              targetBin,
              reason,
            });
          } else if (movementType === 'OUT') {
            await api.stockOut({
              productId: selectedProductId,
              quantity,
              sourceBin,
              reason,
            });
          } else if (movementType === 'TRANSFER') {
            await api.stockTransfer({
              productId: selectedProductId,
              sourceBin,
              targetBin,
              quantity,
            });
          }
        } catch (apiErr) {
          console.warn('Backend unavailable, queueing movement to local Outbox:', apiErr);
          await OfflineSyncManager.queueMovement({
            productId: selectedProductId,
            movementType,
            quantity,
            sourceBin: movementType === 'IN' ? undefined : sourceBin,
            targetBin: movementType === 'OUT' ? undefined : targetBin,
            reason,
            operatorId,
            timestamp: new Date().toISOString(),
          });
        }
      }

      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.message || 'Operation failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="w-full max-w-lg glass-panel p-6 sm:p-8 shadow-2xl ring-1 ring-white/20 dark:ring-white/10 relative">
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-5 right-5 p-2 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-white/40 dark:hover:bg-slate-800/40 transition-all"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Modal Header */}
        <div className="mb-6">
          <h3 className="text-xl font-bold text-slate-900 dark:text-white flex items-center gap-2">
            Record Stock Movement
          </h3>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            Atomic stock updates, WAL logging, and automatic offline outbox queueing.
          </p>
        </div>

        {/* Movement Type Tabs */}
        <div className="grid grid-cols-3 gap-2 mb-6 p-1 rounded-2xl bg-slate-200/50 dark:bg-slate-800/50">
          <button
            type="button"
            onClick={() => setMovementType('IN')}
            className={`py-2 px-3 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition-all ${
              movementType === 'IN'
                ? 'bg-emerald-500 text-white shadow-md'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'
            }`}
          >
            <ArrowDownRight className="w-3.5 h-3.5" />
            <span>Stock In</span>
          </button>
          <button
            type="button"
            onClick={() => setMovementType('OUT')}
            className={`py-2 px-3 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition-all ${
              movementType === 'OUT'
                ? 'bg-rose-500 text-white shadow-md'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'
            }`}
          >
            <ArrowUpRight className="w-3.5 h-3.5" />
            <span>Stock Out</span>
          </button>
          <button
            type="button"
            onClick={() => setMovementType('TRANSFER')}
            className={`py-2 px-3 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition-all ${
              movementType === 'TRANSFER'
                ? 'bg-indigo-500 text-white shadow-md'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'
            }`}
          >
            <ArrowLeftRight className="w-3.5 h-3.5" />
            <span>Transfer</span>
          </button>
        </div>

        {/* Error Alert */}
        {error && (
          <div className="mb-4 p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-600 dark:text-rose-400 text-xs flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Product Select */}
          <div>
            <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1.5">
              Select Product / SKU
            </label>
            <select
              value={selectedProductId}
              onChange={(e) => setSelectedProductId(e.target.value)}
              className="glass-input"
            >
              {allProducts.map((p) => (
                <option key={p.id} value={p.id} className="bg-slate-900 text-white">
                  [{p.sku}] {p.name} (Stock: {p.quantity})
                </option>
              ))}
            </select>
          </div>

          {/* Quantity & Location */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1.5">
                Quantity
              </label>
              <input
                type="number"
                min="1"
                required
                value={quantity}
                onChange={(e) => setQuantity(parseInt(e.target.value) || 0)}
                className="glass-input font-mono"
              />
            </div>

            {movementType === 'IN' && (
              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1.5">
                  Target Bin
                </label>
                <input
                  type="text"
                  required
                  value={targetBin}
                  onChange={(e) => setTargetBin(e.target.value)}
                  className="glass-input font-mono"
                  placeholder="e.g. BIN-A1-01"
                />
              </div>
            )}

            {movementType === 'OUT' && (
              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1.5">
                  Source Bin
                </label>
                <input
                  type="text"
                  required
                  value={sourceBin}
                  onChange={(e) => setSourceBin(e.target.value)}
                  className="glass-input font-mono"
                  placeholder="e.g. BIN-A1-01"
                />
              </div>
            )}
          </div>

          {movementType === 'TRANSFER' && (
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1.5">
                  Source Bin
                </label>
                <input
                  type="text"
                  required
                  value={sourceBin}
                  onChange={(e) => setSourceBin(e.target.value)}
                  className="glass-input font-mono"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1.5">
                  Target Bin
                </label>
                <input
                  type="text"
                  required
                  value={targetBin}
                  onChange={(e) => setTargetBin(e.target.value)}
                  className="glass-input font-mono"
                />
              </div>
            </div>
          )}

          {movementType === 'IN' && (
            <div>
              <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1.5">
                Unit Purchase Price ($)
              </label>
              <input
                type="number"
                step="0.01"
                min="0"
                value={unitPrice}
                onChange={(e) => setUnitPrice(parseFloat(e.target.value) || 0)}
                className="glass-input font-mono"
              />
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1.5">
              Reason / Reference Notes
            </label>
            <input
              type="text"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              className="glass-input text-xs"
              placeholder="e.g., Supplier PO receipt, Customer dispatch, Floor count audit"
            />
          </div>

          {currentSelectedProduct && (
            <div className="p-3 rounded-xl bg-slate-100/60 dark:bg-slate-800/40 text-xs text-slate-600 dark:text-slate-300 flex justify-between items-center">
              <span>Current On-Hand: <strong className="text-slate-900 dark:text-white font-mono">{currentSelectedProduct.quantity} units</strong></span>
              <span>Projected: <strong className="text-indigo-600 dark:text-indigo-400 font-mono">
                {movementType === 'IN'
                  ? currentSelectedProduct.quantity + quantity
                  : movementType === 'OUT'
                  ? Math.max(0, currentSelectedProduct.quantity - quantity)
                  : currentSelectedProduct.quantity} units
              </strong></span>
            </div>
          )}

          <div className="flex items-center justify-end gap-3 pt-3">
            <button
              type="button"
              onClick={onClose}
              className="glass-button-secondary text-xs"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="glass-button-primary text-xs"
            >
              {loading ? (
                <span className="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
              ) : (
                <>
                  <Check className="w-4 h-4" />
                  <span>Execute Movement</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
