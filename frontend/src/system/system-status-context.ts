import { createContext } from "react";

export interface SystemStatusContextValue {
  maintenance: boolean;
  clearMaintenance: () => void;
}

export const SystemStatusContext = createContext<SystemStatusContextValue | null>(null);
