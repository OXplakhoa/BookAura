import type { PageResponse } from "../types/api";

export interface Loan {
  id: string;
  memberId: string;
  userAccountId: string;
  memberName: string;
  bookId: string;
  bookTitle: string;
  isbn: string;
  borrowedAt: string;
  dueAt: string;
  returnedAt: string | null;
  status: "ACTIVE" | "RETURNED" | "OVERDUE";
  overdue: boolean;
}

export type LoanPage = PageResponse<Loan>;
