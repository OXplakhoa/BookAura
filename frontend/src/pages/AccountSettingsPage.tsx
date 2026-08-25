import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { CheckCircle2, LoaderCircle, LockKeyhole, Mail } from "lucide-react";
import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { requestEmailChange, confirmEmailChange } from "../account/account-api";
import { useAuth } from "../auth/use-auth";
import { ApiErrorBanner } from "../components/ApiErrorBanner";
import { FieldError } from "../components/FieldError";
import { toDisplayError, type DisplayError } from "../lib/api-error";
import { useLanguage } from "../i18n/language";

type RequestForm = { newEmail: string };
type ConfirmForm = { code: string };

export function AccountSettingsPage() {
  const auth = useAuth();
  const { t } = useLanguage();
  const requestSchema = useMemo(() => z.object({ newEmail: z.email(t("validation.newEmail")) }), [t]);
  const confirmSchema = useMemo(() => z.object({ code: z.string().regex(/^\d{6}$/, t("validation.sixDigits")) }), [t]);
  const [pendingEmail, setPendingEmail] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [serverError, setServerError] = useState<DisplayError | null>(null);
  const requestForm = useForm<RequestForm>({ resolver: zodResolver(requestSchema), mode: "onBlur" });
  const confirmForm = useForm<ConfirmForm>({ resolver: zodResolver(confirmSchema), mode: "onBlur" });
  const requestMutation = useMutation({ mutationFn: requestEmailChange });
  const confirmMutation = useMutation({ mutationFn: confirmEmailChange });

  async function requestCode(values: RequestForm) {
    setServerError(null); setSuccess(null);
    try {
      await requestMutation.mutateAsync(values.newEmail);
      setPendingEmail(values.newEmail);
    } catch (error) { setServerError(toDisplayError(error)); }
  }
  async function confirmCode(values: ConfirmForm) {
    setServerError(null);
    try {
      const response = await confirmMutation.mutateAsync(values.code);
      auth.syncUser(response.user);
      setSuccess(response.message);
      setPendingEmail(null);
      requestForm.reset(); confirmForm.reset();
    } catch (error) { setServerError(toDisplayError(error)); }
  }

  return <section><p className="eyebrow">{t("account.eyebrow")}</p><h1 className="mt-3 font-display text-4xl font-bold">{t("account.title")}</h1><p className="mt-3 max-w-2xl leading-7 text-muted">{t("account.description")}</p>
    {success && <div className="mt-7 flex gap-3 border-l-4 border-success bg-emerald-50 p-4 text-emerald-950" role="status"><CheckCircle2 size={20} /><p className="font-semibold">{success}</p></div>}
    <div className="mt-8 grid gap-6 lg:grid-cols-[minmax(0,1fr)_320px]"><div className="border border-line bg-surface p-6 sm:p-8"><div className="flex items-start gap-4 border-b border-line pb-6"><span className="grid size-11 place-items-center rounded-xl bg-primary/10 text-primary"><Mail size={21} /></span><div><h2 className="font-display text-2xl font-bold">{t("account.registeredEmail")}</h2><p className="mt-1 break-all text-sm text-muted">{t("account.current")} <strong className="text-ink">{auth.user?.email}</strong></p></div></div><div className="mt-6"><ApiErrorBanner error={serverError} /></div>
      {!pendingEmail ? <form onSubmit={requestForm.handleSubmit(requestCode)} noValidate><label className="field-label" htmlFor="newEmail">{t("account.newEmail")}</label><input id="newEmail" type="email" autoComplete="email" className="field-input" aria-invalid={Boolean(requestForm.formState.errors.newEmail)} aria-describedby={requestForm.formState.errors.newEmail ? "newEmail-error" : "newEmail-help"} {...requestForm.register("newEmail")} /><p id="newEmail-help" className="mt-1.5 text-xs text-muted">{t("account.emailHelp")}</p><FieldError id="newEmail-error" message={requestForm.formState.errors.newEmail?.message} /><button type="submit" className="button button-primary mt-5" disabled={requestMutation.isPending}>{requestMutation.isPending && <LoaderCircle className="animate-spin" size={17} />}{requestMutation.isPending ? t("account.sending") : t("account.sendCode")}</button></form>
        : <form onSubmit={confirmForm.handleSubmit(confirmCode)} noValidate><p className="text-sm leading-6 text-muted">{t("account.codeSent", { email: pendingEmail ?? "" })}</p><div className="mt-5"><label className="field-label" htmlFor="email-code">{t("auth.sixDigitCode")}</label><input id="email-code" inputMode="numeric" autoComplete="one-time-code" maxLength={6} className="field-input text-center text-xl font-bold tracking-[0.35em] tabular-nums" aria-invalid={Boolean(confirmForm.formState.errors.code)} aria-describedby={confirmForm.formState.errors.code ? "email-code-error" : undefined} {...confirmForm.register("code")} /><FieldError id="email-code-error" message={confirmForm.formState.errors.code?.message} /></div><div className="mt-5 flex flex-wrap gap-3"><button type="submit" className="button button-primary" disabled={confirmMutation.isPending}>{confirmMutation.isPending && <LoaderCircle className="animate-spin" size={17} />}{confirmMutation.isPending ? t("account.confirming") : t("account.confirmEmail")}</button><button type="button" className="button button-ghost border border-line" onClick={() => { setPendingEmail(null); setServerError(null); confirmForm.reset(); }}>{t("account.useAnotherEmail")}</button></div></form>}
    </div><aside className="border border-line bg-ink p-6 text-stone-100"><LockKeyhole className="text-amber-300" size={28} /><h2 className="mt-5 font-display text-2xl font-bold">{t("account.security")}</h2><ul className="mt-4 list-disc space-y-2 pl-5 text-sm leading-6 text-stone-300"><li>{t("account.securityHash")}</li><li>{t("account.securityCooldown")}</li><li>{t("account.securityAttempts")}</li><li>{t("account.securitySingleUse")}</li></ul></aside></div>
  </section>;
}
