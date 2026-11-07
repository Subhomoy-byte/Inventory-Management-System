import React, { useState } from 'react';
import { ThemeProvider } from './context/ThemeContext';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Navbar } from './components/layout/Navbar';
import { Dock, type NavTab } from './components/layout/Dock';
import { LoginPage } from './pages/LoginPage';
import { GlassCard } from './components/common/GlassCard';
import { StatusChip } from './components/common/StatusChip';
import { Boxes, Sparkles, Shield, Cpu, Activity } from 'lucide-react';

const MainLayout: React.FC = () => {
  const { isAuthenticated, user } = useAuth();
  const [currentTab, setCurrentTab] = useState<NavTab>('dashboard');

  if (!isAuthenticated) {
    return <LoginPage />;
  }

  return (
    <div className="min-h-screen flex flex-col pb-28">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 py-6">
        {currentTab === 'dashboard' && (
          <div className="space-y-6">
            {/* Hero Welcome Card */}
            <GlassCard className="relative overflow-hidden border-indigo-500/20 bg-gradient-to-br from-indigo-500/10 via-purple-500/5 to-transparent">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <h2 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
                      Welcome back, {user?.username}
                    </h2>
                    <StatusChip status={user?.role} />
                  </div>
                  <p className="text-sm text-slate-500 dark:text-slate-400">
                    Offline-capable, LAN-accessible Inventory & Warehouse Management System is active.
                  </p>
                </div>

                <div className="flex items-center gap-3">
                  <div className="px-4 py-2 rounded-xl glass-card flex items-center gap-2 text-xs font-medium text-emerald-600 dark:text-emerald-400">
                    <Activity className="w-4 h-4 animate-pulse" />
                    <span>Pure Java Domain & WAL Active</span>
                  </div>
                </div>
              </div>
            </GlassCard>

            {/* Quick KPI Preview Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              <GlassCard hoverEffect>
                <div className="flex items-center justify-between text-slate-500 dark:text-slate-400 mb-2">
                  <span className="text-xs font-semibold uppercase tracking-wider">Catalog SKUs</span>
                  <Boxes className="w-4 h-4 text-indigo-500" />
                </div>
                <div className="text-2xl font-bold text-slate-900 dark:text-white">200+</div>
                <div className="text-xs text-emerald-600 dark:text-emerald-400 mt-1 flex items-center gap-1">
                  <span>Pre-seeded polymorphic catalog</span>
                </div>
              </GlassCard>

              <GlassCard hoverEffect>
                <div className="flex items-center justify-between text-slate-500 dark:text-slate-400 mb-2">
                  <span className="text-xs font-semibold uppercase tracking-wider">Storage Engine</span>
                  <Cpu className="w-4 h-4 text-purple-500" />
                </div>
                <div className="text-2xl font-bold text-slate-900 dark:text-white">WAL + JSON</div>
                <div className="text-xs text-purple-600 dark:text-purple-400 mt-1 flex items-center gap-1">
                  <span>Atomic move & zero SQL DB</span>
                </div>
              </GlassCard>

              <GlassCard hoverEffect>
                <div className="flex items-center justify-between text-slate-500 dark:text-slate-400 mb-2">
                  <span className="text-xs font-semibold uppercase tracking-wider">RBAC Security</span>
                  <Shield className="w-4 h-4 text-emerald-500" />
                </div>
                <div className="text-2xl font-bold text-slate-900 dark:text-white">PBKDF2</div>
                <div className="text-xs text-emerald-600 dark:text-emerald-400 mt-1 flex items-center gap-1">
                  <span>100,000 iterations & lockouts</span>
                </div>
              </GlassCard>

              <GlassCard hoverEffect>
                <div className="flex items-center justify-between text-slate-500 dark:text-slate-400 mb-2">
                  <span className="text-xs font-semibold uppercase tracking-wider">PWA Sync</span>
                  <Sparkles className="w-4 h-4 text-amber-500" />
                </div>
                <div className="text-2xl font-bold text-slate-900 dark:text-white">IndexedDB</div>
                <div className="text-xs text-amber-600 dark:text-amber-400 mt-1 flex items-center gap-1">
                  <span>Offline outbox ready</span>
                </div>
              </GlassCard>
            </div>
          </div>
        )}

        {currentTab !== 'dashboard' && (
          <GlassCard className="py-16 text-center space-y-4">
            <div className="w-16 h-16 mx-auto rounded-2xl bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 flex items-center justify-center">
              <Boxes className="w-8 h-8" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-slate-900 dark:text-white capitalize">
                {currentTab.replace('-', ' ')}
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400 max-w-sm mx-auto mt-1">
                Tab active in shell. Ready for screen implementation.
              </p>
            </div>
          </GlassCard>
        )}
      </main>

      <Dock currentTab={currentTab} onSelectTab={setCurrentTab} />
    </div>
  );
};

export default function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <MainLayout />
      </AuthProvider>
    </ThemeProvider>
  );
}
