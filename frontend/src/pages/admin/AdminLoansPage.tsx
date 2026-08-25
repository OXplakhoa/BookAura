import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Library } from "lucide-react";
import { useState } from "react";
import { getAdminLoans, returnLoanAsAdmin } from "../../admin/admin-api";
import { ConfirmDialog } from "../../components/ConfirmDialog";
import { Pagination } from "../../components/Pagination";
import { EmptyState, QueryError } from "../../components/QueryState";
import { toDisplayError } from "../../lib/api-error";
import { LoanCard } from "../../loans/LoanCard";
import type { Loan } from "../../loans/loan-types";
import { useLanguage } from "../../i18n/language";

export function AdminLoansPage() {
  const { t } = useLanguage();
  const [active, setActive] = useState("all");
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<Loan | null>(null);
  const queryClient = useQueryClient();
  const loans = useQuery({ queryKey: ["admin-loans", active, page], queryFn: () => getAdminLoans(active, page), placeholderData: (previous) => previous });
  const returnMutation = useMutation({ mutationFn: returnLoanAsAdmin, onSuccess: async (returned) => { setSelected(null); await Promise.all([queryClient.invalidateQueries({ queryKey: ["admin-loans"] }), queryClient.invalidateQueries({ queryKey: ["loans"] }), queryClient.invalidateQueries({ queryKey: ["book", returned.bookId] }), queryClient.invalidateQueries({ queryKey: ["books"] })]); } });

  return <section><div className="flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between"><div><p className="eyebrow">{t("admin.activity")}</p><h1 className="mt-3 flex items-center gap-3 font-display text-4xl font-bold"><Library className="text-primary" size={34} />{t("admin.loans")}</h1><p className="mt-3 leading-7 text-muted">{t("admin.loanReviewText")}</p></div><div><label className="field-label" htmlFor="loan-status">{t("admin.loanStatus")}</label><select id="loan-status" className="field-input min-w-48" value={active} onChange={(event) => { setActive(event.target.value); setPage(0); }}><option value="all">{t("admin.allLoans")}</option><option value="active">{t("admin.activeOnly")}</option><option value="returned">{t("admin.returnedOnly")}</option></select></div></div>
    <div className="mt-8 space-y-4" aria-busy={loans.isFetching}>{loans.isError && <QueryError message={toDisplayError(loans.error).message} retry={() => loans.refetch()} />}{loans.data?.content.map((loan) => <LoanCard key={loan.id} loan={loan} active={!loan.returnedAt} showMember onReturn={!loan.returnedAt ? setSelected : undefined} />)}{loans.data?.content.length === 0 && <EmptyState title={t("admin.noLoansTitle")} message={t("admin.noLoansMessage")} />}</div>{loans.data && <Pagination page={loans.data.page} totalPages={loans.data.totalPages} onPage={setPage} />}
    <ConfirmDialog open={Boolean(selected)} title={t("admin.returnMemberQuestion")} description={selected ? t("admin.returnMemberDescription", { title: selected.bookTitle, name: selected.memberName ?? "" }) : ""} confirmLabel={t("admin.confirmReturn")} pending={returnMutation.isPending} onCancel={() => { setSelected(null); returnMutation.reset(); }} onConfirm={() => selected && returnMutation.mutate(selected.id)} error={returnMutation.isError ? <p className="mt-4 text-sm font-semibold text-danger" role="alert">{toDisplayError(returnMutation.error).message}</p> : undefined} />
  </section>;
}
