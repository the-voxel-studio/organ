import { UserSummary } from './user.model';

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'WAITING' | 'DONE' | 'CANCELED';

export interface TaskTagSummary {
  uuid: string;
  name: string;
  color: string;
}

export interface TaskLinkSummary {
  uuid: string;
  url: string;
  description: string | null;
}

export interface TaskResponse {
  uuid: string;
  title: string;
  description: string | null;
  status: TaskStatus;
  statusMessage: string | null;
  priority: number;
  estimatedHours: string | null;
  startDate: string | null; // ISO DateTime
  expiresAt: string | null; // ISO DateTime
  createdBy: UserSummary | null;
  manager: UserSummary | null;
  assignees: UserSummary[];
  tags: TaskTagSummary[];
  links: TaskLinkSummary[];
  commentCount: number;
  attachmentCount: number;
  dependencyCount: number;
  createdAt: string; // ISO DateTime
  deletedAt?: string; // ISO DateTime (retourné dans la corbeille)
}

export interface CreateTaskRequest {
  title: string;
  description?: string;
  priority?: number;
  status?: TaskStatus;
  statusMessage?: string;
  estimatedHours?: string;
  startDate?: string; // ISO DateTime string
  expiresAt?: string; // ISO DateTime string
  managerUuid?: string;
  assigneeUuids?: string[];
  tagUuids?: string[];
  dependencyUuids?: string[];
  linkData?: Array<{
    url: string;
    description?: string;
  }>;
}

export interface UpdateTaskRequest {
  title?: string;
  description?: string;
  priority?: number;
  status?: TaskStatus;
  statusMessage?: string;
  estimatedHours?: string;
  startDate?: string | null; // ISO DateTime string
  expiresAt?: string | null; // ISO DateTime string
  managerUuid?: string | null;
}

export interface TaskPermissionsResponse {
  permissions: string[];
  editableFields: string[];
  isProjectAdmin: boolean;
  taskOwnership: {
    isManager: boolean;
    isAssignee: boolean;
    isCreator: boolean;
  };
}

export interface TaskTimelineItem {
  type: 'COMMENT' | 'ATTACHMENT' | 'HISTORY';
  detail?: string;
  createdAt: string; // ISO DateTime string
  userName: string;
  userUuid: string | null;
  actionType?: string;
  fieldName?: string | null;
  oldValue?: any;
  newValue?: any;
}

export interface AddAssigneeRequest {
  userUuid: string;
}

export interface AddAssigneeResponse {
  message: string;
}
