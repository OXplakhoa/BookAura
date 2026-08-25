import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, LoaderCircle, Save } from "lucide-react";
import { cloneElement, useEffect, useState, type ReactElement } from "react";
import { useForm } from "react-hook-form";
import { Link, useNavigate, useParams } from "react-router-dom";
import { z } from "zod";
import { createMember, getMember, updateMember } from "../../admin/admin-api";
import type { MemberCreateInput, MemberUpdateInput } from "../../admin/member-types";
import { ApiErrorBanner } from "../../components/ApiErrorBanner";
import { FieldError } from "../../components/FieldError";
import { InlineLoading, QueryError } from "../../components/QueryState";
import { toDisplayError, type DisplayError } from "../../lib/api-error";
import { useLanguage } from "../../i18n/language";

const today = new Date();
const todayIso = today.toISOString().slice(0, 10);
const yesterday = new Date(today);
yesterday.setUTCDate(yesterday.getUTCDate() - 1);
const yesterdayIso = yesterday.toISOString().slice(0, 10);
const schema = z.object({
  fullName: z.string().trim().min(1, "Full name is required").max(120),
  email: z.union([z.literal(""), z.email("Enter a valid email")]),
  phone: z.string().trim().regex(/^$|^\+?[0-9\s\-.]{8,17}$/, "Enter 8–15 digits, optionally starting with +"),
  initialPassword: z.union([z.literal(""), z.string().min(8).max(72).regex(/[A-Za-z]/, "Add a letter").regex(/\d/, "Add a number")]),
  dateOfBirth: z.string().refine((value) => !value || value < todayIso, "Date of birth must be in the past"),
  address: z.string().max(255), emailVerified: z.boolean(), active: z.boolean(),
});
type MemberForm = z.infer<typeof schema>;

export function MemberFormPage() {
  const { memberId } = useParams();
  const editing = Boolean(memberId);
  const { t } = useLanguage();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [serverError, setServerError] = useState<DisplayError | null>(null);
  const detail = useQuery({ queryKey: ["admin-member", memberId], queryFn: () => getMember(memberId!), enabled: editing, retry: false });
  const { register, handleSubmit, reset, setError, formState: { errors } } = useForm<MemberForm>({ resolver: zodResolver(schema), mode: "onBlur", defaultValues: { fullName: "", email: "", phone: "", initialPassword: "", dateOfBirth: "", address: "", emailVerified: false, active: true } });
  useEffect(() => { if (detail.data) reset({ fullName: detail.data.fullName, email: detail.data.email, phone: detail.data.phone ?? "", initialPassword: "", dateOfBirth: detail.data.dateOfBirth ?? "", address: detail.data.address ?? "", emailVerified: detail.data.emailVerified, active: detail.data.status === "ACTIVE" }); }, [detail.data, reset]);
  const save = useMutation({
    mutationFn: (values: MemberForm) => {
      if (editing) { const input: MemberUpdateInput = { fullName: values.fullName, phone: values.phone || undefined, dateOfBirth: values.dateOfBirth || undefined, address: values.address || undefined, active: values.active }; return updateMember(memberId!, input); }
      const input: MemberCreateInput = { fullName: values.fullName, email: values.email, phone: values.phone || undefined, initialPassword: values.initialPassword, dateOfBirth: values.dateOfBirth || undefined, address: values.address || undefined, emailVerified: values.emailVerified, active: values.active }; return createMember(input);
    },
    onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: ["admin-members"] }); navigate("/admin/members", { replace: true }); },
    onError: (error) => { const display = toDisplayError(error); setServerError(display); Object.entries(display.fields).forEach(([field, message]) => { if (field in schema.shape) setError(field as keyof MemberForm, { message }); }); },
  });
  function submit(values: MemberForm) {
    setServerError(null);
    if (!editing && !values.email) { setError("email", { message: "Email is required" }); return; }
    if (!editing && !values.initialPassword) { setError("initialPassword", { message: "Initial password is required" }); return; }
    save.mutate(values);
  }
  if (editing && detail.isPending) return <InlineLoading label={t("common.loading")} />;
  if (detail.isError) return <QueryError message={toDisplayError(detail.error).message} retry={() => detail.refetch()} />;

  return <section><Link to="/admin/members" className="inline-flex min-h-11 items-center gap-2 text-sm font-bold text-primary"><ArrowLeft size={17} />{t("admin.backMembers")}</Link><p className="eyebrow mt-5">{t("admin.accounts")}</p><h1 className="mt-3 font-display text-4xl font-bold">{editing ? t("admin.memberEdit") : t("admin.memberAdd")}</h1><p className="mt-3 text-muted">{editing ? t("admin.memberEditHelp") : t("admin.memberAddHelp")}</p><div className="mt-7 max-w-3xl"><ApiErrorBanner error={serverError} /></div>
    <form onSubmit={handleSubmit(submit)} noValidate className="mt-7 max-w-3xl space-y-5 border border-line bg-surface p-6 sm:p-8"><div className="grid gap-5 sm:grid-cols-2"><MemberField id="fullName" label={t("auth.fullName")}  error={errors.fullName?.message} input={<input id="fullName" className="field-input" {...register("fullName")} />} /><MemberField id="email" label={t("auth.email")} helper={editing ? t("admin.readOnly") : undefined}  error={errors.email?.message} input={<input id="email" type="email" className="field-input read-only:bg-stone-100" readOnly={editing} aria-readonly={editing} {...register("email")} />} /><MemberField id="phone" label={t("admin.smsPhone")}  error={errors.phone?.message} input={<input id="phone" type="tel" className="field-input" {...register("phone")} />} /><MemberField id="dateOfBirth" label={t("admin.dateOfBirth")} error={errors.dateOfBirth?.message} input={<input id="dateOfBirth" type="date" max={yesterdayIso} className="field-input" {...register("dateOfBirth")} />} />{!editing && <MemberField id="initialPassword" label={t("admin.initialPassword")} helper={t("admin.initialPasswordHelp")}  error={errors.initialPassword?.message} input={<input id="initialPassword" type="password" autoComplete="new-password" className="field-input" {...register("initialPassword")} />} />}<MemberField id="address" label={t("admin.address")}  error={errors.address?.message} input={<input id="address" className="field-input" {...register("address")} />} /></div>{!editing && <label className="flex min-h-11 cursor-pointer items-center gap-3 text-sm font-semibold"><input type="checkbox" className="size-5 accent-primary" {...register("emailVerified")} />{t("admin.emailVerifiedInPerson")}</label>}<label className="flex min-h-11 cursor-pointer items-center gap-3 text-sm font-semibold"><input type="checkbox" className="size-5 accent-primary" {...register("active")} />{t("admin.accountActive")}</label><div className="flex justify-end gap-3 border-t border-line pt-6"><Link to="/admin/members" className="button button-ghost border border-line">{t("admin.memberCancel")}</Link><button type="submit" className="button button-primary" disabled={save.isPending}>{save.isPending ? <LoaderCircle className="animate-spin" size={17} /> : <Save size={17} />}{save.isPending ? t("admin.memberSaving") : t("common.save")}</button></div></form>
  </section>;
}
function MemberField({ id, label, helper, error, input }: { id: string; label: string; helper?: string; error?: string; input: ReactElement<{ "aria-invalid"?: boolean; "aria-describedby"?: string }> }) {
  const errorId = `${id}-error`;
  const accessibleInput = cloneElement(input, {
    "aria-invalid": Boolean(error),
    "aria-describedby": error ? errorId : undefined,
  });
  return <div><div className="flex items-center justify-between"><label className="field-label" htmlFor={id}>{label}</label>{helper && <span className="text-xs text-muted">{helper}</span>}</div>{accessibleInput}<FieldError id={errorId} message={error} /></div>;
}
