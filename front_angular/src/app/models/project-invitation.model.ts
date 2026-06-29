import { ProjectGlobalRole } from './project-member.model';

export interface ProjectInvitationSummary {
  uuid: string;
  email: string;
  role: ProjectGlobalRole;
  createdAt: string; // ISO DateTime
  expiresAt: string; // ISO DateTime
}

export interface UserInvitationResponse {
  uuid: string;
  projectName: string;
  invitedBy: string;
  role: ProjectGlobalRole;
  createdAt: string; // ISO DateTime
  expiresAt: string; // ISO DateTime
}

export interface AcceptInvitationResponse {
  message: string;
}

export interface RefuseInvitationResponse {
  message: string;
}
