import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { Boxes, Lock, User, ArrowRight, ShieldCheck, Sun, Moon, AlertCircle } from 'lucide-react';

const API_BASE = (import.meta as any).env?.VITE_API_BASE || '/api';

export const LoginPage: React.FC = () => {
  const { login } = useAuth();
  const { theme, toggleTheme } = useTheme();

  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const authenticateDemoUser = () => {
    const trimmedUser = username.trim().toLowerCase();
    const trimmedPass = password.trim();

    // Verify credentials for standard demo accounts
    if (trimmedUser === 'admin' && trimmedPass !== 'admin123') {
      setError('Invalid demo password. For admin, use: admin123');
      return;
    }
    if (trimmedUser === 'manager' && trimmedPass !== 'manager123') {
      setError('Invalid demo password. For manager, use: manager123');
      return;
    }
    if (trimmedUser === 'employee' && trimmedPass !== 'emp123') {
      setError('Invalid demo password. For employee, use: emp123');
      return;
    }

    let role: 'ADMIN' | 'MANAGER' | 'EMPLOYEE' = 'EMPLOYEE';
    if (trimmedUser.includes('admin')) role = 'ADMIN';
    else if (trimmedUser.includes('manager')) role = 'MANAGER';

    login('demo-offline-token-' + Date.now(), {
      id: 'user-' + (trimmedUser || 'demo'),
      username: username.trim() || 'Demo User',
      role,
      active: true,
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const response = await fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });

      if (response.ok) {
        const contentType = response.headers.get('content-type') || '';
        if (!contentType.includes('text/html')) {
          const data = await response.json();
          login(data.token, data.user);
          return;
        }
      }

      if (response.status === 401) {
        throw new Error('Invalid username or password');
      } else if (response.status === 423) {
        throw new Error('Account locked due to consecutive failed attempts (5 min cooldown)');
      } else if (response.status === 404 || response.status === 405 || response.status >= 500) {
        // Static hosting environment (Vercel / Netlify) with no backend API running
        authenticateDemoUser();
        return;
      } else {
        throw new Error(`Authentication failed (${response.status})`);
      }
    } catch (err: any) {
      console.warn('Login request failed or backend unavailable:', err);
      if (
        err.message.includes('Failed to fetch') ||
        err.message.includes('NetworkError') ||
        err.message.includes('Load failed') ||
        err.message.includes('Failed to load') ||
        err.message.includes('Unexpected token') ||
        err.message.includes('is not valid JSON')
      ) {
        authenticateDemoUser();
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
            <h1 className="text-3xl font-extrabold tracking-tight bg-gradient-to-r from-indigo-600 to-purple-600 dark:from-indigo-400 dark:to-purple-400 bg-clip-text text-transparent">
              IWMS Pro
            </h1>
            <p className="text-sm text-slate-500 dark:text-slate-400 mt-1 max-w-xs font-medium">
              Inventory & Warehouse Management System
            </p>
          </div>

          {/* Error Message */}
          {error && (
            <div className="mb-6 p-4 rounded-2xl bg-rose-500/10 border border-rose-500/20 text-rose-600 dark:text-rose-400 text-sm font-semibold flex items-center gap-2.5">
              <AlertCircle className="w-5 h-5 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-semibold text-slate-700 dark:text-slate-200 mb-1.5">
                Username
              </label>
              <div className="relative">
                <User className="absolute left-3.5 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                <input
                  type="text"
                  required
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className="glass-input pl-11 text-base"
                  placeholder="admin / manager / employee"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-semibold text-slate-700 dark:text-slate-200 mb-1.5">
                Password
              </label>
              <div className="relative">
                <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="glass-input pl-11 text-base"
                  placeholder="••••••••"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full glass-button-primary mt-3 py-3 text-base font-bold"
            >
              {loading ? (
                <span className="inline-block w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
              ) : (
                <>
                  <span>Sign In</span>
                  <ArrowRight className="w-5 h-5" />
                </>
              )}
            </button>
          </form>

          {/* Quick Demo Credentials */}
          <div className="mt-8 pt-6 border-t border-slate-200/50 dark:border-slate-800/80">
            <div className="flex items-center gap-2 text-sm text-slate-500 dark:text-slate-400 font-semibold mb-3">
              <ShieldCheck className="w-4 h-4 text-indigo-500" />
              <span>Quick Test Roles (Click to auto-fill):</span>
            </div>

            <div className="grid grid-cols-3 gap-2.5">
              <button
                type="button"
                onClick={() => handleQuickDemo('admin', 'admin123')}
                className="px-3 py-2.5 rounded-2xl text-sm font-semibold bg-rose-500/10 hover:bg-rose-500/20 text-rose-700 dark:text-rose-400 border border-rose-500/20 transition-all text-center flex flex-col items-center"
              >
                <span className="font-bold">Administrator</span>
                <span className="text-xs opacity-75 mt-0.5">Full Access</span>
              </button>

              <button
                type="button"
                onClick={() => handleQuickDemo('manager', 'manager123')}
                className="px-3 py-2.5 rounded-2xl text-sm font-semibold bg-blue-500/10 hover:bg-blue-500/20 text-blue-700 dark:text-blue-400 border border-blue-500/20 transition-all text-center flex flex-col items-center"
              >
                <span className="font-bold">Manager</span>
                <span className="text-xs opacity-75 mt-0.5">Warehouse Ops</span>
              </button>

              <button
                type="button"
                onClick={() => handleQuickDemo('employee', 'emp123')}
                className="px-3 py-2.5 rounded-2xl text-sm font-semibold bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-700 dark:text-emerald-400 border border-emerald-500/20 transition-all text-center flex flex-col items-center"
              >
                <span className="font-bold">Staff Member</span>
                <span className="text-xs opacity-75 mt-0.5">Standard View</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
