import type { Language } from "../i18n/language";

function dateFormat(language: Language): Intl.DateTimeFormat {
  return new Intl.DateTimeFormat(language === "vi" ? "vi-VN" : "en-GB", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    timeZone: "UTC",
  });
}

function dateTimeFormat(language: Language): Intl.DateTimeFormat {
  return new Intl.DateTimeFormat(language === "vi" ? "vi-VN" : "en-GB", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function formatDate(value: string, language: Language = "en"): string {
  return dateFormat(language).format(new Date(value));
}

export function formatDateTime(value: string, language: Language = "en"): string {
  return dateTimeFormat(language).format(new Date(value));
}
