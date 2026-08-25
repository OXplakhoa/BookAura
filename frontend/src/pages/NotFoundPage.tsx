import { SearchX } from "lucide-react";
import { Link } from "react-router-dom";
import { LanguageSwitcher } from "../components/LanguageSwitcher";
import { useLanguage } from "../i18n/language";

export function NotFoundPage() {
  const { t } = useLanguage();
  return <main className="relative grid min-h-dvh place-items-center bg-canvas px-5"><div className="absolute right-5 top-5"><LanguageSwitcher /></div><div className="max-w-lg text-center"><SearchX className="mx-auto text-primary" size={48} /><p className="eyebrow mt-6">{t("notFound.eyebrow")}</p><h1 className="mt-3 font-display text-4xl font-bold">{t("notFound.title")}</h1><p className="mt-4 leading-7 text-muted">{t("notFound.text")}</p><Link className="button button-primary mt-7" to="/">{t("notFound.return")}</Link></div></main>;
}
