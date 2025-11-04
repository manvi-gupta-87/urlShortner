import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

  export const authInterceptor: HttpInterceptorFn = (req, next) => {
    const router = inject(Router);
    const token = localStorage.getItem('auth_token');

    // If token exists, clone the request and add Authorization header
    if (token) {
      req = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }
    return next(req).pipe(
        catchError((error:HttpErrorResponse) => {
            if(error.status === 401 || error.status === 403) {
                const errorMessage = error.error?.message || error.statusText || '';
                if(errorMessage.toLowerCase().Contains('jwt') || 
                    errorMessage.toLowerCase().Contains('expired')) {
                        // clear localStorage
                        localStorage.clear();
                        router.navigate(['/login']);
                        console.log('Token expired. Redirected to login.');
                    }
                }
                // Re-throw the error so components can still handle it if needed
                return throwError(() => error);
            })
    );
  }