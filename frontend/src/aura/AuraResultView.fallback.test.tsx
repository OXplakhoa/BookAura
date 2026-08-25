import { cleanup, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { emptyAuraSearch, type AuraRecommendation } from "./aura-api";

vi.mock("./AuraShelf3D", () => ({
  AuraShelf3D: () => {
    throw new Error("simulated shelf chunk failure");
  },
}));

import { AuraResultView } from "./AuraResultView";

const originalGetContext = Object.getOwnPropertyDescriptor(HTMLCanvasElement.prototype, "getContext");
const originalMatchMedia = window.matchMedia;
const book: AuraRecommendation = {
  bookId: "book-1",
  title: "The Hobbit",
  authors: ["J.R.R. Tolkien"],
  categories: ["Fantasy"],
  publicationYear: 1937,
  pageCount: 310,
  availableQuantity: 2,
  score: 8,
  breakdown: { mood: 3, theme: 4, time: 0, intensity: 1 },
  reasons: ["A matching mood"],
  matchedTags: ["cozy"],
};

describe("AuraResultView lazy fallback", () => {
  afterEach(() => {
    cleanup();
    if (originalGetContext) Object.defineProperty(HTMLCanvasElement.prototype, "getContext", originalGetContext);
    Object.defineProperty(window, "matchMedia", { configurable: true, value: originalMatchMedia });
  });

  it("returns to the 2D cards when the lazy shelf cannot render", async () => {
    Object.defineProperty(HTMLCanvasElement.prototype, "getContext", { configurable: true, value: () => ({}) });
    Object.defineProperty(window, "matchMedia", {
      configurable: true,
      value: () => ({ matches: false, addEventListener: () => undefined, removeEventListener: () => undefined }) as unknown as MediaQueryList,
    });
    const error = vi.spyOn(console, "error").mockImplementation(() => undefined);

    render(
      <MemoryRouter>
        <AuraResultView books={[book]} search={emptyAuraSearch} fallback={<div data-testid="cards-fallback">2D recommendation cards</div>} />
      </MemoryRouter>,
    );

    expect(await screen.findByText(/could not render/i)).toBeInTheDocument();
    expect(screen.getByTestId("cards-fallback")).toBeInTheDocument();
    error.mockRestore();
  });
});
