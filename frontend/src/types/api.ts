export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  code: string;
  error: string;
  message: string;
  path: string;
  traceId?: string;
  validationErrors?: Record<string, string>;
}

export interface UserSummary {
  id: string;
  email: string;
  fullName: string;
  roles: string[];
}

export interface AuthResponse {
  accessToken: string;
  tokenType: "Bearer";
  expiresIn: number;
  user: UserSummary;
}

export interface MessageResponse {
  message: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  sort: string[];
}
