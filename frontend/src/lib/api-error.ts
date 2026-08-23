import axios from "axios";
import type { ApiErrorResponse } from "../types/api";

export interface DisplayError {
  code: string;
  message: string;
  traceId?: string;
  fields: Record<string, string>;
}

const FALLBACK: DisplayError = {
  code: "NETWORK_ERROR",
  message: "We could not reach BookAura. Check your connection and try again.",
  fields: {},
};

export function toDisplayError(error: unknown): DisplayError {
  if (!axios.isAxiosError<ApiErrorResponse>(error)) {
    return FALLBACK;
  }
  const body = error.response?.data;
  if (!body || typeof body !== "object") {
    return FALLBACK;
  }
  return {
    code: body.code ?? "REQUEST_FAILED",
    message: body.message ?? "The request could not be completed.",
    traceId: body.traceId,
    fields: body.validationErrors ?? {},
  };
}
