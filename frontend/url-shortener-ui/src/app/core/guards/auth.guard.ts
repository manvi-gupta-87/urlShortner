import { inject } from "@angular/core";
import { CanActivateFn, Router } from "@angular/router";

export const authGuard: CanActivateFn = (route, state) => {
    const router = inject(Router);
    const token = localStorage.getItem('auth_token');

    if(token) {
        // User is authenticated, allow access
      return true;
    } else {
        router.navigate(['/login']);
        return false;
    }

}