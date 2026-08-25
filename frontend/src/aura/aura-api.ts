import { api } from "../lib/api";

export interface AuraRecommendation {
  bookId: string;
  title: string;
  authors: string[];
  categories: string[];
  publicationYear: number;
  pageCount: number | null;
  availableQuantity: number;
  score: number;
  reasons: string[];
  matchedTags: string[];
}

export interface AuraSearch {
  moods: string[];
  timeMinutes: number | null;
  themes: string[];
  intensity: string | null;
}

export const MOOD_VOCABULARY = [
  { value: "cozy", label: "Cozy", icon: "🕯️" },
  { value: "adventurous", label: "Adventurous", icon: "🧭" },
  { value: "romantic", label: "Romantic", icon: "💌" },
  { value: "dark", label: "Dark", icon: "🌒" },
  { value: "funny", label: "Funny", icon: "🎭" },
  { value: "thoughtful", label: "Thoughtful", icon: "💭" },
  { value: "inspiring", label: "Inspiring", icon: "🌱" },
] as const;

export const INTENSITY_OPTIONS = [
  { value: "light", label: "Light", hint: "under 250 pages" },
  { value: "medium", label: "Medium", hint: "250–500 pages" },
  { value: "deep", label: "Deep", hint: "500+ pages" },
] as const;

export const emptyAuraSearch: AuraSearch = { moods: [], timeMinutes: null, themes: [], intensity: null };

export function hasAuraSignals(search: AuraSearch): boolean {
  return search.moods.length > 0 || search.themes.length > 0;
}

export function readAuraSearch(params: URLSearchParams): AuraSearch {
  const time = Number(params.get("time"));
  const intensity = params.get("intensity");
  return {
    moods: (params.get("moods") ?? "").split(",").filter(Boolean),
    timeMinutes: Number.isFinite(time) && time > 0 ? time : null,
    themes: (params.get("themes") ?? "").split(",").filter(Boolean),
    intensity: intensity && ["light", "medium", "deep"].includes(intensity) ? intensity : null,
  };
}

export function writeAuraSearch(search: AuraSearch): URLSearchParams {
  const params = new URLSearchParams();
  if (search.moods.length > 0) params.set("moods", search.moods.join(","));
  if (search.timeMinutes) params.set("time", String(search.timeMinutes));
  if (search.themes.length > 0) params.set("themes", search.themes.join(","));
  if (search.intensity) params.set("intensity", search.intensity);
  return params;
}

export async function getAuraRecommendations(search: AuraSearch): Promise<AuraRecommendation[]> {
  const params: Record<string, string | number> = {};
  if (search.moods.length > 0) params.moods = search.moods.join(",");
  if (search.themes.length > 0) params.themes = search.themes.join(",");
  if (search.timeMinutes) params.timeMinutes = search.timeMinutes;
  if (search.intensity) params.intensity = search.intensity;
  return (await api.get<AuraRecommendation[]>("/recommendations/aura", { params })).data;
}

export async function getCategoryNames(): Promise<string[]> {
  return (await api.get<string[]>("/books/categories")).data;
}
