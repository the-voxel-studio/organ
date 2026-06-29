export interface TagResponse {
  uuid: string;
  name: string;
  color: string;
  deletedAt?: string; // ISO DateTime (retourné dans la corbeille)
}

export interface CreateTagRequest {
  name: string;
  color?: string;
}

export interface CreateTagResponse {
  uuid: string;
  name: string;
  color: string;
}

export interface UpdateTagRequest {
  name?: string;
  color?: string;
}

export interface TagMessageResponse {
  message: string;
}

export interface AddTaskTagRequest {
  tagUuid: string;
}
