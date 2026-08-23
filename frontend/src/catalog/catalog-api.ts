import { api } from "../lib/api";
import type { Book, BookPage, CatalogSearch } from "./catalog-types";
import { toCatalogApiParams } from "./catalog-search";

export async function searchBooks(search: CatalogSearch): Promise<BookPage> {
  return (await api.get<BookPage>("/books", { params: toCatalogApiParams(search) })).data;
}

export async function getBook(bookId: string): Promise<Book> {
  return (await api.get<Book>(`/books/${bookId}`)).data;
}

export async function borrowBook(bookId: string): Promise<void> {
  await api.post("/loans", { bookId });
}
