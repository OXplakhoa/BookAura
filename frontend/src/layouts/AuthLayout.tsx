import { Outlet } from "react-router-dom";
import { Brand } from "../components/Brand";
import { LanguageSwitcher } from "../components/LanguageSwitcher";
import { useLanguage } from "../i18n/language";

export function AuthLayout() {
  const { t } = useLanguage();
  return (
    <main className="grid min-h-dvh bg-canvas lg:grid-cols-[minmax(0,1fr)_minmax(440px,0.72fr)]">
      <section className="relative hidden overflow-hidden bg-ink p-12 text-stone-100 lg:flex lg:flex-col lg:justify-between">
        <Brand inverted />
        <div className="max-w-xl">
          <p className="eyebrow text-amber-300">{t("auth.heroEyebrow")}</p>
          <h1 className="mt-5 font-display text-5xl font-bold leading-[1.08]">{t("auth.heroTitle")}</h1>
          <p className="mt-6 max-w-lg text-lg leading-8 text-stone-300">{t("auth.heroText")}</p>
        </div>
        <div className="flex gap-3" aria-hidden="true">
          {[56, 72, 48, 88, 64, 78].map((height, index) => <span key={height} className={`w-10 rounded-t-sm border border-stone-600 ${index === 3 ? "bg-primary" : "bg-stone-800"}`} style={{ height }} />)}
        </div>
      </section>
      <section className="relative flex min-h-dvh items-center justify-center px-5 py-10 sm:px-10">
        <div className="absolute right-5 top-5 sm:right-10 sm:top-8"><LanguageSwitcher /></div>
        <div className="w-full max-w-md">
          <div className="mb-10 lg:hidden"><Brand /></div>
          <Outlet />
        </div>
      </section>
    </main>
  );
}
