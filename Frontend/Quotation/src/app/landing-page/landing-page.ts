import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { UserSession } from '../Models/UserModel';

@Component({
  selector: 'app-landing-page',
  imports: [CommonModule],
  templateUrl: './landing-page.html',
  styleUrl: './landing-page.css'
})
export class LandingPage implements OnInit {

userEmail: string = '';

  constructor(private router: Router) {}

  ngOnInit(): void {
    // Check if user is logged in

    const userString = localStorage.getItem('user');
    let userData: UserSession | null = null;
    if (userString) {
      try {
        userData = JSON.parse(userString) as UserSession;
      } catch (error) {
        userData = null;
      }
    }

    if (!userData) {
      this.router.navigate(['/login']);
      return;
    }

    this.userEmail = userData.userEmail;
  }
  
  logout(): void {
    localStorage.removeItem('user');
    this.router.navigate(['/login']);
  }
}
