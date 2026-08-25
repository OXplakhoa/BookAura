import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertTriangle, CheckCircle2, Settings2 } from "lucide-react";
import { useState } from "react";
import { getSystemConfiguration, setMaintenanceMode } from "../../admin/admin-api";
import { ConfirmDialog } from "../../components/ConfirmDialog";
import { InlineLoading, QueryError } from "../../components/QueryState";
import { formatDateTime } from "../../lib/date-format";
import { toDisplayError } from "../../lib/api-error";
import { useLanguage } from "../../i18n/language";

export function MaintenanceControlPage() {
  const queryClient = useQueryClient();
  const { language, t } = useLanguage();
  const [confirm, setConfirm] = useState<boolean | null>(null);
  const config = useQuery({ queryKey: ["system-config"], queryFn: getSystemConfiguration, retry: false });
  const toggle = useMutation({
    mutationFn: setMaintenanceMode,
    onSuccess: (value) => {
      queryClient.setQueryData(["system-config"], value);
      setConfirm(null);
    },
  });

  if (config.isPending) return <InlineLoading label={t("common.loading")} />;
  if (config.isError) return <QueryError message={toDisplayError(config.error).message} retry={() => config.refetch()} />;
  const enabled = config.data.maintenanceMode;

  return <section><p className="eyebrow">{t("admin.ops")}</p><h1 className="mt-3 flex items-center gap-3 font-display text-4xl font-bold"><Settings2 className="text-primary" size={34} />{t("admin.maintenanceMode")}</h1><p className="mt-3 max-w-2xl leading-7 text-muted">{t("admin.maintenanceText")}</p>
    <div className={`mt-9 border-l-4 p-6 ${enabled ? "border-accent bg-amber-50" : "border-success bg-emerald-50"}`}>
      <div className="flex flex-col gap-6 sm:flex-row sm:items-center sm:justify-between"><div className="flex items-start gap-4">{enabled ? <AlertTriangle className="mt-1 text-amber-800" size={26} /> : <CheckCircle2 className="mt-1 text-success" size={26} />}<div><p className="text-sm font-bold uppercase tracking-wider text-muted">{t("admin.currentStatus")}</p><h2 className="mt-1 font-display text-3xl font-bold">{enabled ? t("admin.maintenanceOn") : t("admin.bookAuraAvailable")}</h2><p className="mt-2 text-sm text-muted">{t("admin.lastChanged", { date: formatDateTime(config.data.updatedAt, language) })}</p></div></div><button type="button" className={`button min-h-12 ${enabled ? "button-primary" : "bg-amber-700 text-white hover:bg-amber-800"}`} onClick={() => setConfirm(!enabled)}>{enabled ? t("admin.turnOff") : t("admin.turnOn")}</button></div>
    </div>
    <div className="mt-7 border border-line bg-surface p-6"><h2 className="font-display text-2xl font-bold">{t("admin.beforeEnabling")}</h2><ul className="mt-4 list-disc space-y-2 pl-5 leading-7 text-muted"><li>{t("admin.maintenanceBullet1")}</li><li>{t("admin.maintenanceBullet2")}</li><li>{t("admin.maintenanceBullet3")}</li></ul></div>
    <ConfirmDialog open={confirm !== null} title={confirm ? t("admin.enableQuestion") : t("admin.resumeQuestion")} description={confirm ? t("admin.enableDescription") : t("admin.resumeDescription")} confirmLabel={confirm ? t("admin.turnOn") : t("admin.resume")} pending={toggle.isPending} onCancel={() => { setConfirm(null); toggle.reset(); }} onConfirm={() => confirm !== null && toggle.mutate(confirm)} error={toggle.isError ? <p className="mt-4 border-l-4 border-danger bg-red-50 p-3 text-sm text-red-950" role="alert">{toDisplayError(toggle.error).message}</p> : undefined} />
  </section>;
}
