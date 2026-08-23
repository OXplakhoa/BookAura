import { api, refreshSession } from "../lib/api";
import type { AuthResponse, MessageResponse, UserSummary } from "../types/api";

export interface LoginInput {
  identifier: string;
  password: string;
}

export interface RegisterInput {
  fullName: string;
  email: string;
  phone?: string;
  password: string;
}

export async function login(input: LoginInput): Promise<AuthResponse> {
  return (await api.post<AuthResponse>("/auth/login", input)).data;
}

export async function register(input: RegisterInput): Promise<MessageResponse> {
  return (await api.post<MessageResponse>("/auth/register", input)).data;
}

export async function verifyEmail(token: string): Promise<MessageResponse> {
  return (await api.post<MessageResponse>("/auth/verify-email", { token })).data;
}

export async function resendVerification(email: string): Promise<MessageResponse> {
  return (await api.post<MessageResponse>("/auth/resend-verification", { email })).data;
}

export async function getOAuthProviders(): Promise<{ google: boolean }> {
  return (await api.get<{ google: boolean }>("/auth/oauth/providers")).data;
}

export async function exchangeOAuthCode(code: string): Promise<AuthResponse> {
  return (await api.post<AuthResponse>("/auth/oauth/exchange", { code })).data;
}

export function googleAuthorizationUrl(): string {
  const baseUrl = import.meta.env.VITE_OAUTH_BASE_URL ?? (import.meta.env.DEV ? "http://localhost:8080" : "");
  return `${baseUrl}/oauth2/authorization/google`;
}

export async function requestPhoneOtp(phone: string): Promise<MessageResponse> {
  return (await api.post<MessageResponse>("/auth/phone-otp/request", { phone })).data;
}

export async function confirmPhoneOtp(phone: string, code: string): Promise<AuthResponse> {
  return (await api.post<AuthResponse>("/auth/phone-otp/confirm", { phone, code })).data;
}

export async function currentUser(): Promise<UserSummary> {
  return (await api.get<UserSummary>("/auth/me")).data;
}

export async function logout(): Promise<void> {
  await api.post("/auth/logout");
}

export { refreshSession };
