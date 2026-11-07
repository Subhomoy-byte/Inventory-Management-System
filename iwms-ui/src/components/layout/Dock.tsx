import React from 'react';
import {
  LayoutDashboard,
  Boxes,
  MapPin,
  History,
  ShoppingCart,
  ClipboardCheck,
  FileText,
  RefreshCw,
} from 'lucide-react';

export type NavTab =
  | 'dashboard'
  | 'inventory'
  | 'warehouse'
  | 'movements'
  | 'orders'
  | 'cyclecount'
  | 'reports'
  | 'conflicts';

interface DockProps {
  currentTab: NavTab;
  onSelectTab: (tab: NavTab) => void;
  pendingSyncCount?: number;
}

export const Dock: React.FC<DockProps> = ({ currentTab, onSelectTab, pendingSyncCount = 0 }) => {
  const items: { id: NavTab; label: string; icon: React.ReactNode; badge?: number }[] = [
    { id: 'dashboard', label: 'Dashboard', icon: <LayoutDashboard className="w-5 h-5" /> },
    { id: 'inventory', label: 'Inventory', icon: <Boxes className="w-5 h-5" /> },
    { id: 'warehouse', label: 'Warehouse Map', icon: <MapPin className="w-5 h-5" /> },
    { id: 'movements', label: 'Time-Travel', icon: <History className="w-5 h-5" /> },
    { id: 'orders', label: 'Purchase Orders', icon: <ShoppingCart className="w-5 h-5" /> },
    { id: 'cyclecount', label: 'Cycle Count', icon: <ClipboardCheck className="w-5 h-5" /> },
    { id: 'reports', label: 'PDF Reports', icon: <FileText className="w-5 h-5" /> },
    {
      id: 'conflicts',
      label: 'Sync Outbox',
      icon: <RefreshCw className="w-5 h-5" />,
      badge: pendingSyncCount > 0 ? pendingSyncCount : undefined,
    },
  ];

  return (
    <div className="fixed bottom-6 inset-x-0 z-40 flex justify-center pointer-events-none">
      <nav className="glass-dock pointer-events-auto shadow-2xl ring-1 ring-black/5 dark:ring-white/10 px-2 py-1.5 flex items-center gap-1 sm:gap-2">
        {items.map((item) => {
          const isActive = currentTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => onSelectTab(item.id)}
              className={`group relative p-2.5 sm:px-3 sm:py-2 rounded-2xl flex flex-col items-center justify-center transition-all duration-200 ${
                isActive
                  ? 'bg-gradient-to-tr from-indigo-500 to-purple-600 text-white shadow-md shadow-indigo-500/30 scale-105'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-100 hover:bg-white/60 dark:hover:bg-slate-800/60'
              }`}
              title={item.label}
            >
              <div className="relative">
                {item.icon}
                {item.badge !== undefined && (
                  <span className="absolute -top-1.5 -right-2 px-1.5 py-0.2 bg-rose-500 text-white font-mono text-[9px] font-bold rounded-full animate-pulse shadow-sm">
                    {item.badge}
                  </span>
                )}
              </div>
              <span className="text-[10px] font-medium tracking-tight hidden lg:block mt-0.5">
                {item.label}
              </span>

              {/* Tooltip for small screens */}
              <div className="absolute -top-10 px-2 py-1 bg-slate-900/90 text-white text-[11px] font-medium rounded-lg opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none whitespace-nowrap lg:hidden shadow-lg backdrop-blur-sm">
                {item.label}
              </div>
            </button>
          );
        })}
      </nav>
    </div>
  );
};
