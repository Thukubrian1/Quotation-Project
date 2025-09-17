import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private authUrl = 'http://localhost:8443/auth'; // your auth service base URL
  private TOKEN_KEY = 'authToken';

  constructor(private http: HttpClient) {}

  login(username: string, password: string): Observable<any> {
    return this.http.post(`${this.authUrl}/login`, { username, password });
  }

  saveToken(token: string): void {
    if (this.isBrowser()) {
      localStorage.setItem(this.TOKEN_KEY, token);
    }
  }

  getToken(): string | null {
    if (this.isBrowser()) {
      return localStorage.getItem(this.TOKEN_KEY);
    }
    return null; // when running in SSR or Vite dev server
  }

  logout(): void {
    if (this.isBrowser()) {
      localStorage.removeItem(this.TOKEN_KEY);
    }
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  // 👇 Helper to check if code is running in browser
  private isBrowser(): boolean {
    return typeof window !== 'undefined' && !!window.localStorage;
  }
}
