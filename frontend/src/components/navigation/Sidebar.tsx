import { NavLink } from 'react-router-dom';
import { useAppContext } from '../../hooks/useAppContext';
import { useLanguage } from '../../hooks/useLanguage';
import { UserRole } from '../../types/app';

const navigationItems = [
  { to: '/', labelKey: 'nav.home', visibility: 'public' },
  { to: '/login', labelKey: 'nav.login', visibility: 'guest' },
  { to: '/register', labelKey: 'nav.register', visibility: 'guest' },
  { to: '/report-issue', labelKey: 'nav.reportIssue', visibility: 'citizen' },
  { to: '/issues', labelKey: 'nav.issues', visibility: 'public' },
  { to: '/about', labelKey: 'nav.about', visibility: 'public' },
  { to: '/profile', labelKey: 'nav.profile', visibility: 'authenticated' },
  { to: '/my-reports', labelKey: 'nav.myReports', visibility: 'citizen' },
  { to: '/administrator', labelKey: 'nav.adminDashboard', visibility: 'administrator' },
  { to: '/duplicates', labelKey: 'nav.duplicates', visibility: 'administrator' },
  { to: '/admin', labelKey: 'nav.systemAdmin', visibility: 'system_admin' },
];

export function Sidebar() {
  const { currentRole, isAuthenticated } = useAppContext();
  const { t } = useLanguage();
  const visibleNavigationItems = navigationItems.filter((item) =>
    canShowNavigationItem(item.visibility, isAuthenticated, currentRole),
  );

  return (
    <aside className="sidebar">
      <div className="brand-block">
        <p className="brand-label">TF</p>
        <h2>{t('common.appName')}</h2>
        <div className="sidebar-session">
          {isAuthenticated
            ? t('common.signedInAs', { role: formatRoleLabel(currentRole, t) })
            : t('common.notSignedIn')}
        </div>
      </div>

      <nav className="sidebar-nav" aria-label="Main navigation">
        {visibleNavigationItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
          >
            {t(item.labelKey)}
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}

function canShowNavigationItem(
  visibility: string,
  isAuthenticated: boolean,
  currentRole: UserRole | null,
) {
  if (visibility === 'guest') {
    return !isAuthenticated;
  }

  if (visibility === 'authenticated') {
    return isAuthenticated;
  }

  if (visibility === 'citizen') {
    return currentRole === 'citizen';
  }

  if (visibility === 'administrator') {
    return currentRole === 'administrator' || currentRole === 'system_admin';
  }

  if (visibility === 'system_admin') {
    return currentRole === 'system_admin';
  }

  return true;
}

function formatRoleLabel(role: UserRole | null, t: (key: string) => string) {
  if (!role) {
    return '';
  }

  return t(`role.${role}`);
}
