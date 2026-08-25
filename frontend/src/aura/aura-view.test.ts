import { afterEach, describe, expect, it } from "vitest";
import { chooseAuraViewMode, supportsAuraShelf } from "./aura-view";

const originalGetContext = Object.getOwnPropertyDescriptor(HTMLCanvasElement.prototype, "getContext");

describe("Shelf Aura view selection", () => {
  it("chooses the 3D shelf only when motion and browser support allow it", () => {
    expect(chooseAuraViewMode({ reducedMotion: false, supports3d: true })).toBe("shelf");
    expect(chooseAuraViewMode({ reducedMotion: true, supports3d: true })).toBe("cards");
    expect(chooseAuraViewMode({ reducedMotion: false, supports3d: false })).toBe("cards");
  });

  it("detects a usable WebGL context", () => {
    Object.defineProperty(HTMLCanvasElement.prototype, "getContext", { configurable: true, value: () => ({}) });
    expect(supportsAuraShelf()).toBe(true);

    Object.defineProperty(HTMLCanvasElement.prototype, "getContext", { configurable: true, value: () => null });
    expect(supportsAuraShelf()).toBe(false);
  });

  afterEach(() => {
    if (originalGetContext) Object.defineProperty(HTMLCanvasElement.prototype, "getContext", originalGetContext);
  });
});
