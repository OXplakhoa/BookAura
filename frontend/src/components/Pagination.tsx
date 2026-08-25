import { ChevronLeft, ChevronRight } from "lucide-react";
import { useLanguage } from "../i18n/language";

export function Pagination({ page, totalPages, onPage }: { page: number; totalPages: number; onPage: (page: number) => void }) {
  const { t } = useLanguage();
  if (totalPages <= 1) return null;
  return (
    <nav className="mt-9 flex items-center justify-between border-t border-line pt-6" aria-label={t("pagination.label")}>
      <button type="button" className="button button-ghost border border-line bg-surface" disabled={page === 0} onClick={() => onPage(page - 1)}><ChevronLeft size={18} />{t("common.previous")}</button>
      <p className="text-sm font-semibold tabular-nums text-muted">{t("pagination.page")} <span className="text-ink">{page + 1}</span> {t("pagination.of")} {totalPages}</p>
      <button type="button" className="button button-ghost border border-line bg-surface" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>{t("common.next")}<ChevronRight size={18} /></button>
    </nav>
  );
}
