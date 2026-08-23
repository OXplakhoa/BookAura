import { BookOpen, History, LayoutDashboard, Library, LogOut, ShieldCheck, UserRound } from "lucide-react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/use-auth";
import { Brand } from "../components/Brand";

const userNav = [
  { to: "/catalog", label: "Browse", icon: BookOpen },
  { to: "/app/loans", label: "Active loans", icon: Library },
  { to: "/app/history", label: "History", icon: History },
  { to: "/app/account", label: "Account", icon: UserRound },
];

export function AppShell() {
  const auth = useAuth();
  const navigate = useNavigate();

  async function handleSignOut() {
    await auth.signOut();
    navigate("/", { replace: true });
  }

  return (
    <div className="min-h-dvh bg-canvas text-ink lg:grid lg:grid-cols-[264px_1fr]">
      <a href="#app-content" className="skip-link">Skip to main content</a>
      <aside className="hidden border-r border-line bg-surface lg:sticky lg:top-0 lg:flex lg:h-dvh lg:flex-col">
        <div className="border-b border-line px-6 py-5"><Brand /></div>
        <nav className="flex-1 space-y-1 p-4" aria-label="Dashboard navigation">
          {userNav.map(({ to, label, icon: Icon }) => (
            <NavLink key={to} to={to} className={({ isActive }) => `side-link ${isActive ? "side-link-active" : ""}`}>
              <Icon size={19} aria-hidden="true" />{label}
            </NavLink>
          ))}
          {auth.isAdmin && (
            <NavLink to="/admin" className={({ isActive }) => `side-link ${isActive ? "side-link-active" : ""}`}>
              <ShieldCheck size={19} aria-hidden="true" />Admin workspace
            </NavLink>
          )}
        </nav>
        <div className="border-t border-line p-4">
          <div className="mb-3 px-3">
            <p className="truncate text-sm font-semibold">{auth.user?.fullName}</p>
            <p className="truncate text-xs text-muted">{auth.user?.email}</p>
          </div>
          <button type="button" onClick={handleSignOut} className="side-link w-full text-danger">
            <LogOut size={19} aria-hidden="true" />Sign out
          </button>
        </div>
      </aside>

      <div className="min-w-0">
        <header className="sticky top-0 z-20 border-b border-line bg-surface/95 lg:hidden">
          <div className="flex min-h-[68px] items-center justify-between gap-3 px-4">
            <Brand compact />
            <div className="flex min-w-0 items-center gap-1">
              <span className="truncate text-sm font-semibold">{auth.user?.fullName}</span>
              <button type="button" onClick={handleSignOut} className="icon-button shrink-0 text-danger" aria-label="Sign out"><LogOut size={20} /></button>
            </div>
          </div>
          <nav className="flex overflow-x-auto border-t border-line px-2" aria-label="Mobile dashboard navigation">
            {userNav.map(({ to, label, icon: Icon }) => (
              <NavLink key={to} to={to} className={({ isActive }) => `mobile-tab ${isActive ? "mobile-tab-active" : ""}`}>
                <Icon size={18} aria-hidden="true" />{label}
              </NavLink>
            ))}
            {auth.isAdmin && <NavLink to="/admin" className={({ isActive }) => `mobile-tab ${isActive ? "mobile-tab-active" : ""}`}><LayoutDashboard size={18} />Admin</NavLink>}
          </nav>
        </header>
        <main id="app-content" tabIndex={-1} className="page-container py-8 lg:py-10"><Outlet /></main>
      </div>
    </div>
  );
}
