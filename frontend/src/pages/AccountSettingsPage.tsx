import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { CheckCircle2, LoaderCircle, LockKeyhole, Mail } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { requestEmailChange, confirmEmailChange } from "../account/account-api";
import { useAuth } from "../auth/use-auth";
import { ApiErrorBanner } from "../components/ApiErrorBanner";
import { FieldError } from "../components/FieldError";
import { toDisplayError, type DisplayError } from "../lib/api-error";

const requestSchema = z.object({ newEmail: z.email("Enter a valid new email address") });
const confirmSchema = z.object({ code: z.string().regex(/^\d{6}$/, "Enter the six-digit code") });
type RequestForm = z.infer<typeof requestSchema>;
type ConfirmForm = z.infer<typeof confirmSchema>;

export function AccountSettingsPage() {
  const auth = useAuth();
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

  return <section><p className="eyebrow">My account</p><h1 className="mt-3 font-display text-4xl font-bold">Account settings</h1><p className="mt-3 max-w-2xl leading-7 text-muted">Update your registered email only after proving access to the new inbox.</p>
    {success && <div className="mt-7 flex gap-3 border-l-4 border-success bg-emerald-50 p-4 text-emerald-950" role="status"><CheckCircle2 size={20} /><p className="font-semibold">{success}</p></div>}
    <div className="mt-8 grid gap-6 lg:grid-cols-[minmax(0,1fr)_320px]"><div className="border border-line bg-surface p-6 sm:p-8"><div className="flex items-start gap-4 border-b border-line pb-6"><span className="grid size-11 place-items-center rounded-xl bg-primary/10 text-primary"><Mail size={21} /></span><div><h2 className="font-display text-2xl font-bold">Registered email</h2><p className="mt-1 break-all text-sm text-muted">Current: <strong className="text-ink">{auth.user?.email}</strong></p></div></div><div className="mt-6"><ApiErrorBanner error={serverError} /></div>
      {!pendingEmail ? <form onSubmit={requestForm.handleSubmit(requestCode)} noValidate><label className="field-label" htmlFor="newEmail">New email</label><input id="newEmail" type="email" autoComplete="email" className="field-input" aria-invalid={Boolean(requestForm.formState.errors.newEmail)} aria-describedby={requestForm.formState.errors.newEmail ? "newEmail-error" : "newEmail-help"} {...requestForm.register("newEmail")} /><p id="newEmail-help" className="mt-1.5 text-xs text-muted">We send a six-digit code here; your current email stays unchanged until confirmation.</p><FieldError id="newEmail-error" message={requestForm.formState.errors.newEmail?.message} /><button type="submit" className="button button-primary mt-5" disabled={requestMutation.isPending}>{requestMutation.isPending && <LoaderCircle className="animate-spin" size={17} />}{requestMutation.isPending ? "Sending…" : "Send confirmation code"}</button></form>
        : <form onSubmit={confirmForm.handleSubmit(confirmCode)} noValidate><p className="text-sm leading-6 text-muted">Code sent to <strong className="break-all text-ink">{pendingEmail}</strong>. It expires in 10 minutes and allows five attempts.</p><div className="mt-5"><label className="field-label" htmlFor="email-code">Six-digit code</label><input id="email-code" inputMode="numeric" autoComplete="one-time-code" maxLength={6} className="field-input text-center text-xl font-bold tracking-[0.35em] tabular-nums" aria-invalid={Boolean(confirmForm.formState.errors.code)} aria-describedby={confirmForm.formState.errors.code ? "email-code-error" : undefined} {...confirmForm.register("code")} /><FieldError id="email-code-error" message={confirmForm.formState.errors.code?.message} /></div><div className="mt-5 flex flex-wrap gap-3"><button type="submit" className="button button-primary" disabled={confirmMutation.isPending}>{confirmMutation.isPending && <LoaderCircle className="animate-spin" size={17} />}{confirmMutation.isPending ? "Confirming…" : "Confirm new email"}</button><button type="button" className="button button-ghost border border-line" onClick={() => { setPendingEmail(null); setServerError(null); confirmForm.reset(); }}>Use another email</button></div></form>}
    </div><aside className="border border-line bg-ink p-6 text-stone-100"><LockKeyhole className="text-amber-300" size={28} /><h2 className="mt-5 font-display text-2xl font-bold">Security guarantees</h2><ul className="mt-4 list-disc space-y-2 pl-5 text-sm leading-6 text-stone-300"><li>Code stored as SHA-256, never raw.</li><li>60-second resend cooldown.</li><li>Five failed attempts maximum.</li><li>Single-use atomic confirmation.</li></ul></aside></div>
  </section>;
}
