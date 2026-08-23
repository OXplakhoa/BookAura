import { useQuery } from "@tanstack/react-query";
import { AlertCircle, CheckCircle2, LoaderCircle } from "lucide-react";
import { Link, useSearchParams } from "react-router-dom";
import { verifyEmail } from "../auth/auth-api";
import { toDisplayError } from "../lib/api-error";

export function VerifyEmailPage() {
  const [params] = useSearchParams();
  const token = params.get("token") ?? "";
  const verification = useQuery({
    queryKey: ["verify-email", token],
    queryFn: () => verifyEmail(token),
    enabled: Boolean(token),
    retry: false,
    staleTime: Number.POSITIVE_INFINITY,
  });

  if (!token) {
    return <VerificationState icon="error" title="Verification link is incomplete" message="Open the full link from your email, or request a new one." />;
  }
  if (verification.isPending) {
    return <div className="text-center" role="status"><LoaderCircle className="mx-auto animate-spin text-primary motion-reduce:animate-none" size={38} /><h1 className="mt-6 font-display text-3xl font-bold">Verifying your email…</h1><p className="mt-3 text-muted">This should only take a moment.</p></div>;
  }
  if (verification.isError) {
    return <VerificationState icon="error" title="We couldn't verify this link" message={toDisplayError(verification.error).message} />;
  }
  return <VerificationState icon="success" title="Email verified" message={verification.data.message} />;
}

function VerificationState({ icon, title, message }: { icon: "success" | "error"; title: string; message: string }) {
  const success = icon === "success";
  return (
    <div className="text-center" role="status">
      <span className={`mx-auto grid size-16 place-items-center rounded-full ${success ? "bg-emerald-100 text-success" : "bg-red-100 text-danger"}`}>{success ? <CheckCircle2 size={32} /> : <AlertCircle size={32} />}</span>
      <h1 className="mt-6 font-display text-4xl font-bold">{title}</h1><p className="mt-4 leading-7 text-muted">{message}</p>
      <Link to="/login" className="button button-primary mt-8 min-h-12 w-full">Go to sign in</Link>
    </div>
  );
}
