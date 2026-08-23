import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { AuthContext, type AuthContextValue } from "../auth/auth-context";
import { OAuthCallbackPage } from "./OAuthCallbackPage";

function authValue(completeOAuth: AuthContextValue["completeOAuth"]): AuthContextValue {
  return {
    user: null, ready: true, authenticated: false, isAdmin: false,
    signIn: vi.fn(), signUp: vi.fn(), signOut: vi.fn(), completeOAuth, syncUser: vi.fn(),
  };
}

function renderCallback(entry: string, auth: AuthContextValue) {
  return render(<AuthContext.Provider value={auth}><MemoryRouter initialEntries={[entry]}><Routes><Route path="/oauth/callback" element={<OAuthCallbackPage />} /><Route path="/app/loans" element={<p>Active loans</p>} /></Routes></MemoryRouter></AuthContext.Provider>);
}

describe("OAuthCallbackPage", () => {
  it("exchanges the one-time code, removes it from browser history and navigates", async () => {
    const exchange = vi.fn().mockResolvedValue({ id: "1", email: "reader@test.dev", fullName: "Reader", roles: ["USER"] });
    window.history.pushState({}, "", "/oauth/callback?code=one-time-secret");
    renderCallback("/oauth/callback?code=one-time-secret", authValue(exchange));

    await waitFor(() => expect(exchange).toHaveBeenCalledWith("one-time-secret"));
    expect(window.location.search).toBe("");
    expect(await screen.findByText("Active loans")).toBeInTheDocument();
  });

  it("shows a safe recovery path when the provider redirects an error", async () => {
    renderCallback("/oauth/callback?error=access_denied", authValue(vi.fn()));
    expect(await screen.findByRole("heading", { name: "Sign-in interrupted" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Return to sign in" })).toBeInTheDocument();
  });
});
