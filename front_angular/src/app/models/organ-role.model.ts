import { IconType } from './project.model';

export interface OrganRoleSummary {
  uuid: string;
  name: string;
  iconType: IconType;
  iconData: string | null;
}

export interface OrganRoleDefinition extends OrganRoleSummary {
  permissions: string[];
  members: Array<{
    uuid: string;
    firstName: string;
    lastName: string;
    email: string;
  }>;
}

export interface TrashedOrganRoleSummary extends OrganRoleSummary {
  deletedAt: string; // ISO DateTime
}

export interface CreateOrganRoleRequest {
  name: string;
  iconType?: IconType;
  iconData?: string;
  permissions?: string[];
}

export interface CreateOrganRoleResponse {
  uuid: string;
  name: string;
}

export interface UpdateOrganRoleRequest {
  name?: string;
  iconType?: IconType;
  iconData?: string | null;
  permissions?: string[];
}

export interface AssignRoleRequest {
  userUuid: string;
}

export interface AssignRoleResponse {
  message: string;
}

export interface TrashedRoleMember {
  uuid: number; // UserOrganRole DB primary key (numeric)
  user: {
    uuid: string;
    firstName: string;
    lastName: string;
    email: string;
  };
  role: {
    uuid: string;
    name: string;
  };
  deletedAt: string; // ISO DateTime
}

export interface RestoreRoleMemberResponse {
  message: string;
}

export interface RestoreOrganRoleResponse {
  uuid: string;
  name: string;
}
