import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

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
export class Login {
  // Form data model
  loginData: LoginCredentials = {
    email: '',
    password: ''
  };

  // Component state
  isLoading: boolean = false;
  errorMessage: string = '';
  showError: boolean = false;

  // Mock user data (replace with actual API call)
  private validUsers = [
    { email: 'admin@example.com', password: 'admin123' },
    { email: 'user@example.com', password: 'user123' },
    { email: 'test@example.com', password: 'test123' }
  ];

  constructor(private router: Router) {}

  // Handle form submission
  onSubmit(): void {
    // Reset error state
    this.showError = false;
    this.errorMessage = '';

    // Validate form
    if (!this.validateForm()) {
      return;
    }

    // Set loading state
    this.isLoading = true;

    // Simulate API call delay
    setTimeout(() => {
      this.authenticateUser();
    }, 500);
  }

  // Validate form inputs
  private validateForm(): boolean {
    if (!this.loginData.email || !this.loginData.password) {
      this.showError = true;
      this.errorMessage = 'Please fill in all fields';
      return false;
    }

    if (!this.isValidEmail(this.loginData.email)) {
      this.showError = true;
      this.errorMessage = 'Please enter a valid email address';
      return false;
    }

    if (this.loginData.password.length < 6) {
      this.showError = true;
      this.errorMessage = 'Password must be at least 6 characters long';
      return false;
    }

    return true;
  }

  // Email validation
  private isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }

  // Authenticate user (replace with actual API call)
  private authenticateUser(): void {
    const user = this.validUsers.find(
      u => u.email === this.loginData.email && u.password === this.loginData.password
    );

    this.isLoading = false;

    if (user) {

      // Store user session (you can use localStorage, sessionStorage, or a service)
      localStorage.setItem('user', JSON.stringify({
        email: user.email,
        loginTime: new Date().toISOString()
      }));

      // Navigate to landing page
      this.router.navigate(['/landing']);
    } 
    
    else {
      this.showError = true;
      this.errorMessage = 'Invalid email or password';
    }
    
  }

  // Navigate to register page
  goToRegister(): void {
    this.router.navigate(['/register']);
  }

  // Clear error message
  clearError(): void {
    this.showError = false;
    this.errorMessage = '';
  }

  // For demo purposes - show available test credentials
  getTestCredentials(): string {
    return `Test credentials:\n${this.validUsers.map(u => `${u.email} / ${u.password}`).join('\n')}`;
  }
}