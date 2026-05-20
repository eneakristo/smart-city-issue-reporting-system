import {
  AssignableUserRole,
  CreateManagedUserPayload,
  CityZone,
  ManagedUser,
  UpdateManagedUserRolePayload,
} from '../../types/app';
import { apiClient } from './apiClient';

const SYSTEM_ADMIN_API_BASE = '/api/system-admin';

type SystemAdminUserResponse = Omit<ManagedUser, 'role'> & {
  role: string;
  assignedZone: CityZone | null;
};

export async function listUsers(accessToken: string): Promise<ManagedUser[]> {
  const users = await apiClient<SystemAdminUserResponse[]>(`${SYSTEM_ADMIN_API_BASE}/users`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });

  return users.map(normalizeManagedUser);
}

export async function createUser(
  payload: CreateManagedUserPayload,
  accessToken: string,
): Promise<ManagedUser> {
  const response = await apiClient<SystemAdminUserResponse>(`${SYSTEM_ADMIN_API_BASE}/users`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify({
      fullName: payload.fullName,
      email: payload.email,
      password: payload.password,
      role: toBackendRole(payload.role),
      assignedZone: payload.role === 'administrator' ? payload.assignedZone ?? 'ZONE_1' : null,
    }),
  });

  return normalizeManagedUser(response);
}

export async function removeUser(userId: string, accessToken: string): Promise<void> {
  return apiClient<void>(`${SYSTEM_ADMIN_API_BASE}/users/${userId}`, {
    method: 'DELETE',
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}

export async function updateUserRole(
  userId: string,
  payload: UpdateManagedUserRolePayload,
  accessToken: string,
): Promise<ManagedUser> {
  const response = await apiClient<SystemAdminUserResponse>(`${SYSTEM_ADMIN_API_BASE}/users/${userId}/role`, {
    method: 'PATCH',
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify({
      role: toBackendRole(payload.role),
      assignedZone: payload.role === 'administrator' ? payload.assignedZone ?? 'ZONE_1' : null,
    }),
  });

  return normalizeManagedUser(response);
}

function normalizeManagedUser(user: SystemAdminUserResponse): ManagedUser {
  return {
    ...user,
    role: normalizeRole(user.role),
    assignedZone: user.assignedZone,
  };
}

function normalizeRole(role: string): AssignableUserRole {
  const normalizedRole = role.toUpperCase();

  if (normalizedRole === 'ADMINISTRATOR' || normalizedRole === 'ADMIN') {
    return 'administrator';
  }

  if (normalizedRole === 'SYSTEM_ADMIN') {
    return 'system_admin';
  }

  return 'citizen';
}

function toBackendRole(role: AssignableUserRole) {
  if (role === 'administrator') {
    return 'ADMINISTRATOR';
  }

  if (role === 'system_admin') {
    return 'SYSTEM_ADMIN';
  }

  return 'CITIZEN';
}
