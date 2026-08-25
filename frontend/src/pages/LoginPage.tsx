import { zodResolver } from "@hookform/resolvers/zod";
import { useQuery } from "@tanstack/react-query";
import { Eye, EyeOff, LoaderCircle } from "lucide-react";
import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { z } from "zod";
import { facebookAuthorizationUrl, getOAuthProviders, googleAuthorizationUrl } from "../auth/auth-api";
import { useAuth } from "../auth/use-auth";
import { ApiErrorBanner } from "../components/ApiErrorBanner";
import { FieldError } from "../components/FieldError";
import { toDisplayError, type DisplayError } from "../lib/api-error";
import { useLanguage } from "../i18n/language";

type LoginForm = { identifier: string; password: string };

export function LoginPage() {
  const auth = useAuth();
  const navigate = useNavigate();
  const { t } = useLanguage();
  const loginSchema = useMemo(() => z.object({
    identifier: z.string().trim().min(1, t("validation.emailOrPhone")),
    password: z.string().min(1, t("validation.password")),
  }), [t]);
  const location = useLocation();
  const [showPassword, setShowPassword] = useState(false);
  const [serverError, setServerError] = useState<DisplayError | null>(null);
  const providers = useQuery({
    queryKey: ["oauth-providers"],
    queryFn: getOAuthProviders,
    staleTime: Number.POSITIVE_INFINITY,
    retry: false,
  });
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    mode: "onBlur",
  });

  async function submit(values: LoginForm) {
    setServerError(null);
    try {
      const user = await auth.signIn(values);
      const requested = (location.state as { from?: string } | null)?.from;
      navigate(requested ?? (user.roles.includes("ADMIN") ? "/admin" : "/app/loans"), { replace: true });
    } catch (error) {
      setServerError(toDisplayError(error));
    }
  }

  return (
    <div>
      <p className="eyebrow">{t("auth.welcome")}</p>
      <h1 className="mt-3 font-display text-4xl font-bold tracking-tight">{t("auth.returnShelf")}</h1>
      <p className="mt-3 leading-7 text-muted">{t("auth.loginText")}</p>
      <div className="mt-8"><ApiErrorBanner error={serverError} /></div>
      <button type="button" className="button min-h-12 w-full border border-line bg-surface text-ink hover:bg-canvas" disabled={!providers.data?.google} onClick={() => window.location.assign(googleAuthorizationUrl())}>{t("auth.continueGoogle")}</button>
      <button type="button" className="button mt-3 min-h-12 w-full border border-line bg-surface text-ink hover:bg-canvas" disabled={!providers.data?.facebook} onClick={() => window.location.assign(facebookAuthorizationUrl())}>{t("auth.continueFacebook")}</button>
      {providers.isSuccess && !providers.data.google && !providers.data.facebook && <p className="mt-2 text-center text-xs text-muted">{t("auth.socialUnavailable")}</p>}
      <div className="my-6 flex items-center gap-4" aria-hidden="true"><span className="h-px flex-1 bg-line" /><span className="text-xs font-bold uppercase tracking-wider text-muted">{t("auth.orPassword")}</span><span className="h-px flex-1 bg-line" /></div>
      <form onSubmit={handleSubmit(submit)} noValidate className="space-y-5">
        <div>
          <label className="field-label" htmlFor="identifier">{t("auth.emailOrPhone")}</label>
          <input id="identifier" className="field-input" autoComplete="username" inputMode="email" aria-invalid={Boolean(errors.identifier)} aria-describedby={errors.identifier ? "identifier-error" : undefined} {...register("identifier")} />
          <FieldError id="identifier-error" message={errors.identifier?.message} />
        </div>
        <div>
          <div className="flex items-center justify-between gap-4"><label className="field-label" htmlFor="password">{t("auth.password")}</label><span className="text-xs text-muted">{t("auth.passwordLength")}</span></div>
          <div className="relative">
            <input id="password" type={showPassword ? "text" : "password"} className="field-input pr-12" autoComplete="current-password" aria-invalid={Boolean(errors.password)} aria-describedby={errors.password ? "password-error" : undefined} {...register("password")} />
            <button className="absolute inset-y-0 right-0 grid w-12 place-items-center rounded-r-[10px] text-muted hover:text-ink focus:outline-none focus-visible:ring-[3px] focus-visible:ring-primary/30" type="button" onClick={() => setShowPassword((value) => !value)} aria-label={showPassword ? t("auth.hidePassword") : t("auth.showPassword")}>
              {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
            </button>
          </div>
          <FieldError id="password-error" message={errors.password?.message} />
        </div>
        <button className="button button-primary min-h-12 w-full" type="submit" disabled={isSubmitting}>
          {isSubmitting && <LoaderCircle className="animate-spin motion-reduce:animate-none" size={18} />}{isSubmitting ? t("auth.signingIn") : t("common.signIn")}
        </button>
      </form>
      <div className="mt-6 flex flex-col items-center gap-3 text-sm"><Link to="/phone-login" className="font-semibold text-primary underline-offset-4 hover:underline">{t("auth.phoneInstead")}</Link><p className="text-muted">{t("auth.newToBookAura")} <Link to="/register" className="font-semibold text-primary underline-offset-4 hover:underline">{t("auth.createAccount")}</Link></p></div>
    </div>
  );
}
