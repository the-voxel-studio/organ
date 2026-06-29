export interface TaskDependencyResponse {
  dependsOnTaskUuid: string;
  title: string;
  status: string; // Task status value
  deletedAt?: string; // ISO DateTime (retourné dans la corbeille)
}

export interface AddTaskDependencyRequest {
  dependsOnTaskUuid: string;
}

export interface TaskDependencyMessageResponse {
  message: string;
}
