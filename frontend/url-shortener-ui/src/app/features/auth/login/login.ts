import { Component, signal, Signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldControl, MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { LoginRequest } from '../../../core/models/user.model';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    CommonModule
  ],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class Login {
  loginForm: FormGroup;
  loading = signal<boolean> (false);
  errorMessage = signal<string>('');

  constructor(private fb: FormBuilder, private authService: AuthService) {
    this.loginForm = this.fb.group({
      username : ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    })
  }

  onSubmit(): void {
    if(this.loginForm.valid) {
      this.loading.set(true);
      this.errorMessage.set('');

      const credentials:LoginRequest = this.loginForm.value;
      this.authService.login(credentials).subscribe({
        next: (response) => {
          // Success - AuthService will navigate to dashboard
            console.log('Login successful:', response);
            this.loading.set(false);
        }, 
        error: (error) => {
          // Error - Show error message
            console.error('Login failed:', error);
            this.loading.set(false);

            // Extract error message
            const message = error.error?.message || 'Login failed. Please try again.';
            this.errorMessage.set(message);
        }
      })
    }
  }

  get username() {
    return this.loginForm.get('username');
  }

  get password() {
    return this.loginForm.get('password');
  }
}
