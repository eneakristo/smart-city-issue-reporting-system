import { apiClient } from './apiClient';
import {
  CreateIssuePayload,
  CreateIssueResponse,
  IssueDetails,
  IssueListFilters,
  IssueSummary,
  VoteIssueResponse,
} from '../../types/app';

export async function createIssue(
  payload: CreateIssuePayload,
  images: File[],
  accessToken: string,
): Promise<CreateIssueResponse> {
  const formData = new FormData();
  formData.append('payload', new Blob([JSON.stringify(payload)], { type: 'application/json' }));

  images.forEach((image) => {
    formData.append('images', image);
  });

  return apiClient<CreateIssueResponse>('/api/issues', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
    body: formData,
  });
}

export async function listIssues(
  filters: IssueListFilters = {},
  accessToken?: string | null,
): Promise<IssueSummary[]> {
  const query = buildQueryString(filters);
  const path = query ? `/api/issues?${query}` : '/api/issues';
  return apiClient<IssueSummary[]>(path, {
    headers: accessToken
      ? {
          Authorization: `Bearer ${accessToken}`,
        }
      : undefined,
  });
}

export async function listCurrentUserIssues(
  filters: IssueListFilters = {},
  accessToken: string,
): Promise<IssueSummary[]> {
  const query = buildQueryString(filters);
  const path = query ? `/api/issues/me?${query}` : '/api/issues/me';

  return apiClient<IssueSummary[]>(path, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}

export async function getIssueById(
  issueId: string,
  accessToken?: string | null,
): Promise<IssueDetails> {
  return apiClient<IssueDetails>(`/api/issues/${issueId}`, {
    headers: accessToken
      ? {
          Authorization: `Bearer ${accessToken}`,
        }
      : undefined,
  });
}

export async function voteForIssue(issueId: string, accessToken: string): Promise<VoteIssueResponse> {
  return apiClient<VoteIssueResponse>(`/api/issues/${issueId}/votes`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}

export async function removeVote(issueId: string, accessToken: string): Promise<VoteIssueResponse> {
  return apiClient<VoteIssueResponse>(`/api/issues/${issueId}/votes`, {
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
