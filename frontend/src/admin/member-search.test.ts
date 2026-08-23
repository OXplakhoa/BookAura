import { describe, expect, it } from "vitest";
import { toBackendMemberDate } from "./member-search";

describe("member search date contract", () => {
  it("maps the browser ISO date to the strict backend yyyy/MM/d shape", () => {
    expect(toBackendMemberDate("1990-02-09")).toBe("1990/02/09");
  });

  it("omits an empty date filter", () => {
    expect(toBackendMemberDate("")).toBeUndefined();
  });
});
