import { describe, expect, it } from "vitest";
import { formatDate, formatDateTime } from "./date-format";

describe("loan date formatting", () => {
  it("renders due dates in a stable UTC day format", () => {
    expect(formatDate("2026-08-23T23:30:00-07:00")).toBe("24 Aug 2026");
  });

  it("includes the return time in the local display", () => {
    expect(formatDateTime("2026-08-23T13:45:00Z")).toMatch(/23 Aug 2026/);
  });
});
