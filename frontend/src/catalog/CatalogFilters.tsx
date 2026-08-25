import { RotateCcw, Search, SlidersHorizontal } from "lucide-react";
import { useState, type FormEvent } from "react";
import type { CatalogSearch } from "./catalog-types";
import { useLanguage } from "../i18n/language";

interface Props {
  search: CatalogSearch;
  onApply: (next: CatalogSearch) => void;
  onReset: () => void;
}

export function CatalogFilters({ search, onApply, onReset }: Props) {
  const [draft, setDraft] = useState(search);
  const { t } = useLanguage();
  const [advanced, setAdvanced] = useState(Boolean(search.author || search.category || search.isbn || search.publicationYear));

  function submit(event: FormEvent) {
    event.preventDefault();
    onApply({ ...draft, page: 0 });
  }

  return (
    <form onSubmit={submit} className="border border-line bg-surface p-5 sm:p-6" role="search">
      <div className="grid gap-4 lg:grid-cols-[1fr_190px_auto]">
        <div>
          <label className="field-label" htmlFor="catalog-title">{t("catalog.filter.title")}</label>
          <div className="relative"><Search className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-muted" size={19} /><input id="catalog-title" className="field-input pl-11" value={draft.title} onChange={(event) => setDraft({ ...draft, title: event.target.value })} placeholder={t("catalog.filter.titlePlaceholder")} /></div>
        </div>
        <div><label className="field-label" htmlFor="availability">{t("catalog.filter.availability")}</label><select id="availability" className="field-input" value={draft.availability} onChange={(event) => setDraft({ ...draft, availability: event.target.value as CatalogSearch["availability"] })}><option value="all">{t("catalog.filter.all")}</option><option value="available">{t("catalog.filter.available")}</option><option value="unavailable">{t("catalog.filter.unavailable")}</option></select></div>
        <button type="submit" className="button button-primary self-end lg:min-h-[46px]"><Search size={18} />{t("common.search")}</button>
      </div>

      <button type="button" className="mt-4 inline-flex min-h-11 items-center gap-2 text-sm font-bold text-primary" onClick={() => setAdvanced((value) => !value)} aria-expanded={advanced} aria-controls="advanced-filters"><SlidersHorizontal size={17} />{advanced ? t("catalog.filter.hide") : t("catalog.filter.more")}</button>
      {advanced && (
        <div id="advanced-filters" className="mt-3 grid gap-4 border-t border-line pt-5 sm:grid-cols-2 lg:grid-cols-4">
          <div><label className="field-label" htmlFor="author">{t("catalog.filter.author")}</label><input id="author" className="field-input" value={draft.author} onChange={(event) => setDraft({ ...draft, author: event.target.value })} /></div>
          <div><label className="field-label" htmlFor="category">{t("catalog.filter.category")}</label><input id="category" className="field-input" value={draft.category} onChange={(event) => setDraft({ ...draft, category: event.target.value })} /></div>
          <div><label className="field-label" htmlFor="isbn">{t("catalog.filter.isbn")}</label><input id="isbn" className="field-input" inputMode="numeric" value={draft.isbn} onChange={(event) => setDraft({ ...draft, isbn: event.target.value })} /></div>
          <div><label className="field-label" htmlFor="publicationYear">{t("catalog.filter.year")}</label><input id="publicationYear" className="field-input" type="number" min="0" max="9999" inputMode="numeric" value={draft.publicationYear} onChange={(event) => setDraft({ ...draft, publicationYear: event.target.value })} /></div>
        </div>
      )}
      <div className="mt-5 flex flex-wrap items-end justify-between gap-4 border-t border-line pt-5">
        <div><label className="field-label" htmlFor="catalog-sort">{t("catalog.filter.sort")}</label><select id="catalog-sort" className="field-input min-w-52" value={draft.sort} onChange={(event) => setDraft({ ...draft, sort: event.target.value as CatalogSearch["sort"] })}><option value="title:asc">{t("catalog.filter.titleAsc")}</option><option value="title:desc">{t("catalog.filter.titleDesc")}</option><option value="publicationYear:desc">{t("catalog.filter.newest")}</option><option value="createdAt:desc">{t("catalog.filter.recent")}</option></select></div>
        <button type="button" className="button button-ghost" onClick={() => { setDraft({ ...search, title: "", author: "", category: "", isbn: "", publicationYear: "", availability: "all", page: 0 }); onReset(); }}><RotateCcw size={17} />{t("catalog.filter.reset")}</button>
      </div>
    </form>
  );
}
