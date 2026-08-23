import { api } from "../lib/api";
import type { Loan, LoanPage } from "./loan-types";

export async function getActiveLoans(page: number): Promise<LoanPage> {
  return (await api.get<LoanPage>("/loans/active", { params: { page, size: 10, sort: "dueAt:asc" } })).data;
}

export async function getLoanHistory(page: number): Promise<LoanPage> {
  return (await api.get<LoanPage>("/loans/history", { params: { page, size: 10, sort: "returnedAt:desc" } })).data;
}

export async function returnLoan(loanId: string): Promise<Loan> {
  return (await api.post<Loan>(`/loans/${loanId}/return`)).data;
}
