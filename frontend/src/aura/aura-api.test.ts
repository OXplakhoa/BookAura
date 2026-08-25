import { describe, expect, it } from "vitest";
import {
  emptyAuraSearch,
  hasAuraSignals,
  readAuraSearch,
  writeAuraSearch,
} from "./aura-api";

describe("Shelf Aura URL state", () => {
  it("reads moods, time, themes, and intensity", () => {
    expect(readAuraSearch(new URLSearchParams("moods=cozy,dark&time=180&themes=Poetry,Fantasy&intensity=deep")))
      .toEqual({ moods: ["cozy", "dark"], timeMinutes: 180, themes: ["Poetry", "Fantasy"], intensity: "deep" });
  });

  it("rejects invalid intensity and non-positive time values", () => {
    expect(readAuraSearch(new URLSearchParams("time=0&intensity=chaos")))
      .toEqual(emptyAuraSearch);
  });

  it("writes only selected signals for shareable URLs", () => {
    expect(writeAuraSearch({ moods: ["cozy"], timeMinutes: 120, themes: ["Poetry"], intensity: "light" }).toString())
      .toBe("moods=cozy&time=120&themes=Poetry&intensity=light");
    expect(writeAuraSearch(emptyAuraSearch).toString()).toBe("");
  });

  it("requires at least one mood or theme before querying", () => {
    expect(hasAuraSignals(emptyAuraSearch)).toBe(false);
    expect(hasAuraSignals({ ...emptyAuraSearch, moods: ["inspiring"] })).toBe(true);
    expect(hasAuraSignals({ ...emptyAuraSearch, themes: ["History"] })).toBe(true);
  });
});
