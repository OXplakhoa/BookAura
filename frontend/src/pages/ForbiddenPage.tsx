import { ShieldX } from "lucide-react";
import { Link } from "react-router-dom";

export function ForbiddenPage() {
  return <main className="grid min-h-dvh place-items-center bg-canvas px-5"><div className="max-w-lg text-center"><ShieldX className="mx-auto text-danger" size={48} /><p className="eyebrow mt-6">403 · Access denied</p><h1 className="mt-3 font-display text-4xl font-bold">This shelf needs another role.</h1><p className="mt-4 leading-7 text-muted">Your account is signed in, but it does not have permission to open this workspace.</p><Link className="button button-primary mt-7" to="/catalog">Return to catalog</Link></div></main>;
}
