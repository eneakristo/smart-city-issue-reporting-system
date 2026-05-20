import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAppContext } from '../../hooks/useAppContext';
import { useLanguage } from '../../hooks/useLanguage';
import { LANGUAGES, Language } from '../../i18n/translations';
import {
  listAdminIssueNotifications,
} from '../../services/api/adminService';
import { AdminIssueNotification, UserRole } from '../../types/app';

export function Topbar() {
  const { accessToken, currentRole, currentUser, isAuthenticated, logout } = useAppContext();
  const { language, setLanguage, t } = useLanguage();
  const [notifications, setNotifications] = useState<AdminIssueNotification[]>([]);
  const [isNotificationsOpen, setIsNotificationsOpen] = useState(false);
  const [notificationError, setNotificationError] = useState<string | null>(null);
  const notificationRef = useRef<HTMLDivElement | null>(null);
  const navigate = useNavigate();

  const canShowNotifications =
    isAuthenticated && (currentRole === 'administrator' || currentRole === 'system_admin') && Boolean(accessToken);

  useEffect(() => {
    if (!canShowNotifications || !accessToken) {
      setNotifications([]);
      setIsNotificationsOpen(false);
      return;
    }

    void loadNotifications(accessToken);
  }, [accessToken, canShowNotifications]);

  useEffect(() => {
    if (!isNotificationsOpen) {
      return;
    }

    const handlePointerDown = (event: PointerEvent) => {
      const notificationPanel = notificationRef.current;

      if (!notificationPanel || !(event.target instanceof Node) || notificationPanel.contains(event.target)) {
        return;
      }

      setIsNotificationsOpen(false);
    };

    document.addEventListener('pointerdown', handlePointerDown);

    return () => {
      document.removeEventListener('pointerdown', handlePointerDown);
    };
  }, [isNotificationsOpen]);

  const loadNotifications = async (token: string) => {
    setNotificationError(null);

    try {
      setNotifications(await listAdminIssueNotifications(token));
    } catch {
      setNotificationError(t('topbar.notificationsError'));
    }
  };

  const handleToggleNotifications = () => {
    const nextOpenState = !isNotificationsOpen;
    setIsNotificationsOpen(nextOpenState);

    if (nextOpenState && accessToken) {
      void loadNotifications(accessToken);
    }
  };

  const handleOpenNotification = async (notification: AdminIssueNotification) => {
    if (!accessToken) {
      return;
    }

    setIsNotificationsOpen(false);
    navigate(`/administrator?issueId=${notification.issueId}`);
  };

  return (
    <header className="topbar">
      <div className="topbar-actions">
        {isAuthenticated && currentUser ? (
          <div className="session-summary">
            <div className="session-copy">
              <strong>{currentUser.fullName}</strong>
              <span>{currentUser.email}</span>
            </div>
            <Link className="profile-avatar-link" to="/profile" aria-label="Open profile">
              {currentUser.profilePictureUrl ? (
                <img src={currentUser.profilePictureUrl} alt="" />
              ) : (
                <span>{getInitials(currentUser.fullName)}</span>
              )}
            </Link>
            <div className="role-badge">{formatRoleLabel(currentRole, t)}</div>
            <label className="language-switcher" aria-label={t('common.selectLanguage')}>
              <select
                value={language}
                onChange={(event) => setLanguage(event.target.value as Language)}
              >
                {LANGUAGES.map((item) => (
                  <option key={item.code} value={item.code}>
                    {item.shortLabel}
                  </option>
                ))}
              </select>
            </label>
            {canShowNotifications ? (
              <div className="notification-menu" ref={notificationRef}>
                <button
                  className="notification-button"
                  type="button"
                  onClick={handleToggleNotifications}
                  aria-label={t('topbar.notifications')}
                  aria-expanded={isNotificationsOpen}
                >
                  <BellIcon />
                  {notifications.length > 0 ? (
                    <span className="notification-count">{notifications.length}</span>
                  ) : null}
                </button>

                {isNotificationsOpen ? (
                  <div className="notification-dropdown">
                    <div className="notification-dropdown-header">
                      <strong>{t('topbar.newReports')}</strong>
                      <span>{t('topbar.reportedCount', { count: notifications.length })}</span>
                    </div>

                    {notificationError ? <div className="form-alert error">{notificationError}</div> : null}

                    <div className="notification-dropdown-list">
                      {notifications.length === 0 && !notificationError ? (
                        <p className="detail-muted">{t('topbar.noReports')}</p>
                      ) : null}

                      {notifications.map((notification) => (
                        <button
                          key={notification.issueId}
                          type="button"
                          className="notification-dropdown-item"
                          onClick={() => void handleOpenNotification(notification)}
                        >
                          <strong>{notification.reporterName}</strong>
                          <span>{notification.title}</span>
                          <small>{formatDateTime(notification.createdAt)}</small>
                        </button>
                      ))}
                    </div>
                  </div>
                ) : null}
              </div>
            ) : null}
            <button className="secondary-button" type="button" onClick={logout}>
              {t('common.logout')}
            </button>
          </div>
        ) : (
          <div className="guest-badge">{t('common.guestSession')}</div>
        )}
      </div>
    </header>
  );
}

function formatRoleLabel(role: UserRole | null, t: (key: string) => string) {
  if (!role) {
    return '';
  }

  return t(`role.${role}`);
}

function getInitials(fullName: string) {
  return fullName
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('');
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

function BellIcon() {
  return (
    <svg aria-hidden="true" viewBox="0 0 24 24" className="notification-icon">
      <path
        d="M18 9.7V13l1.7 3H4.3L6 13V9.7C6 6.4 8.5 4 12 4s6 2.4 6 5.7Z"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
      />
      <path
        d="M10 19a2.2 2.2 0 0 0 4 0"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeWidth="2"
      />
    </svg>
  );
}
