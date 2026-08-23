type MaintenanceListener = () => void;

const listeners = new Set<MaintenanceListener>();
let maintenanceNotified = false;

export function notifyMaintenance(): void {
  maintenanceNotified = true;
  listeners.forEach((listener) => listener());
}

export function isMaintenanceNotified(): boolean {
  return maintenanceNotified;
}

export function clearMaintenanceNotification(): void {
  maintenanceNotified = false;
}

export function subscribeMaintenance(listener: MaintenanceListener): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}
