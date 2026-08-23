import { useEffect, useMemo, useState, type ReactNode } from "react";
import { clearMaintenanceNotification, isMaintenanceNotified, subscribeMaintenance } from "../lib/system-events";
import { SystemStatusContext } from "./system-status-context";

export function SystemStatusProvider({ children }: { children: ReactNode }) {
  const [maintenance, setMaintenance] = useState(isMaintenanceNotified);

  useEffect(() => subscribeMaintenance(() => setMaintenance(true)), []);

  const value = useMemo(() => ({
    maintenance,
    clearMaintenance: () => {
      clearMaintenanceNotification();
      setMaintenance(false);
    },
  }), [maintenance]);

  return <SystemStatusContext.Provider value={value}>{children}</SystemStatusContext.Provider>;
}
