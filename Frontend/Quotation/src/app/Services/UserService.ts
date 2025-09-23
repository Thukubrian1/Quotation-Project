import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import { tap, catchError, retry, timeout, map } from 'rxjs/operators';
import { User, UserDto, GenericResponse, ResponseStatus } from '../Models/UserModel';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private userUrl = 'http://localhost:8085/users';

  constructor(private http: HttpClient) {}

  // Get user by username - matches your backend /getuser endpoint
  getUserByUsername(username: string): Observable<GenericResponse<UserDto>> {
    console.log('UserService: Getting user by username:', username);
    const params = new HttpParams().set('userName', username);

    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    });

    return this.http.get<GenericResponse<UserDto>>(`${this.userUrl}/getuser`, { 
      params, 
      headers
    }).pipe(
      timeout(5000),
      retry(1),
      tap(response => {
        console.log('UserService: Get user response:', response);
      }),
      catchError(error => {
        console.error('UserService: Get user error:', error);
        return throwError(() => this.handleHttpError(error, 'Failed to get user details'));
      })
    );
  }

  // Create user - matches your backend /adduser endpoint
  createUser(userDto: UserDto): Observable<GenericResponse<UserDto>> {
    console.log('UserService: Creating user:', userDto);

    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    });

    return this.http.post<GenericResponse<UserDto>>(`${this.userUrl}/adduser`, userDto, { 
      headers
    }).pipe(
      timeout(8000),
      retry(1),
      tap(response => {
        console.log('UserService: Create user response:', response);
      }),
      catchError(error => {
        console.error('UserService: Create user error:', error);
        return throwError(() => this.handleHttpError(error, 'Failed to create user'));
      })
    );
  }

  // FIXED: Method to authenticate user - matches your backend /login endpoint exactly
  authenticateUser(email: string, password: string): Observable<GenericResponse<UserDto>> {
    console.log('UserService: Authenticating user with email:', email);

    const params = new HttpParams()
      .set('userEmail', email.trim())
      .set('userPassword', password.trim());

    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    });

    return this.http.get<GenericResponse<UserDto>>(`${this.userUrl}/login`, { 
      params, 
      headers
    }).pipe(
      timeout(8000),
      retry(1),
      tap(response => {
        console.log('UserService: Authentication response:', response);
      }),
      catchError(error => {
        console.error('UserService: Authentication error:', error);
        return throwError(() => this.handleHttpError(error, 'Authentication failed'));
      })
    );
  }

  // Check email exists - matches your backend /check-email endpoint
  checkEmailExists(email: string): Observable<GenericResponse<boolean>> {
    console.log('UserService: Checking if email exists:', email);

    const params = new HttpParams().set('email', email.trim());

    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    });

    return this.http.get<GenericResponse<boolean>>(`${this.userUrl}/check-email`, { 
      params, 
      headers
    }).pipe(
      timeout(5000),
      retry(1),
      tap(response => {
        console.log('UserService: Check email response:', response);
      }),
      catchError(error => {
        console.error('UserService: Check email error:', error);
        return throwError(() => this.handleHttpError(error, 'Failed to check email'));
      })
    );
  }

  // Health check - matches your backend /health endpoint
  checkServiceHealth(): Observable<GenericResponse<any>> {
    console.log('UserService: Checking service health');

    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    });

    return this.http.get<GenericResponse<any>>(`${this.userUrl}/health`, { 
      headers
    }).pipe(
      timeout(3000),
      tap(response => console.log('UserService: Health check response:', response)),
      map(response => response),
      catchError(error => {
        console.log('UserService: Health check failed:', error);
        return throwError(() => this.handleHttpError(error, 'Health check failed'));
      })
    );
  }

  // Test connection - matches your backend /test endpoint
  testConnection(): Observable<any> {
    console.log('UserService: Testing connection...');
    
    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    });

    return this.http.get(`${this.userUrl}/test`, { headers }).pipe(
      timeout(3000),
      tap(response => console.log('UserService: Connection test response:', response)),
      catchError(error => {
        console.error('UserService: Connection test failed:', error);
        return throwError(() => this.handleHttpError(error, 'Connection test failed'));
      })
    );
  }

  // Ping endpoint - matches your backend /ping endpoint
  pingService(): Observable<string> {
    console.log('UserService: Pinging service...');

    return this.http.get(`${this.userUrl}/ping`, { responseType: 'text' }).pipe(
      timeout(3000),
      tap(response => console.log('UserService: Ping response:', response)),
      catchError(error => {
        console.error('UserService: Ping failed:', error);
        return throwError(() => this.handleHttpError(error, 'Ping failed'));
      })
    );
  }

  // Check if user service is available - simplified version
  isServiceAvailable(): Observable<boolean> {
    return this.testConnection().pipe(
      map(() => true),
      catchError(() => of(false))
    );
  }

  // Enhanced error handling method
  private handleHttpError(error: any, context: string = 'Operation'): Error {
    if (error instanceof HttpErrorResponse) {
      const statusCode = error.status;
      const errorMessage = error.error?.message || error.message || 'Unknown error occurred';
      
      switch (statusCode) {
        case 0:
          return new Error(`${context}: Connection failed. Please check if the user service is running on port 8085 and your internet connection.`);
        case 400:
          return new Error(`${context}: Bad request - ${errorMessage}`);
        case 401:
          return new Error(`${context}: Invalid credentials or unauthorized access.`);
        case 403:
          return new Error(`${context}: Access denied. You may not have permission for this operation.`);
        case 404:
          return new Error(`${context}: Service endpoint not found. The user service may not be available at ${this.userUrl}.`);
        case 408:
          return new Error(`${context}: Request timeout. Please try again.`);
        case 409:
          return new Error(`${context}: Conflict - ${errorMessage}`);
        case 422:
          return new Error(`${context}: Validation error - ${errorMessage}`);
        case 429:
          return new Error(`${context}: Too many requests. Please wait a moment and try again.`);
        case 500:
        case 502:
        case 503:
        case 504:
          return new Error(`${context}: Server error (${statusCode}). Please try again later.`);
        default:
          return new Error(`${context}: HTTP ${statusCode} - ${errorMessage}`);
      }
    }
    
    if (error.name === 'TimeoutError') {
      return new Error(`${context}: Request timed out. Please check your connection and try again.`);
    }
    
    return new Error(`${context}: ${error.message || 'An unexpected error occurred.'}`);
  }

  // Utility method to validate user data
  validateUserData(user: User): { isValid: boolean; errors: string[] } {
    const errors: string[] = [];

    if (!user.userName || user.userName.trim().length === 0) {
      errors.push('Username is required');
    }

    if (!user.userEmail || user.userEmail.trim().length === 0) {
      errors.push('Email is required');
    } else {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(user.userEmail)) {
        errors.push('Invalid email format');
      }
    }

    // Only validate password if it's provided
    if (user.userPassword && user.userPassword.length < 6) {
      errors.push('Password must be at least 6 characters long');
    }

    return {
      isValid: errors.length === 0,
      errors: errors
    };
  }

  // Alternative validation method specifically for UserDto (without password)
  validateUserDto(userDto: UserDto): { isValid: boolean; errors: string[] } {
    const errors: string[] = [];

    if (!userDto.userName || userDto.userName.trim().length === 0) {
      errors.push('Username is required');
    }

    if (!userDto.userEmail || userDto.userEmail.trim().length === 0) {
      errors.push('Email is required');
    } else {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(userDto.userEmail)) {
        errors.push('Invalid email format');
      }
    }

    if (!userDto.userRole || userDto.userRole.trim().length === 0) {
      errors.push('User role is required');
    }

    return {
      isValid: errors.length === 0,
      errors: errors
    };
  }

  // Get service configuration info
  getServiceInfo(): { baseUrl: string; endpoints: string[] } {
    return {
      baseUrl: this.userUrl,
      endpoints: [
        '/login',
        '/getuser',
        '/adduser',
        '/check-email',
        '/health',
        '/test',
        '/ping'
      ]
    };
  }
}