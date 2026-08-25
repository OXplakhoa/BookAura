import { ArrowRight, BookCheck, Clock3, Search, ShieldCheck } from "lucide-react";
import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useLanguage } from "../i18n/language";

const features = [
  { icon: Search, titleKey: "home.feature.searchTitle", textKey: "home.feature.searchText" },
  { icon: BookCheck, titleKey: "home.feature.borrowTitle", textKey: "home.feature.borrowText" },
  { icon: Clock3, titleKey: "home.feature.historyTitle", textKey: "home.feature.historyText" },
];

export function HomePage() {
  const [query, setQuery] = useState("");
  const navigate = useNavigate();
  const { t } = useLanguage();

  function submitSearch(event: FormEvent) {
    event.preventDefault();
    const params = new URLSearchParams();
    if (query.trim()) params.set("title", query.trim());
    navigate(`/catalog${params.size ? `?${params}` : ""}`);
  }

  return (
    <>
      <section className="overflow-hidden border-b border-line bg-surface">
        <div className="page-container grid items-center gap-12 py-16 md:py-24 lg:grid-cols-[1.08fr_0.92fr] lg:py-28">
          <div>
            <p className="eyebrow">{t("home.eyebrow")}</p>
            <h1 className="mt-5 max-w-3xl font-display text-5xl font-bold leading-[1.04] tracking-tight sm:text-6xl lg:text-7xl">{t("home.title")}</h1>
            <p className="mt-7 max-w-2xl text-lg leading-8 text-muted">{t("home.description")}</p>
            <form onSubmit={submitSearch} className="mt-9 flex max-w-2xl flex-col gap-3 rounded-2xl border border-line bg-canvas p-3 shadow-card sm:flex-row" role="search">
              <label htmlFor="hero-search" className="sr-only">{t("home.searchLabel")}</label>
              <div className="flex min-h-12 flex-1 items-center gap-3 px-3">
                <Search className="shrink-0 text-muted" size={21} aria-hidden="true" />
                <input id="hero-search" value={query} onChange={(event) => setQuery(event.target.value)} className="min-w-0 flex-1 bg-transparent text-base outline-none placeholder:text-muted/80" placeholder={t("home.searchPlaceholder")} />
              </div>
              <button className="button button-primary min-h-12" type="submit">{t("home.explore")} <ArrowRight size={18} /></button>
            </form>
            <div className="mt-6 flex flex-wrap gap-x-6 gap-y-3 text-sm font-medium text-muted">
              <span className="inline-flex items-center gap-2"><ShieldCheck size={17} className="text-primary" />{t("home.liveAvailability")}</span>
              <span className="inline-flex items-center gap-2"><ShieldCheck size={17} className="text-primary" />{t("home.clearDueDates")}</span>
              <span className="inline-flex items-center gap-2"><ShieldCheck size={17} className="text-primary" />{t("home.noCheckout")}</span>
            </div>
          </div>

          <div className="relative mx-auto w-full max-w-lg" aria-label="A stylized bookshelf showing available books">
            <div className="border border-line bg-canvas p-6 shadow-card sm:p-9">
              <div className="mb-8 flex items-center justify-between border-b border-line pb-5">
                <div><p className="text-sm font-semibold text-primary">{t("home.readingShelf")}</p><p className="mt-1 font-display text-2xl font-bold">{t("home.ready")}</p></div>
                <span className="border border-primary px-3 py-1 text-xs font-bold uppercase tracking-widest text-primary">{t("home.available")}</span>
              </div>
              <div className="flex h-72 items-end justify-center gap-2 border-b-[10px] border-ink px-3" aria-hidden="true">
                <span className="h-52 w-14 bg-book-coral p-2 text-center text-xs font-bold uppercase leading-4 text-white [writing-mode:vertical-rl]">A Quiet Morning</span>
                <span className="h-64 w-16 bg-primary p-2 text-center text-xs font-bold uppercase leading-4 text-white [writing-mode:vertical-rl]">The Long Way Home</span>
                <span className="h-44 w-12 bg-amber-600 p-2 text-center text-xs font-bold uppercase leading-4 text-white [writing-mode:vertical-rl]">Field Notes</span>
                <span className="h-60 w-16 bg-book-navy p-2 text-center text-xs font-bold uppercase leading-4 text-white [writing-mode:vertical-rl]">Stories of Light</span>
                <span className="h-48 w-12 bg-book-olive p-2 text-center text-xs font-bold uppercase leading-4 text-white [writing-mode:vertical-rl]">North</span>
              </div>
            </div>
            <div className="absolute -bottom-5 -left-4 border border-line bg-surface px-5 py-4 shadow-card sm:-left-10">
              <p className="font-display text-3xl font-bold text-primary">10</p><p className="text-xs font-semibold uppercase tracking-wider text-muted">{t("home.resultsPerPage")}</p>
            </div>
          </div>
        </div>
      </section>

      <section className="page-container py-16 md:py-24">
        <div className="max-w-2xl"><p className="eyebrow">{t("home.featuresEyebrow")}</p><h2 className="mt-4 font-display text-4xl font-bold">{t("home.featuresTitle")}</h2></div>
        <div className="mt-12 grid gap-5 md:grid-cols-3">
          {features.map(({ icon: Icon, titleKey, textKey }, index) => (
            <article key={titleKey} className="feature-card">
              <div className="flex items-center justify-between"><span className="grid size-12 place-items-center rounded-xl bg-primary/10 text-primary"><Icon size={23} /></span><span className="font-display text-3xl text-line">0{index + 1}</span></div>
              <h3 className="mt-8 font-display text-2xl font-bold">{t(titleKey)}</h3><p className="mt-3 leading-7 text-muted">{t(textKey)}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="border-t border-line bg-ink py-16 text-stone-100">
        <div className="page-container flex flex-col gap-8 md:flex-row md:items-center md:justify-between">
          <div><p className="eyebrow text-amber-300">{t("home.ctaEyebrow")}</p><h2 className="mt-3 max-w-xl font-display text-4xl font-bold">{t("home.ctaTitle")}</h2></div>
          <Link to="/register" className="button min-h-12 self-start bg-stone-100 text-ink hover:bg-white">{t("home.cta")} <ArrowRight size={18} /></Link>
        </div>
      </section>
    </>
  );
}
