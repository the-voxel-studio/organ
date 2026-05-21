export interface TaskLinkResponse {
  uuid: string;
  url: string;
  description: string | null;
  deletedAt?: string; // ISO DateTime (returned in trash)
}

export interface CreateTaskLinkRequest {
  url: string;
  description?: string;
}

export interface CreateTaskLinkResponse {
  uuid: string;
  url: string;
}
