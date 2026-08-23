import { describe, expect, it } from "vitest";
import { isValidIsbn, normalizeIsbn } from "./isbn";

describe("frontend ISBN validation", () => {
  it("accepts normalized ISBN-10 and ISBN-13 checksums", () => {
    expect(isValidIsbn("0-306-40615-2")).toBe(true);
    expect(isValidIsbn("978-0-306-40615-7")).toBe(true);
  });
  it("rejects a bad checksum instead of waiting for the API", () => {
    expect(isValidIsbn("9780306406158")).toBe(false);
  });
  it("normalizes spaces, hyphens and X", () => {
    expect(normalizeIsbn(" 0-8044-2957-x ")).toBe("080442957X");
  });
});
