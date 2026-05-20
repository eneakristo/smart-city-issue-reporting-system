import { Link } from 'react-router-dom';
import { useAppContext } from '../hooks/useAppContext';
import { useLanguage } from '../hooks/useLanguage';

export function HomePage() {
  const { currentRole, isAuthenticated } = useAppContext();
  const { t } = useLanguage();
  const primaryAction = currentRole === 'system_admin'
    ? { to: '/admin', label: t('home.primarySystemAdmin') }
    : currentRole === 'administrator'
      ? { to: '/administrator', label: t('home.primaryAdministrator') }
      : isAuthenticated
        ? { to: '/report-issue', label: t('home.primaryCitizen') }
        : { to: '/register', label: t('home.primaryGuest') };
  const secondaryAction = isAuthenticated
    ? { to: '/issues', label: t('home.secondaryIssues') }
    : { to: '/login', label: t('home.secondaryLogin') };

  return (
    <section className="home-page">
      <div className="home-hero">
        <div className="home-hero-copy">
          <p className="page-kicker">{t('common.platform')}</p>
          <h1>{t('common.appName')}</h1>
          <p>{t('home.heroCopy')}</p>

          <div className="home-actions">
            <Link className="primary-button home-action-button" to={primaryAction.to}>
              {primaryAction.label}
            </Link>
            <Link className="secondary-button home-action-button" to={secondaryAction.to}>
              {secondaryAction.label}
            </Link>
          </div>
        </div>

        <div className="home-hero-visual" aria-label="City street view">
          <div className="home-visual-card">
            <span>Skanderbeg Square</span>
            <strong>Tirana city reports</strong>
          </div>
        </div>
      </div>

      <div className="home-stat-grid" aria-label="Platform highlights">
        <div className="home-stat">
          <span>01</span>
          <strong>{t('home.stat1.title')}</strong>
          <p>{t('home.stat1.copy')}</p>
        </div>
        <div className="home-stat">
          <span>02</span>
          <strong>{t('home.stat2.title')}</strong>
          <p>{t('home.stat2.copy')}</p>
        </div>
        <div className="home-stat">
          <span>03</span>
          <strong>{t('home.stat3.title')}</strong>
          <p>{t('home.stat3.copy')}</p>
        </div>
      </div>

      <div className="home-workflow-grid">
        <section className="home-workflow-panel">
          <p className="issue-category">{t('home.citizens')}</p>
          <h2>{t('home.citizensTitle')}</h2>
          <ul className="home-check-list">
            <li>{t('home.citizenItem1')}</li>
            <li>{t('home.citizenItem2')}</li>
            <li>{t('home.citizenItem3')}</li>
          </ul>
        </section>

        <section className="home-workflow-panel accent">
          <p className="issue-category">{t('home.admins')}</p>
          <h2>{t('home.adminsTitle')}</h2>
          <ul className="home-check-list">
            <li>{t('home.adminItem1')}</li>
            <li>{t('home.adminItem2')}</li>
            <li>{t('home.adminItem3')}</li>
          </ul>
        </section>
      </div>
    </section>
  );
}
