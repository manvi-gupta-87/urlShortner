import { Component, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ValidationErrors,
  Validators,
  ReactiveFormsModule,
} from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { RegisterRequest } from '../../../core/models/user.model';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { PasswordValidators } from '../../../core/validators/password.validators';
import zxcvbn from 'zxcvbn';

@Component({
  selector: 'app-register',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    CommonModule,
  ],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register {
  registerForm: FormGroup;
  loading = signal<boolean>(false);
  errorMessage = signal<string>('');
  passwordStrength = signal<number>(0); // 0-4 score
  hidePassword = signal<boolean>(true);
  hideConfirmPassword = signal<boolean>(true);


  constructor(private fb: FormBuilder, private authService: AuthService) {
    this.registerForm = this.fb.group(
      {
        username: ['', [Validators.required, Validators.minLength(3)]],
        email: ['', [Validators.required, Validators.email]],
        password: ['', [Validators.required, PasswordValidators.minLength(8),
          PasswordValidators.hasLowerCase(),
          PasswordValidators.hasUpperCase(),
          PasswordValidators.hasNumber(),
          PasswordValidators.hasSpecialCharacter()
        ]],
        confirmPassword: ['', [Validators.required]],
      },
      { validators: this.passwordMatchValidator }
    );
  }

  private passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password');
    const confirmPassword = control.get('confirmPassword');

    if (!password || !confirmPassword) {
      return null;
    }

    return password.value == confirmPassword.value ? null : { passwordMismatch: true };
  }

  onSubmit() {
    if (this.registerForm.valid) {
      this.loading.set(true);
      this.errorMessage.set('');

      const { confirmPassword, ...registerData } = this.registerForm.value;
      const credentials: RegisterRequest = registerData;

      this.authService.register(credentials).subscribe({
        next: (response) => {
          console.log('Registration successful:', response);
          this.loading.set(false);
        },
        error: (error) => {
          console.error('Registration failed:', error);
          this.loading.set(false);

          const message = error.error?.message || 'Registration failed. Please try again.';
          this.errorMessage.set(message);
        },
      });
    }
  }

  get username() {
    return this.registerForm.get('username');
  }

  get email() {
    return this.registerForm.get('email');
  }

  get password() {
    return this.registerForm.get('password');
  }

  get confirmPassword() {
    return this.registerForm.get('confirmPassword');
  }

  calculatePasswordStrength():void {
    const password = this.password?.value; 
    if (password) {
      const result = zxcvbn(password);
      this.passwordStrength.set(result.score);
    }else {
      this.passwordStrength.set(0);
    }
  }

  // Get password strength label
  getStrengthLabel(): string {
    const score = this.passwordStrength();
    switch(score) {
      case 0: return 'Very Weak';
      case 1: return 'Weak';
      case 2: return 'Fair';
      case 3: return 'Good';
      case 4: return 'Strong';
      default: return '';
    }
  }

  // Get password strength color
  getStrengthColor(): string {
    const score = this.passwordStrength();
    switch(score) {
      case 0: return '#f44336';
      case 1: return '#ff9800';
      case 2: return '#ffc107';
      case 3: return '#8bc34a';
      case 4: return '#4caf50';
      default: return '#ddd';
    }
  }
  // Toggle password visibility
  togglePasswordVisibility(): void {
    this.hidePassword.set(!this.hidePassword());
  }

  toggleConfirmPasswordVisibility(): void {
    this.hideConfirmPassword.set(!this.hideConfirmPassword());
  }

  // Check if validation rule is met
  hasValidationError(errorKey: string): boolean {
    return this.password?.hasError(errorKey) ?? false;
  }
}

