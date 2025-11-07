import { AbstractControl, ValidationErrors, ValidatorFn } from "@angular/forms";
import zxcvbn from "zxcvbn";

export class PasswordValidators {
// Password must be at least 8 characters
    static minLength(min: number): ValidatorFn {
      return (control: AbstractControl): ValidationErrors | null => {
        if (!control.value) {
          return null;
        }
        return control.value.length >= min ? null : { minLength: { requiredLength: min, actualLength: control.value.length }
   };
      };
    }

    // Password must contain at least one uppercase letter
    static hasUpperCase(): ValidatorFn {
      return (control: AbstractControl): ValidationErrors | null => {
        if (!control.value) {
          return null;
        }
        return /[A-Z]/.test(control.value) ? null : { hasUpperCase: true };
      };
    }

    // Password must contain at least one lowercase letter
    static hasLowerCase(): ValidatorFn {
      return (control: AbstractControl): ValidationErrors | null => {
        if (!control.value) {
          return null;
        }
        return /[a-z]/.test(control.value) ? null : { hasLowerCase: true };
      };
    }

    // Password must contain at least one number
    static hasNumber(): ValidatorFn {
      return (control: AbstractControl): ValidationErrors | null => {
        if (!control.value) {
          return null;
        }
        return /[0-9]/.test(control.value) ? null : { hasNumber: true };
      };
    }

    // Password must contain at least one special character
    static hasSpecialCharacter(): ValidatorFn {
      return (control: AbstractControl): ValidationErrors | null => {
        if (!control.value) {
          return null;
        }
        return /[!@#$%^&*(),.?":{}|<>]/.test(control.value) ? null : { hasSpecialCharacter: true };
      };
    }

    // Check password strength using zxcvbn (returns score 0-4)
    static strength(minScore: number = 2): ValidatorFn {
      return (control: AbstractControl): ValidationErrors | null => {
        if (!control.value) {
          return null;
        }
        const result = zxcvbn(control.value);
        return result.score >= minScore ? null : { weakPassword: { score: result.score, minScore } };
      };
    }

    // Check if passwords match
    static passwordsMatch(passwordField: string, confirmPasswordField: string): ValidatorFn {
      return (formGroup: AbstractControl): ValidationErrors | null => {
        const password = formGroup.get(passwordField)?.value;
        const confirmPassword = formGroup.get(confirmPasswordField)?.value;

        if (!password || !confirmPassword) {
          return null;
        }

        return password === confirmPassword ? null : { passwordsMismatch: true };
      };
    }
  }