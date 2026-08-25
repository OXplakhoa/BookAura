import { BookOpen } from "lucide-react";
import { Link } from "react-router-dom";
import { useLanguage } from "../i18n/language";

export function Brand({ compact = false, inverted = false }: { compact?: boolean; inverted?: boolean }) {
  const { t } = useLanguage();
  return (
    <Link to="/" className={`inline-flex min-h-11 items-center gap-3 rounded-lg focus:outline-none focus-visible:ring-[3px] focus-visible:ring-primary/30 ${inverted ? "text-stone-100" : "text-ink"}`} aria-label={t("brand.home")}>
      <span className="grid size-10 place-items-center rounded-[10px] bg-primary text-white" aria-hidden="true">
        <BookOpen size={22} strokeWidth={1.8} />
      </span>
      {!compact && (
        <span>
          <span className="block font-display text-xl font-bold leading-none tracking-tight">BookAura</span>
          <span className={`mt-1 block text-[11px] font-semibold uppercase tracking-[0.16em] ${inverted ? "text-stone-400" : "text-muted"}`}>{t("brand.subtitle")}</span>
        </span>
      )}
    </Link>
  );
}
