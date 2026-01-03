import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { UserService } from '../Services/UserService';
import { UserDto, ResponseStatus } from '../Models/UserModel';
import { NotificationService } from '../Services/notificationservice';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class Register {
  registerData = {
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    confirmPassword: ''
  };

  isLoading = false;
  showError = false;
  errorMessage = '';

  constructor(
    private router: Router,
    private userService: UserService,
    private notificationService: NotificationService
  ) { }

  onSubmit(): void {
    // Basic validation
    if (this.registerData.password !== this.registerData.confirmPassword) {
      this.showErrorMessage("Passwords do not match");
      return;
    }

    if (this.registerData.password.length < 6) {
      this.showErrorMessage("Password must be at least 6 characters");
      return;
    }

    this.isLoading = true;
    this.showError = false;

    // Create UserDto
    const userDto: UserDto = {
      userName: `${this.registerData.firstName} ${this.registerData.lastName}`,
      userEmail: this.registerData.email,
      userPassword: this.registerData.password,
      userRole: 'Regular', // Default role
      status: 'Active'
    };

    console.log('Registering user:', userDto);

    this.userService.createUser(userDto).subscribe({
      next: (response) => {
        this.isLoading = false;
        if (response.status === ResponseStatus.SUCCESS) {
          this.notificationService.showSuccess("Registration Successful", "Please log in with your new account");
          setTimeout(() => {
            this.router.navigate(['/login']);
          }, 1500);
        } else {
          this.showErrorMessage(response.message || "Registration failed");
        }
      },
      error: (error) => {
        this.isLoading = false;
        console.error('Registration error:', error);
        this.showErrorMessage(error.message || "An unexpected error occurred");
      }
    });
  }

  showErrorMessage(msg: string) {
    this.errorMessage = msg;
    this.showError = true;
    this.notificationService.showError("Registration Failed", msg);
  }

  // Navigate to Login page
  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
