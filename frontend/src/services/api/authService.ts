import { apiClient } from './apiClient';
import {
  AuthResponse,
  AuthSession,
  ChangePasswordPayload,
  ForgotPasswordPayload,
  LoginPayload,
  RegisterPayload,
  ResetPasswordPayload,
  UpdateProfilePayload,
  UserProfile,
  UserRole,
} from '../../types/app';

const AUTH_STORAGE_KEY = 'smart-city-auth-session';

export async function register(payload: RegisterPayload): Promise<AuthResponse> {
  return apiClient<AuthResponse>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function login(payload: LoginPayload): Promise<AuthResponse> {
  return apiClient<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function requestPasswordReset(payload: ForgotPasswordPayload): Promise<{ message: string }> {
  return apiClient<{ message: string }>('/api/auth/forgot-password', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function resetPassword(payload: ResetPasswordPayload): Promise<{ message: string }> {
  return apiClient<{ message: string }>('/api/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function requestCurrentUser(accessToken: string): Promise<UserProfile> {
  const response = await apiClient<{
    id: string;
    fullName: string;
    email: string;
    phoneNumber: string | null;
    bio: string | null;
    profilePictureUrl: string | null;
    role: string;
  }>('/api/auth/me', {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });

  return {
    id: response.id,
    fullName: response.fullName,
    email: response.email,
    phoneNumber: response.phoneNumber,
    bio: response.bio,
    profilePictureUrl: response.profilePictureUrl,
    role: normalizeRole(response.role),
  };
}

export async function updateProfile(payload: UpdateProfilePayload, accessToken: string): Promise<UserProfile> {
  const response = await apiClient<{
    id: string;
    fullName: string;
    email: string;
    phoneNumber: string | null;
    bio: string | null;
    profilePictureUrl: string | null;
    role: string;
  }>('/api/auth/me', {
    method: 'PUT',
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(payload),
  });

  return {
    id: response.id,
    fullName: response.fullName,
    email: response.email,
    phoneNumber: response.phoneNumber,
    bio: response.bio,
    profilePictureUrl: response.profilePictureUrl,
    role: normalizeRole(response.role),
  };
}

export async function changePassword(payload: ChangePasswordPayload, accessToken: string): Promise<{ message: string }> {
  return apiClient<{ message: string }>('/api/auth/change-password', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(payload),
  });
}

export function storeAuthSession(session: AuthSession) {
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
}

export function readStoredAuth(): AuthSession | null {
  const rawSession = localStorage.getItem(AUTH_STORAGE_KEY);

  if (!rawSession) {
    return null;
  }

  try {
    const session = JSON.parse(rawSession) as AuthSession;

    return {
      ...session,
      user: {
        ...session.user,
        role: normalizeRole(session.user.role),
      },
    };
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
}

export function logout() {
  localStorage.removeItem(AUTH_STORAGE_KEY);
}

function normalizeRole(role: string): UserRole {
  const normalizedRole = role.toUpperCase();

  if (normalizedRole === 'ADMINISTRATOR' || normalizedRole === 'ADMIN') {
    return 'administrator';
  }

  if (normalizedRole === 'SYSTEM_ADMIN') {
    return 'system_admin';
  }

  return 'citizen';
}
