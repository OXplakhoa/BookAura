import { useQuery } from "@tanstack/react-query";
import { useMemo } from "react";
import { useSearchParams } from "react-router-dom";
import { BookCard } from "../catalog/BookCard";
import { CatalogFilters } from "../catalog/CatalogFilters";
import { emptyCatalogSearch, readCatalogSearch, writeCatalogSearch } from "../catalog/catalog-search";
import { searchBooks } from "../catalog/catalog-api";
import { EmptyState, QueryError } from "../components/QueryState";
import { Pagination } from "../components/Pagination";
import { toDisplayError } from "../lib/api-error";

export function CatalogPage() {
  const [params, setParams] = useSearchParams();
  const search = useMemo(() => readCatalogSearch(params), [params]);
  const books = useQuery({
    queryKey: ["books", search],
    queryFn: () => searchBooks(search),
    placeholderData: (previous) => previous,
  });

  function updatePage(page: number) {
    setParams(writeCatalogSearch({ ...search, page }));
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  return (
    <div className="page-container py-12 md:py-16">
      <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div><p className="eyebrow">Public collection</p><h1 className="mt-3 font-display text-4xl font-bold sm:text-5xl">Find your next book</h1><p className="mt-3 max-w-2xl leading-7 text-muted">Search active titles and see inventory before signing in. Results are limited to ten per page for a focused shelf.</p></div>
        {books.data && <p className="text-sm font-semibold tabular-nums text-muted" aria-live="polite"><span className="text-2xl font-bold text-ink">{books.data.totalElements}</span> {books.data.totalElements === 1 ? "book" : "books"}</p>}
      </div>

      <div className="mt-9"><CatalogFilters key={params.toString()} search={search} onApply={(next) => setParams(writeCatalogSearch(next))} onReset={() => setParams(writeCatalogSearch(emptyCatalogSearch))} /></div>

      <section className="mt-9" aria-label="Catalog results" aria-busy={books.isFetching}>
        {books.isError && <QueryError message={toDisplayError(books.error).message} retry={() => books.refetch()} />}
        {books.isPending && <CatalogSkeleton />}
        {books.data && books.data.content.length === 0 && <EmptyState title="No books matched those filters" message="Try a shorter title, remove one filter, or include books that are currently borrowed." />}
        {books.data && books.data.content.length > 0 && (
          <>
            {books.isFetching && !books.isPending && <p className="mb-4 text-sm font-semibold text-primary" role="status">Updating results…</p>}
            <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">{books.data.content.map((book) => <BookCard key={book.id} book={book} />)}</div>
            <Pagination page={books.data.page} totalPages={books.data.totalPages} onPage={updatePage} />
          </>
        )}
      </section>
    </div>
  );
}

function CatalogSkeleton() {
  return <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3" role="status" aria-label="Loading books">{Array.from({ length: 6 }, (_, index) => <div key={index} className="grid min-h-60 grid-cols-[92px_1fr] gap-5 border border-line bg-surface p-5"><div className="animate-pulse bg-line motion-reduce:animate-none" /><div className="space-y-4"><div className="h-3 w-24 animate-pulse bg-line motion-reduce:animate-none" /><div className="h-7 w-full animate-pulse bg-line motion-reduce:animate-none" /><div className="h-4 w-4/5 animate-pulse bg-line motion-reduce:animate-none" /></div></div>)}</div>;
}
