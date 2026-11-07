import React from 'react';
import { cn } from '../../utils/cn';
import type { ProductStatus, MovementType } from '../../types';

interface StatusChipProps {
  status?: ProductStatus | MovementType | string;
  label?: string;
  className?: string;
}

export const StatusChip: React.FC<StatusChipProps> = ({ status, label, className }) => {
  const text = label || status || 'UNKNOWN';

  let colorClasses = 'bg-slate-500/10 text-slate-700 dark:text-slate-300 border-slate-400/20';

  switch (status) {
    case 'OPTIMAL':
      colorClasses = 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-400 border-emerald-500/30';
      break;
    case 'LOW_STOCK':
      colorClasses = 'bg-amber-500/10 text-amber-700 dark:text-amber-400 border-amber-500/30';
      break;
    case 'CRITICAL':
      colorClasses = 'bg-rose-500/10 text-rose-700 dark:text-rose-400 border-rose-500/30 animate-pulse-subtle';
      break;
    case 'OVERSTOCKED':
      colorClasses = 'bg-purple-500/10 text-purple-700 dark:text-purple-400 border-purple-500/30';
      break;
    case 'IN':
      colorClasses = 'bg-teal-500/10 text-teal-700 dark:text-teal-400 border-teal-500/30';
      break;
    case 'OUT':
      colorClasses = 'bg-indigo-500/10 text-indigo-700 dark:text-indigo-400 border-indigo-500/30';
      break;
    case 'TRANSFER':
      colorClasses = 'bg-sky-500/10 text-sky-700 dark:text-sky-400 border-sky-500/30';
      break;
    case 'ADJUST':
      colorClasses = 'bg-violet-500/10 text-violet-700 dark:text-violet-400 border-violet-500/30';
      break;
    case 'ADMIN':
      colorClasses = 'bg-rose-500/10 text-rose-600 dark:text-rose-400 border-rose-500/30';
      break;
    case 'MANAGER':
      colorClasses = 'bg-blue-500/10 text-blue-600 dark:text-blue-400 border-blue-500/30';
      break;
    case 'EMPLOYEE':
      colorClasses = 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/30';
      break;
  }

  return (
    <span
      className={cn(
        'badge-glow font-medium text-xs',
        colorClasses,
        className
      )}
    >
      <span className="w-1.5 h-1.5 rounded-full bg-current opacity-75 mr-1.5 inline-block" />
      {text}
    </span>
  );
};
