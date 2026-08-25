import type { CSSProperties } from "react";
import { Link } from "react-router-dom";
import type { AuraRecommendation } from "./aura-api";

const PALETTES = [
  { color: "#0f6b62", shadow: "#0a514b", ink: "#fffdf8", accent: "#e4b86e" },
  { color: "#c9785b", shadow: "#914b3a", ink: "#fffdf8", accent: "#f0c38b" },
  { color: "#31455c", shadow: "#1e2e42", ink: "#fffdf8", accent: "#b8c7d6" },
  { color: "#77735f", shadow: "#514e40", ink: "#fffdf8", accent: "#e8d39e" },
  { color: "#9a6b32", shadow: "#70481f", ink: "#fffdf8", accent: "#f0d194" },
  { color: "#6b4a58", shadow: "#49313d", ink: "#fffdf8", accent: "#e7b5a9" },
] as const;

const BOOK_HEIGHTS = ["88%", "98%", "80%", "94%", "82%", "96%"];
const BOOK_WIDTHS = ["38%", "44%", "34%", "42%", "36%", "43%"];
const BOOK_TILTS = ["-2deg", "1deg", "-1deg", "2deg", "-2deg", "1deg"];

function paletteFor(book: AuraRecommendation, rank: number) {
  const seed = Array.from(`${book.bookId}${book.title}`).reduce((sum, character) => sum + character.charCodeAt(0), rank);
  return PALETTES[seed % PALETTES.length];
}

function bookStyle(book: AuraRecommendation, rank: number): CSSProperties {
  const palette = paletteFor(book, rank);
  return {
    "--book-color": palette.color,
    "--book-shadow": palette.shadow,
    "--book-ink": palette.ink,
    "--book-accent": palette.accent,
    "--book-height": BOOK_HEIGHTS[(rank - 1) % BOOK_HEIGHTS.length],
    "--book-width": BOOK_WIDTHS[(rank - 1) % BOOK_WIDTHS.length],
    "--book-tilt": BOOK_TILTS[(rank - 1) % BOOK_TILTS.length],
  } as CSSProperties;
}

export function AuraShelf3D({ books }: { books: AuraRecommendation[] }) {
  const rankedBooks = books.slice(0, 6);
  const firstShelf = rankedBooks.slice(0, 3);
  const secondShelf = rankedBooks.slice(3, 6);

  return (
    <section className="aura-shelf" aria-label="3D Shelf Aura">
      <div className="aura-shelf-heading">
        <div>
          <p className="eyebrow">A little room on the shelf</p>
          <h3 className="mt-2 font-display text-3xl font-bold">Step inside your recommendations</h3>
        </div>
        <p className="max-w-sm text-sm leading-6 text-muted">Every cover is a ranked match. Choose one to open its full book detail.</p>
      </div>

      <div className="aura-shelf-stage">
        <div className="aura-shelf-scene">
          <div className="aura-shelf-unit">
            <div className="aura-shelf-back" aria-hidden="true" />
            <div className="aura-shelf-glow aura-shelf-glow-one" aria-hidden="true" />
            <div className="aura-shelf-glow aura-shelf-glow-two" aria-hidden="true" />
            <ShelfRow books={firstShelf} startRank={1} position="top" />
            <ShelfRow books={secondShelf} startRank={4} position="bottom" />
            <div className="aura-shelf-board aura-shelf-board-top" aria-hidden="true" />
            <div className="aura-shelf-board aura-shelf-board-bottom" aria-hidden="true" />
            <div className="aura-shelf-rail aura-shelf-rail-left" aria-hidden="true" />
            <div className="aura-shelf-rail aura-shelf-rail-right" aria-hidden="true" />
          </div>
        </div>
      </div>

      <div className="aura-shelf-caption">
        <span className="aura-shelf-caption-mark" aria-hidden="true">✦</span>
        <p><strong>Pick a book.</strong> The list view keeps every reason, score, and matched tag close at hand.</p>
      </div>
    </section>
  );
}

function ShelfRow({ books, startRank, position }: { books: AuraRecommendation[]; startRank: number; position: "top" | "bottom" }) {
  return (
    <ol className={`aura-book-row ${position === "top" ? "aura-book-row-top" : "aura-book-row-bottom"}`} start={startRank} aria-label={`${position === "top" ? "Upper" : "Lower"} shelf recommendations`}>
      {books.map((book, index) => {
        const rank = startRank + index;
        return (
          <li key={book.bookId} className="aura-book-slot" style={bookStyle(book, rank)}>
            <Link
              to={`/books/${book.bookId}`}
              className="aura-book-link"
              aria-label={`Open aura pick ${rank}: ${book.title}. Score ${book.score}.`}
              aria-describedby={`aura-preview-${book.bookId}`}
            >
              <span className="aura-book-cover">
                <span className="aura-book-cover-frame" aria-hidden="true" />
                <span className="aura-book-crest" aria-hidden="true">✦</span>
                <span className="aura-book-title">{book.title}</span>
                <span className="aura-book-author">{book.authors.join(", ") || "BookAura collection"}</span>
                <span className="aura-book-rank" aria-hidden="true">0{rank}</span>
                <span className="aura-book-score" aria-hidden="true">{book.score}</span>
              </span>
              <span id={`aura-preview-${book.bookId}`} className="aura-book-preview" role="tooltip">
                <span className="aura-book-preview-kicker">Aura pick #{rank}</span>
                <strong className="aura-book-preview-title">{book.title}</strong>
                <span className="aura-book-preview-author">{book.authors.join(", ") || "Author not listed"}</span>
                <span className="aura-book-preview-meta">
                  <span><b>{book.score}</b> aura score</span>
                  <span>{book.availableQuantity > 0 ? `${book.availableQuantity} available` : "All copies borrowed"}</span>
                  {book.pageCount && <span>{book.pageCount} pages</span>}
                </span>
                {book.matchedTags.length > 0 && (
                  <span className="aura-book-preview-section">
                    <span className="aura-book-preview-label">Matched signals</span>
                    <span className="aura-book-preview-tags">{book.matchedTags.map((tag) => <span key={tag}>{tag}</span>)}</span>
                  </span>
                )}
                {book.reasons.length > 0 && (
                  <span className="aura-book-preview-section">
                    <span className="aura-book-preview-label">Why it fits</span>
                    <span className="aura-book-preview-reasons">{book.reasons.map((reason) => <span key={reason}>✦ {reason}</span>)}</span>
                  </span>
                )}
              </span>
            </Link>
          </li>
        );
      })}
    </ol>
  );
}
