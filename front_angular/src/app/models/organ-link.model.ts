export interface OrganLinkSummary {
  uuid: string;
  url: string;
  description: string | null;
}

export interface TrashedOrganLinkSummary extends OrganLinkSummary {
  deletedAt: string; // ISO DateTime
}

export interface CreateOrganLinkRequest {
  url: string;
  description?: string;
}

export interface UpdateOrganLinkRequest {
  url?: string;
  description?: string | null;
}
