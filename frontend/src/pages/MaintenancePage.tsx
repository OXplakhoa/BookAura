import { useMutation } from "@tanstack/react-query";
import { BookOpenCheck, LoaderCircle, RefreshCw } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/use-auth";
import { api } from "../lib/api";
import { useSystemStatus } from "../system/use-system-status";
import { LanguageSwitcher } from "../components/LanguageSwitcher";
import { useLanguage } from "../i18n/language";

export function MaintenancePage() {
  const systemStatus = useSystemStatus();
  const auth = useAuth();
  const { t } = useLanguage();
  const navigate = useNavigate();
  const retry = useMutation({
    mutationFn: () => api.get("/books", { params: { size: 1 } }),
    onSuccess: systemStatus.clearMaintenance,
  });

  return (
    <main className="relative grid min-h-dvh place-items-center bg-ink px-5 text-stone-100">
      <div className="absolute right-5 top-5"><LanguageSwitcher inverted /></div>
      <div className="max-w-xl text-center">
        <span className="mx-auto grid size-16 place-items-center rounded-2xl border border-stone-600 bg-stone-800 text-amber-300"><BookOpenCheck size={32} /></span>
        <p className="eyebrow mt-8 text-amber-300">{t("maintenance.eyebrow")}</p>
        <h1 className="mt-4 font-display text-5xl font-bold">{t("maintenance.title")}</h1>
        <p className="mt-5 text-lg leading-8 text-stone-300">{t("maintenance.text")}</p>
        <div className="mt-8 flex flex-col justify-center gap-3 sm:flex-row">
          <button type="button" className="button min-h-12 bg-stone-100 text-ink hover:bg-white" onClick={() => retry.mutate()} disabled={retry.isPending}>{retry.isPending ? <LoaderCircle className="animate-spin motion-reduce:animate-none" size={18} /> : <RefreshCw size={18} />}{retry.isPending ? t("maintenance.checking") : t("maintenance.tryAgain")}</button>
          {auth.isAdmin && <button type="button" className="button min-h-12 border border-stone-500 text-stone-100 hover:bg-stone-800" onClick={() => { systemStatus.clearMaintenance(); navigate("/admin/maintenance"); }}>{t("maintenance.openControl")}</button>}
        </div>
        {retry.isError && <p className="mt-4 text-sm text-stone-400" role="status">{t("maintenance.still")}</p>}
      </div>
    </main>
  );
}
