import type { CatalogSearch } from "./catalog-types";

const allowedSort = new Set<CatalogSearch["sort"]>([
  "title:asc", "title:desc", "publicationYear:desc", "createdAt:desc",
]);

export const emptyCatalogSearch: CatalogSearch = {
  title: "",
  author: "",
  category: "",
  isbn: "",
  publicationYear: "",
  availability: "all",
  page: 0,
  sort: "title:asc",
};

export function readCatalogSearch(params: URLSearchParams): CatalogSearch {
  const page = Number.parseInt(params.get("page") ?? "0", 10);
  const sort = params.get("sort") as CatalogSearch["sort"] | null;
  const availability = params.get("availability");
  const publicationYear = params.get("publicationYear") ?? "";
  return {
    title: params.get("title") ?? "",
    author: params.get("author") ?? "",
    category: params.get("category") ?? "",
    isbn: params.get("isbn") ?? "",
    publicationYear: /^\d{1,4}$/.test(publicationYear) ? publicationYear : "",
    availability: availability === "available" || availability === "unavailable" ? availability : "all",
    page: Number.isFinite(page) && page > 0 ? page : 0,
    sort: sort && allowedSort.has(sort) ? sort : "title:asc",
  };
}

export function writeCatalogSearch(search: CatalogSearch): URLSearchParams {
  const params = new URLSearchParams();
  (["title", "author", "category", "isbn", "publicationYear"] as const).forEach((key) => {
    const value = search[key].trim();
    if (value) params.set(key, value);
  });
  if (search.availability !== "all") params.set("availability", search.availability);
  if (search.page > 0) params.set("page", String(search.page));
  if (search.sort !== "title:asc") params.set("sort", search.sort);
  return params;
}

export function toCatalogApiParams(search: CatalogSearch): Record<string, string | number | boolean> {
  const params: Record<string, string | number | boolean> = { page: search.page, size: 10, sort: search.sort };
  (["title", "author", "category", "isbn"] as const).forEach((key) => {
    if (search[key].trim()) params[key] = search[key].trim();
  });
  if (search.publicationYear.trim()) params.publicationYear = Number(search.publicationYear);
  if (search.availability !== "all") params.available = search.availability === "available";
  return params;
}
