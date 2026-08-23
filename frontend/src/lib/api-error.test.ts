import { AxiosError, AxiosHeaders } from "axios";
import { describe, expect, it } from "vitest";
import type { ApiErrorResponse } from "../types/api";
import { toDisplayError } from "./api-error";

describe("toDisplayError", () => {
  it("preserves safe API message, trace id and field errors", () => {
    const body: ApiErrorResponse = {
      timestamp: "2026-08-23T00:00:00Z",
      status: 400,
      code: "VALIDATION_ERROR",
      error: "Bad Request",
      message: "Validation failed",
      path: "/api/auth/register",
      traceId: "trace-123",
      validationErrors: { email: "must be a well-formed email address" },
    };
    const error = new AxiosError("bad request", "ERR_BAD_REQUEST", undefined, undefined, {
      data: body,
      status: 400,
      statusText: "Bad Request",
      headers: {},
      config: { headers: new AxiosHeaders() },
    });

    expect(toDisplayError(error)).toEqual({
      code: "VALIDATION_ERROR",
      message: "Validation failed",
      traceId: "trace-123",
      fields: body.validationErrors,
    });
  });

  it("returns a recovery-oriented network fallback", () => {
    expect(toDisplayError(new Error("offline"))).toMatchObject({ code: "NETWORK_ERROR" });
  });
});
