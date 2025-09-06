import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

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
    if (!userString) {
      this.router.navigate(['/login']);
      return;
    }

    try {
      const user = JSON.parse(userString);
      this.userEmail = user.email;
    } catch (error) {
      this.router.navigate(['/login']);
    }
  }

  logout(): void {
    localStorage.removeItem('user');
    this.router.navigate(['/login']);
  }
}
