import { Menu, X } from "lucide-react";
import { useState } from "react";
import { Link, NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/use-auth";
import { Brand } from "../components/Brand";
import { LanguageSwitcher } from "../components/LanguageSwitcher";
import { useLanguage } from "../i18n/language";

const navClass = ({ isActive }: { isActive: boolean }) =>
  `nav-link ${isActive ? "nav-link-active" : ""}`;

export function PublicLayout() {
  const [open, setOpen] = useState(false);
  const auth = useAuth();
  const { t } = useLanguage();

  return (
    <div className="min-h-dvh bg-canvas text-ink">
      <a href="#main-content" className="skip-link">{t("common.skip")}</a>
      <header className="border-b border-line bg-surface/95">
        <div className="page-container flex min-h-[76px] items-center justify-between gap-5">
          <Brand />
          <button type="button" className="icon-button md:hidden" onClick={() => setOpen((value) => !value)} aria-label={open ? t("common.close") : t("common.open")} aria-expanded={open}>
            {open ? <X size={22} /> : <Menu size={22} />}
          </button>
          <div className={`${open ? "flex" : "hidden"} absolute inset-x-0 top-[76px] z-30 flex-col gap-3 border-b border-line bg-surface p-5 shadow-card md:static md:z-auto md:flex md:flex-row md:items-center md:border-0 md:bg-transparent md:p-0 md:shadow-none`}>
            <nav className="flex flex-col gap-1 md:flex-row md:items-center" aria-label={t("nav.main")}>
              <NavLink to="/catalog" className={navClass} onClick={() => setOpen(false)}>{t("nav.browse")}</NavLink>
              <NavLink to="/aura" className={navClass} onClick={() => setOpen(false)}>{t("nav.aura")}</NavLink>
              {auth.authenticated && <NavLink to="/app/loans" className={navClass} onClick={() => setOpen(false)}>{t("nav.library")}</NavLink>}
              {auth.isAdmin && <NavLink to="/admin" className={navClass} onClick={() => setOpen(false)}>{t("nav.admin")}</NavLink>}
            </nav>
            <div className="mt-2 flex flex-wrap items-center gap-2 border-t border-line pt-4 md:ml-3 md:mt-0 md:border-l md:border-t-0 md:pl-5 md:pt-0">
              <LanguageSwitcher />
              {auth.authenticated ? (
                <Link className="button button-primary" to={auth.isAdmin ? "/admin" : "/app/loans"}>{t("nav.dashboard")}</Link>
              ) : (
                <>
                  <Link className="button button-ghost" to="/login">{t("common.signIn")}</Link>
                  <Link className="button button-primary" to="/register">{t("common.register")}</Link>
                </>
              )}
            </div>
          </div>
        </div>
      </header>
      <main id="main-content" tabIndex={-1}><Outlet /></main>
      <footer className="border-t border-line bg-ink py-8 text-stone-200">
        <div className="page-container flex flex-col gap-3 text-sm sm:flex-row sm:items-center sm:justify-between">
          <p>{t("footer.copyright")}</p>
          <p className="text-stone-400">{t("footer.tagline")}</p>
        </div>
      </footer>
    </div>
  );
}
