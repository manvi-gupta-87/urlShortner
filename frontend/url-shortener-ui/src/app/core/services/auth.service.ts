import { HttpClient } from "@angular/common/http";
import { Injectable, signal } from "@angular/core";
import { Router } from "@angular/router";
import { AuthResponse, LoginRequest, RegisterRequest } from "../models/user.model";
import { Observable, tap } from "rxjs";
import { environment } from '../../environments/environment';

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private readonly TOKEN_KEY = 'auth_token';
    private readonly USERNAME_KEY = 'username';
    private apiUrl = environment.apiUrl;

    private isAuthenticatedSignal = signal<boolean>(this.hasToken()); //creates a reactive value that stores a boolean (true/false). When this signal’s value changes later (isAuthenticatedSignal.set(true) or .set(false)), Angular automatically updates any UI or computed logic that depends on it — no manual Subject, BehaviorSubject, or EventEmitter required.
    private currentUserSignal = signal<string|null> (this.getStoredUserName()); // creates a reactive variable that holds either a string (the username) or null (no user logged in).

    constructor(private http: HttpClient, private router: Router) {}

    /**
     * Login user - calls backend API
     */
    login(credentials: LoginRequest) :Observable<AuthResponse> {
        return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, credentials)
        .pipe(
            tap(response => {
                this.storeToken(response.token);
                this.storeUserName(response.username);

                this.isAuthenticatedSignal.set(true);
                this.currentUserSignal.set(response.username);

                // Navigate to dashboard
                this.router.navigate(['/dashboard']);

            })
        );

    }

    register(credentials: RegisterRequest) :Observable<AuthResponse> {
        const url = `${this.apiUrl}/auth/register`;
        console.log('Registering user at URL:', url);
        console.log('API URL from environment:', this.apiUrl);
        return this.http.post<AuthResponse>(url, credentials)
        .pipe(
            tap(response => {
                this.storeToken(response.token);
                this.storeUserName(response.username);

                this.isAuthenticatedSignal.set(true);
                this.currentUserSignal.set(response.username);

                // Navigate to dashboard
                this.router.navigate(['/dashboard']);

            })
        );

    }

    private hasToken() : boolean {
        return this.getToken() != null;
    }
 
    private storeToken(token : string) {
        localStorage.setItem(this.TOKEN_KEY, token);
    }

    private storeUserName(username: string) {
        localStorage.setItem(this.USERNAME_KEY, username);
    }

    private getStoredUserName(): string | null {
        return localStorage.getItem(this.USERNAME_KEY);
    }

    private getToken() : string | null {
        return localStorage.getItem(this.TOKEN_KEY);
    }
}