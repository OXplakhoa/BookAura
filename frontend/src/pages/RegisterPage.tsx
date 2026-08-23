import { zodResolver } from "@hookform/resolvers/zod";
import { CheckCircle2, Eye, EyeOff, LoaderCircle } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Link } from "react-router-dom";
import { z } from "zod";
import { useAuth } from "../auth/use-auth";
import { ApiErrorBanner } from "../components/ApiErrorBanner";
import { FieldError } from "../components/FieldError";
import { toDisplayError, type DisplayError } from "../lib/api-error";

const registerSchema = z.object({
  fullName: z.string().trim().min(2, "Enter at least 2 characters").max(120, "Use 120 characters or fewer"),
  email: z.email("Enter a valid email address"),
  phone: z.string().trim().regex(/^\+?[0-9\s\-.]{8,17}$/, "Enter 8–15 digits, optionally starting with +").or(z.literal("")),
  password: z.string().min(8, "Use at least 8 characters").max(72, "Use 72 characters or fewer").regex(/[A-Za-z]/, "Add at least one letter").regex(/\d/, "Add at least one number"),
  confirmPassword: z.string(),
}).refine((values) => values.password === values.confirmPassword, {
  path: ["confirmPassword"],
  message: "Passwords do not match",
});

type RegisterForm = z.infer<typeof registerSchema>;

export function RegisterPage() {
  const auth = useAuth();
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
        <h1 className="mt-6 font-display text-4xl font-bold">Check your inbox</h1>
        <p className="mt-4 leading-7 text-muted">{success}</p>
        <Link to="/login" className="button button-primary mt-8 min-h-12 w-full">Continue to sign in</Link>
      </div>
    );
  }

  return (
    <div>
      <p className="eyebrow">Library membership</p>
      <h1 className="mt-3 font-display text-4xl font-bold tracking-tight">Create your BookAura account</h1>
      <p className="mt-3 leading-7 text-muted">We will send one verification link before your first sign-in.</p>
      <div className="mt-7"><ApiErrorBanner error={serverError} /></div>
      <form onSubmit={handleSubmit(submit)} noValidate className="space-y-4">
        <FormInput id="fullName" label="Full name" autoComplete="name" error={errors.fullName?.message} registration={register("fullName")} />
        <FormInput id="email" label="Email" type="email" autoComplete="email" error={errors.email?.message} registration={register("email")} />
        <FormInput id="phone" label="Phone (optional)" type="tel" autoComplete="tel" helper="8–15 digits" error={errors.phone?.message} registration={register("phone")} />
        <div>
          <label className="field-label" htmlFor="new-password">Password</label>
          <div className="relative">
            <input id="new-password" type={showPassword ? "text" : "password"} className="field-input pr-12" autoComplete="new-password" aria-invalid={Boolean(errors.password)} aria-describedby={errors.password ? "new-password-error" : "new-password-help"} {...register("password")} />
            <button className="absolute inset-y-0 right-0 grid w-12 place-items-center rounded-r-[10px] text-muted hover:text-ink focus:outline-none focus-visible:ring-[3px] focus-visible:ring-primary/30" type="button" onClick={() => setShowPassword((value) => !value)} aria-label={showPassword ? "Hide password" : "Show password"}>{showPassword ? <EyeOff size={20} /> : <Eye size={20} />}</button>
          </div>
          <p id="new-password-help" className="mt-1.5 text-xs text-muted">8–72 characters with at least one letter and number.</p>
          <FieldError id="new-password-error" message={errors.password?.message} />
        </div>
        <FormInput id="confirmPassword" label="Confirm password" type={showPassword ? "text" : "password"} autoComplete="new-password" error={errors.confirmPassword?.message} registration={register("confirmPassword")} />
        <button className="button button-primary mt-2 min-h-12 w-full" type="submit" disabled={isSubmitting}>{isSubmitting && <LoaderCircle className="animate-spin motion-reduce:animate-none" size={18} />}{isSubmitting ? "Creating account…" : "Create account"}</button>
      </form>
      <p className="mt-6 text-center text-sm text-muted">Already a member? <Link to="/login" className="font-semibold text-primary underline-offset-4 hover:underline">Sign in</Link></p>
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
