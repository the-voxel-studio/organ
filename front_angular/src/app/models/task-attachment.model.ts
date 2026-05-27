import { UserSummary } from './user.model';

export interface TaskAttachmentResponse {
  uuid: string;
  fileName: string;
  fileSize: string; // File size as a string in bytes
  fileType: string | null;
  filePath: string;
  uploadedBy: UserSummary;
  createdAt: string; // ISO DateTime
  deletedAt?: string; // ISO DateTime (retourné dans la corbeille)
}

export interface InitUploadRequest {
  fileSize: number;
  fileType: string;
}

export type InitUploadResponse = 
  | { action: 'upload_to_drive'; folderId: string; accessToken: string }
  | { action: 'upload_local' };

export interface ConfirmDriveUploadRequest {
  driveId: string;
  fileName: string;
  fileSize?: number;
  fileType?: string;
}

export interface AttachmentUploadConfirmResponse {
  uuid: string;
  fileName: string;
}
