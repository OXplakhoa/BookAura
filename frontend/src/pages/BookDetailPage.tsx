import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, BookCheck, BookOpen, CalendarDays, CheckCircle2, Hash, Library, LoaderCircle } from "lucide-react";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useAuth } from "../auth/use-auth";
import { borrowBook, getBook } from "../catalog/catalog-api";
import { QueryError, InlineLoading } from "../components/QueryState";
import { toDisplayError, type DisplayError } from "../lib/api-error";
import { useLanguage } from "../i18n/language";

export function BookDetailPage() {
  const { bookId = "" } = useParams();
  const auth = useAuth();
  const queryClient = useQueryClient();
  const { t } = useLanguage();
  const [borrowError, setBorrowError] = useState<DisplayError | null>(null);
  const [borrowed, setBorrowed] = useState(false);
  const book = useQuery({ queryKey: ["book", bookId], queryFn: () => getBook(bookId), enabled: Boolean(bookId), retry: false });
  const borrow = useMutation({
    mutationFn: () => borrowBook(bookId),
    onSuccess: async () => {
      setBorrowed(true);
      setBorrowError(null);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["book", bookId] }),
        queryClient.invalidateQueries({ queryKey: ["books"] }),
        queryClient.invalidateQueries({ queryKey: ["loans"] }),
      ]);
    },
    onError: (error) => setBorrowError(toDisplayError(error)),
  });

  if (book.isPending) return <div className="page-container py-16"><InlineLoading label={t("common.loading")} /></div>;
  if (book.isError) return <div className="page-container py-16"><QueryError message={toDisplayError(book.error).message} retry={() => book.refetch()} /></div>;

  const value = book.data;
  const available = value.availableQuantity > 0;
  const member = auth.user?.roles.includes("USER");

  return (
    <div className="page-container py-10 md:py-16">
      <Link to="/catalog" className="inline-flex min-h-11 items-center gap-2 text-sm font-bold text-primary underline-offset-4 hover:underline"><ArrowLeft size={18} />{t("book.back")}</Link>
      <div className="mt-7 grid gap-10 lg:grid-cols-[minmax(280px,0.72fr)_minmax(0,1.28fr)] lg:gap-16">
        <div>
          <div className="mx-auto flex aspect-[2/3] max-w-sm flex-col bg-primary p-8 text-white shadow-card" aria-hidden="true">
            <BookOpen size={28} className="opacity-80" /><p className="mt-14 font-display text-4xl font-bold leading-tight">{value.title}</p><p className="mt-auto border-t border-white/30 pt-5 text-sm">{value.authors.join(", ") || t("book.collectionName")}</p>
          </div>
        </div>
        <article>
          <div className="flex flex-wrap gap-2">{value.categories.map((category) => <span key={category} className="border border-line bg-surface px-3 py-1.5 text-xs font-bold text-muted">{category}</span>)}</div>
          <h1 className="mt-5 font-display text-5xl font-bold leading-tight tracking-tight sm:text-6xl">{value.title}</h1>
          <p className="mt-4 text-lg font-semibold text-muted">{value.authors.join(", ") || "Author not listed"}</p>
          <dl className="mt-8 grid gap-3 border-y border-line py-6 sm:grid-cols-3">
            <Metadata icon={Hash} label="ISBN" value={value.isbn} />
            <Metadata icon={CalendarDays} label={t("book.published")} value={value.publicationYear?.toString() ?? t("admin.notListed")} />
            <Metadata icon={Library} label={t("book.collection")} value={t("book.availableOf", { available: value.availableQuantity, total: value.totalQuantity })} />
          </dl>
          <section className="mt-8"><h2 className="font-display text-2xl font-bold">{t("book.about")}</h2><p className="mt-3 max-w-3xl whitespace-pre-line leading-8 text-muted">{value.description || t("book.noDescription")}</p></section>

          <div className="mt-9 border border-line bg-surface p-6">
            <div className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
              <div><p className={`inline-flex items-center gap-2 font-bold ${available ? "text-success" : "text-amber-800"}`}><CheckCircle2 size={19} />{available ? t("book.availableNow") : t("book.allBorrowed")}</p><p className="mt-2 text-sm text-muted">{t("book.dueDates")}</p></div>
              {borrowed ? <Link to="/app/loans" className="button border border-success bg-emerald-50 text-success"><BookCheck size={18} />{t("book.viewActiveLoan")}</Link>
                : !auth.ready ? <button type="button" className="button button-primary" disabled><LoaderCircle className="animate-spin motion-reduce:animate-none" size={18} />{t("book.checkingSession")}</button>
                  : !auth.authenticated ? <Link to="/login" state={{ from: `/books/${value.id}` }} className="button button-primary">{t("book.signInToBorrow")}</Link>
                  : member ? <button type="button" className="button button-primary" disabled={!available || borrow.isPending} onClick={() => borrow.mutate()}>{borrow.isPending ? <LoaderCircle className="animate-spin motion-reduce:animate-none" size={18} /> : <BookCheck size={18} />}{borrow.isPending ? t("book.borrowing") : t("book.borrow")}</button>
                    : <p className="max-w-xs text-sm text-muted">{t("book.adminBorrowNote")}</p>}
            </div>
            {borrowError && <p className="mt-4 border-l-4 border-danger bg-red-50 p-3 text-sm font-medium text-red-950" role="alert">{borrowError.message}{borrowError.traceId && <span className="mt-1 block text-xs">Trace: {borrowError.traceId}</span>}</p>}
          </div>
        </article>
      </div>
    </div>
  );
}

function Metadata({ icon: Icon, label, value }: { icon: typeof Hash; label: string; value: string }) {
  return <div className="flex items-start gap-3"><Icon className="mt-0.5 shrink-0 text-primary" size={19} /><div><dt className="text-xs font-bold uppercase tracking-wider text-muted">{label}</dt><dd className="mt-1 break-words text-sm font-semibold tabular-nums">{value}</dd></div></div>;
}
