export interface RegisterRequest {
  email: string;
  firstName: string;
  lastName: string;
  password?: string;
}

export interface RegisterResponse {
  status: string;
  user: {
    uuid: string;
    email: string;
  };
}

export interface LoginRequest {
  username?: string; // Standard LexikJWT login
  password?: string;
}

export interface GoogleLoginRequest {
  idToken?: string;
  token?: string;
}

export interface LoginResponse {
  token: string;
}

export interface LogoutResponse {
  message: string;
}
