import type { PageResponse } from "../types/api";

export interface Book {
  id: string;
  title: string;
  isbn: string;
  description: string | null;
  publicationYear: number | null;
  totalQuantity: number;
  availableQuantity: number;
  active: boolean;
  deletedAt: string | null;
  authors: string[];
  categories: string[];
  pageCount: number | null;
  tags: string[];
  createdAt: string;
  updatedAt: string;
}

export interface CatalogSearch {
  title: string;
  author: string;
  category: string;
  isbn: string;
  publicationYear: string;
  availability: "all" | "available" | "unavailable";
  page: number;
  sort: "title:asc" | "title:desc" | "publicationYear:desc" | "createdAt:desc";
}

export type BookPage = PageResponse<Book>;
