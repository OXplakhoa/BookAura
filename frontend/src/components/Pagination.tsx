import { ChevronLeft, ChevronRight } from "lucide-react";

export function Pagination({ page, totalPages, onPage }: { page: number; totalPages: number; onPage: (page: number) => void }) {
  if (totalPages <= 1) return null;
  return (
    <nav className="mt-9 flex items-center justify-between border-t border-line pt-6" aria-label="Pagination">
      <button type="button" className="button button-ghost border border-line bg-surface" disabled={page === 0} onClick={() => onPage(page - 1)}><ChevronLeft size={18} />Previous</button>
      <p className="text-sm font-semibold tabular-nums text-muted">Page <span className="text-ink">{page + 1}</span> of {totalPages}</p>
      <button type="button" className="button button-ghost border border-line bg-surface" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>Next<ChevronRight size={18} /></button>
    </nav>
  );
}
