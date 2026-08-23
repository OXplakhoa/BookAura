import { api } from "../lib/api";
import type { UserSummary } from "../types/api";

export async function requestEmailChange(newEmail: string): Promise<{ message: string }> {
  return (await api.post<{ message: string }>("/account/email-change/request", { newEmail })).data;
}

export async function confirmEmailChange(code: string): Promise<{ message: string; user: UserSummary }> {
  return (await api.post<{ message: string; user: UserSummary }>("/account/email-change/confirm", { code })).data;
}
