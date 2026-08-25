import { ArrowRight, BookOpen, CheckCircle2, Clock3 } from "lucide-react";
import { Link } from "react-router-dom";
import type { Book } from "./catalog-types";
import { useLanguage } from "../i18n/language";

const coverColors = ["bg-primary", "bg-book-coral", "bg-book-navy", "bg-accent", "bg-book-olive"];

function colorFor(title: string): string {
  return coverColors[Array.from(title).reduce((sum, char) => sum + char.charCodeAt(0), 0) % coverColors.length];
}

export function BookCard({ book }: { book: Book }) {
  const available = book.availableQuantity > 0;
  const { t } = useLanguage();
  return (
    <article className="group grid min-h-full grid-cols-[92px_1fr] gap-5 border border-line bg-surface p-5 transition-[transform,box-shadow] duration-200 hover:-translate-y-1 hover:shadow-card sm:grid-cols-[112px_1fr]">
      <div className={`relative aspect-[2/3] self-start overflow-hidden rounded-r-md ${colorFor(book.title)} p-3 text-white shadow-md`} aria-hidden="true">
        <BookOpen size={18} className="opacity-80" />
        <p className="mt-4 break-words font-display text-sm font-bold leading-4">{book.title}</p>
        <span className="absolute inset-y-0 left-2 w-px bg-white/30" />
      </div>
      <div className="flex min-w-0 flex-col">
        <div className="flex flex-wrap items-start justify-between gap-2">
          <span className={`inline-flex items-center gap-1.5 text-xs font-bold ${available ? "text-success" : "text-amber-800"}`}>
            {available ? <CheckCircle2 size={15} /> : <Clock3 size={15} />}{available ? t("book.available", { count: book.availableQuantity }) : t("book.borrowed")}
          </span>
          {book.publicationYear && <span className="text-xs font-semibold tabular-nums text-muted">{book.publicationYear}</span>}
        </div>
        <h2 className="mt-3 font-display text-xl font-bold leading-6 group-hover:text-primary">{book.title}</h2>
        <p className="mt-2 line-clamp-2 text-sm leading-6 text-muted">{book.authors.length ? book.authors.join(", ") : t("book.authorUnknown")}</p>
        <div className="mt-3 flex flex-wrap gap-1.5">{book.categories.slice(0, 2).map((category) => <span key={category} className="border border-line bg-canvas px-2 py-1 text-[11px] font-semibold text-muted">{category}</span>)}</div>
        <Link to={`/books/${book.id}`} className="mt-auto inline-flex min-h-11 items-center gap-2 pt-4 text-sm font-bold text-primary underline-offset-4 hover:underline">{t("book.viewDetails")} <ArrowRight size={16} /></Link>
      </div>
    </article>
  );
}
