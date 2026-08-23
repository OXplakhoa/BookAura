import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertTriangle, CheckCircle2, Settings2 } from "lucide-react";
import { useState } from "react";
import { getSystemConfiguration, setMaintenanceMode } from "../../admin/admin-api";
import { ConfirmDialog } from "../../components/ConfirmDialog";
import { InlineLoading, QueryError } from "../../components/QueryState";
import { formatDateTime } from "../../lib/date-format";
import { toDisplayError } from "../../lib/api-error";

export function MaintenanceControlPage() {
  const queryClient = useQueryClient();
  const [confirm, setConfirm] = useState<boolean | null>(null);
  const config = useQuery({ queryKey: ["system-config"], queryFn: getSystemConfiguration, retry: false });
  const toggle = useMutation({
    mutationFn: setMaintenanceMode,
    onSuccess: (value) => {
      queryClient.setQueryData(["system-config"], value);
      setConfirm(null);
    },
  });

  if (config.isPending) return <InlineLoading label="Reading operational status" />;
  if (config.isError) return <QueryError message={toDisplayError(config.error).message} retry={() => config.refetch()} />;
  const enabled = config.data.maintenanceMode;

  return <section><p className="eyebrow">Administration · Operations</p><h1 className="mt-3 flex items-center gap-3 font-display text-4xl font-bold"><Settings2 className="text-primary" size={34} />Maintenance mode</h1><p className="mt-3 max-w-2xl leading-7 text-muted">Pause normal API traffic during operational work. This control endpoint and the health probe remain available.</p>
    <div className={`mt-9 border-l-4 p-6 ${enabled ? "border-accent bg-amber-50" : "border-success bg-emerald-50"}`}>
      <div className="flex flex-col gap-6 sm:flex-row sm:items-center sm:justify-between"><div className="flex items-start gap-4">{enabled ? <AlertTriangle className="mt-1 text-amber-800" size={26} /> : <CheckCircle2 className="mt-1 text-success" size={26} />}<div><p className="text-sm font-bold uppercase tracking-wider text-muted">Current status</p><h2 className="mt-1 font-display text-3xl font-bold">{enabled ? "Maintenance is ON" : "BookAura is available"}</h2><p className="mt-2 text-sm text-muted">Last changed {formatDateTime(config.data.updatedAt)}</p></div></div><button type="button" className={`button min-h-12 ${enabled ? "button-primary" : "bg-amber-700 text-white hover:bg-amber-800"}`} onClick={() => setConfirm(!enabled)}>{enabled ? "Turn maintenance off" : "Turn maintenance on"}</button></div>
    </div>
    <div className="mt-7 border border-line bg-surface p-6"><h2 className="font-display text-2xl font-bold">Before enabling</h2><ul className="mt-4 list-disc space-y-2 pl-5 leading-7 text-muted"><li>Normal catalog, auth, member and loan API calls return a stable 503 contract.</li><li>Existing database data and active loans remain unchanged.</li><li>Keep this page open; refreshing the browser loses the in-memory ADMIN access token while refresh is paused.</li></ul></div>
    <ConfirmDialog open={confirm !== null} title={confirm ? "Enable maintenance mode?" : "Resume normal traffic?"} description={confirm ? "Readers will be redirected to the maintenance screen. Only this protected control and health remain available." : "Normal API traffic will immediately resume."} confirmLabel={confirm ? "Enable maintenance" : "Resume BookAura"} pending={toggle.isPending} onCancel={() => { setConfirm(null); toggle.reset(); }} onConfirm={() => confirm !== null && toggle.mutate(confirm)} error={toggle.isError ? <p className="mt-4 border-l-4 border-danger bg-red-50 p-3 text-sm text-red-950" role="alert">{toDisplayError(toggle.error).message}</p> : undefined} />
  </section>;
}
