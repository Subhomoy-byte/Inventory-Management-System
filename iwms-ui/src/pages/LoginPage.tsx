import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { Boxes, Lock, User, ArrowRight, ShieldCheck, Sun, Moon, AlertCircle } from 'lucide-react';

export const LoginPage: React.FC = () => {
  const { login } = useAuth();
  const { theme, toggleTheme } = useTheme();

  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });

      if (!response.ok) {
        if (response.status === 401) {
          throw new Error('Invalid username or password');
        } else if (response.status === 423) {
          throw new Error('Account locked due to consecutive failed attempts (5 min cooldown)');
        } else {
          throw new Error(`Authentication failed (${response.status})`);
        }
      }

      const data = await response.json();
      login(data.token, data.user);
    } catch (err: any) {
      console.warn('Login request failed:', err);
      // Fallback for offline demo testing if backend is offline
      if (err.message.includes('Failed to fetch') || err.message.includes('NetworkError')) {
        let role: 'ADMIN' | 'MANAGER' | 'EMPLOYEE' = 'EMPLOYEE';
        if (username.toLowerCase().includes('admin')) role = 'ADMIN';
        else if (username.toLowerCase().includes('manager')) role = 'MANAGER';

        login('demo-offline-token-' + Date.now(), {
          id: 'user-' + username,
          username,
          role,
          active: true,
        });
      } else {
        setError(err.message || 'Authentication error');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleQuickDemo = (user: string, pass: string) => {
    setUsername(user);
    setPassword(pass);
    setError(null);
  };

  return (
    <div className="min-h-screen w-full flex items-center justify-center p-4 relative overflow-hidden">
      {/* Background ambient blobs */}
      <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-indigo-500/15 dark:bg-indigo-500/20 rounded-full blur-3xl pointer-events-none -translate-x-1/2 -translate-y-1/2" />
      <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-purple-500/15 dark:bg-purple-500/20 rounded-full blur-3xl pointer-events-none translate-x-1/2 translate-y-1/2" />

      {/* Top right theme toggle */}
      <div className="absolute top-6 right-6">
        <button
          onClick={toggleTheme}
          className="p-2.5 rounded-2xl glass-card text-slate-600 dark:text-slate-300 hover:text-indigo-600 transition-all shadow-sm"
          title="Toggle Theme"
        >
          {theme === 'dark' ? <Sun className="w-5 h-5 text-amber-400" /> : <Moon className="w-5 h-5 text-indigo-600" />}
        </button>
      </div>

      {/* Login Card */}
      <div className="w-full max-w-md z-10">
        <div className="glass-panel p-8 sm:p-10 shadow-2xl ring-1 ring-white/30 dark:ring-white/10">
          {/* Logo & Header */}
          <div className="flex flex-col items-center text-center mb-8">
            <div className="w-16 h-16 rounded-2xl bg-gradient-to-tr from-indigo-600 to-purple-600 flex items-center justify-center text-white shadow-xl shadow-indigo-500/30 mb-4 ring-4 ring-white/20 dark:ring-white/10">
              <Boxes className="w-8 h-8" />
            </div>
            <h1 className="text-2xl font-bold tracking-tight bg-gradient-to-r from-indigo-600 to-purple-600 dark:from-indigo-400 dark:to-purple-400 bg-clip-text text-transparent">
              IWMS Pro
            </h1>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 max-w-xs">
              Inventory & Warehouse Management System
            </p>
          </div>

          {/* Error Message */}
          {error && (
            <div className="mb-6 p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-600 dark:text-rose-400 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1.5">
                Username
              </label>
              <div className="relative">
                <User className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                <input
                  type="text"
                  required
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className="glass-input pl-10"
                  placeholder="admin / manager / employee"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1.5">
                Password
              </label>
              <div className="relative">
                <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="glass-input pl-10"
                  placeholder="••••••••"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full glass-button-primary mt-2"
            >
              {loading ? (
                <span className="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
              ) : (
                <>
                  <span>Sign In</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Quick Demo Credentials */}
          <div className="mt-8 pt-6 border-t border-slate-200/50 dark:border-slate-800/80">
            <div className="flex items-center gap-1.5 text-xs text-slate-500 dark:text-slate-400 font-medium mb-3">
              <ShieldCheck className="w-3.5 h-3.5 text-indigo-500" />
              <span>Quick Demo Roles (Click to fill):</span>
            </div>

            <div className="grid grid-cols-3 gap-2">
              <button
                type="button"
                onClick={() => handleQuickDemo('admin', 'admin123')}
                className="px-2.5 py-2 rounded-xl text-xs font-medium bg-rose-500/10 hover:bg-rose-500/20 text-rose-700 dark:text-rose-400 border border-rose-500/20 transition-all text-center flex flex-col items-center"
              >
                <span className="font-bold">Admin</span>
                <span className="text-[10px] opacity-75">Full Access</span>
              </button>

              <button
                type="button"
                onClick={() => handleQuickDemo('manager', 'manager123')}
                className="px-2.5 py-2 rounded-xl text-xs font-medium bg-blue-500/10 hover:bg-blue-500/20 text-blue-700 dark:text-blue-400 border border-blue-500/20 transition-all text-center flex flex-col items-center"
              >
                <span className="font-bold">Manager</span>
                <span className="text-[10px] opacity-75">Inventory Ops</span>
              </button>

              <button
                type="button"
                onClick={() => handleQuickDemo('employee', 'emp123')}
                className="px-2.5 py-2 rounded-xl text-xs font-medium bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-700 dark:text-emerald-400 border border-emerald-500/20 transition-all text-center flex flex-col items-center"
              >
                <span className="font-bold">Employee</span>
                <span className="text-[10px] opacity-75">Masked Prices</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
