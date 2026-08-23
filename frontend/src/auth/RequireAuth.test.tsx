import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { AuthContext, type AuthContextValue } from "./auth-context";
import { RequireAuth } from "./RequireAuth";

const baseAuth: AuthContextValue = {
  user: null,
  ready: true,
  authenticated: false,
  isAdmin: false,
  signIn: vi.fn(),
  signUp: vi.fn(),
  signOut: vi.fn(),
};

function renderGuard(auth: AuthContextValue, admin = false) {
  render(
    <AuthContext.Provider value={auth}>
      <MemoryRouter initialEntries={["/private"]}>
        <Routes>
          <Route path="/login" element={<p>Login page</p>} />
          <Route path="/forbidden" element={<p>Forbidden page</p>} />
          <Route element={<RequireAuth admin={admin} />}>
            <Route path="/private" element={<p>Protected content</p>} />
          </Route>
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}

describe("RequireAuth", () => {
  it("redirects anonymous visitors to login", () => {
    renderGuard(baseAuth);
    expect(screen.getByText("Login page")).toBeInTheDocument();
  });

  it("keeps non-admin users out of admin routes", () => {
    renderGuard({ ...baseAuth, authenticated: true, user: { id: "1", email: "u@test.dev", fullName: "User", roles: ["USER"] } }, true);
    expect(screen.getByText("Forbidden page")).toBeInTheDocument();
  });

  it("renders protected content for an authorized user", () => {
    renderGuard({ ...baseAuth, authenticated: true, isAdmin: true, user: { id: "1", email: "a@test.dev", fullName: "Admin", roles: ["ADMIN"] } }, true);
    expect(screen.getByText("Protected content")).toBeInTheDocument();
  });
});
