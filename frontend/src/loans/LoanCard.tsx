import { ArrowRight, BookOpen, CalendarClock, CheckCircle2, Hash, RotateCcw } from "lucide-react";
import { Link } from "react-router-dom";
import { formatDate, formatDateTime } from "../lib/date-format";
import type { Loan } from "./loan-types";
import { useLanguage } from "../i18n/language";

export function LoanCard({ loan, active, onReturn, showMember = false }: { loan: Loan; active: boolean; onReturn?: (loan: Loan) => void; showMember?: boolean }) {
  const { language, t } = useLanguage();
  return (
    <article className="grid gap-5 border border-line bg-surface p-5 sm:grid-cols-[76px_1fr_auto] sm:items-center">
      <div className="grid aspect-[2/3] w-[68px] place-items-center rounded-r-md bg-book-navy text-white shadow-md" aria-hidden="true"><BookOpen size={25} /></div>
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-3">
          <span className={`inline-flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider ${loan.overdue ? "text-danger" : active ? "text-primary" : "text-success"}`}><CheckCircle2 size={14} />{loan.overdue ? t("loan.overdue") : active ? t("loan.active") : t("loan.returned")}</span>
          <span className="inline-flex items-center gap-1.5 text-xs tabular-nums text-muted"><Hash size={14} />{loan.isbn}</span>
        </div>
        <h2 className="mt-2 font-display text-xl font-bold">{loan.bookTitle}</h2>
        {showMember && <p className="mt-1 text-sm font-semibold text-primary">{t("loan.member", { name: loan.memberName ?? "" })}</p>}
        <div className="mt-3 flex flex-wrap gap-x-6 gap-y-2 text-sm text-muted">
          <span>{t("loan.borrowed")} <strong className="font-semibold text-ink">{formatDate(loan.borrowedAt, language)}</strong></span>
          {active ? <span className="inline-flex items-center gap-1.5"><CalendarClock size={16} />{t("loan.due")} <strong className={loan.overdue ? "text-danger" : "text-ink"}>{formatDate(loan.dueAt, language)}</strong></span>
            : loan.returnedAt && <span>{t("loan.returnedAt")} <strong className="font-semibold text-ink">{formatDateTime(loan.returnedAt, language)}</strong></span>}
        </div>
      </div>
      <div className="flex flex-wrap gap-2 sm:flex-col sm:items-stretch">
        <Link className="button button-ghost border border-line" to={`/books/${loan.bookId}`}>{t("common.details")} <ArrowRight size={16} /></Link>
        {active && onReturn && <button type="button" className="button button-primary" onClick={() => onReturn(loan)}><RotateCcw size={17} />{t("common.return")}</button>}
      </div>
    </article>
  );
}
