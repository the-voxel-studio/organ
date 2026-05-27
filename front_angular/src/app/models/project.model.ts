export type ProjectStatus = 'ACTIVE' | 'ARCHIVED' | 'TRASHED' | 'DRAFT' | 'IN_PROGRESS' | 'COMPLETED';
export type IconType = 'ICON' | 'EMOJI' | 'IMAGE' | 'SVG' | 'BLOB';

export interface ProjectSummary {
  uuid: string;
  title: string;
  description: string | null;
  status: ProjectStatus;
  color: string;
  iconType: IconType;
  iconData: string | null;
  createdAt: string; // ISO DateTime
  deletedAt?: string; // ISO DateTime
}

export interface ProjectDetailResponse extends ProjectSummary {
  role: string;
}

export interface CreateProjectRequest {
  title: string;
  description?: string;
  color?: string;
  status?: ProjectStatus;
  iconType?: IconType;
  iconData?: string;
}

export interface CreateProjectResponse {
  uuid: string;
  title: string;
  role: string;
}

export interface ProjectDetailedViewResponse {
  project: ProjectSummary & {
    description: string | null;
    role: string;
  };
  admin: {
    firstName: string;
    lastName: string;
    email: string;
  } | null;
  organs: Array<{
    uuid: string;
    title: string;
    description: string | null;
    iconType: string;
    iconData: string | null;
    highlightColor: string | null;
    activeTasksCount: number;
  }>;
  members: Array<{
    uuid: string;
    user: {
      uuid: string;
      email: string;
      firstName: string;
      lastName: string;
    };
    globalRole: string;
  }>;
  activities: Array<ProjectActivityFeedItem>;
  securityLogs: any[];
}

export interface ProjectActivityFeedItem {
  type: 'COMMENT' | 'ATTACHMENT' | 'HISTORY';
  detail?: string;
  created_at: string | null;
  field_name: string | null;
  action_type: string | null;
  user_first_name: string;
  user_last_name: string;
  task_title: string | null;
  task_uuid: string | null;
  organ_title: string | null;
  old_value?: any;
  new_value?: any;
}

export interface UpdateProjectRequest {
  title?: string;
  description?: string;
  status?: ProjectStatus;
  color?: string;
  iconType?: IconType;
  iconData?: string | null;
}

export interface UpdateProjectResponse {
  uuid: string;
  title: string;
  status: ProjectStatus;
  color: string;
}

export interface ProjectPermissionsResponse {
  role: string;
}

export interface ProjectStatsResponse {
  history: Array<{
    date: string;
    tasksCreated: number;
    tasksCompleted: number;
    tasksCanceled: number;
    commentsAdded: number;
    attachmentsAdded: number;
    membersActive: number;
    consultations: number;
    statusChanges: number;
  }>;
  current: Array<{
    organ_name: string;
    status: string;
    task_count: number;
    total_hours: number;
  }>;
}

export interface ProjectAuditLogItem {
  id: string;
  actionType: string;
  fieldName: string | null;
  oldValues: any;
  newValues: any;
  context: any;
  createdAt: string;
  user: {
    uuid: string;
    firstName: string;
    lastName: string;
    email: string;
  } | null;
  taskUuid: string | null;
  taskTitle: string | null;
  organUuid: string | null;
  organTitle: string | null;
}

export interface MemberActionStat {
  type: string;
  count: number;
}

export interface BackendMemberStat {
  uuid: string;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  actions: MemberActionStat[];
}

export interface MemberActivityStats {
  uuid: string;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  totalActions: number;
  filteredActionsCount: number; // Actions correspondant aux cases à cocher visibles
  createdTasks: number;
  statusChanges: number;
  comments: number;
  attachments: number;
  updates: number;
  consultations: number;
  projectConsultations: number;
  organConsultations: number;
  taskConsultations: number;
  totalModifications: number;
}

