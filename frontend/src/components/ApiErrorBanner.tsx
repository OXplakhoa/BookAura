import { AlertCircle } from "lucide-react";
import type { DisplayError } from "../lib/api-error";

export function ApiErrorBanner({ error }: { error: DisplayError | null }) {
  if (!error) return null;
  return (
    <div className="mb-6 flex gap-3 border-l-4 border-danger bg-red-50 p-4 text-red-950" role="alert">
      <AlertCircle className="mt-0.5 shrink-0 text-danger" size={20} aria-hidden="true" />
      <div><p className="font-semibold">We couldn't complete that request</p><p className="mt-1 text-sm leading-6">{error.message}</p>{error.traceId && <p className="mt-2 text-xs text-red-800">Trace: {error.traceId}</p>}</div>
    </div>
  );
}
