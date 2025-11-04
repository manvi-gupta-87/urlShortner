// User model
export interface User {
  id?: number;
  username: string;
  email: string;
  role?: 'USER' | 'ADMIN';
}

// Login request DTO (matches backend LoginRequest)
export interface LoginRequest {
  username: string;
  password: string;
}

// Register request DTO (matches backend RegisterRequest)
export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

// Auth response DTO (matches backend AuthResponse exactly!)
export interface AuthResponse {
  token: string;
  username: string;
  // NO email - backend doesn't send it!
}
