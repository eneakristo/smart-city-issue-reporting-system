import { Navigate, Outlet } from 'react-router-dom';
import { useAppContext } from '../../hooks/useAppContext';
import { useLanguage } from '../../hooks/useLanguage';

export function PublicOnlyRoute() {
  const { isAuthenticated, isLoadingAuth } = useAppContext();
  const { t } = useLanguage();

  if (isLoadingAuth) {
    return <div className="route-status">{t('common.loadingSession')}</div>;
  }

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}
