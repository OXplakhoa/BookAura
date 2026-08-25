import { api } from "../lib/api";
import type { Book, BookPage } from "../catalog/catalog-types";
import type { Loan, LoanPage } from "../loans/loan-types";
import type { Member, MemberCreateInput, MemberPage, MemberUpdateInput } from "./member-types";

export interface BookInput {
  title: string;
  isbn: string;
  description?: string;
  publicationYear: number;
  totalQuantity: number;
  authors: string[];
  categories: string[];
  pageCount?: number | null;
  tags?: string[];
  active: boolean;
}

export interface SystemConfiguration {
  maintenanceMode: boolean;
  updatedAt: string;
  updatedBy: string | null;
}

export async function searchAdminBooks(title: string, active: string, page: number): Promise<BookPage> {
  return (await api.get<BookPage>("/admin/books", { params: {
    title: title || undefined,
    active: active === "all" ? undefined : active === "active",
    page,
    size: 10,
    sort: "createdAt:desc",
  } })).data;
}
export async function getAdminBook(id: string): Promise<Book> { return (await api.get<Book>(`/admin/books/${id}`)).data; }
export async function createBook(input: BookInput): Promise<Book> { return (await api.post<Book>("/admin/books", input)).data; }
export async function updateBook(id: string, input: BookInput): Promise<Book> { return (await api.put<Book>(`/admin/books/${id}`, input)).data; }
export async function deleteBook(id: string): Promise<void> { await api.delete(`/admin/books/${id}`); }
export async function importBooks(file: File): Promise<{ importedCount: number }> {
  const form = new FormData();
  form.append("file", file);
  return (await api.post<{ importedCount: number }>("/admin/books/import", form)).data;
}

export async function searchMembers(params: Record<string, string | number | boolean | undefined>): Promise<MemberPage> {
  return (await api.get<MemberPage>("/admin/members", { params: { ...params, size: 10 } })).data;
}
export async function getMember(id: string): Promise<Member> { return (await api.get<Member>(`/admin/members/${id}`)).data; }
export async function createMember(input: MemberCreateInput): Promise<Member> { return (await api.post<Member>("/admin/members", input)).data; }
export async function updateMember(id: string, input: MemberUpdateInput): Promise<Member> { return (await api.put<Member>(`/admin/members/${id}`, input)).data; }
export async function disableMember(id: string): Promise<void> { await api.delete(`/admin/members/${id}`); }

export async function getAdminLoans(active: string, page: number): Promise<LoanPage> {
  return (await api.get<LoanPage>("/admin/loans", { params: {
    active: active === "all" ? undefined : active === "active",
    page, size: 10, sort: "borrowedAt:desc",
  } })).data;
}
export async function returnLoanAsAdmin(id: string): Promise<Loan> { return (await api.post<Loan>(`/admin/loans/${id}/return`)).data; }

export async function getSystemConfiguration(): Promise<SystemConfiguration> { return (await api.get<SystemConfiguration>("/admin/system-config")).data; }
export async function setMaintenanceMode(enabled: boolean): Promise<SystemConfiguration> { return (await api.put<SystemConfiguration>("/admin/system-config/maintenance", { enabled })).data; }
