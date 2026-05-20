import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAppContext } from '../../hooks/useAppContext';
import { useLanguage } from '../../hooks/useLanguage';

type ProtectedRouteProps = {
  administratorOnly?: boolean;
  systemAdminOnly?: boolean;
};

export function ProtectedRoute({
  administratorOnly = false,
  systemAdminOnly = false,
}: ProtectedRouteProps) {
  const { currentRole, isAuthenticated, isLoadingAuth } = useAppContext();
  const { t } = useLanguage();
  const location = useLocation();

  if (isLoadingAuth) {
    return <div className="route-status">{t('common.loadingSession')}</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (administratorOnly && currentRole !== 'administrator' && currentRole !== 'system_admin') {
    return <Navigate to="/" replace />;
  }

  if (systemAdminOnly && currentRole !== 'system_admin') {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}
