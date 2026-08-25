import { useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Sparkles, Clock, Tag } from "lucide-react";
import {
  INTENSITY_OPTIONS, MOOD_VOCABULARY, emptyAuraSearch, getAuraRecommendations,
  getCategoryNames, hasAuraSignals, readAuraSearch, writeAuraSearch,
  type AuraRecommendation, type AuraSearch,
} from "../aura/aura-api";
import { EmptyState, QueryError } from "../components/QueryState";
import { toDisplayError } from "../lib/api-error";

const TIME_MIN = 30;
const TIME_MAX = 600;
const TIME_STEP = 30;

function timeLabel(minutes: number | null): string {
  if (!minutes) return "Any length";
  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  return hours > 0 ? `${hours}h${rest > 0 ? ` ${rest}m` : ""}` : `${rest}m`;
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
      <p className="eyebrow">Shelf Aura</p>
      <h1 className="mt-3 font-display text-4xl font-bold sm:text-5xl">Find the book your mood is asking for</h1>
      <p className="mt-3 max-w-2xl leading-7 text-muted">
        Tell the shelf how you feel and how long you have. Every suggestion is scored by transparent rules —
        and tells you exactly <em>why</em> it was chosen.
      </p>

      <section className="mt-9 border border-line bg-surface p-6 md:p-8" aria-label="Aura preferences">
        <fieldset>
          <legend className="field-label flex items-center gap-2"><Sparkles size={16} /> How do you feel? <span className="font-normal text-muted">(pick any)</span></legend>
          <div className="mt-3 flex flex-wrap gap-2">
            {MOOD_VOCABULARY.map((mood) => {
              const active = draft.moods.includes(mood.value);
              return (
                <button key={mood.value} type="button" aria-pressed={active}
                  className={`button min-h-11 border ${active ? "border-primary bg-primary text-white" : "border-line bg-canvas text-ink hover:bg-surface"}`}
                  onClick={() => setDraft({ ...draft, moods: toggle(draft.moods, mood.value) })}>
                  <span aria-hidden="true">{mood.icon}</span> {mood.label}
                </button>
              );
            })}
          </div>
        </fieldset>

        <div className="mt-7 grid gap-7 md:grid-cols-2">
          <fieldset>
            <legend className="field-label flex items-center gap-2"><Clock size={16} /> How long do you have?</legend>
            <div className="mt-3 flex items-center gap-4">
              <input type="range" min={TIME_MIN} max={TIME_MAX} step={TIME_STEP}
                value={draft.timeMinutes ?? TIME_MIN} disabled={draft.timeMinutes === null}
                onChange={(event) => setDraft({ ...draft, timeMinutes: Number(event.target.value) })}
                className="h-2 w-full accent-primary disabled:opacity-40"
                aria-label="Reading time in minutes" />
              <span className="w-20 text-right text-sm font-bold tabular-nums">{timeLabel(draft.timeMinutes)}</span>
            </div>
            <label className="mt-2 flex items-center gap-2 text-sm text-muted">
              <input type="checkbox" checked={draft.timeMinutes === null}
                onChange={(event) => setDraft({ ...draft, timeMinutes: event.target.checked ? null : 120 })} />
              No time limit
            </label>
          </fieldset>

          <fieldset>
            <legend className="field-label">Reading depth</legend>
            <div className="mt-3 flex flex-wrap gap-2">
              {INTENSITY_OPTIONS.map((option) => {
                const active = draft.intensity === option.value;
                return (
                  <button key={option.value} type="button" aria-pressed={active} title={option.hint}
                    className={`button min-h-11 border ${active ? "border-primary bg-primary text-white" : "border-line bg-canvas text-ink hover:bg-surface"}`}
                    onClick={() => setDraft({ ...draft, intensity: active ? null : option.value })}>
                    {option.label}
                  </button>
                );
              })}
            </div>
          </fieldset>
        </div>

        <fieldset className="mt-7">
          <legend className="field-label flex items-center gap-2"><Tag size={16} /> Themes <span className="font-normal text-muted">(from the collection)</span></legend>
          <div className="mt-3 flex flex-wrap gap-2">
            {categories.isPending && <span className="text-sm text-muted">Loading themes…</span>}
            {categories.isError && <span className="text-sm text-danger">Could not load themes.</span>}
            {categories.data?.length === 0 && <span className="text-sm text-muted">The shelf is empty — add books first.</span>}
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
            <Sparkles size={17} /> Find my aura
          </button>
          <button type="button" className="button button-ghost min-h-12" onClick={() => { setDraft(emptyAuraSearch); onReset(); }}>Reset</button>
          {!hasAuraSignals(draft) && <span className="text-sm text-muted">Pick at least one mood or theme.</span>}
        </div>
      </section>

      <section className="mt-10" aria-label="Aura results" aria-busy={aura.isFetching}>
        {aura.isError && <QueryError message={toDisplayError(aura.error).message} retry={() => aura.refetch()} />}
        {aura.isPending && hasAuraSignals(applied) && <AuraSkeleton />}
        {aura.data && aura.data.length === 0 && (
          <EmptyState title="No aura answered this combination"
            message="The shelf has no strong match for those signals. Try a broader mood, drop the time limit, or add more books to the collection." />
        )}
        {aura.data && aura.data.length > 0 && (
          <ol className="grid gap-5 md:grid-cols-2">
            {aura.data.map((book, index) => <AuraCard key={book.bookId} book={book} rank={index + 1} />)}
          </ol>
        )}
        {!hasAuraSignals(applied) && (
          <EmptyState title="The shelf is listening"
            message="Choose a mood, a theme, or both — then let the rules find your next read." />
        )}
      </section>
    </div>
  );
}

function AuraCard({ book, rank }: { book: AuraRecommendation; rank: number }) {
  return (
    <li className="relative border border-line bg-surface p-6 transition-shadow hover:shadow-card">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-xs font-bold uppercase tracking-wider text-muted">Aura pick #{rank}</p>
          <Link to={`/books/${book.bookId}`} className="mt-1 block font-display text-2xl font-bold leading-tight hover:text-primary">
            {book.title}
          </Link>
          <p className="mt-1 text-sm text-muted">
            {book.authors.join(", ")} · {book.publicationYear}{book.pageCount ? ` · ${book.pageCount} pages` : ""}
          </p>
        </div>
        <div className="shrink-0 border border-primary/30 bg-primary/5 px-3 py-2 text-center" title="Rule-based aura score">
          <span className="block text-xl font-bold tabular-nums text-primary">{book.score}</span>
          <span className="block text-[10px] font-bold uppercase tracking-wider text-muted">score</span>
        </div>
      </div>

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
          ? <span className="ml-auto text-xs font-bold text-emerald-700">{book.availableQuantity} on shelf</span>
          : <span className="ml-auto text-xs font-bold text-danger">All copies borrowed</span>}
      </div>
    </li>
  );
}

function AuraSkeleton() {
  return <div className="grid gap-5 md:grid-cols-2" role="status" aria-label="Reading your aura">{Array.from({ length: 4 }, (_, index) => <div key={index} className="space-y-4 border border-line bg-surface p-6"><div className="h-3 w-24 animate-pulse bg-line motion-reduce:animate-none" /><div className="h-7 w-3/4 animate-pulse bg-line motion-reduce:animate-none" /><div className="h-4 w-full animate-pulse bg-line motion-reduce:animate-none" /><div className="h-4 w-5/6 animate-pulse bg-line motion-reduce:animate-none" /></div>)}</div>;
}
