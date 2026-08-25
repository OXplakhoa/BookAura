import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import type { AuraRecommendation } from "./aura-api";

vi.mock("./ArcaneShelfCanvas", () => ({
  ArcaneShelfCanvas: ({ books, onActive, onOpen }: {
    books: AuraRecommendation[];
    onActive: (index: number) => void;
    onOpen: (bookId: string) => void;
  }) => (
    <div data-testid="webgl-scene">
      {books.map((book, index) => (
        <button key={book.bookId} type="button" onMouseEnter={() => onActive(index)} onFocus={() => onActive(index)} onClick={() => onOpen(book.bookId)}>
          3D {book.title}
        </button>
      ))}
    </div>
  ),
}));

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
    reasons: ["A matching mood", "A matching theme", "A third reason"],
    matchedTags: ["thoughtful"],
  };
}

describe("AuraShelf3D", () => {
  it("shows the mood-reactive reading, updates previews, and navigates a selected 3D book", async () => {
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
          <Route path="/aura" element={<AuraShelf3D books={books} search={{ moods: ["cozy"], themes: [], timeMinutes: null, intensity: null }} />} />
          <Route path="/books/:bookId" element={<p>Book detail destination</p>} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByRole("region", { name: "3D Shelf Aura" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "The Ember Opus" })).toBeInTheDocument();
    expect(screen.getByLabelText("Aura reading for The Hobbit")).toHaveTextContent("8Aura score");
    expect(screen.getByLabelText("Aura reading for The Hobbit")).toHaveTextContent("A matching mood");
    expect(screen.getByLabelText("Aura reading for The Hobbit")).not.toHaveTextContent("A third reason");

    await user.hover(screen.getByRole("button", { name: "3D Meditations" }));
    expect(screen.getByLabelText("Aura reading for Meditations")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "3D The Hobbit" }));
    expect(screen.getByText("Book detail destination")).toBeInTheDocument();
  });
});
