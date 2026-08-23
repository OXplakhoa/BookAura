import { useMutation } from "@tanstack/react-query";
import { BookOpenCheck, LoaderCircle, RefreshCw } from "lucide-react";
import { api } from "../lib/api";
import { useSystemStatus } from "../system/use-system-status";

export function MaintenancePage() {
  const systemStatus = useSystemStatus();
  const retry = useMutation({
    mutationFn: () => api.get("/books", { params: { size: 1 } }),
    onSuccess: systemStatus.clearMaintenance,
  });

  return (
    <main className="grid min-h-dvh place-items-center bg-ink px-5 text-stone-100">
      <div className="max-w-xl text-center">
        <span className="mx-auto grid size-16 place-items-center rounded-2xl border border-stone-600 bg-stone-800 text-amber-300"><BookOpenCheck size={32} /></span>
        <p className="eyebrow mt-8 text-amber-300">A short intermission</p>
        <h1 className="mt-4 font-display text-5xl font-bold">We are tending the shelves.</h1>
        <p className="mt-5 text-lg leading-8 text-stone-300">BookAura is temporarily unavailable while an administrator completes maintenance. Your loans and history remain safe.</p>
        <button type="button" className="button mt-8 min-h-12 bg-stone-100 text-ink hover:bg-white" onClick={() => retry.mutate()} disabled={retry.isPending}>{retry.isPending ? <LoaderCircle className="animate-spin motion-reduce:animate-none" size={18} /> : <RefreshCw size={18} />}{retry.isPending ? "Checking…" : "Try again"}</button>
        {retry.isError && <p className="mt-4 text-sm text-stone-400" role="status">Still under maintenance. Please try again shortly.</p>}
      </div>
    </main>
  );
}
