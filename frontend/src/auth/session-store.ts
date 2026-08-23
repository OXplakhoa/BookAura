import type { AuthResponse, UserSummary } from "../types/api";

export interface SessionSnapshot {
  accessToken: string | null;
  user: UserSummary | null;
}

type Listener = (snapshot: SessionSnapshot) => void;

let snapshot: SessionSnapshot = { accessToken: null, user: null };
const listeners = new Set<Listener>();

export function getSession(): SessionSnapshot {
  return snapshot;
}

export function setSession(response: AuthResponse): void {
  snapshot = { accessToken: response.accessToken, user: response.user };
  listeners.forEach((listener) => listener(snapshot));
}

export function setSessionUser(user: UserSummary): void {
  if (!snapshot.accessToken) return;
  snapshot = { ...snapshot, user };
  listeners.forEach((listener) => listener(snapshot));
}

export function clearSession(): void {
  snapshot = { accessToken: null, user: null };
  listeners.forEach((listener) => listener(snapshot));
}

export function subscribeSession(listener: Listener): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}
