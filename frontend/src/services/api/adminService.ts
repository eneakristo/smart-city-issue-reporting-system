import {
  AdminDuplicateReview,
  AdminDuplicateReviewDecision,
  AdminIssueNotification,
  AdminIssueSeverityResponse,
  AdminSeverity,
  AdminStatusUpdatePayload,
  IssueListFilters,
  IssueSummary,
} from '../../types/app';
import { apiClient } from './apiClient';

const ADMINISTRATOR_API_BASE = '/api/administrator';

export async function listDuplicateReviews(accessToken: string): Promise<AdminDuplicateReview[]> {
  return apiClient<AdminDuplicateReview[]>(`${ADMINISTRATOR_API_BASE}/duplicates`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}

export async function reviewDuplicate(
  reviewId: string,
  decision: Exclude<AdminDuplicateReviewDecision, 'PENDING'>,
  accessToken: string,
): Promise<AdminDuplicateReview> {
  return apiClient<AdminDuplicateReview>(`${ADMINISTRATOR_API_BASE}/duplicates/${reviewId}`, {
    method: 'PATCH',
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify({ decision }),
  });
}

export async function listAdminIssues(
  filters: IssueListFilters,
  accessToken: string,
): Promise<IssueSummary[]> {
  const query = buildQueryString(filters);
  const path = query ? `${ADMINISTRATOR_API_BASE}/issues?${query}` : `${ADMINISTRATOR_API_BASE}/issues`;

  return apiClient<IssueSummary[]>(path, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}

export async function listAdminIssueNotifications(accessToken: string): Promise<AdminIssueNotification[]> {
  return apiClient<AdminIssueNotification[]>(`${ADMINISTRATOR_API_BASE}/notifications/issues`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}

export async function markAdminIssueNotificationSeen(
  issueId: string,
  accessToken: string,
): Promise<void> {
  return apiClient<void>(`${ADMINISTRATOR_API_BASE}/notifications/issues/${issueId}/seen`, {
    method: 'PATCH',
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}

export async function updateAdminIssueStatus(
  issueId: string,
  payload: AdminStatusUpdatePayload,
  accessToken: string,
): Promise<IssueSummary> {
  if (payload.resolutionImage) {
    const formData = new FormData();
    formData.append('status', payload.status);

    if (payload.note) {
      formData.append('note', payload.note);
    }

    formData.append('resolutionImage', payload.resolutionImage);

    return apiClient<IssueSummary>(`${ADMINISTRATOR_API_BASE}/issues/${issueId}/status`, {
      method: 'PATCH',
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
      body: formData,
    });
  }

  return apiClient<IssueSummary>(`${ADMINISTRATOR_API_BASE}/issues/${issueId}/status`, {
    method: 'PATCH',
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(payload),
  });
}

export async function updateAdminIssueSeverity(
  issueId: string,
  severity: AdminSeverity,
  accessToken: string,
): Promise<AdminIssueSeverityResponse> {
  return apiClient<AdminIssueSeverityResponse>(`${ADMINISTRATOR_API_BASE}/issues/${issueId}/severity`, {
    method: 'PATCH',
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify({ severity }),
  });
}

export async function deleteAdminIssue(issueId: string, accessToken: string): Promise<void> {
  return apiClient<void>(`${ADMINISTRATOR_API_BASE}/issues/${issueId}`, {
    method: 'DELETE',
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}

function buildQueryString(filters: IssueListFilters): string {
  const params = new URLSearchParams();

  if (filters.keyword) {
    params.set('keyword', filters.keyword);
  }

  if (filters.category) {
    params.set('category', filters.category);
  }

  if (filters.status) {
    params.set('status', filters.status);
  }

  if (filters.locationText) {
    params.set('locationText', filters.locationText);
  }

  if (filters.sort) {
    params.set('sort', filters.sort);
  }

  return params.toString();
}
