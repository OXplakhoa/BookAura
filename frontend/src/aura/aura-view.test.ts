import { afterEach, describe, expect, it } from "vitest";
import { chooseAuraViewMode, supportsAuraShelf } from "./aura-view";

const originalCss = window.CSS;

describe("Shelf Aura view selection", () => {
  it("chooses the 3D shelf only when motion and browser support allow it", () => {
    expect(chooseAuraViewMode({ reducedMotion: false, supports3d: true })).toBe("shelf");
    expect(chooseAuraViewMode({ reducedMotion: true, supports3d: true })).toBe("cards");
    expect(chooseAuraViewMode({ reducedMotion: false, supports3d: false })).toBe("cards");
  });

  it("detects the CSS 3D capabilities used by the shelf", () => {
    Object.defineProperty(window, "CSS", { configurable: true, value: { supports: () => true } });
    expect(supportsAuraShelf()).toBe(true);

    Object.defineProperty(window, "CSS", { configurable: true, value: { supports: () => false } });
    expect(supportsAuraShelf()).toBe(false);

    Object.defineProperty(window, "CSS", { configurable: true, value: originalCss });
  });

  afterEach(() => {
    // Keep jsdom's global capability object intact for the rest of the suite.
    Object.defineProperty(window, "CSS", { configurable: true, value: originalCss });
  });
});
