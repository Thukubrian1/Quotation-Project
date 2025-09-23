export interface User {
  userId?: number;
  userEmail: string;
  userName: string;
  userPhone?: string;
  userRole: string;
  status: UserStatus;
  userPassword?: string; // Optional for responses
}

// FIXED: UserDto now includes optional password for registration
export interface UserDto {
  userEmail: string;
  userName: string;
  userPhone?: string;
  userRole: string;
  status: UserStatus;
  userPassword?: string; // Optional - used for registration, not included in responses
}

export type UserStatus = 'Active' | 'Inactive' | 'Suspended';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  access_token: string;
  token_type: string;
  username: string;
}

export interface GenericResponse<T> {
  status: ResponseStatus;
  message: string;
  data?: T;
  debugMessage?: string;
}

export enum ResponseStatus {
  SUCCESS = 'SUCCESS',
  ERROR = 'ERROR',
  NOT_FOUND = 'NOT_FOUND',
  CONFLICT = 'CONFLICT',
  UNAUTHORIZED = 'UNAUTHORIZED',
  BAD_REQUEST = 'BAD_REQUEST'
}

// Additional interfaces for better type safety
export interface UserLoginCredentials {
  userEmail: string;
  userPassword: string;
}

export interface AuthTokenResponse {
  access_token: string;
  token_type: string;
  username: string;
}

export interface UserSession extends UserDto {
  loginTime: string;
}

// Registration specific interface
export interface UserRegistrationDto extends UserDto {
  userPassword: string; // Required for registration
  confirmPassword?: string; // Optional confirmation field for frontend validation
}