import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Ban, Pencil, Plus, Search, UserCheck, Users } from "lucide-react";
import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { disableMember, searchMembers } from "../../admin/admin-api";
import { toBackendMemberDate } from "../../admin/member-search";
import type { Member } from "../../admin/member-types";
import { ConfirmDialog } from "../../components/ConfirmDialog";
import { Pagination } from "../../components/Pagination";
import { EmptyState, QueryError } from "../../components/QueryState";
import { formatDate } from "../../lib/date-format";
import { toDisplayError } from "../../lib/api-error";
import { useLanguage } from "../../i18n/language";

interface MemberFilters {
  name: string; emailOrPhone: string; borrowedBookTitle: string; status: string;
  dateOfBirthFrom: string; dateOfBirthTo: string; role: string; emailVerified: string;
}
const emptyFilters: MemberFilters = { name: "", emailOrPhone: "", borrowedBookTitle: "", status: "", dateOfBirthFrom: "", dateOfBirthTo: "", role: "", emailVerified: "" };

export function AdminMembersPage() {
  const { language, t } = useLanguage();
  const [draft, setDraft] = useState(emptyFilters);
  const [filters, setFilters] = useState(emptyFilters);
  const [advanced, setAdvanced] = useState(false);
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<Member | null>(null);
  const queryClient = useQueryClient();
  const queryParams = {
    ...filters,
    dateOfBirthFrom: toBackendMemberDate(filters.dateOfBirthFrom),
    dateOfBirthTo: toBackendMemberDate(filters.dateOfBirthTo),
    emailVerified: filters.emailVerified ? filters.emailVerified === "true" : undefined,
    status: filters.status || undefined, role: filters.role || undefined, page, sort: "fullName:asc",
  };
  const members = useQuery({ queryKey: ["admin-members", queryParams], queryFn: () => searchMembers(queryParams), placeholderData: (previous) => previous });
  const disable = useMutation({ mutationFn: disableMember, onSuccess: async () => { setSelected(null); await queryClient.invalidateQueries({ queryKey: ["admin-members"] }); } });
  function submit(event: FormEvent) { event.preventDefault(); setPage(0); setFilters(draft); }
  function update<K extends keyof MemberFilters>(key: K, value: MemberFilters[K]) { setDraft((current) => ({ ...current, [key]: value })); }

  return <section><div className="flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between"><div><p className="eyebrow">{t("admin.accounts")}</p><h1 className="mt-3 flex items-center gap-3 font-display text-4xl font-bold"><Users className="text-primary" size={34} />{t("admin.members")}</h1><p className="mt-3 leading-7 text-muted">{t("admin.memberSearchText")}</p></div><Link to="/admin/members/new" className="button button-primary"><Plus size={18} />{t("admin.addMember")}</Link></div>
    <form onSubmit={submit} className="mt-8 border border-line bg-surface p-5"><div className="grid gap-4 md:grid-cols-3"><Filter id="member-name" label={t("admin.name")}  value={draft.name} onChange={(value) => update("name", value)} /><Filter id="member-identity" label={t("admin.emailPhone")}  value={draft.emailOrPhone} onChange={(value) => update("emailOrPhone", value)} /><Filter id="borrowed-title" label={t("admin.borrowedTitle")}  value={draft.borrowedBookTitle} onChange={(value) => update("borrowedBookTitle", value)} /></div><button type="button" className="mt-4 min-h-11 text-sm font-bold text-primary" onClick={() => setAdvanced((value) => !value)} aria-expanded={advanced}>{advanced ? t("admin.advancedHide") : t("admin.advancedShow")}</button>{advanced && <div className="grid gap-4 border-t border-line pt-5 sm:grid-cols-2 lg:grid-cols-5"><Filter id="dob-from" label={t("admin.bornFrom")}  type="date" value={draft.dateOfBirthFrom} onChange={(value) => update("dateOfBirthFrom", value)} /><Filter id="dob-to" label={t("admin.bornTo")}  type="date" value={draft.dateOfBirthTo} onChange={(value) => update("dateOfBirthTo", value)} /><SelectFilter id="member-status" label={t("admin.status")} value={draft.status} onChange={(value) => update("status", value)} options={[["", t("admin.any")], ["ACTIVE", t("admin.active")], ["DISABLED", t("admin.disabled")]]} /><SelectFilter id="member-role" label={t("admin.role")}  value={draft.role} onChange={(value) => update("role", value)} options={[["", t("admin.any")], ["USER", "USER"], ["ADMIN", "ADMIN"]]} /><SelectFilter id="verified" label={t("admin.emailVerified")}  value={draft.emailVerified} onChange={(value) => update("emailVerified", value)} options={[["", t("admin.any")], ["true", t("admin.verified")], ["false", t("admin.unverified")]]} /></div>}<div className="mt-5 flex flex-wrap justify-end gap-3 border-t border-line pt-5"><button type="button" className="button button-ghost" onClick={() => { setDraft(emptyFilters); setFilters(emptyFilters); setPage(0); }}>{t("admin.reset")}</button><button type="submit" className="button button-primary"><Search size={17} />{t("admin.searchMembers")}</button></div></form>

    <div className="mt-7 space-y-3" aria-busy={members.isFetching}>{members.isError && <QueryError message={toDisplayError(members.error).message} retry={() => members.refetch()} />}{members.data?.content.map((member) => <article key={member.id} className="flex flex-col gap-4 border border-line bg-surface p-5 lg:flex-row lg:items-center"><span className="grid size-12 shrink-0 place-items-center rounded-full bg-primary/10 font-display text-lg font-bold text-primary" aria-hidden="true">{member.fullName.slice(0, 2).toUpperCase()}</span><div className="min-w-0 flex-1"><div className="flex flex-wrap items-center gap-2"><h2 className="font-display text-xl font-bold">{member.fullName}</h2><span className={`text-xs font-bold uppercase tracking-wider ${member.status === "ACTIVE" ? "text-success" : "text-danger"}`}>{member.status === "ACTIVE" ? t("admin.active") : t("admin.disabled")}</span>{member.emailVerified && <span className="inline-flex items-center gap-1 text-xs font-semibold text-primary"><UserCheck size={14} />{t("admin.verified")}</span>}</div><p className="mt-1 break-all text-sm text-muted">{member.email}{member.phone ? ` · ${member.phone}` : ""}</p><p className="mt-2 text-xs text-muted">{t("admin.born")} {member.dateOfBirth ? formatDate(`${member.dateOfBirth}T00:00:00Z`, language) : t("admin.notListed")} · {member.roles.join(", ")}</p></div><div className="flex gap-2"><Link to={`/admin/members/${member.id}/edit`} className="button button-ghost border border-line"><Pencil size={16} />{t("common.edit")}</Link>{member.status === "ACTIVE" && <button type="button" className="button border border-red-200 text-danger hover:bg-red-50" onClick={() => setSelected(member)}><Ban size={16} />{t("common.disable")}</button>}</div></article>)}{members.data?.content.length === 0 && <EmptyState title={t("admin.noMembersTitle")} message={t("admin.noMembersMessage")} />}</div>{members.data && <Pagination page={members.data.page} totalPages={members.data.totalPages} onPage={setPage} />}
    <ConfirmDialog open={Boolean(selected)} title={t("admin.disableQuestion")} description={selected ? t("admin.disableDescription", { name: selected.fullName }) : ""} confirmLabel={t("admin.disableConfirm")} pending={disable.isPending} onCancel={() => { setSelected(null); disable.reset(); }} onConfirm={() => selected && disable.mutate(selected.id)} error={disable.isError ? <p className="mt-4 text-sm font-semibold text-danger" role="alert">{toDisplayError(disable.error).message}</p> : undefined} />
  </section>;
}

function Filter({ id, label, value, onChange, type = "text" }: { id: string; label: string; value: string; onChange: (value: string) => void; type?: string }) { return <div><label className="field-label" htmlFor={id}>{label}</label><input id={id} type={type} className="field-input" value={value} onChange={(event) => onChange(event.target.value)} /></div>; }
function SelectFilter({ id, label, value, onChange, options }: { id: string; label: string; value: string; onChange: (value: string) => void; options: string[][] }) { return <div><label className="field-label" htmlFor={id}>{label}</label><select id={id} className="field-input" value={value} onChange={(event) => onChange(event.target.value)}>{options.map(([optionValue, text]) => <option key={optionValue} value={optionValue}>{text}</option>)}</select></div>; }
