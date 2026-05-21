import { IconType } from './project.model';
import { OrganLinkSummary } from './organ-link.model';

export interface OrganSummary {
  uuid: string;
  title: string;
  description: string | null;
  iconType: IconType;
  iconData: string | null;
  highlightColor: string;
}

export interface CreateOrganRequest {
  title: string;
  description?: string;
  highlightColor?: string;
  iconType?: IconType;
  iconData?: string;
}

export interface CreateOrganResponse {
  uuid: string;
  title: string;
}

export interface TrashedOrganSummary {
  uuid: string;
  title: string;
  description: string | null;
  deletedAt: string; // ISO DateTime
}

export interface OrganDetailResponse extends OrganSummary {
  createdAt: string; // ISO DateTime
  links: OrganLinkSummary[];
}

export interface UpdateOrganRequest {
  title?: string;
  description?: string;
  highlightColor?: string;
  iconType?: IconType;
  iconData?: string | null;
}

export interface UpdateOrganResponse {
  message: string;
}

export interface RestoreOrganResponse {
  uuid: string;
  title: string;
}

export interface OrganPermissionsResponse {
  permissions: string[];
}

export interface OrganMember {
  uuid: string;
  firstName: string;
  lastName: string;
  email: string;
}

export interface CheckPermissionResponse {
  allowed: boolean;
}

export interface ReadyTaskSummary {
  uuid: string;
  title: string;
  priority: string;
}
