import { createContext } from "react";
import type { LoginInput, RegisterInput } from "./auth-api";
import type { UserSummary } from "../types/api";

export interface AuthContextValue {
  user: UserSummary | null;
  ready: boolean;
  authenticated: boolean;
  isAdmin: boolean;
  signIn: (input: LoginInput) => Promise<UserSummary>;
  signUp: (input: RegisterInput) => Promise<string>;
  completeOAuth: (code: string) => Promise<UserSummary>;
  signOut: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextValue | null>(null);
