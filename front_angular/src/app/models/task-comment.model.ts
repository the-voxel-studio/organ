import { UserSummary } from './user.model';

export interface TaskCommentResponse {
  uuid: string;
  content: string;
  user: UserSummary;
  createdAt: string; // ISO DateTime
  updatedAt: string; // ISO DateTime
  deletedAt?: string; // ISO DateTime (returned in trash)
}

export interface CreateCommentRequest {
  content: string;
}

export interface CreateCommentResponse {
  uuid: string;
  content: string;
  user: UserSummary;
  createdAt: string; // ISO DateTime
}

export interface UpdateCommentRequest {
  content: string;
}

export interface CommentMessageResponse {
  message: string;
}
