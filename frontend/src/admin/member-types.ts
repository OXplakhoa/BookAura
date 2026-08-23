import type { PageResponse } from "../types/api";

export interface Member {
  id: string;
  userAccountId: string;
  fullName: string;
  email: string;
  phone: string | null;
  dateOfBirth: string | null;
  address: string | null;
  status: "ACTIVE" | "DISABLED";
  emailVerified: boolean;
  roles: string[];
  createdAt: string;
  updatedAt: string;
}

export interface MemberCreateInput {
  fullName: string;
  email: string;
  phone?: string;
  initialPassword: string;
  dateOfBirth?: string;
  address?: string;
  emailVerified: boolean;
  active: boolean;
}

export interface MemberUpdateInput {
  fullName: string;
  phone?: string;
  dateOfBirth?: string;
  address?: string;
  active: boolean;
}

export type MemberPage = PageResponse<Member>;
