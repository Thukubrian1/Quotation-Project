import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { AuthService } from '../Services/authservice';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService) { }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // START: Fix for login error handling
    // Don't intercept 401s for login requests - let the component handle them
    if (req.url.includes('/login')) {
      return next.handle(req);
    }
    // END: Fix for login error handling

    const token = this.authService.getToken();

    if (token) {
      const cloned = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
      return next.handle(cloned).pipe(
        catchError(error => {
          if (error.status === 401) {
            this.authService.logout();
            // Optional: Reload or navigate to login
            window.location.reload();
          }
          return throwError(() => error);
        })
      );
    }

    return next.handle(req).pipe(
      catchError(error => {
        if (error.status === 401) {
          this.authService.logout();
          window.location.reload();
        }
        return throwError(() => error);
      })
    );
  }
}

