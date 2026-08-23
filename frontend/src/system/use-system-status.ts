import { useContext } from "react";
import { SystemStatusContext, type SystemStatusContextValue } from "./system-status-context";

export function useSystemStatus(): SystemStatusContextValue {
  const context = useContext(SystemStatusContext);
  if (!context) {
    throw new Error("useSystemStatus must be used within SystemStatusProvider");
  }
  return context;
}
