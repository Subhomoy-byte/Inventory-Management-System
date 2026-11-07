import React, { useState, useEffect } from 'react';
import { ThemeProvider } from './context/ThemeContext';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Navbar } from './components/layout/Navbar';
import { Dock, type NavTab } from './components/layout/Dock';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { InventoryPage } from './pages/InventoryPage';
import { WarehouseMapPage } from './pages/WarehouseMapPage';
import { MovementsPage } from './pages/MovementsPage';
import { OrdersPage } from './pages/OrdersPage';
import { CycleCountPage } from './pages/CycleCountPage';
import { ReportsPage } from './pages/ReportsPage';
import { ConflictReviewPanel } from './components/sync/ConflictReviewPanel';
import { StockMovementModal } from './components/modals/StockMovementModal';
import { CommandPalette } from './components/CommandPalette';
import { OfflineSyncManager } from './storage/sync';
import { api } from './services/api';
import type { Product, MovementType } from './types';

const MainLayout: React.FC = () => {
  const { isAuthenticated } = useAuth();
  const [currentTab, setCurrentTab] = useState<NavTab>('dashboard');
  const [allProducts, setAllProducts] = useState<Product[]>([]);
  const [pendingSyncCount, setPendingSyncCount] = useState<number>(0);
  const [isCommandPaletteOpen, setIsCommandPaletteOpen] = useState<boolean>(false);

  // Movement modal state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalType, setModalType] = useState<MovementType>('IN');
  const [modalProduct, setModalProduct] = useState<Product | null>(null);

  const refreshCatalog = async () => {
    try {
      const data = await api.getProducts().catch(() => OfflineSyncManager.getCachedProducts());
      setAllProducts(data);
    } catch {
      const cached = await OfflineSyncManager.getCachedProducts();
      setAllProducts(cached);
    }
  };

  const updatePendingCount = async () => {
    const count = await OfflineSyncManager.getPendingCount();
    setPendingSyncCount(count);
  };

  useEffect(() => {
    OfflineSyncManager.init();
    updatePendingCount();
    refreshCatalog();

    const handleOutboxUpdate = () => {
      updatePendingCount();
      refreshCatalog();
    };

    window.addEventListener('iwms:outbox-updated', handleOutboxUpdate);
    window.addEventListener('iwms:sync-complete', handleOutboxUpdate);

    return () => {
      window.removeEventListener('iwms:outbox-updated', handleOutboxUpdate);
      window.removeEventListener('iwms:sync-complete', handleOutboxUpdate);
    };
  }, []);

  const handleOpenMovementModal = (type: MovementType, product?: Product) => {
    setModalType(type);
    setModalProduct(product || null);
    setIsModalOpen(true);
  };

  if (!isAuthenticated) {
    return <LoginPage />;
  }

  return (
    <div className="min-h-screen flex flex-col pb-28">
      <Navbar onOpenCommandPalette={() => setIsCommandPaletteOpen(true)} />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 py-6">
        {currentTab === 'dashboard' && (
          <DashboardPage
            onOpenMovementModal={handleOpenMovementModal}
            onNavigateToTab={setCurrentTab}
          />
        )}

        {currentTab === 'inventory' && (
          <InventoryPage onOpenMovementModal={handleOpenMovementModal} />
        )}

        {currentTab === 'warehouse' && (
          <WarehouseMapPage onOpenMovementModal={handleOpenMovementModal} />
        )}

        {currentTab === 'movements' && (
          <MovementsPage />
        )}

        {currentTab === 'orders' && (
          <OrdersPage products={allProducts} onRefreshCatalog={refreshCatalog} />
        )}

        {currentTab === 'cyclecount' && (
          <CycleCountPage products={allProducts} onRefreshCatalog={refreshCatalog} />
        )}

        {currentTab === 'reports' && (
          <ReportsPage products={allProducts} />
        )}

        {currentTab === 'conflicts' && (
          <ConflictReviewPanel />
        )}
      </main>

      <Dock
        currentTab={currentTab}
        onSelectTab={setCurrentTab}
        pendingSyncCount={pendingSyncCount}
      />

      <StockMovementModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        product={modalProduct}
        allProducts={allProducts}
        initialType={modalType}
        onSuccess={refreshCatalog}
      />

      <CommandPalette
        isOpen={isCommandPaletteOpen}
        onClose={() => setIsCommandPaletteOpen(false)}
        onSelectTab={setCurrentTab}
        products={allProducts}
      />
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
