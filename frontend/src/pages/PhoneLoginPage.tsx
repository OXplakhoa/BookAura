import { zodResolver } from "@hookform/resolvers/zod";
import { LoaderCircle, MessageSquareText, Phone } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Link, useNavigate } from "react-router-dom";
import { z } from "zod";
import { requestPhoneOtp } from "../auth/auth-api";
import { useAuth } from "../auth/use-auth";
import { ApiErrorBanner } from "../components/ApiErrorBanner";
import { FieldError } from "../components/FieldError";
import { toDisplayError, type DisplayError } from "../lib/api-error";

const phoneSchema = z.object({ phone: z.string().regex(/^\+?[0-9\s\-.]{8,17}$/, "Enter 8–15 digits, optionally starting with +") });
const codeSchema = z.object({ code: z.string().regex(/^\d{6}$/, "Enter the six-digit code") });
type PhoneForm = z.infer<typeof phoneSchema>;
type CodeForm = z.infer<typeof codeSchema>;

export function PhoneLoginPage() {
  const auth = useAuth();
  const navigate = useNavigate();
  const [phone, setPhone] = useState<string | null>(null);
  const [serverError, setServerError] = useState<DisplayError | null>(null);
  const [requesting, setRequesting] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const phoneForm = useForm<PhoneForm>({ resolver: zodResolver(phoneSchema), mode: "onBlur" });
  const codeForm = useForm<CodeForm>({ resolver: zodResolver(codeSchema), mode: "onBlur" });

  async function send(values: PhoneForm) {
    setServerError(null); setRequesting(true);
    try { await requestPhoneOtp(values.phone); setPhone(values.phone); }
    catch (error) { setServerError(toDisplayError(error)); }
    finally { setRequesting(false); }
  }
  async function confirm(values: CodeForm) {
    if (!phone) return;
    setServerError(null); setConfirming(true);
    try {
      const user = await auth.completePhoneOtp(phone, values.code);
      navigate(user.roles.includes("ADMIN") ? "/admin" : "/app/loans", { replace: true });
    } catch (error) { setServerError(toDisplayError(error)); }
    finally { setConfirming(false); }
  }

  return <div><p className="eyebrow">Passwordless sign-in</p><h1 className="mt-3 font-display text-4xl font-bold">Use your registered phone</h1><p className="mt-3 leading-7 text-muted">A five-minute, single-use code will be sent if an active account uses this number.</p><div className="mt-7"><ApiErrorBanner error={serverError} /></div>
    {!phone ? <form onSubmit={phoneForm.handleSubmit(send)} noValidate><label className="field-label" htmlFor="login-phone">Phone number</label><div className="relative"><Phone className="absolute left-3.5 top-1/2 -translate-y-1/2 text-muted" size={19} /><input id="login-phone" type="tel" autoComplete="tel" className="field-input pl-11" aria-invalid={Boolean(phoneForm.formState.errors.phone)} aria-describedby={phoneForm.formState.errors.phone ? "login-phone-error" : undefined} {...phoneForm.register("phone")} /></div><FieldError id="login-phone-error" message={phoneForm.formState.errors.phone?.message} /><button type="submit" className="button button-primary mt-5 min-h-12 w-full" disabled={requesting}>{requesting && <LoaderCircle className="animate-spin" size={17} />}{requesting ? "Requesting…" : "Send phone code"}</button></form>
      : <form onSubmit={codeForm.handleSubmit(confirm)} noValidate><div className="flex gap-3 border-l-4 border-primary bg-primary/5 p-4"><MessageSquareText className="shrink-0 text-primary" size={20} /><p className="text-sm leading-6">If the account exists, a code was sent to <strong>{phone}</strong>. Wait 60 seconds before requesting again.</p></div><div className="mt-5"><label className="field-label" htmlFor="phone-code">Six-digit code</label><input id="phone-code" inputMode="numeric" autoComplete="one-time-code" maxLength={6} className="field-input text-center text-xl font-bold tracking-[0.35em]" aria-invalid={Boolean(codeForm.formState.errors.code)} aria-describedby={codeForm.formState.errors.code ? "phone-code-error" : undefined} {...codeForm.register("code")} /><FieldError id="phone-code-error" message={codeForm.formState.errors.code?.message} /></div><button type="submit" className="button button-primary mt-5 min-h-12 w-full" disabled={confirming}>{confirming && <LoaderCircle className="animate-spin" size={17} />}{confirming ? "Signing in…" : "Confirm and sign in"}</button><button type="button" className="button button-ghost mt-2 w-full" onClick={() => { setPhone(null); setServerError(null); codeForm.reset(); }}>Use another phone</button>{import.meta.env.DEV && <p className="mt-4 text-center text-xs leading-5 text-muted">Local fake SMS never logs codes. An ADMIN can read the in-memory outbox at <code>/admin/sms-outbox</code>.</p>}</form>}
    <p className="mt-7 text-center text-sm"><Link to="/login" className="font-semibold text-primary hover:underline">Back to password sign-in</Link></p>
  </div>;
}
