import { beforeEach, describe, expect, it, vi } from "vitest";
import { clearSession, getSession, setSession, setSessionUser, subscribeSession } from "./session-store";

const authResponse = {
  accessToken: "memory-only-token",
  tokenType: "Bearer" as const,
  expiresIn: 900,
  user: { id: "user-1", email: "reader@test.dev", fullName: "Reader", roles: ["USER"] },
};

describe("session store", () => {
  beforeEach(clearSession);

  it("keeps the access token in memory and notifies subscribers", () => {
    const listener = vi.fn();
    const unsubscribe = subscribeSession(listener);

    setSession(authResponse);

    expect(getSession()).toEqual({ accessToken: "memory-only-token", user: authResponse.user });
    expect(listener).toHaveBeenCalledWith(getSession());
    unsubscribe();
  });

  it("updates returned profile data without replacing the access token", () => {
    setSession(authResponse);
    setSessionUser({ ...authResponse.user, email: "changed@test.dev" });
    expect(getSession()).toEqual({
      accessToken: "memory-only-token",
      user: { ...authResponse.user, email: "changed@test.dev" },
    });
  });

  it("removes both identity and access token on clear", () => {
    setSession(authResponse);
    clearSession();
    expect(getSession()).toEqual({ accessToken: null, user: null });
  });
});
