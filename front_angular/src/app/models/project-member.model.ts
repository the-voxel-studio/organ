export type ProjectGlobalRole = 'ADMIN' | 'MANAGER' | 'MEMBER';

export interface ProjectMemberSummary {
  uuid: string;
  user: {
    uuid: string;
    email: string;
    firstName: string;
    lastName: string;
  };
  role: ProjectGlobalRole;
  deletedAt?: string; // ISO DateTime
}

export interface InviteMemberRequest {
  email: string;
  role?: ProjectGlobalRole;
}

export interface InviteMemberResponse {
  uuid: string;
  message: string;
}

export interface UpdateMemberRequest {
  role: ProjectGlobalRole;
}

export interface UpdateMemberResponse {
  message: string;
}

export interface RestoreMemberResponse {
  message: string;
}
