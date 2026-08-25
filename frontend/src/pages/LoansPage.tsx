import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BookCheck, History, Library, LoaderCircle } from "lucide-react";
import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { ConfirmDialog } from "../components/ConfirmDialog";
import { EmptyState, QueryError } from "../components/QueryState";
import { Pagination } from "../components/Pagination";
import { toDisplayError } from "../lib/api-error";
import { useLanguage } from "../i18n/language";
import { getActiveLoans, getLoanHistory, returnLoan } from "../loans/loan-api";
import { LoanCard } from "../loans/LoanCard";
import type { Loan } from "../loans/loan-types";

export function LoansPage({ mode }: { mode: "active" | "history" }) {
  const [params, setParams] = useSearchParams();
  const { t } = useLanguage();
  const parsedPage = Number.parseInt(params.get("page") ?? "0", 10);
  const page = Number.isFinite(parsedPage) && parsedPage > 0 ? parsedPage : 0;
  const active = mode === "active";
  const [selected, setSelected] = useState<Loan | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const queryClient = useQueryClient();
  const loans = useQuery({
    queryKey: ["loans", mode, page],
    queryFn: () => active ? getActiveLoans(page) : getLoanHistory(page),
    placeholderData: (previous) => previous,
  });
  const returnMutation = useMutation({
    mutationFn: (loanId: string) => returnLoan(loanId),
    onSuccess: async (returned) => {
      setSelected(null);
      setSuccess(t("loans.returnSuccess", { title: returned.bookTitle }));
      if (page > 0 && loans.data?.content.length === 1) {
        setParams(page > 1 ? { page: String(page - 1) } : {});
      }
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["loans"] }),
        queryClient.invalidateQueries({ queryKey: ["book", returned.bookId] }),
        queryClient.invalidateQueries({ queryKey: ["books"] }),
      ]);
    },
  });

  function changePage(nextPage: number) {
    setParams(nextPage > 0 ? { page: String(nextPage) } : {});
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  return (
    <section>
      <div className="flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between">
        <div><p className="eyebrow">{t("loans.eyebrow")}</p><h1 className="mt-3 flex items-center gap-3 font-display text-4xl font-bold">{active ? <Library className="text-primary" size={34} /> : <History className="text-primary" size={34} />}{active ? t("loans.activeTitle") : t("loans.historyTitle")}</h1><p className="mt-3 leading-7 text-muted">{active ? t("loans.activeDescription") : t("loans.historyDescription")}</p></div>
        {loans.data && <p className="text-sm font-semibold tabular-nums text-muted"><span className="text-2xl font-bold text-ink">{loans.data.totalElements}</span> {active ? t("loans.activeCount") : t("loans.returnedCount")}</p>}
      </div>

      {success && <div className="mt-7 flex items-center gap-3 border-l-4 border-success bg-emerald-50 p-4 text-emerald-950" role="status"><BookCheck size={20} /><p className="font-semibold">{success}</p></div>}
      <div className="mt-8 space-y-4" aria-busy={loans.isFetching}>
        {loans.isPending && <div className="flex min-h-52 items-center justify-center gap-3 text-muted" role="status"><LoaderCircle className="animate-spin text-primary motion-reduce:animate-none" />{t("loans.loading")}</div>}
        {loans.isError && <QueryError message={toDisplayError(loans.error).message} retry={() => loans.refetch()} />}
        {loans.data?.content.map((loan) => <LoanCard key={loan.id} loan={loan} active={active} onReturn={active ? setSelected : undefined} />)}
        {loans.data?.content.length === 0 && <div><EmptyState title={active ? t("loans.clearTitle") : t("loans.noHistoryTitle")} message={active ? t("loans.clearMessage") : t("loans.noHistoryMessage")} /><div className="mt-5 text-center"><Link to="/catalog" className="button button-primary">{t("loans.browse")}</Link></div></div>}
      </div>
      {loans.data && <Pagination page={loans.data.page} totalPages={loans.data.totalPages} onPage={changePage} />}

      <ConfirmDialog open={Boolean(selected)} title={t("loans.returnQuestion")} description={selected ? t("loans.returnDescription", { title: selected.bookTitle }) : ""} confirmLabel={t("loans.confirmReturn")} pending={returnMutation.isPending} onCancel={() => { setSelected(null); returnMutation.reset(); }} onConfirm={() => selected && returnMutation.mutate(selected.id)} error={returnMutation.isError ? <p className="mt-4 border-l-4 border-danger bg-red-50 p-3 text-sm text-red-950" role="alert">{toDisplayError(returnMutation.error).message}</p> : undefined} />
    </section>
  );
}
