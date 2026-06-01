import { ChangeEvent, FormEvent, useEffect, useMemo, useState } from 'react';
import { PageSection } from '../components/common/PageSection';
import { useAppContext } from '../hooks/useAppContext';
import { useLanguage } from '../hooks/useLanguage';
import { ApiError } from '../services/api/apiClient';
import { createUser, listUsers, removeUser, updateUserRole } from '../services/api/systemAdminService';
import { AssignableUserRole, CityZone, ManagedUser } from '../types/app';
import { isStrongPassword } from '../utils/authValidation';

const ROLE_OPTIONS: AssignableUserRole[] = ['citizen', 'administrator'];
const ZONE_OPTIONS: CityZone[] = ['ZONE_1', 'ZONE_2', 'ZONE_3', 'ZONE_4'];

export function SystemAdminDashboardPage() {
  const { accessToken, currentUser } = useAppContext();
  const { t, language } = useLanguage();
  const [users, setUsers] = useState<ManagedUser[]>([]);
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<AssignableUserRole>('citizen');
  const [assignedZone, setAssignedZone] = useState<CityZone>('ZONE_1');
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [removingUserId, setRemovingUserId] = useState<string | null>(null);
  const [updatingRoleUserId, setUpdatingRoleUserId] = useState<string | null>(null);
  const [draftRoles, setDraftRoles] = useState<Record<string, AssignableUserRole>>({});
  const [draftZones, setDraftZones] = useState<Record<string, CityZone>>({});
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const userStats = useMemo(() => {
    const activeCount = users.filter((user) => user.active).length;
    const removedCount = users.length - activeCount;
    const systemAdminCount = users.filter((user) => user.role === 'system_admin' && user.active).length;

    return { activeCount, removedCount, systemAdminCount };
  }, [users]);

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    void loadUsers(accessToken);
  }, [accessToken]);

  const loadUsers = async (token: string) => {
    setIsLoading(true);
    setError(null);

    try {
      setUsers(await listUsers(token));
    } catch (caughtError) {
      setError(caughtError instanceof ApiError ? caughtError.message : 'Unable to load users right now.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateUser = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!accessToken) {
      return;
    }

    setError(null);
    setSuccessMessage(null);

    if (!fullName.trim() || !email.trim()) {
      setError(t('system.createValidation'));
      return;
    }

    if (!isStrongPassword(password)) {
      setError(t('validation.passwordMin'));
      return;
    }

    setIsSubmitting(true);

    try {
      const createdUser = await createUser(
        {
          fullName: fullName.trim(),
          email: email.trim(),
          password,
          role,
          assignedZone: role === 'administrator' ? assignedZone : null,
        },
        accessToken,
      );

      setUsers((currentUsers) => [createdUser, ...currentUsers]);
      setFullName('');
      setEmail('');
      setPassword('');
      setRole('citizen');
      setAssignedZone('ZONE_1');
      setSuccessMessage(t('system.userCreated'));
    } catch (caughtError) {
      setError(caughtError instanceof ApiError ? caughtError.message : 'Unable to create user.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRemoveUser = async (user: ManagedUser) => {
    if (!accessToken || !user.active) {
      return;
    }

    const shouldRemove = window.confirm(t('system.confirmRemove', { name: user.fullName }));
    if (!shouldRemove) {
      return;
    }

    setRemovingUserId(user.id);
    setError(null);
    setSuccessMessage(null);

    try {
      await removeUser(user.id, accessToken);
      setUsers((currentUsers) =>
        currentUsers.map((currentUserItem) =>
          currentUserItem.id === user.id ? { ...currentUserItem, active: false } : currentUserItem,
        ),
      );
      setSuccessMessage(t('system.userRemoved'));
    } catch (caughtError) {
      setError(caughtError instanceof ApiError ? caughtError.message : 'Unable to remove user.');
    } finally {
      setRemovingUserId(null);
    }
  };

  const handleRoleDraftChange = (userId: string, nextRole: AssignableUserRole) => {
    setDraftRoles((currentDrafts) => ({
      ...currentDrafts,
      [userId]: nextRole,
    }));
  };

  const handleZoneDraftChange = (userId: string, nextZone: CityZone) => {
    setDraftZones((currentDrafts) => ({
      ...currentDrafts,
      [userId]: nextZone,
    }));
  };

  const handleUpdateRole = async (user: ManagedUser) => {
    if (!accessToken || !user.active || user.role === 'system_admin') {
      return;
    }

    const selectedRole = draftRoles[user.id] ?? user.role;
    const selectedZone = draftZones[user.id] ?? user.assignedZone ?? 'ZONE_1';

    if (selectedRole === 'system_admin') {
      return;
    }

    if (selectedRole === user.role && (selectedRole !== 'administrator' || selectedZone === user.assignedZone)) {
      return;
    }

    setUpdatingRoleUserId(user.id);
    setError(null);
    setSuccessMessage(null);

    try {
      const updatedUser = await updateUserRole(
        user.id,
        {
          role: selectedRole,
          assignedZone: selectedRole === 'administrator' ? selectedZone : null,
        },
        accessToken,
      );
      setUsers((currentUsers) =>
        currentUsers.map((currentUserItem) => (currentUserItem.id === user.id ? updatedUser : currentUserItem)),
      );
      setDraftRoles((currentDrafts) => {
        const nextDrafts = { ...currentDrafts };
        delete nextDrafts[user.id];
        return nextDrafts;
      });
      setDraftZones((currentDrafts) => {
        const nextDrafts = { ...currentDrafts };
        delete nextDrafts[user.id];
        return nextDrafts;
      });
      setSuccessMessage(t('system.roleUpdated'));
    } catch (caughtError) {
      setError(caughtError instanceof ApiError ? caughtError.message : 'Unable to update user role.');
    } finally {
      setUpdatingRoleUserId(null);
    }
  };

  return (
    <PageSection
      title={t('system.title')}
      description={t('system.description')}
    >
      <div className="system-admin-dashboard">
        <section className="admin-summary-grid">
          <div className="admin-summary-tile">
            <span>{t('system.totalUsers')}</span>
            <strong>{users.length}</strong>
          </div>
          <div className="admin-summary-tile">
            <span>{t('common.active')}</span>
            <strong>{userStats.activeCount}</strong>
          </div>
          <div className="admin-summary-tile">
            <span>{t('common.removed')}</span>
            <strong>{userStats.removedCount}</strong>
          </div>
          <div className="admin-summary-tile">
            <span>{t('system.systemAdmins')}</span>
            <strong>{userStats.systemAdminCount}</strong>
          </div>
        </section>

        <section className="system-admin-layout">
          <form className="system-user-form" onSubmit={handleCreateUser} noValidate>
            <div>
              <p className="issue-category">{t('system.createUser')}</p>
              <h2>{t('system.newAccount')}</h2>
            </div>

            <label className="form-field">
              {t('common.fullName')}
              <input
                type="text"
                value={fullName}
                onChange={(event) => setFullName(event.target.value)}
                placeholder="User full name"
              />
            </label>

            <label className="form-field">
              {t('common.email')}
              <input
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="user@example.com"
              />
            </label>

            <label className="form-field">
              {t('common.password')}
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder={t('system.passwordHint')}
              />
            </label>

            <label className="form-field">
              {t('common.role')}
              <select
                value={role}
                onChange={(event: ChangeEvent<HTMLSelectElement>) =>
                  setRole(event.target.value as AssignableUserRole)
                }
              >
                {ROLE_OPTIONS.map((option) => (
                  <option key={option} value={option}>
                    {t(`role.${option}`)}
                  </option>
                ))}
              </select>
            </label>

            {role === 'administrator' ? (
              <label className="form-field">
                {t('common.zone')}
                <select
                  value={assignedZone}
                  onChange={(event: ChangeEvent<HTMLSelectElement>) => setAssignedZone(event.target.value as CityZone)}
                >
                  {ZONE_OPTIONS.map((option) => (
                    <option key={option} value={option}>
                      {t(`zone.${option}`)}
                    </option>
                  ))}
                </select>
              </label>
            ) : null}

            <button className="primary-button" type="submit" disabled={isSubmitting}>
              {isSubmitting ? t('system.creatingUser') : t('system.createUser')}
            </button>
          </form>

          <section className="system-user-management">
            <div className="issues-results-bar">
              <p>{isLoading ? t('system.loadingUsers') : t('system.usersShown', { count: users.length, plural: users.length === 1 ? '' : 's' })}</p>
            </div>

            {error ? <div className="form-alert error">{error}</div> : null}
            {successMessage ? <div className="form-alert success">{successMessage}</div> : null}

            <div className="system-user-table">
              <div className="system-user-row system-user-header">
                <span>{t('common.user')}</span>
                <span>{t('common.role')}</span>
                <span>{t('common.zone')}</span>
                <span>{t('common.status')}</span>
                <span>{t('common.created')}</span>
                <span>{t('common.actions')}</span>
              </div>

              {!isLoading && users.length === 0 ? (
                <div className="issues-empty-state">
                  <h2>{t('system.noUsers')}</h2>
                  <p>{t('system.noUsersCopy')}</p>
                </div>
              ) : null}

              {users.map((user) => {
                const isCurrentUser = currentUser?.id === user.id;
                const canChangeRole = user.active && !isCurrentUser && user.role !== 'system_admin';
                const draftRole = draftRoles[user.id] ?? user.role;
                const draftZone = draftZones[user.id] ?? user.assignedZone ?? 'ZONE_1';
                const hasRoleChange = draftRole !== user.role
                  || (draftRole === 'administrator' && draftZone !== user.assignedZone);

                return (
                  <article key={user.id} className={`system-user-row ${user.active ? '' : 'removed'}`}>
                    <div className="system-user-cell">
                      <strong>{user.fullName}</strong>
                      <span>{user.email}</span>
                    </div>
                    <div className="system-user-role-control">
                      <select
                        value={draftRole}
                        disabled={!canChangeRole || updatingRoleUserId === user.id}
                        onChange={(event: ChangeEvent<HTMLSelectElement>) =>
                          handleRoleDraftChange(user.id, event.target.value as AssignableUserRole)
                        }
                      >
                        {ROLE_OPTIONS.map((option) => (
                          <option key={option} value={option}>
                            {t(`role.${option}`)}
                          </option>
                        ))}
                        {user.role === 'system_admin' ? <option value="system_admin">{t('role.system_admin')}</option> : null}
                      </select>
                      <button
                        className="secondary-button"
                        type="button"
                        disabled={!canChangeRole || !hasRoleChange || updatingRoleUserId === user.id}
                        onClick={() => void handleUpdateRole(user)}
                      >
                        {updatingRoleUserId === user.id ? t('common.saving') : t('common.save')}
                      </button>
                    </div>
                    <div className="system-user-role-control">
                      {draftRole === 'administrator' ? (
                        <select
                          value={draftZone}
                          disabled={!canChangeRole || updatingRoleUserId === user.id}
                          onChange={(event: ChangeEvent<HTMLSelectElement>) =>
                            handleZoneDraftChange(user.id, event.target.value as CityZone)
                          }
                        >
                          {ZONE_OPTIONS.map((option) => (
                            <option key={option} value={option}>
                              {t(`zone.${option}`)}
                            </option>
                          ))}
                        </select>
                      ) : (
                        <span>{t('common.notApplicable')}</span>
                      )}
                    </div>
                    <span className={`issue-status-badge ${user.active ? 'status-confirmed' : 'status-rejected'}`}>
                      {user.active ? t('common.active') : t('common.removed')}
                    </span>
                    <span>{formatDate(user.createdAt, language)}</span>
                    <button
                      className="danger-button"
                      type="button"
                      disabled={!user.active || isCurrentUser || removingUserId === user.id}
                      onClick={() => void handleRemoveUser(user)}
                    >
                      {removingUserId === user.id ? t('system.removing') : t('common.remove')}
                    </button>
                  </article>
                );
              })}
            </div>
          </section>
        </section>
      </div>
    </PageSection>
  );
}

function formatDate(value: string, language: string) {
  return new Intl.DateTimeFormat(language === 'sq' ? 'sq-AL' : undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(new Date(value));
}
