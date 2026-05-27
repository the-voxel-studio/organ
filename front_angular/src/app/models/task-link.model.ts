export interface TaskLinkResponse {
  uuid: string;
  url: string;
  description: string | null;
  deletedAt?: string; // ISO DateTime (retourné dans la corbeille)
}

export interface CreateTaskLinkRequest {
  url: string;
  description?: string;
}

export interface CreateTaskLinkResponse {
  uuid: string;
  url: string;
}
