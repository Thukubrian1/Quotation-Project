// login.ts - Fixed to work with your backend
import { UserDto, UserSession } from './../Models/UserModel';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { timeout, catchError, throwError, Subject, takeUntil } from 'rxjs';
import { AuthService } from '../Services/authservice';
import { UserService } from '../Services/UserService';
import { ResponseStatus } from '../Models/UserModel';
import { NotificationService } from '../Services/notificationservice';

interface LoginCredentials {
  email: string;
  password: string;
}

@Component({
  selector: 'app-login',
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login implements OnDestroy {
  // Form data model
  loginData: LoginCredentials = {
    email: '',
    password: ''
  };

  // Component state
  isLoading: boolean = false;
  emailError: string = '';
  passwordError: string = '';
  showDebug: boolean = true; // Set to true for development
  servicesAvailable = {
    authService: false,
    userService: false
  };

  private destroy$ = new Subject<void>();

  constructor(
    private router: Router,
    private authService: AuthService,
    private userService: UserService,
    private notificationService: NotificationService
  ) {}


  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // Handle form submission
  onSubmit(): void {
    // Clear previous notifications
    this.notificationService.dismissAll();

    // Validate form
    if (!this.validateForm()) {
      return;
    }

    // Set loading state
    this.isLoading = true;

    // Call backend authentication
    this.authenticateUser();
  }

  // Validate form inputs
  private validateForm(): boolean {
    this.clearFieldErrors();

    let isValid = true;
    const errors: string[] = [];

    // Email validation
    if (!this.loginData.email || !this.loginData.email.trim()) {
      this.emailError = 'Email is required';
      errors.push('Email is required');
      isValid = false;
    } else {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(this.loginData.email.trim())) {
        this.emailError = 'Please enter a valid email address';
        errors.push('Please enter a valid email address');
        isValid = false;
      }
    }

    // Password validation
    if (!this.loginData.password || !this.loginData.password.trim()) {
      this.passwordError = 'Password is required';
      errors.push('Password is required');
      isValid = false;
    } else if (this.loginData.password.length < 6) {
      this.passwordError = 'Password must be at least 6 characters long';
      errors.push('Password must be at least 6 characters long');
      isValid = false;
    }

    if (!isValid) {
      this.notificationService.showError(
        'Validation Error',
        errors.join('. '),
        5000
      );
    }

    return isValid;
  }

  // Check if form is valid for button state
  isFormValid(): boolean {
    const emailValid = this.loginData.email &&
                      this.loginData.email.trim().length > 0 &&
                      /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.loginData.email.trim());
    const passwordValid = this.loginData.password &&
                         this.loginData.password.trim().length >= 6;
    return !!(emailValid && passwordValid);
  }

  // Clear field-specific errors
  private clearFieldErrors(): void {
    this.emailError = '';
    this.passwordError = '';
  }

  // FIXED: Authenticate user with the proper flow matching your backend
  private authenticateUser(): void {
    const email = this.loginData.email.trim();
    const password = this.loginData.password.trim();

    console.log('Login: Starting authentication for email:', email);

    // Set 10-second timeout for the request
    const timeoutDuration = 10000; // 10 seconds

    // Use the AuthService login method which now properly handles your backend flow
    this.authService.login(email, password).pipe(
      timeout(timeoutDuration),
      takeUntil(this.destroy$)
    ).subscribe({
      next: (response) => {
        this.isLoading = false;
        console.log('Login: Authentication response:', response);

        if (response.status === ResponseStatus.SUCCESS && response.data) {
          console.log('Login: Authentication successful');

          const username = response.data.username;
          const token = response.data.access_token;

          // Show success notification
          this.notificationService.showLoginSuccess(username);

          // Navigate after a brief delay to show success message
          setTimeout(() => {
            this.fetchUserDetailsAndNavigate(username);
          }, 1500);
        } else {
          this.handleLoginError(response.message || 'Login failed');
        }
      },
      error: (error) => {
        this.isLoading = false;
        console.error('Login: Authentication error:', error);

        // Handle specific error types
        this.handleAuthenticationError(error);
      }
    });
  }

  // Handle authentication errors with more specific messaging
  private handleAuthenticationError(error: any): void {
    const errorMessage = error.message || 'Unknown error occurred';

    if (error.name === 'TimeoutError') {
      this.notificationService.showLoginError('timeout');
    } else if (errorMessage.includes('Connection failed') || errorMessage.includes('Cannot connect')) {
      this.notificationService.showLoginError('connection', 'Cannot connect to authentication servers. Please check if the services are running.');
    } else if (errorMessage.includes('Service not found') || errorMessage.includes('404')) {
      this.notificationService.showError(
        'Service Unavailable',
        'The authentication service is currently unavailable. Please try again later or contact support.',
        8000
      );
    } else if (errorMessage.includes('Invalid credentials') || errorMessage.includes('Invalid email or password')) {
      this.notificationService.showLoginError('credentials');
    } else if (errorMessage.includes('Access denied') || errorMessage.includes('suspended') || errorMessage.includes('inactive')) {
      this.notificationService.showLoginError('account', errorMessage);
    } else if (errorMessage.includes('Server error') || errorMessage.includes('500')) {
      this.notificationService.showLoginError('server');
    } else if (errorMessage.includes('User must have admin Role')) {
      this.notificationService.showError(
        'Access Denied',
        'This application requires administrator privileges. Please contact your system administrator.',
        8000
      );
    } else {
      this.notificationService.showLoginError('generic', errorMessage);
    }
  }

  // Handle different types of login errors
  private handleLoginError(message: string): void {
    // Provide specific error messages based on the response
    if (message.toLowerCase().includes('invalid email') ||
        message.toLowerCase().includes('invalid username or password')) {
      this.notificationService.showLoginError('credentials');
    } else if (message.toLowerCase().includes('inactive')) {
      this.notificationService.showLoginError('account', 'Your account is inactive. Please contact support.');
    } else if (message.toLowerCase().includes('suspended')) {
      this.notificationService.showLoginError('account', 'Your account has been suspended. Please contact support.');
    } else if (message.toLowerCase().includes('admin role')) {
      this.notificationService.showError(
        'Access Denied',
        'This application requires administrator privileges. Please contact your system administrator.',
        8000
      );
    } else {
      this.notificationService.showLoginError('generic', message || 'Login failed. Please try again.');
    }
  }

  // FIXED: Fetch user details after successful login and navigate
  private fetchUserDetailsAndNavigate(username: string): void {
    console.log('Login: Fetching user details for username:', username);

    // Use the AuthService method to get user details
    this.authService.getUserWithToken(username).subscribe({
      next: (userResponse) => {
        console.log('Login: User details response:', userResponse);

        if (userResponse.status === ResponseStatus.SUCCESS && userResponse.data) {
          // Extend UserDto with loginTime
          const userData: UserSession = {
            ...userResponse.data,
            loginTime: new Date().toISOString()
          };

          localStorage.setItem('user', JSON.stringify(userData));
          console.log('Login: User data stored:', userData);

          // Show additional success message for complete login
          this.notificationService.showSuccess(
            'Welcome!',
            `Profile loaded successfully. Redirecting to dashboard...`,
            3000
          );

          setTimeout(() => {
            this.router.navigate(['/payment']);
          }, 2000);
        } else {
          console.warn('Login: Could not fetch user details but proceeding:', userResponse.message);
          this.proceedWithLimitedProfile(username);
        }
      },
      error: (error) => {
        console.error('Login: Error fetching user details but proceeding:', error);
        this.proceedWithLimitedProfile(username);
      }
    });
  }

  // Proceed with limited profile when user details can't be fetched
  private proceedWithLimitedProfile(username: string): void {
    this.notificationService.showWarning(
      'Partial Login',
      'Logged in successfully but could not load full profile. Some features may be limited.',
      6000
    );

    // Create basic user session data
    const basicUserData: UserSession = {
      userName: username,
      userEmail: this.loginData.email,
      userRole: 'User', // Default role
      status: 'Active',
      loginTime: new Date().toISOString()
    };

    localStorage.setItem('user', JSON.stringify(basicUserData));

    setTimeout(() => {
      this.router.navigate(['/payment']);
    }, 1000);
  }

  // Navigate to register page
  goToRegister(): void {
    this.router.navigate(['/register']);
  }

  // Clear all messages
  clearMessages(): void {
    this.clearFieldErrors();
  }

  // Check if there's a stored token (for debugging)
  hasStoredToken(): boolean {
    return this.authService.isLoggedIn();
  }

  // Method to test backend connection (useful for debugging)
  testBackendConnection(): void {
    console.log('Login: Testing backend connections...');

    this.notificationService.showInfo(
      'Testing Connection',
      'Checking backend connectivity...',
      3000
    );

    // Test auth service
    this.authService.testConnection().subscribe({
      next: (response) => {
        console.log('Login: Auth service test successful:', response);
        this.notificationService.showSuccess(
          'Auth Service Online',
          'Authentication service is responding normally.',
          4000
        );
      },
      error: (error) => {
        console.error('Login: Auth service test failed:', error);
        this.notificationService.showError(
          'Auth Service Offline',
          'Cannot reach authentication service. Check if it\'s running on port 8443.',
          6000
        );
      }
    });

    // Test user service
    this.userService.testConnection().subscribe({
      next: (response) => {
        console.log('Login: User service test successful:', response);
        this.notificationService.showSuccess(
          'User Service Online',
          'User service is responding normally.',
          4000
        );
      },
      error: (error) => {
        console.error('Login: User service test failed:', error);
        this.notificationService.showError(
          'User Service Offline',
          'Cannot reach user service. Check if it\'s running on port 8085.',
          6000
        );
      }
    });

  }

  // Get service status for display
  getServiceStatus(): string {
    if (this.servicesAvailable.authService && this.servicesAvailable.userService) {
      return 'All services online';
    } else if (this.servicesAvailable.authService || this.servicesAvailable.userService) {
      return 'Limited services available';
    } else {
      return 'Services offline';
    }
  }

  // Get service status color class
  getServiceStatusClass(): string {
    if (this.servicesAvailable.authService && this.servicesAvailable.userService) {
      return 'text-green-400';
    } else if (this.servicesAvailable.authService || this.servicesAvailable.userService) {
      return 'text-yellow-400';
    } else {
      return 'text-red-400';
    }
  }
}