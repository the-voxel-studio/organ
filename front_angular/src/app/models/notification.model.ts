export interface NotificationResponse {
  uuid: string;
  type: string;
  message: string;
  isRead?: boolean;
  taskUuid?: string | null;
  createdAt?: string; // ISO DateTime
  deletedAt?: string; // ISO DateTime
}

export interface SubscribeUrlResponse {
  hubUrl: string;
  topic: string;
  token: string;
}

export interface NotificationMessageResponse {
  message: string;
}
