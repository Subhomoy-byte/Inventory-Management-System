import React, { useState } from 'react';
import type { Product } from '../types';
import { api } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { GlassCard } from '../components/common/GlassCard';
import {
  FileText,
  FileSpreadsheet,
  Database,
  Printer,
  ShieldCheck,
  CheckCircle,
} from 'lucide-react';

interface ReportsPageProps {
  products: Product[];
}

export const ReportsPage: React.FC<ReportsPageProps> = ({ products }) => {
  const { isEmployee } = useAuth();
  const [selectedCategory, setSelectedCategory] = useState<string>('');
  const [backupLoading, setBackupLoading] = useState(false);
  const [backupMessage, setBackupMessage] = useState<string | null>(null);

  const categories = Array.from(new Set(products.map((p) => p.category))).filter(Boolean);
  const pdfUrl = api.getPdfReportUrl(selectedCategory || undefined);

  const handleDownloadCsv = async () => {
    try {
      const csv = await api.getCsvCatalog();
      const blob = new Blob([csv], { type: 'text/csv' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `inventory_catalog_${new Date().toISOString().slice(0, 10)}.csv`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
    } catch (err: any) {
      alert('CSV Download failed: ' + err.message);
    }
  };

  const handleCreateBackup = async () => {
    setBackupLoading(true);
    setBackupMessage(null);
    try {
      const res = await api.createBackup();
      setBackupMessage(`Backup generated: ${res.backupPath}`);
    } catch (err: any) {
      setBackupMessage(`Backup failed: ${err.message}`);
    } finally {
      setBackupLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight text-slate-900 dark:text-white flex items-center gap-3">
            <FileText className="w-8 h-8 text-indigo-500" />
            <span>Reports & Printable Floor Sheets</span>
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Physical stock audit sheets, spreadsheet exports, and secure database archive backups.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={handleDownloadCsv}
            className="glass-button-secondary text-sm py-2.5 px-4"
          >
            <FileSpreadsheet className="w-4 h-4 text-emerald-500" />
            <span>Download Catalog Spreadsheet</span>
          </button>
          <button
            onClick={handleCreateBackup}
            disabled={backupLoading}
            className="glass-button-secondary text-sm py-2.5 px-4"
          >
            <Database className={`w-4 h-4 text-purple-500 ${backupLoading ? 'animate-spin' : ''}`} />
            <span>Create System Archive Backup</span>
          </button>
        </div>
      </div>

      {/* Backup Notification */}
      {backupMessage && (
        <div className="p-4 rounded-xl bg-purple-500/10 border border-purple-500/30 text-purple-700 dark:text-purple-400 text-sm font-semibold flex items-center justify-between">
          <div className="flex items-center gap-2">
            <CheckCircle className="w-5 h-5 text-purple-500" />
            <span>{backupMessage}</span>
          </div>
          <button onClick={() => setBackupMessage(null)} className="text-sm font-bold hover:underline">
            Dismiss
          </button>
        </div>
      )}

      {/* PDF Controls & Role Redaction Notice */}
      <GlassCard className="p-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <span className="text-sm font-semibold text-slate-700 dark:text-slate-300">
              Filter Report by Category:
            </span>
            <select
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
              className="glass-input text-sm py-2 w-56"
            >
              <option value="" className="bg-slate-900 text-white">All Categories (Full Report)</option>
              {categories.map((c) => (
                <option key={c} value={c} className="bg-slate-900 text-white">
                  {c}
                </option>
              ))}
            </select>
          </div>

          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2 px-3.5 py-1.5 rounded-full text-xs font-semibold bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 border border-indigo-500/20">
              <ShieldCheck className="w-4 h-4" />
              <span>
                {isEmployee ? 'Staff View (Unit Costs Hidden)' : 'Administrator View (Full Financials)'}
              </span>
            </div>

            <a
              href={pdfUrl}
              target="_blank"
              rel="noreferrer"
              className="glass-button-primary text-sm py-2.5 px-4"
            >
              <Printer className="w-4 h-4" />
              <span>Open Printable Report</span>
            </a>
          </div>
        </div>
      </GlassCard>

      {/* In-Browser PDF Preview Frame */}
      <GlassCard className="p-2 overflow-hidden shadow-2xl">
        <div className="w-full bg-slate-900/10 dark:bg-slate-950/40 rounded-xl overflow-hidden">
          <iframe
            src={pdfUrl}
            title="OpenPDF Floor Count Sheet"
            className="w-full h-[650px] border-0 rounded-xl"
          />
        </div>
      </GlassCard>
    </div>
  );
};
