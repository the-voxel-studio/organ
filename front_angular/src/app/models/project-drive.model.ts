export interface ProjectDriveConfigResponse {
  uuid: string;
  driveFolderId: string | null;
  isActive: boolean;
}

export interface ConnectGoogleRequest {
  authCode: string;
}

export interface ConnectGoogleResponse {
  message: string;
}

export interface CreateFolderResponse {
  message: string;
  driveFolderId: string;
  folderName: string;
}

export interface GoogleDriveFolderInfo {
  id: string;
  name: string;
}

export interface SelectFolderRequest {
  driveFolderId: string;
}

export interface SelectFolderResponse {
  message: string;
  driveFolderId: string;
}

export interface UpdateDriveConfigRequest {
  driveFolderId?: string;
  refreshToken?: string;
  isActive?: boolean;
}

export interface UpdateDriveConfigResponse {
  message: string;
}
