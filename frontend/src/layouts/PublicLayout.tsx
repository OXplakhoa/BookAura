import { Menu, X } from "lucide-react";
import { useState } from "react";
import { Link, NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/use-auth";
import { Brand } from "../components/Brand";

const navClass = ({ isActive }: { isActive: boolean }) =>
  `nav-link ${isActive ? "nav-link-active" : ""}`;

export function PublicLayout() {
  const [open, setOpen] = useState(false);
  const auth = useAuth();

  return (
    <div className="min-h-dvh bg-canvas text-ink">
      <a href="#main-content" className="skip-link">Skip to main content</a>
      <header className="border-b border-line bg-surface/95">
        <div className="page-container flex min-h-[76px] items-center justify-between gap-5">
          <Brand />
          <button type="button" className="icon-button md:hidden" onClick={() => setOpen((value) => !value)} aria-label={open ? "Close navigation" : "Open navigation"} aria-expanded={open}>
            {open ? <X size={22} /> : <Menu size={22} />}
          </button>
          <div className={`${open ? "flex" : "hidden"} absolute inset-x-0 top-[76px] z-30 flex-col gap-3 border-b border-line bg-surface p-5 shadow-card md:static md:z-auto md:flex md:flex-row md:items-center md:border-0 md:bg-transparent md:p-0 md:shadow-none`}>
            <nav className="flex flex-col gap-1 md:flex-row md:items-center" aria-label="Main navigation">
              <NavLink to="/catalog" className={navClass} onClick={() => setOpen(false)}>Browse books</NavLink>
              <NavLink to="/aura" className={navClass} onClick={() => setOpen(false)}>Shelf Aura</NavLink>
              {auth.authenticated && <NavLink to="/app/loans" className={navClass} onClick={() => setOpen(false)}>My library</NavLink>}
              {auth.isAdmin && <NavLink to="/admin" className={navClass} onClick={() => setOpen(false)}>Admin</NavLink>}
            </nav>
            <div className="mt-2 flex gap-2 border-t border-line pt-4 md:ml-3 md:mt-0 md:border-l md:border-t-0 md:pl-5 md:pt-0">
              {auth.authenticated ? (
                <Link className="button button-primary" to={auth.isAdmin ? "/admin" : "/app/loans"}>Open dashboard</Link>
              ) : (
                <>
                  <Link className="button button-ghost" to="/login">Sign in</Link>
                  <Link className="button button-primary" to="/register">Join BookAura</Link>
                </>
              )}
            </div>
          </div>
        </div>
      </header>
      <main id="main-content" tabIndex={-1}><Outlet /></main>
      <footer className="border-t border-line bg-ink py-8 text-stone-200">
        <div className="page-container flex flex-col gap-3 text-sm sm:flex-row sm:items-center sm:justify-between">
          <p>© 2026 BookAura. A focused library experience.</p>
          <p className="text-stone-400">Browse · Borrow · Return · Remember</p>
        </div>
      </footer>
    </div>
  );
}
