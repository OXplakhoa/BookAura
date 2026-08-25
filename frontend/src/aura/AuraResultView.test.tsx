import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it } from "vitest";
import type { AuraRecommendation } from "./aura-api";
import { AuraResultView } from "./AuraResultView";

const originalCss = window.CSS;
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

function setEnvironment({ reducedMotion, supports3d }: { reducedMotion: boolean; supports3d: boolean }) {
  Object.defineProperty(window, "CSS", { configurable: true, value: { supports: () => supports3d } });
  Object.defineProperty(window, "matchMedia", {
    configurable: true,
    value: () => ({
      matches: reducedMotion,
      media: "(prefers-reduced-motion: reduce)",
      onchange: null,
      addEventListener: () => undefined,
      removeEventListener: () => undefined,
      addListener: () => undefined,
      removeListener: () => undefined,
      dispatchEvent: () => false,
    }) as unknown as MediaQueryList,
  });
}

function renderView() {
  return render(
    <MemoryRouter>
      <AuraResultView books={[book]} fallback={<div data-testid="cards-fallback">2D recommendation cards</div>} />
    </MemoryRouter>,
  );
}

describe("AuraResultView", () => {
  afterEach(() => {
    cleanup();
    Object.defineProperty(window, "CSS", { configurable: true, value: originalCss });
    Object.defineProperty(window, "matchMedia", { configurable: true, value: originalMatchMedia });
  });

  it("lazy-loads the shelf view and keeps a list-view control available", async () => {
    setEnvironment({ reducedMotion: false, supports3d: true });
    const user = userEvent.setup();
    renderView();

    expect(await screen.findByRole("region", { name: "3D Shelf Aura" })).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: /List view/i }));
    expect(screen.getByTestId("cards-fallback")).toBeInTheDocument();
    expect(screen.queryByRole("region", { name: "3D Shelf Aura" })).not.toBeInTheDocument();
  });

  it("selects the 2D cards automatically when reduced motion is preferred", () => {
    setEnvironment({ reducedMotion: true, supports3d: true });
    renderView();

    expect(screen.getByTestId("cards-fallback")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /3D shelf/i })).toBeDisabled();
    expect(screen.getByText(/Reduced motion is enabled/i)).toBeInTheDocument();
  });

  it("uses the 2D cards when CSS 3D support is unavailable", () => {
    setEnvironment({ reducedMotion: false, supports3d: false });
    renderView();

    expect(screen.getByTestId("cards-fallback")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /3D shelf/i })).not.toBeInTheDocument();
    expect(screen.getByText(/does not support the 3D shelf/i)).toBeInTheDocument();
  });
});
