import { useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Sparkles, Clock, Tag } from "lucide-react";
import {
  INTENSITY_OPTIONS, MOOD_VOCABULARY, emptyAuraSearch, getAuraRecommendations,
  getCategoryNames, hasAuraSignals, readAuraSearch, writeAuraSearch,
  type AuraRecommendation, type AuraSearch,
} from "../aura/aura-api";
import { AuraResultView } from "../aura/AuraResultView";
import { EmptyState, QueryError } from "../components/QueryState";
import { toDisplayError } from "../lib/api-error";
import { useLanguage } from "../i18n/language";

const TIME_MIN = 30;
const TIME_MAX = 600;
const TIME_STEP = 30;

function timeLabel(minutes: number | null, t: ReturnType<typeof useLanguage>["t"]): string {
  if (!minutes) return t("aura.anyLength");
  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  if (hours > 0 && rest > 0) return t("aura.hoursMinutes", { hours, minutes: rest });
  return hours > 0 ? t("aura.hours", { hours }) : t("aura.minutes", { minutes: rest });
}

export function AuraPage() {
  const [params, setParams] = useSearchParams();
  const applied = useMemo(() => readAuraSearch(params), [params]);
  // Draft form state is keyed by the applied URL state, so back/forward resets the form (house rule).
  return <AuraPageInner key={params.toString()} applied={applied} onApply={(next) => setParams(writeAuraSearch(next))} onReset={() => setParams(writeAuraSearch(emptyAuraSearch))} />;
}

function AuraPageInner({ applied, onApply, onReset }: {
  applied: AuraSearch;
  onApply: (next: AuraSearch) => void;
  onReset: () => void;
}) {
  const [draft, setDraft] = useState<AuraSearch>(applied);
  const { t } = useLanguage();
  const categories = useQuery({ queryKey: ["aura-categories"], queryFn: getCategoryNames, staleTime: 300_000 });
  const aura = useQuery({
    queryKey: ["aura", applied],
    queryFn: () => getAuraRecommendations(applied),
    enabled: hasAuraSignals(applied),
  });

  function toggle(list: string[], value: string): string[] {
    return list.includes(value) ? list.filter((item) => item !== value) : [...list, value];
  }

  return (
    <div className="page-container py-12 md:py-16">
      <p className="eyebrow">{t("aura.eyebrow")}</p>
      <h1 className="mt-3 font-display text-4xl font-bold sm:text-5xl">{t("aura.title")}</h1>
      <p className="mt-3 max-w-2xl leading-7 text-muted">{t("aura.description")}</p>

      <section className="mt-9 border border-line bg-surface p-6 md:p-8" aria-label={t("aura.preferences")}>
        <fieldset>
          <legend className="field-label flex items-center gap-2"><Sparkles size={16} /> {t("aura.moodLegend")} <span className="font-normal text-muted">{t("aura.pickAny")}</span></legend>
          <div className="mt-3 flex flex-wrap gap-2">
            {MOOD_VOCABULARY.map((mood) => {
              const active = draft.moods.includes(mood.value);
              return (
                <button key={mood.value} type="button" aria-pressed={active}
                  className={`button min-h-11 border ${active ? "border-primary bg-primary text-white" : "border-line bg-canvas text-ink hover:bg-surface"}`}
                  onClick={() => setDraft({ ...draft, moods: toggle(draft.moods, mood.value) })}>
                  <span aria-hidden="true">{mood.icon}</span> {t(`mood.${mood.value}`)}
                </button>
              );
            })}
          </div>
        </fieldset>

        <div className="mt-7 grid gap-7 md:grid-cols-2">
          <fieldset>
            <legend className="field-label flex items-center gap-2"><Clock size={16} /> {t("aura.timeLegend")}</legend>
            <div className="mt-3 flex items-center gap-4">
              <input type="range" min={TIME_MIN} max={TIME_MAX} step={TIME_STEP}
                value={draft.timeMinutes ?? TIME_MIN} disabled={draft.timeMinutes === null}
                onChange={(event) => setDraft({ ...draft, timeMinutes: Number(event.target.value) })}
                className="h-2 w-full accent-primary disabled:opacity-40"
                aria-label={t("aura.readingTime")} />
              <span className="w-20 text-right text-sm font-bold tabular-nums">{timeLabel(draft.timeMinutes, t)}</span>
            </div>
            <label className="mt-2 flex items-center gap-2 text-sm text-muted">
              <input type="checkbox" checked={draft.timeMinutes === null}
                onChange={(event) => setDraft({ ...draft, timeMinutes: event.target.checked ? null : 120 })} />
              {t("aura.noTimeLimit")}
            </label>
          </fieldset>

          <fieldset>
            <legend className="field-label">{t("aura.depth")}</legend>
            <div className="mt-3 flex flex-wrap gap-2">
              {INTENSITY_OPTIONS.map((option) => {
                const active = draft.intensity === option.value;
                return (
                  <button key={option.value} type="button" aria-pressed={active} title={t(`intensity.${option.value}Hint`)}
                    className={`button min-h-11 border ${active ? "border-primary bg-primary text-white" : "border-line bg-canvas text-ink hover:bg-surface"}`}
                    onClick={() => setDraft({ ...draft, intensity: active ? null : option.value })}>
                    {t(`intensity.${option.value}`)}
                  </button>
                );
              })}
            </div>
          </fieldset>
        </div>

        <fieldset className="mt-7">
          <legend className="field-label flex items-center gap-2"><Tag size={16} /> {t("aura.themes")} <span className="font-normal text-muted">{t("aura.fromCollection")}</span></legend>
          <div className="mt-3 flex flex-wrap gap-2">
            {categories.isPending && <span className="text-sm text-muted">{t("aura.loadingThemes")}</span>}
            {categories.isError && <span className="text-sm text-danger">{t("aura.themeError")}</span>}
            {categories.data?.length === 0 && <span className="text-sm text-muted">{t("aura.emptyShelf")}</span>}
            {categories.data?.map((name) => {
              const active = draft.themes.some((theme) => theme.toLowerCase() === name.toLowerCase());
              return (
                <button key={name} type="button" aria-pressed={active}
                  className={`button min-h-11 border ${active ? "border-primary bg-primary text-white" : "border-line bg-canvas text-ink hover:bg-surface"}`}
                  onClick={() => setDraft({ ...draft, themes: toggle(draft.themes, name) })}>
                  {name}
                </button>
              );
            })}
          </div>
        </fieldset>

        <div className="mt-8 flex flex-wrap items-center gap-3">
          <button type="button" className="button button-primary min-h-12 px-8" disabled={!hasAuraSignals(draft)}
            onClick={() => onApply(draft)}>
            <Sparkles size={17} /> {t("aura.find")}
          </button>
          <button type="button" className="button button-ghost min-h-12" onClick={() => { setDraft(emptyAuraSearch); onReset(); }}>{t("aura.reset")}</button>
          {!hasAuraSignals(draft) && <span className="text-sm text-muted">{t("aura.pickSignal")}</span>}
        </div>
      </section>

      <section className="mt-10" aria-label={t("aura.results")} aria-busy={aura.isFetching}>
        {aura.isError && <QueryError message={toDisplayError(aura.error).message} retry={() => aura.refetch()} />}
        {aura.isPending && hasAuraSignals(applied) && <AuraSkeleton />}
        {aura.data && aura.data.length === 0 && (
          <EmptyState title={t("aura.noMatchTitle")} message={t("aura.noMatchMessage")} />
        )}
        {aura.data && aura.data.length > 0 && (
          <AuraResultView
            books={aura.data}
            search={applied}
            fallback={(
              <ol className="grid gap-5 md:grid-cols-2">
                {aura.data.map((book, index) => <AuraCard key={book.bookId} book={book} rank={index + 1} />)}
              </ol>
            )}
          />
        )}
        {!hasAuraSignals(applied) && (
          <EmptyState title={t("aura.listeningTitle")} message={t("aura.listeningMessage")} />
        )}
      </section>
    </div>
  );
}

function AuraCard({ book, rank }: { book: AuraRecommendation; rank: number }) {
  const { t } = useLanguage();
  return (
    <li className="relative border border-line bg-surface p-6 transition-shadow hover:shadow-card">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-xs font-bold uppercase tracking-wider text-muted">{t("aura.pick", { rank })}</p>
          <Link to={`/books/${book.bookId}`} className="mt-1 block font-display text-2xl font-bold leading-tight hover:text-primary">
            {book.title}
          </Link>
          <p className="mt-1 text-sm text-muted">
            {book.authors.join(", ")} · {book.publicationYear}{book.pageCount ? ` · ${book.pageCount} ${t("aura.pages")}` : ""}
          </p>
        </div>
        <div className="shrink-0 border border-primary/30 bg-primary/5 px-3 py-2 text-center" title={t("aura.relevanceTitle")}>
          <span className="block text-xl font-bold tabular-nums text-primary">{book.score}</span>
          <span className="block text-[10px] font-bold uppercase tracking-wider text-muted">{t("aura.score")}</span>
        </div>
      </div>

      <ScoreBreakdown breakdown={book.breakdown} />

      <ul className="mt-4 space-y-1.5 text-sm leading-6 text-ink">
        {book.reasons.map((reason) => (
          <li key={reason} className="flex gap-2"><Sparkles size={14} className="mt-1.5 shrink-0 text-primary" aria-hidden="true" />{reason}</li>
        ))}
      </ul>

      <div className="mt-4 flex flex-wrap items-center gap-2">
        {book.matchedTags.map((tag) => (
          <span key={tag} className="border border-line bg-canvas px-2.5 py-1 text-xs font-semibold text-muted">{tag}</span>
        ))}
        {book.availableQuantity > 0
          ? <span className="ml-auto text-xs font-bold text-emerald-700">{t("aura.onShelf", { count: book.availableQuantity })}</span>
          : <span className="ml-auto text-xs font-bold text-danger">{t("aura.allCopiesBorrowed")}</span>}
      </div>
    </li>
  );
}

function ScoreBreakdown({ breakdown }: { breakdown: AuraRecommendation["breakdown"] }) {
  const { t } = useLanguage();
  const parts = [
    [t("aura.mood"), breakdown.mood],
    [t("aura.theme"), breakdown.theme],
    [t("aura.time"), breakdown.time],
    [t("aura.pace"), breakdown.intensity],
  ] as const;
  if (parts.every(([, value]) => value === 0)) {
    return (
      <div className="mt-4 border border-line bg-canvas px-3 py-2 text-sm text-muted" aria-label={t("aura.semanticTitle")}>
        <span className="font-bold text-ink">{t("aura.semanticTitle")}</span> {t("aura.semanticText")}
      </div>
    );
  }
  return (
    <div className="mt-4 grid grid-cols-2 border border-line bg-canvas sm:grid-cols-4" aria-label={t("aura.scoreBreakdown")}>
      {parts.map(([label, value]) => (
        <div key={label} className="border-b border-line px-3 py-2 last:border-0 sm:border-b-0 sm:border-r sm:last:border-r-0">
          <span className="block text-[10px] font-bold uppercase tracking-wider text-muted">{label}</span>
          <span className={`block text-sm font-bold tabular-nums ${value < 0 ? "text-danger" : "text-ink"}`}>
            {value > 0 ? `+${value}` : value}
          </span>
        </div>
      ))}
    </div>
  );
}

function AuraSkeleton() {
  const { t } = useLanguage();
  return <div className="grid gap-5 md:grid-cols-2" role="status" aria-label={t("aura.reading")}>{Array.from({ length: 4 }, (_, index) => <div key={index} className="space-y-4 border border-line bg-surface p-6"><div className="h-3 w-24 animate-pulse bg-line motion-reduce:animate-none" /><div className="h-7 w-3/4 animate-pulse bg-line motion-reduce:animate-none" /><div className="h-4 w-full animate-pulse bg-line motion-reduce:animate-none" /><div className="h-4 w-5/6 animate-pulse bg-line motion-reduce:animate-none" /></div>)}</div>;
}
