import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, LoaderCircle, Save } from "lucide-react";
import { cloneElement, useEffect, useState, type ReactElement } from "react";
import { useForm } from "react-hook-form";
import { Link, useNavigate, useParams } from "react-router-dom";
import { z } from "zod";
import { createBook, getAdminBook, updateBook, type BookInput } from "../../admin/admin-api";
import { ApiErrorBanner } from "../../components/ApiErrorBanner";
import { FieldError } from "../../components/FieldError";
import { InlineLoading, QueryError } from "../../components/QueryState";
import { isValidIsbn } from "../../catalog/isbn";
import { toDisplayError, type DisplayError } from "../../lib/api-error";

const currentYear = new Date().getFullYear();
const schema = z.object({
  title: z.string().trim().min(1, "Title is required").max(255),
  isbn: z.string().trim().refine(isValidIsbn, "Enter a valid ISBN-10 or ISBN-13 checksum"),
  publicationYear: z.number().int().min(1450).max(2100),
  totalQuantity: z.number().int().min(0).max(100000),
  authorsText: z.string().trim().min(1, "Add at least one author"),
  categoriesText: z.string().trim().min(1, "Add at least one category"),
  pageCount: z.union([z.number().int().min(1).max(20000), z.nan()]).optional(),
  tagsText: z.string().trim().max(600),
  description: z.string().max(4000),
  active: z.boolean(),
});
type BookForm = z.infer<typeof schema>;

export function BookFormPage() {
  const { bookId } = useParams();
  const editing = Boolean(bookId);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [serverError, setServerError] = useState<DisplayError | null>(null);
  const detail = useQuery({ queryKey: ["admin-book", bookId], queryFn: () => getAdminBook(bookId!), enabled: editing, retry: false });
  const { register, handleSubmit, reset, setError, formState: { errors } } = useForm<BookForm>({ resolver: zodResolver(schema), mode: "onBlur", defaultValues: { publicationYear: currentYear, totalQuantity: 1, description: "", active: true, authorsText: "", categoriesText: "", title: "", isbn: "", pageCount: undefined, tagsText: "" } });
  useEffect(() => { if (detail.data) reset({ title: detail.data.title, isbn: detail.data.isbn, publicationYear: detail.data.publicationYear ?? currentYear, totalQuantity: detail.data.totalQuantity, authorsText: detail.data.authors.join(", "), categoriesText: detail.data.categories.join(", "), pageCount: detail.data.pageCount ?? undefined, tagsText: detail.data.tags.join(", "), description: detail.data.description ?? "", active: detail.data.active }); }, [detail.data, reset]);
  const save = useMutation({
    mutationFn: (input: BookInput) => editing ? updateBook(bookId!, input) : createBook(input),
    onSuccess: async () => { await Promise.all([queryClient.invalidateQueries({ queryKey: ["admin-books"] }), queryClient.invalidateQueries({ queryKey: ["books"] })]); navigate("/admin/books", { replace: true }); },
    onError: (error) => {
      const display = toDisplayError(error); setServerError(display);
      Object.entries(display.fields).forEach(([field, message]) => { const mapped = field === "authors" ? "authorsText" : field === "categories" ? "categoriesText" : field === "tags" ? "tagsText" : field; if (mapped in schema.shape) setError(mapped as keyof BookForm, { message }); });
    },
  });
  function submit(values: BookForm) {
    setServerError(null);
    const split = (value: string) => value.split(/[,|]/).map((item) => item.trim()).filter(Boolean);
    save.mutate({ title: values.title, isbn: values.isbn, publicationYear: values.publicationYear, totalQuantity: values.totalQuantity, authors: split(values.authorsText), categories: split(values.categoriesText), pageCount: values.pageCount && !Number.isNaN(values.pageCount) ? values.pageCount : null, tags: split(values.tagsText), description: values.description || undefined, active: values.active });
  }

  if (editing && detail.isPending) return <InlineLoading label="Loading book" />;
  if (detail.isError) return <QueryError message={toDisplayError(detail.error).message} retry={() => detail.refetch()} />;
  return <section><Link to="/admin/books" className="inline-flex min-h-11 items-center gap-2 text-sm font-bold text-primary"><ArrowLeft size={17} />Back to books</Link><p className="eyebrow mt-5">Administration · Catalog</p><h1 className="mt-3 font-display text-4xl font-bold">{editing ? "Edit book" : "Add a book"}</h1><p className="mt-3 text-muted">Inventory updates preserve the number of copies currently on loan.</p><div className="mt-7 max-w-3xl"><ApiErrorBanner error={serverError} /></div>
    <form onSubmit={handleSubmit(submit)} noValidate className="mt-7 max-w-3xl space-y-5 border border-line bg-surface p-6 sm:p-8"><div className="grid gap-5 sm:grid-cols-2"><AdminField id="title" label="Title" error={errors.title?.message} input={<input id="title" className="field-input" {...register("title")} />} /><AdminField id="isbn" label="ISBN" helper="ISBN-10 or ISBN-13" error={errors.isbn?.message} input={<input id="isbn" className="field-input" {...register("isbn")} />} /><AdminField id="publicationYear" label="Publication year" error={errors.publicationYear?.message} input={<input id="publicationYear" type="number" min="1450" max="2100" className="field-input" {...register("publicationYear", { valueAsNumber: true })} />} /><AdminField id="totalQuantity" label="Total copies" error={errors.totalQuantity?.message} input={<input id="totalQuantity" type="number" min="0" max="100000" className="field-input" {...register("totalQuantity", { valueAsNumber: true })} />} /><AdminField id="authorsText" label="Authors" helper="Comma-separated" error={errors.authorsText?.message} input={<input id="authorsText" className="field-input" {...register("authorsText")} />} /><AdminField id="categoriesText" label="Categories" helper="Comma-separated" error={errors.categoriesText?.message} input={<input id="categoriesText" className="field-input" {...register("categoriesText")} />} /><AdminField id="pageCount" label="Page count" helper="Optional — powers Shelf Aura time matching" error={errors.pageCount?.message} input={<input id="pageCount" type="number" min="1" max="20000" className="field-input" {...register("pageCount", { valueAsNumber: true })} />} /><AdminField id="tagsText" label="Aura tags" helper="Comma-separated vibes, e.g. cozy, slow-burn, satire" error={errors.tagsText?.message} input={<input id="tagsText" className="field-input" {...register("tagsText")} />} /></div><div><label className="field-label" htmlFor="description">Description</label><textarea id="description" rows={6} className="field-input py-3" aria-invalid={Boolean(errors.description)} aria-describedby={errors.description ? "description-error" : undefined} {...register("description")} /><FieldError id="description-error" message={errors.description?.message} /></div><label className="flex min-h-11 cursor-pointer items-center gap-3 text-sm font-semibold"><input type="checkbox" className="size-5 accent-primary" {...register("active")} />Active in public catalog</label><div className="flex justify-end gap-3 border-t border-line pt-6"><Link to="/admin/books" className="button button-ghost border border-line">Cancel</Link><button className="button button-primary" type="submit" disabled={save.isPending}>{save.isPending ? <LoaderCircle className="animate-spin" size={17} /> : <Save size={17} />}{save.isPending ? "Saving…" : "Save book"}</button></div></form>
  </section>;
}

function AdminField({ id, label, helper, error, input }: { id: string; label: string; helper?: string; error?: string; input: ReactElement<{ "aria-invalid"?: boolean; "aria-describedby"?: string }> }) {
  const errorId = `${id}-error`;
  const accessibleInput = cloneElement(input, {
    "aria-invalid": Boolean(error),
    "aria-describedby": error ? errorId : undefined,
  });
  return <div><div className="flex items-center justify-between"><label className="field-label" htmlFor={id}>{label}</label>{helper && <span className="text-xs text-muted">{helper}</span>}</div>{accessibleInput}<FieldError id={errorId} message={error} /></div>;
}
