import { zodResolver } from "@hookform/resolvers/zod";
import { CheckCircle2, Eye, EyeOff, LoaderCircle } from "lucide-react";
import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { Link } from "react-router-dom";
import { z } from "zod";
import { useAuth } from "../auth/use-auth";
import { ApiErrorBanner } from "../components/ApiErrorBanner";
import { FieldError } from "../components/FieldError";
import { toDisplayError, type DisplayError } from "../lib/api-error";
import { useLanguage } from "../i18n/language";

type RegisterForm = {
  fullName: string;
  email: string;
  phone: string;
  password: string;
  confirmPassword: string;
};

export function RegisterPage() {
  const auth = useAuth();
  const { t } = useLanguage();
  const registerSchema = useMemo(() => z.object({
    fullName: z.string().trim().min(2, t("validation.nameMin")).max(120, t("validation.nameMax")),
    email: z.email(t("validation.email")),
    phone: z.string().trim().regex(/^\+?[0-9\s\-.]{8,17}$/, t("validation.phone")).or(z.literal("")),
    password: z.string().min(8, t("validation.passwordMin")).max(72, t("validation.passwordMax")).regex(/[A-Za-z]/, t("validation.passwordLetter")).regex(/\d/, t("validation.passwordNumber")),
    confirmPassword: z.string(),
  }).refine((values) => values.password === values.confirmPassword, {
    path: ["confirmPassword"],
    message: t("validation.passwordMatch"),
  }), [t]);
  const [showPassword, setShowPassword] = useState(false);
  const [serverError, setServerError] = useState<DisplayError | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const { register, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
    mode: "onBlur",
    defaultValues: { phone: "" },
  });

  async function submit(values: RegisterForm) {
    setServerError(null);
    try {
      const message = await auth.signUp({
        fullName: values.fullName,
        email: values.email,
        phone: values.phone || undefined,
        password: values.password,
      });
      setSuccess(message);
    } catch (error) {
      const displayError = toDisplayError(error);
      setServerError(displayError);
      Object.entries(displayError.fields).forEach(([field, message]) => {
        if (field in values) setError(field as keyof RegisterForm, { message });
      });
    }
  }

  if (success) {
    return (
      <div className="text-center" role="status">
        <span className="mx-auto grid size-16 place-items-center rounded-full bg-emerald-100 text-success"><CheckCircle2 size={32} /></span>
        <h1 className="mt-6 font-display text-4xl font-bold">{t("auth.checkInbox")}</h1>
        <p className="mt-4 leading-7 text-muted">{success}</p>
        <Link to="/login" className="button button-primary mt-8 min-h-12 w-full">{t("auth.continueSignIn")}</Link>
      </div>
    );
  }

  return (
    <div>
      <p className="eyebrow">{t("auth.membership")}</p>
      <h1 className="mt-3 font-display text-4xl font-bold tracking-tight">{t("auth.createTitle")}</h1>
      <p className="mt-3 leading-7 text-muted">{t("auth.registerText")}</p>
      <div className="mt-7"><ApiErrorBanner error={serverError} /></div>
      <form onSubmit={handleSubmit(submit)} noValidate className="space-y-4">
        <FormInput id="fullName" label={t("auth.fullName")} autoComplete="name" error={errors.fullName?.message} registration={register("fullName")} />
        <FormInput id="email" label={t("auth.email")} type="email" autoComplete="email" error={errors.email?.message} registration={register("email")} />
        <FormInput id="phone" label={t("auth.phoneOptional")} type="tel" autoComplete="tel" helper={t("auth.phoneDigits")} error={errors.phone?.message} registration={register("phone")} />
        <div>
          <label className="field-label" htmlFor="new-password">{t("auth.password")}</label>
          <div className="relative">
            <input id="new-password" type={showPassword ? "text" : "password"} className="field-input pr-12" autoComplete="new-password" aria-invalid={Boolean(errors.password)} aria-describedby={errors.password ? "new-password-error" : "new-password-help"} {...register("password")} />
            <button className="absolute inset-y-0 right-0 grid w-12 place-items-center rounded-r-[10px] text-muted hover:text-ink focus:outline-none focus-visible:ring-[3px] focus-visible:ring-primary/30" type="button" onClick={() => setShowPassword((value) => !value)} aria-label={showPassword ? t("auth.hidePassword") : t("auth.showPassword")}>{showPassword ? <EyeOff size={20} /> : <Eye size={20} />}</button>
          </div>
          <p id="new-password-help" className="mt-1.5 text-xs text-muted">{t("auth.passwordHelp")}</p>
          <FieldError id="new-password-error" message={errors.password?.message} />
        </div>
        <FormInput id="confirmPassword" label={t("auth.confirmPassword")} type={showPassword ? "text" : "password"} autoComplete="new-password" error={errors.confirmPassword?.message} registration={register("confirmPassword")} />
        <button className="button button-primary mt-2 min-h-12 w-full" type="submit" disabled={isSubmitting}>{isSubmitting && <LoaderCircle className="animate-spin motion-reduce:animate-none" size={18} />}{isSubmitting ? t("auth.creating") : t("common.register")}</button>
      </form>
      <p className="mt-6 text-center text-sm text-muted">{t("auth.alreadyMember")} <Link to="/login" className="font-semibold text-primary underline-offset-4 hover:underline">{t("common.signIn")}</Link></p>
    </div>
  );
}

interface FormInputProps {
  id: keyof RegisterForm;
  label: string;
  type?: string;
  autoComplete?: string;
  helper?: string;
  error?: string;
  registration: ReturnType<ReturnType<typeof useForm<RegisterForm>>["register"]>;
}

function FormInput({ id, label, type = "text", autoComplete, helper, error, registration }: FormInputProps) {
  const errorId = `${id}-error`;
  return (
    <div>
      <div className="flex items-center justify-between gap-4"><label className="field-label" htmlFor={id}>{label}</label>{helper && <span className="text-xs text-muted">{helper}</span>}</div>
      <input id={id} type={type} autoComplete={autoComplete} className="field-input" aria-invalid={Boolean(error)} aria-describedby={error ? errorId : undefined} {...registration} />
      <FieldError id={errorId} message={error} />
    </div>
  );
}
