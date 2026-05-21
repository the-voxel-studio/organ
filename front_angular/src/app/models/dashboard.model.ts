import { ProjectSummary } from './project.model';
import { TaskResponse } from './task.model';

export interface DashboardTaskResponse extends TaskResponse {
  projectName: string;
  projectUuid: string;
  organName: string;
  organUuid: string;
}

export interface DashboardResponse {
  projects: ProjectSummary[];
  tasks: DashboardTaskResponse[];
}
