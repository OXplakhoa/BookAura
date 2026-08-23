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

export async function currentUser(): Promise<UserSummary> {
  return (await api.get<UserSummary>("/auth/me")).data;
}

export async function logout(): Promise<void> {
  await api.post("/auth/logout");
}

export { refreshSession };
