import { zodResolver } from "@hookform/resolvers/zod";
import { useQuery } from "@tanstack/react-query";
import { Eye, EyeOff, LoaderCircle } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { z } from "zod";
import { getOAuthProviders, googleAuthorizationUrl } from "../auth/auth-api";
import { useAuth } from "../auth/use-auth";
import { ApiErrorBanner } from "../components/ApiErrorBanner";
import { FieldError } from "../components/FieldError";
import { toDisplayError, type DisplayError } from "../lib/api-error";

const loginSchema = z.object({
  identifier: z.string().trim().min(1, "Enter your email or phone number"),
  password: z.string().min(1, "Enter your password"),
});

type LoginForm = z.infer<typeof loginSchema>;

export function LoginPage() {
  const auth = useAuth();
  const navigate = useNavigate();
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
      <p className="eyebrow">Welcome back</p>
      <h1 className="mt-3 font-display text-4xl font-bold tracking-tight">Return to your shelf</h1>
      <p className="mt-3 leading-7 text-muted">Sign in with the email or phone number connected to your library account.</p>
      <div className="mt-8"><ApiErrorBanner error={serverError} /></div>
      <button type="button" className="button min-h-12 w-full border border-line bg-surface text-ink hover:bg-canvas" disabled={!providers.data?.google} onClick={() => window.location.assign(googleAuthorizationUrl())}>Continue with Google</button>
      {providers.isSuccess && !providers.data.google && <p className="mt-2 text-center text-xs text-muted">Google sign-in is not configured in this environment.</p>}
      <div className="my-6 flex items-center gap-4" aria-hidden="true"><span className="h-px flex-1 bg-line" /><span className="text-xs font-bold uppercase tracking-wider text-muted">or use password</span><span className="h-px flex-1 bg-line" /></div>
      <form onSubmit={handleSubmit(submit)} noValidate className="space-y-5">
        <div>
          <label className="field-label" htmlFor="identifier">Email or phone</label>
          <input id="identifier" className="field-input" autoComplete="username" inputMode="email" aria-invalid={Boolean(errors.identifier)} aria-describedby={errors.identifier ? "identifier-error" : undefined} {...register("identifier")} />
          <FieldError id="identifier-error" message={errors.identifier?.message} />
        </div>
        <div>
          <div className="flex items-center justify-between gap-4"><label className="field-label" htmlFor="password">Password</label><span className="text-xs text-muted">8–72 characters</span></div>
          <div className="relative">
            <input id="password" type={showPassword ? "text" : "password"} className="field-input pr-12" autoComplete="current-password" aria-invalid={Boolean(errors.password)} aria-describedby={errors.password ? "password-error" : undefined} {...register("password")} />
            <button className="absolute inset-y-0 right-0 grid w-12 place-items-center rounded-r-[10px] text-muted hover:text-ink focus:outline-none focus-visible:ring-[3px] focus-visible:ring-primary/30" type="button" onClick={() => setShowPassword((value) => !value)} aria-label={showPassword ? "Hide password" : "Show password"}>
              {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
            </button>
          </div>
          <FieldError id="password-error" message={errors.password?.message} />
        </div>
        <button className="button button-primary min-h-12 w-full" type="submit" disabled={isSubmitting}>
          {isSubmitting && <LoaderCircle className="animate-spin motion-reduce:animate-none" size={18} />}{isSubmitting ? "Signing in…" : "Sign in"}
        </button>
      </form>
      <div className="mt-6 flex flex-col items-center gap-3 text-sm"><Link to="/phone-login" className="font-semibold text-primary underline-offset-4 hover:underline">Sign in with a phone code instead</Link><p className="text-muted">New to BookAura? <Link to="/register" className="font-semibold text-primary underline-offset-4 hover:underline">Create your account</Link></p></div>
    </div>
  );
}
