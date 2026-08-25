import { Languages } from "lucide-react";
import { useLanguage } from "../i18n/language";

export function LanguageSwitcher({ inverted = false }: { inverted?: boolean }) {
  const { language, setLanguage, t } = useLanguage();
  const muted = inverted ? "text-stone-300" : "text-muted";
  const idle = inverted ? "text-stone-300 hover:bg-stone-800" : "text-muted hover:bg-canvas";

  return (
    <div className={`inline-flex min-h-11 items-center gap-1 rounded-full border px-1.5 ${inverted ? "border-stone-600 bg-stone-900/60" : "border-line bg-surface"}`} role="group" aria-label={t("language.label")}>
      <Languages size={15} className={`ml-1 ${muted}`} aria-hidden="true" />
      <button
        type="button"
        className={`min-h-8 rounded-full px-2.5 text-[11px] font-bold tracking-wide transition-colors ${language === "vi" ? "bg-primary text-white" : idle}`}
        aria-pressed={language === "vi"}
        title={t("language.vietnamese")}
        onClick={() => setLanguage("vi")}
      >
        VN
      </button>
      <button
        type="button"
        className={`min-h-8 rounded-full px-2.5 text-[11px] font-bold tracking-wide transition-colors ${language === "en" ? "bg-primary text-white" : idle}`}
        aria-pressed={language === "en"}
        title={t("language.english")}
        onClick={() => setLanguage("en")}
      >
        EN
      </button>
    </div>
  );
}
