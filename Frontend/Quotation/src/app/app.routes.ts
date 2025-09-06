import { Routes } from '@angular/router';
import { Login } from './login/login';
import { NotFound } from './not-found/not-found';
import { Register } from './register/register';
import { LandingPage } from './landing-page/landing-page';

export const routes: Routes = [
    
    {path: '', redirectTo: 'login', pathMatch: 'full'},
    
    {path: 'login', component: Login },
    
    {path: 'register', component: Register},

    {path: 'landing', component: LandingPage},

    {path: '**', component: NotFound}

];
