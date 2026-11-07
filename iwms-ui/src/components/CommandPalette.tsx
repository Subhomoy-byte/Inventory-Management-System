import React, { useState, useEffect } from 'react';
import type { NavTab } from './layout/Dock';
import type { Product } from '../types';
import {
  Search,
  LayoutDashboard,
  Boxes,
  MapPin,
  History,
  ShoppingCart,
  ClipboardCheck,
  FileText,
  RefreshCw,
  ArrowRight,
  X,
} from 'lucide-react';

interface CommandPaletteProps {
  isOpen: boolean;
  onClose: () => void;
  onSelectTab: (tab: NavTab) => void;
  products: Product[];
  onSelectProduct?: (product: Product) => void;
}

export const CommandPalette: React.FC<CommandPaletteProps> = ({
  isOpen,
  onClose,
  onSelectTab,
  products,
  onSelectProduct,
}) => {
  const [query, setQuery] = useState('');

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
        e.preventDefault();
        if (isOpen) onClose();
        else setQuery('');
      } else if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const tabs: { id: NavTab; label: string; icon: React.ReactNode; desc: string }[] = [
    { id: 'dashboard', label: 'Dashboard', icon: <LayoutDashboard className="w-4 h-4" />, desc: 'System overview, KPI cards & charts' },
    { id: 'inventory', label: 'Inventory Catalog', icon: <Boxes className="w-4 h-4" />, desc: 'Live catalog, filtering & movements' },
    { id: 'warehouse', label: 'Warehouse Map', icon: <MapPin className="w-4 h-4" />, desc: '2D Zone and Storage Shelf capacity visualization' },
    { id: 'movements', label: 'Time-Travel Ledger', icon: <History className="w-4 h-4" />, desc: 'Transaction history and historical state' },
    { id: 'orders', label: 'Purchase Orders', icon: <ShoppingCart className="w-4 h-4" />, desc: 'Supplier orders and receiving' },
    { id: 'cyclecount', label: 'Cycle Count', icon: <ClipboardCheck className="w-4 h-4" />, desc: 'Guided floor count and variance' },
    { id: 'reports', label: 'PDF Reports', icon: <FileText className="w-4 h-4" />, desc: 'OpenPDF floor count sheets and CSV' },
    { id: 'conflicts', label: 'Offline Outbox', icon: <RefreshCw className="w-4 h-4" />, desc: 'IndexedDB queue and sync conflicts' },
  ];

  const filteredTabs = tabs.filter(
    (t) =>
      t.label.toLowerCase().includes(query.toLowerCase()) ||
      t.desc.toLowerCase().includes(query.toLowerCase())
  );

  const matchedProducts = query.trim().length > 1
    ? products
        .filter(
          (p) =>
            p.sku.toLowerCase().includes(query.toLowerCase()) ||
            p.name.toLowerCase().includes(query.toLowerCase())
        )
        .slice(0, 5)
    : [];

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center pt-24 p-4 bg-black/60 backdrop-blur-md animate-in fade-in duration-150">
      <div className="w-full max-w-xl glass-panel p-4 shadow-2xl ring-1 ring-white/20">
        {/* Search Input Bar */}
        <div className="relative flex items-center border-b border-slate-200/60 dark:border-slate-800 pb-3">
          <Search className="w-5 h-5 text-indigo-500 ml-2 mr-3 shrink-0" />
          <input
            type="text"
            autoFocus
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search screens, product names, or item codes..."
            className="w-full bg-transparent border-0 outline-none text-base text-slate-800 dark:text-slate-100 placeholder-slate-400 font-medium"
          />
          <button
            onClick={onClose}
            className="p-1.5 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Results Body */}
        <div className="mt-3.5 max-h-80 overflow-y-auto space-y-3.5">
          {/* Screens Navigation */}
          {filteredTabs.length > 0 && (
            <div>
              <div className="text-xs font-bold uppercase tracking-wider text-slate-400 px-2.5 mb-2">
                Navigation Screens
              </div>
              <div className="space-y-1.5">
                {filteredTabs.map((t) => (
                  <button
                    key={t.id}
                    onClick={() => {
                      onSelectTab(t.id);
                      onClose();
                    }}
                    className="w-full p-2.5 rounded-2xl flex items-center justify-between text-left hover:bg-indigo-500/10 dark:hover:bg-indigo-500/20 text-slate-700 dark:text-slate-200 transition-colors group"
                  >
                    <div className="flex items-center gap-3">
                      <div className="p-2 rounded-xl bg-slate-200/60 dark:bg-slate-800/60 text-indigo-600 dark:text-indigo-400">
                        {t.icon}
                      </div>
                      <div>
                        <div className="text-sm font-bold text-slate-900 dark:text-white">
                          {t.label}
                        </div>
                        <div className="text-xs text-slate-400 mt-0.5">{t.desc}</div>
                      </div>
                    </div>
                    <ArrowRight className="w-4 h-4 text-slate-400 opacity-0 group-hover:opacity-100 transition-opacity" />
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Matched Products */}
          {matchedProducts.length > 0 && (
            <div className="pt-2.5 border-t border-slate-200/50 dark:border-slate-800">
              <div className="text-xs font-bold uppercase tracking-wider text-slate-400 px-2.5 mb-2">
                Matching Products
              </div>
              <div className="space-y-1.5">
                {matchedProducts.map((p) => (
                  <button
                    key={p.id}
                    onClick={() => {
                      onSelectTab('inventory');
                      if (onSelectProduct) onSelectProduct(p);
                      onClose();
                    }}
                    className="w-full p-2.5 rounded-2xl flex items-center justify-between text-left hover:bg-emerald-500/10 dark:hover:bg-emerald-500/20 transition-colors"
                  >
                    <div>
                      <div className="text-sm font-bold text-slate-900 dark:text-white">
                        {p.name}
                      </div>
                      <div className="text-xs font-mono font-semibold text-indigo-500 mt-0.5">
                        Code: {p.sku} &bull; Shelf: {p.binLocation}
                      </div>
                    </div>
                    <span className="text-sm font-mono font-bold text-slate-700 dark:text-slate-300">
                      {p.quantity} units
                    </span>
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="mt-3 pt-2.5 border-t border-slate-200/50 dark:border-slate-800 flex items-center justify-between text-[11px] text-slate-400">
          <span>Use &uarr; &darr; to navigate &bull; Esc to close</span>
          <kbd className="px-1.5 py-0.5 rounded bg-slate-200/80 dark:bg-slate-800/80 font-mono text-[10px]">
            Ctrl + K
          </kbd>
        </div>
      </div>
    </div>
  );
};
