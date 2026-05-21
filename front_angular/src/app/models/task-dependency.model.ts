export interface TaskDependencyResponse {
  dependsOnTaskUuid: string;
  title: string;
  status: string; // Task status value
  deletedAt?: string; // ISO DateTime (returned in trash)
}

export interface AddTaskDependencyRequest {
  dependsOnTaskUuid: string;
}

export interface TaskDependencyMessageResponse {
  message: string;
}
