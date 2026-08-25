import { describe, expect, it } from "vitest";
import { emptyAuraSearch } from "./aura-api";
import { resolveArcaneAtmosphere } from "./arcane-atmosphere";

describe("Arcane Opus atmosphere", () => {
  it("reacts to the primary selected mood", () => {
    expect(resolveArcaneAtmosphere({ ...emptyAuraSearch, moods: ["cozy"] }).name).toBe("The Ember Opus");
    expect(resolveArcaneAtmosphere({ ...emptyAuraSearch, moods: ["dark"] }).name).toBe("The Nocturne Opus");
    expect(resolveArcaneAtmosphere({ ...emptyAuraSearch, moods: ["romantic"] }).name).toBe("The Rosebound Opus");
  });

  it("derives a restrained atmosphere from themes when no mood is selected", () => {
    expect(resolveArcaneAtmosphere({ ...emptyAuraSearch, themes: ["Adventure"] }).name).toBe("The Wayfinder Opus");
    expect(resolveArcaneAtmosphere({ ...emptyAuraSearch, themes: ["Philosophy"] }).name).toBe("The Oracle Opus");
  });
});
