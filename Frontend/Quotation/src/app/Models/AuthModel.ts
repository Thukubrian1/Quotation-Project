// AuthModel.ts - Fixed to match your backend structure

// Request model for auth service login - matches your AuthController
export interface AuthRequest {
  username: string;
  email: string;
}

// Response from auth service - matches your AuthController response structure
export interface AuthResponse {
  access_token: string;
  token_type: string;
  username: string;
  expires_in?: number;
}

// Login credentials for user service - matches your UserController login endpoint
export interface LoginCredentials {
  userEmail: string;
  userPassword: string;
}

// Token validation response - matches your AuthController validate endpoint
export interface TokenValidationResponse {
  valid: boolean;
  username?: string;
  error?: string;
}

// Service health check response
export interface ServiceHealthResponse {
  service: string;
  status: string;
  timestamp: number;
  port?: number;
  environment?: string;
  memory?: number;
  processors?: number;
}

// Test endpoint response
export interface ServiceTestResponse {
  service: string;
  message: string;
  timestamp: number;
  status: string;
  version?: string;
}