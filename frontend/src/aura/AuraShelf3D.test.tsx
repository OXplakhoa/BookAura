import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it } from "vitest";
import type { AuraRecommendation } from "./aura-api";
import { AuraShelf3D } from "./AuraShelf3D";

function recommendation(bookId: string, title: string, score: number): AuraRecommendation {
  return {
    bookId,
    title,
    authors: ["A BookAura author"],
    categories: ["Fiction"],
    publicationYear: 2024,
    pageCount: 240,
    availableQuantity: 2,
    score,
    breakdown: { mood: 3, theme: 4, time: 0, intensity: 1 },
    reasons: ["A matching mood"],
    matchedTags: ["thoughtful"],
  };
}

describe("AuraShelf3D", () => {
  it("renders ranked, labelled books and navigates a selected spine to book details", async () => {
    const user = userEvent.setup();
    const books = [
      recommendation("book-1", "The Hobbit", 8),
      recommendation("book-2", "Meditations", 7),
      recommendation("book-3", "Pride and Prejudice", 6),
      recommendation("book-4", "Dune", 5),
    ];

    render(
      <MemoryRouter initialEntries={["/aura"]}>
        <Routes>
          <Route path="/aura" element={<AuraShelf3D books={books} />} />
          <Route path="/books/:bookId" element={<p>Book detail destination</p>} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByRole("region", { name: "3D Shelf Aura" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Open aura pick 1: The Hobbit\. Score 8/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Open aura pick 4: Dune\. Score 5/i })).toBeInTheDocument();

    await user.click(screen.getByRole("link", { name: /Open aura pick 1: The Hobbit/i }));
    expect(screen.getByText("Book detail destination")).toBeInTheDocument();
  });
});
