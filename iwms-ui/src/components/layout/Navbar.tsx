import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { StatusChip } from '../common/StatusChip';
import {
  Sun,
  Moon,
  LogOut,
  Wifi,
  WifiOff,
  Boxes,
  Search,
} from 'lucide-react';

interface NavbarProps {
  onOpenCommandPalette?: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ onOpenCommandPalette }) => {
  const { user, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  const [time, setTime] = useState(new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }));

  useEffect(() => {
    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    const timer = setInterval(() => {
      setTime(new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }));
    }, 1000);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
      clearInterval(timer);
    };
  }, []);

  return (
    <header className="sticky top-0 z-30 w-full px-6 py-3.5 backdrop-blur-xl bg-white/60 dark:bg-slate-950/60 border-b border-white/20 dark:border-white/10 transition-colors">
      <div className="max-w-7xl mx-auto flex items-center justify-between">
        {/* Brand & System Title */}
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-600 via-indigo-500 to-purple-600 flex items-center justify-center text-white shadow-lg shadow-indigo-500/25 ring-2 ring-white/20 dark:ring-white/10">
            <Boxes className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-base tracking-tight bg-gradient-to-r from-indigo-600 to-purple-600 dark:from-indigo-400 dark:to-purple-400 bg-clip-text text-transparent">
                IWMS Pro
              </span>
              <span className="text-[10px] px-1.5 py-0.5 rounded font-mono bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 border border-indigo-200/50 dark:border-indigo-800/50">
                v1.0.0
              </span>
            </div>
            <p className="text-xs text-slate-500 dark:text-slate-400 hidden sm:block">
              Inventory & Warehouse Management System
            </p>
          </div>
        </div>

        {/* Global Search Bar Trigger */}
        <button
          onClick={onOpenCommandPalette}
          className="hidden md:flex items-center gap-3 px-4 py-1.5 rounded-xl bg-slate-200/50 dark:bg-slate-800/50 hover:bg-slate-200/80 dark:hover:bg-slate-800/80 border border-slate-300/40 dark:border-slate-700/40 text-slate-500 dark:text-slate-400 text-xs transition-all shadow-inner"
        >
          <Search className="w-3.5 h-3.5" />
          <span>Quick search or command...</span>
          <kbd className="px-1.5 py-0.5 rounded bg-white/80 dark:bg-slate-900/80 font-mono text-[10px] shadow-sm border border-slate-300/50 dark:border-slate-700/50">
            Ctrl + K
          </kbd>
        </button>

        {/* Right Actions: Connection, Clock, Theme, User Role & Logout */}
        <div className="flex items-center gap-3">
          {/* LAN Status Pill */}
          <div
            className={`flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border backdrop-blur-md ${
              isOnline
                ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/30'
                : 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/30 animate-pulse'
            }`}
            title={isOnline ? 'Online (LAN Ready)' : 'Offline (Outbox Queue Active)'}
          >
            {isOnline ? <Wifi className="w-3 h-3" /> : <WifiOff className="w-3 h-3" />}
            <span className="hidden sm:inline">{isOnline ? 'LAN Online' : 'Offline Mode'}</span>
          </div>

          {/* Clock */}
          <span className="text-xs font-mono text-slate-500 dark:text-slate-400 hidden lg:inline">
            {time}
          </span>

          {/* Theme Toggle */}
          <button
            onClick={toggleTheme}
            className="p-2 rounded-xl bg-white/50 dark:bg-slate-800/50 hover:bg-white/80 dark:hover:bg-slate-800/80 border border-slate-200/50 dark:border-slate-700/50 text-slate-600 dark:text-slate-300 transition-all shadow-sm active:scale-95"
            aria-label="Toggle theme"
          >
            {theme === 'dark' ? <Sun className="w-4 h-4 text-amber-400" /> : <Moon className="w-4 h-4 text-indigo-600" />}
          </button>

          {/* Current User & Role */}
          {user && (
            <div className="flex items-center gap-2 pl-2 border-l border-slate-200/60 dark:border-slate-800">
              <div className="hidden sm:flex flex-col items-end">
                <span className="text-xs font-semibold text-slate-800 dark:text-slate-200">
                  {user.username}
                </span>
                <StatusChip status={user.role} />
              </div>
              <button
                onClick={logout}
                className="p-2 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 text-rose-600 dark:text-rose-400 border border-rose-500/20 transition-all shadow-sm active:scale-95"
                title="Log Out"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};
