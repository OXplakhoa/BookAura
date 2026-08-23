import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import { clearSession, getSession, setSession } from "../auth/session-store";
import type { ApiErrorResponse, AuthResponse } from "../types/api";
import { notifyMaintenance } from "./system-events";

const baseURL = import.meta.env.VITE_API_URL ?? "/api";

export const api = axios.create({ baseURL, withCredentials: true });
const refreshClient = axios.create({ baseURL, withCredentials: true });

interface RetryableRequest extends InternalAxiosRequestConfig {
  _bookAuraRetried?: boolean;
}

let refreshPromise: Promise<AuthResponse> | null = null;

function isAuthLifecycleRequest(url?: string): boolean {
  return Boolean(url && ["/auth/login", "/auth/register", "/auth/refresh", "/auth/verify-email"].some(
    (path) => url.startsWith(path),
  ));
}

function handleMaintenance(error: AxiosError<ApiErrorResponse>): void {
  if (error.response?.status === 503 && error.response.data?.code === "MAINTENANCE_MODE") {
    notifyMaintenance();
  }
}

export async function refreshSession(): Promise<AuthResponse> {
  if (!refreshPromise) {
    refreshPromise = refreshClient.post<AuthResponse>("/auth/refresh")
      .then(({ data }) => {
        setSession(data);
        return data;
      })
      .catch((error: AxiosError<ApiErrorResponse>) => {
        handleMaintenance(error);
        clearSession();
        throw error;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

api.interceptors.request.use((config) => {
  const token = getSession().accessToken;
  if (token) {
    config.headers.set("Authorization", `Bearer ${token}`);
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiErrorResponse>) => {
    handleMaintenance(error);
    const request = error.config as RetryableRequest | undefined;
    if (!request || error.response?.status !== 401 || request._bookAuraRetried
        || isAuthLifecycleRequest(request.url)) {
      return Promise.reject(error);
    }

    request._bookAuraRetried = true;
    try {
      await refreshSession();
      return api.request(request);
    } catch {
      clearSession();
      return Promise.reject(error);
    }
  },
);
