import { useEffect, useMemo, useState, type ReactNode } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { login, logout, refreshSession, register, type LoginInput, type RegisterInput } from "./auth-api";
import { AuthContext, type AuthContextValue } from "./auth-context";
import { clearSession, getSession, setSession, subscribeSession } from "./session-store";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSnapshot] = useState(getSession);
  const [ready, setReady] = useState(false);
  const queryClient = useQueryClient();

  useEffect(() => {
    const unsubscribe = subscribeSession(setSnapshot);
    refreshSession().catch(() => undefined).finally(() => setReady(true));
    return unsubscribe;
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    user: session.user,
    ready,
    authenticated: Boolean(session.accessToken && session.user),
    isAdmin: Boolean(session.user?.roles.includes("ADMIN")),
    signIn: async (input: LoginInput) => {
      const response = await login(input);
      setSession(response);
      return response.user;
    },
    signUp: async (input: RegisterInput) => (await register(input)).message,
    signOut: async () => {
      try {
        if (getSession().accessToken) {
          await logout();
        }
      } finally {
        clearSession();
        queryClient.clear();
      }
    },
  }), [queryClient, ready, session]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
