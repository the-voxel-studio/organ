export interface UserSummary {
  uuid: string;
  email: string;
  firstName: string;
  lastName: string;
}

export interface UserMeResponse {
  uuid: string;
  email: string;
  firstName: string;
  lastName: string;
  isVerified: boolean;
  createdAt: string; // ISO 8601 string
  authWithGoogle: boolean;
}

export interface UpdateUserRequest {
  firstName?: string;
  lastName?: string;
  email?: string;
}

export interface UpdateUserResponse {
  message: string;
  refresh: boolean;
  user: {
    firstName: string;
    lastName: string;
    email: string;
  };
}

export interface UserConnection {
  uuid: string;
  deviceName: string;
  browserName: string;
  location: string;
  ipAddress: string;
  lastUsedAt: string; // ISO 8601 string
  createdAt: string;  // ISO 8601 string
  isCurrent: boolean;
}

export interface UpdatePasswordRequest {
  currentPassword?: string;
  newPassword?: string;
}

export interface LinkGoogleRequest {
  idToken?: string;
  token?: string;
}

export interface LinkGoogleResponse {
  message: string;
  refresh: boolean;
  user: {
    email: string;
    authWithGoogle: boolean;
  };
}

export interface DeleteUserResponse {
  message: string;
  deletedAt: string; // ISO 8601 string
}
