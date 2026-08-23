import { SearchX } from "lucide-react";
import { Link } from "react-router-dom";

export function NotFoundPage() {
  return <main className="grid min-h-dvh place-items-center bg-canvas px-5"><div className="max-w-lg text-center"><SearchX className="mx-auto text-primary" size={48} /><p className="eyebrow mt-6">404 · Page not found</p><h1 className="mt-3 font-display text-4xl font-bold">That page is not on our shelves.</h1><p className="mt-4 leading-7 text-muted">The address may have changed, or the page may no longer be available.</p><Link className="button button-primary mt-7" to="/">Return home</Link></div></main>;
}
