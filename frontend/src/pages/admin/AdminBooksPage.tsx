import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Archive, BookOpen, FileUp, LoaderCircle, Pencil, Plus, Search } from "lucide-react";
import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { deleteBook, importBooks, searchAdminBooks } from "../../admin/admin-api";
import type { Book } from "../../catalog/catalog-types";
import { ConfirmDialog } from "../../components/ConfirmDialog";
import { Pagination } from "../../components/Pagination";
import { EmptyState, QueryError } from "../../components/QueryState";
import { toDisplayError } from "../../lib/api-error";
import { useLanguage } from "../../i18n/language";

const MAX_CSV_BYTES = 5 * 1024 * 1024;

export function AdminBooksPage() {
  const { t } = useLanguage();
  const [titleDraft, setTitleDraft] = useState("");
  const [title, setTitle] = useState("");
  const [active, setActive] = useState("all");
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<Book | null>(null);
  const [file, setFile] = useState<File | null>(null);
  const [fileError, setFileError] = useState<string | null>(null);
  const queryClient = useQueryClient();
  const books = useQuery({ queryKey: ["admin-books", title, active, page], queryFn: () => searchAdminBooks(title, active, page), placeholderData: (previous) => previous });
  const archive = useMutation({ mutationFn: deleteBook, onSuccess: async () => { setSelected(null); await Promise.all([queryClient.invalidateQueries({ queryKey: ["admin-books"] }), queryClient.invalidateQueries({ queryKey: ["books"] })]); } });
  const csvImport = useMutation({ mutationFn: importBooks, onSuccess: async () => { setFile(null); setFileError(null); await Promise.all([queryClient.invalidateQueries({ queryKey: ["admin-books"] }), queryClient.invalidateQueries({ queryKey: ["books"] })]); } });
  const csvError = csvImport.isError ? toDisplayError(csvImport.error) : null;

  function search(event: FormEvent) { event.preventDefault(); setPage(0); setTitle(titleDraft.trim()); }
  function chooseFile(next: File | null) {
    csvImport.reset();
    if (!next) { setFile(null); setFileError(null); return; }
    if (!next.name.toLowerCase().endsWith(".csv")) { setFile(null); setFileError(t("admin.fileCsv")); return; }
    if (next.size >= MAX_CSV_BYTES) { setFile(null); setFileError(t("admin.fileSize")); return; }
    setFile(next); setFileError(null);
  }

  return <section><div className="flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between"><div><p className="eyebrow">{t("admin.catalog")}</p><h1 className="mt-3 flex items-center gap-3 font-display text-4xl font-bold"><BookOpen className="text-primary" size={34} />{t("admin.books")}</h1><p className="mt-3 leading-7 text-muted">{t("admin.manageInventory")}</p></div><Link to="/admin/books/new" className="button button-primary"><Plus size={18} />{t("admin.addBook")}</Link></div>
    <form onSubmit={search} className="mt-8 grid gap-4 border border-line bg-surface p-5 sm:grid-cols-[1fr_180px_auto]"><div><label className="field-label" htmlFor="admin-book-title">{t("admin.title")}</label><input id="admin-book-title" className="field-input" value={titleDraft} onChange={(event) => setTitleDraft(event.target.value)} /></div><div><label className="field-label" htmlFor="admin-book-active">{t("admin.status")}</label><select id="admin-book-active" className="field-input" value={active} onChange={(event) => { setActive(event.target.value); setPage(0); }}><option value="all">{t("admin.allStatuses")}</option><option value="active">{t("admin.active")}</option><option value="inactive">{t("admin.archived")}</option></select></div><button type="submit" className="button button-primary self-end"><Search size={17} />{t("common.search")}</button></form>

    <div className="mt-6 border border-line bg-surface p-5"><div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between"><div><h2 className="font-display text-xl font-bold">{t("admin.transactionalImport")}</h2><p className="mt-1 text-sm leading-6 text-muted">{t("admin.csvHelp")}</p></div><div className="flex flex-col gap-3 sm:flex-row sm:items-center"><label className="button button-ghost cursor-pointer border border-line"><FileUp size={17} />{t("admin.chooseCsv")}<input type="file" accept=".csv,text/csv" className="sr-only" onChange={(event) => chooseFile(event.target.files?.[0] ?? null)} /></label><span className="max-w-56 truncate text-sm text-muted">{file?.name ?? t("admin.noFile")}</span><button type="button" className="button button-primary" disabled={!file || csvImport.isPending} onClick={() => file && csvImport.mutate(file)}>{csvImport.isPending && <LoaderCircle className="animate-spin" size={17} />}{t("admin.import")}</button></div></div>{fileError && <p className="mt-3 text-sm font-semibold text-danger" role="alert">{fileError}</p>}{csvImport.isSuccess && <p className="mt-3 text-sm font-semibold text-success" role="status">{t("admin.imported", { count: csvImport.data.importedCount })}</p>}{csvError && <div className="mt-3 text-sm text-danger" role="alert"><p className="font-semibold">{csvError.message}</p>{Object.keys(csvError.fields).length > 0 && <ul className="mt-2 list-disc space-y-1 pl-5">{Object.entries(csvError.fields).map(([row, message]) => <li key={row}><strong>{row}:</strong> {message}</li>)}</ul>}</div>}</div>

    <div className="mt-7 space-y-3" aria-busy={books.isFetching}>{books.isError && <QueryError message={toDisplayError(books.error).message} retry={() => books.refetch()} />}{books.data?.content.map((book) => <article key={book.id} className="flex flex-col gap-4 border border-line bg-surface p-5 md:flex-row md:items-center"><div className="grid size-12 shrink-0 place-items-center rounded-xl bg-primary/10 text-primary"><BookOpen size={22} /></div><div className="min-w-0 flex-1"><div className="flex flex-wrap items-center gap-2"><h2 className="font-display text-xl font-bold">{book.title}</h2><span className={`text-xs font-bold uppercase tracking-wider ${book.active ? "text-success" : "text-danger"}`}>{book.active ? t("admin.activeLabel") : t("admin.archivedLabel")}</span></div><p className="mt-1 text-sm text-muted">{book.authors.join(", ")} · ISBN {book.isbn}</p><p className="mt-2 text-sm tabular-nums">{t("admin.availableTotal", { available: book.availableQuantity, total: book.totalQuantity })}</p></div><div className="flex gap-2"><Link to={`/admin/books/${book.id}/edit`} className="button button-ghost border border-line"><Pencil size={16} />{t("common.edit")}</Link>{book.active && <button type="button" className="button border border-red-200 text-danger hover:bg-red-50" onClick={() => setSelected(book)}><Archive size={16} />{t("common.archive")}</button>}</div></article>)}{books.data?.content.length === 0 && <EmptyState title={t("admin.noBooksTitle")} message={t("admin.noBooksMessage")} />}</div>
    {books.data && <Pagination page={books.data.page} totalPages={books.data.totalPages} onPage={setPage} />}
    <ConfirmDialog open={Boolean(selected)} title={t("admin.archiveQuestion")} description={selected ? t("admin.archiveDescription", { title: selected.title }) : ""} confirmLabel={t("admin.archiveConfirm")} pending={archive.isPending} onCancel={() => { setSelected(null); archive.reset(); }} onConfirm={() => selected && archive.mutate(selected.id)} error={archive.isError ? <p className="mt-4 text-sm font-semibold text-danger" role="alert">{toDisplayError(archive.error).message}</p> : undefined} />
  </section>;
}
