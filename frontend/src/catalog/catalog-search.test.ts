import { describe, expect, it } from "vitest";
import { emptyCatalogSearch, readCatalogSearch, toCatalogApiParams, writeCatalogSearch } from "./catalog-search";

describe("catalog URL state", () => {
  it("reads composable filters and an allowed sort", () => {
    const result = readCatalogSearch(new URLSearchParams("title=Clean&author=Martin&page=2&availability=available&sort=publicationYear%3Adesc"));
    expect(result).toMatchObject({ title: "Clean", author: "Martin", page: 2, availability: "available", sort: "publicationYear:desc" });
  });

  it("normalizes invalid page, availability and sort values", () => {
    const result = readCatalogSearch(new URLSearchParams("page=-5&availability=secret&sort=password%3Aasc"));
    expect(result).toMatchObject({ page: 0, availability: "all", sort: "title:asc" });
  });

  it("writes only non-default values for stable shareable URLs", () => {
    expect(writeCatalogSearch({ ...emptyCatalogSearch, title: "  Domain Design  ", page: 1 }).toString())
      .toBe("title=Domain+Design&page=1");
  });

  it("maps unavailable books to the backend boolean filter", () => {
    expect(toCatalogApiParams({ ...emptyCatalogSearch, availability: "unavailable" }))
      .toEqual({ page: 0, size: 10, sort: "title:asc", available: false });
  });
});
