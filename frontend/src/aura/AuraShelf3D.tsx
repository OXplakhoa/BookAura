import { BookOpen, ChevronLeft, ChevronRight, Sparkles } from "lucide-react";
import { useState, useSyncExternalStore, type CSSProperties, type KeyboardEvent } from "react";
import { useNavigate } from "react-router-dom";
import type { AuraRecommendation, AuraSearch } from "./aura-api";
import { ArcaneShelfCanvas } from "./ArcaneShelfCanvas";
import { resolveArcaneAtmosphere } from "./arcane-atmosphere";

const MOBILE_QUERY = "(max-width: 767px)";

function getMobileQuery(): MediaQueryList | null {
  return typeof window !== "undefined" && typeof window.matchMedia === "function"
    ? window.matchMedia(MOBILE_QUERY)
    : null;
}

function useMobileShelf(): boolean {
  return useSyncExternalStore(
    (onChange) => {
      const query = getMobileQuery();
      query?.addEventListener("change", onChange);
      return () => query?.removeEventListener("change", onChange);
    },
    () => getMobileQuery()?.matches ?? false,
    () => false,
  );
}

export function AuraShelf3D({ books, search }: { books: AuraRecommendation[]; search: AuraSearch }) {
  const rankedBooks = books.slice(0, 6);
  const [activeIndex, setActiveIndex] = useState(0);
  const mobile = useMobileShelf();
  const navigate = useNavigate();
  const atmosphere = resolveArcaneAtmosphere(search);
  const activeBook = rankedBooks[activeIndex] ?? rankedBooks[0];

  function moveSelection(direction: number) {
    setActiveIndex((current) => (current + direction + rankedBooks.length) % rankedBooks.length);
  }

  function handleKeyboard(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === "ArrowRight" || event.key === "ArrowDown") {
      event.preventDefault();
      moveSelection(1);
    } else if (event.key === "ArrowLeft" || event.key === "ArrowUp") {
      event.preventDefault();
      moveSelection(-1);
    } else if (event.key === "Enter" && activeBook) {
      event.preventDefault();
      navigate(`/books/${activeBook.bookId}`);
    }
  }

  if (!activeBook) return null;

  return (
    <section
      className="arcane-opus"
      aria-label="3D Shelf Aura"
      style={{ "--arcane-aura": atmosphere.aura, "--arcane-background": atmosphere.background } as CSSProperties}
    >
      <header className="arcane-opus__header">
        <div>
          <p className="arcane-opus__eyebrow"><Sparkles size={14} aria-hidden="true" /> Shelf Aura · Arcane collection</p>
          <h3>{atmosphere.name}</h3>
          <p className="arcane-opus__incantation">{atmosphere.incantation}</p>
        </div>
        <p className="arcane-opus__instructions">
          <span className="arcane-opus__instruction-mark" aria-hidden="true">✦</span>
          Hover to consult a volume. Click to open it.
        </p>
      </header>

      <div className="arcane-opus__experience">
        <div
          className="arcane-opus__canvas-shell"
          tabIndex={0}
          onKeyDown={handleKeyboard}
          aria-label={`Interactive enchanted shelf. Selected book ${activeIndex + 1} of ${rankedBooks.length}: ${activeBook.title}. Use arrow keys to explore and Enter to open.`}
        >
          <ArcaneShelfCanvas
            books={rankedBooks}
            atmosphere={atmosphere}
            activeIndex={activeIndex}
            mobile={mobile}
            onActive={setActiveIndex}
            onOpen={(bookId) => navigate(`/books/${bookId}`)}
          />
          <div className="arcane-opus__vignette" aria-hidden="true" />
          <span className="arcane-opus__scene-title" aria-hidden="true">BOOKAURA · OPUS VII</span>
        </div>

        <AuraBookReading book={activeBook} rank={activeIndex + 1} />
      </div>

      <footer className="arcane-opus__controls">
        {mobile && (
          <button type="button" className="arcane-opus__arrow" aria-label="Previous recommendation" onClick={() => moveSelection(-1)}>
            <ChevronLeft size={19} aria-hidden="true" />
          </button>
        )}
        <div className="arcane-opus__book-selector" role="group" aria-label="Preview a recommendation">
          {rankedBooks.map((book, index) => (
            <button
              key={book.bookId}
              type="button"
              aria-pressed={index === activeIndex}
              aria-label={`Preview aura pick ${index + 1}: ${book.title}`}
              title={book.title}
              onMouseEnter={() => setActiveIndex(index)}
              onFocus={() => setActiveIndex(index)}
              onClick={() => setActiveIndex(index)}
            >
              <span>0{index + 1}</span>
              <span className="arcane-opus__selector-title">{book.title}</span>
            </button>
          ))}
        </div>
        {mobile && (
          <button type="button" className="arcane-opus__arrow" aria-label="Next recommendation" onClick={() => moveSelection(1)}>
            <ChevronRight size={19} aria-hidden="true" />
          </button>
        )}
      </footer>
    </section>
  );
}

function AuraBookReading({ book, rank }: { book: AuraRecommendation; rank: number }) {
  const breakdown = [
    ["Mood", book.breakdown.mood],
    ["Theme", book.breakdown.theme],
    ["Time", book.breakdown.time],
    ["Pace", book.breakdown.intensity],
  ] as const;

  return (
    <aside className="arcane-reading" aria-label={`Aura reading for ${book.title}`}>
      <div className="arcane-reading__rank">
        <span>Selected volume</span>
        <strong>0{rank}</strong>
      </div>

      <div>
        <p className="arcane-reading__kicker">The shelf has answered</p>
        <h4>{book.title}</h4>
        <p className="arcane-reading__author">{book.authors.join(", ") || "Author not listed"}</p>
      </div>

      <div className="arcane-reading__score-row">
        <div><strong>{book.score}</strong><span>Aura score</span></div>
        <div><strong>{book.availableQuantity}</strong><span>{book.availableQuantity === 1 ? "Copy available" : "Copies available"}</span></div>
        <div><strong>{book.pageCount ?? "—"}</strong><span>Pages</span></div>
      </div>

      <div className="arcane-reading__breakdown" aria-label="Score breakdown">
        {breakdown.map(([label, value]) => (
          <span key={label}><small>{label}</small><b className={value < 0 ? "arcane-reading__negative" : ""}>{value > 0 ? `+${value}` : value}</b></span>
        ))}
      </div>

      {book.matchedTags.length > 0 && (
        <div className="arcane-reading__section">
          <p>Resonant signs</p>
          <div className="arcane-reading__tags">{book.matchedTags.slice(0, 4).map((tag) => <span key={tag}>{tag}</span>)}</div>
        </div>
      )}

      <div className="arcane-reading__section">
        <p>Why this volume appeared</p>
        <ul>
          {book.reasons.slice(0, 2).map((reason) => <li key={reason}><Sparkles size={13} aria-hidden="true" />{reason}</li>)}
        </ul>
      </div>

      <p className="arcane-reading__open-hint"><BookOpen size={16} aria-hidden="true" /> Click the book in the chamber to open its full record.</p>
    </aside>
  );
}
