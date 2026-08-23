import { AlertCircle, CheckCircle2, LoaderCircle } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../auth/use-auth";
import { toDisplayError } from "../lib/api-error";

type CallbackState = { status: "working" | "success" | "error"; message: string };

export function OAuthCallbackPage() {
  const [params] = useSearchParams();
  const [code] = useState(() => params.get("code"));
  const [providerError] = useState(() => params.get("error"));
  const [state, setState] = useState<CallbackState>(() => providerError || !code
    ? { status: "error", message: "Google sign-in was cancelled or could not be completed." }
    : { status: "working", message: "Finishing secure sign-in…" });
  const started = useRef(false);
  const auth = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (started.current) return;
    started.current = true;
    // Remove the one-time code from address bar/history before making the exchange request.
    window.history.replaceState({}, "", "/oauth/callback");
    if (providerError || !code) return;
    auth.completeOAuth(code)
      .then((user) => {
        setState({ status: "success", message: "Signed in. Opening your BookAura workspace…" });
        navigate(user.roles.includes("ADMIN") ? "/admin" : "/app/loans", { replace: true });
      })
      .catch((error) => setState({ status: "error", message: toDisplayError(error).message }));
  }, [auth, code, navigate, providerError]);

  const success = state.status === "success";
  const error = state.status === "error";
  return (
    <div className="text-center" role="status" aria-live="polite">
      <span className={`mx-auto grid size-16 place-items-center rounded-full ${error ? "bg-red-100 text-danger" : success ? "bg-emerald-100 text-success" : "bg-primary/10 text-primary"}`}>
        {error ? <AlertCircle size={32} /> : success ? <CheckCircle2 size={32} /> : <LoaderCircle className="animate-spin motion-reduce:animate-none" size={32} />}
      </span>
      <p className="eyebrow mt-7">Google sign-in</p>
      <h1 className="mt-3 font-display text-4xl font-bold">{error ? "Sign-in interrupted" : "Connecting your account"}</h1>
      <p className="mt-4 leading-7 text-muted">{state.message}</p>
      {error && <Link to="/login" className="button button-primary mt-8 min-h-12 w-full">Return to sign in</Link>}
    </div>
  );
}
