import type { AuraSearch } from "./aura-api";

export interface ArcaneAtmosphere {
  name: string;
  incantation: string;
  background: string;
  wood: string;
  woodDark: string;
  aura: string;
  auraSoft: string;
  accent: string;
}

const ATMOSPHERES: Record<string, ArcaneAtmosphere> = {
  cozy: {
    name: "The Ember Opus",
    incantation: "A warm constellation for quiet chapters",
    background: "#100b09",
    wood: "#392218",
    woodDark: "#170d09",
    aura: "#e6a85c",
    auraSoft: "#7d3d25",
    accent: "#f0cf8c",
  },
  adventurous: {
    name: "The Wayfinder Opus",
    incantation: "A compass drawn in ink and old starlight",
    background: "#07110f",
    wood: "#34251a",
    woodDark: "#130d09",
    aura: "#69b88c",
    auraSoft: "#244f3b",
    accent: "#d6b96f",
  },
  romantic: {
    name: "The Rosebound Opus",
    incantation: "Tender pages gathered beneath a crimson moon",
    background: "#13090d",
    wood: "#3d211d",
    woodDark: "#180b0c",
    aura: "#d77d8d",
    auraSoft: "#6b293f",
    accent: "#e7c58f",
  },
  dark: {
    name: "The Nocturne Opus",
    incantation: "Forbidden volumes awakened after midnight",
    background: "#05070d",
    wood: "#24202a",
    woodDark: "#0b0910",
    aura: "#788bc7",
    auraSoft: "#262b5c",
    accent: "#c0acd7",
  },
  funny: {
    name: "The Mercurial Opus",
    incantation: "Mischief, wit, and improbable little worlds",
    background: "#100d06",
    wood: "#3e2a17",
    woodDark: "#171006",
    aura: "#e0b84f",
    auraSoft: "#715326",
    accent: "#efe0a0",
  },
  thoughtful: {
    name: "The Oracle Opus",
    incantation: "Ideas held in the blue hour between worlds",
    background: "#06100f",
    wood: "#2d2920",
    woodDark: "#100e0a",
    aura: "#63bfb2",
    auraSoft: "#1f5b57",
    accent: "#d4c58b",
  },
  inspiring: {
    name: "The Verdant Opus",
    incantation: "New beginnings illuminated in living gold",
    background: "#08100a",
    wood: "#31291b",
    woodDark: "#100e08",
    aura: "#8dc77b",
    auraSoft: "#355d34",
    accent: "#e2c975",
  },
};

const DEFAULT_ATMOSPHERE = ATMOSPHERES.thoughtful;

export function resolveArcaneAtmosphere(search: AuraSearch): ArcaneAtmosphere {
  const mood = search.moods[0]?.toLowerCase();
  if (mood && ATMOSPHERES[mood]) return ATMOSPHERES[mood];

  const signal = search.themes.join(" ").toLowerCase();
  if (/romance|love/.test(signal)) return ATMOSPHERES.romantic;
  if (/horror|dystopia|crime|dark/.test(signal)) return ATMOSPHERES.dark;
  if (/adventure|fantasy|travel/.test(signal)) return ATMOSPHERES.adventurous;
  if (/comedy|humor/.test(signal)) return ATMOSPHERES.funny;
  if (/self-help|inspir|biography/.test(signal)) return ATMOSPHERES.inspiring;
  return DEFAULT_ATMOSPHERE;
}
