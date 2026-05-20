import { PageSection } from '../components/common/PageSection';
import { useLanguage } from '../hooks/useLanguage';

export function AboutPage() {
  const { t } = useLanguage();

  return (
    <PageSection
      title={t('about.title')}
      description={t('about.description')}
    >
      <section className="about-platform-section standalone" aria-labelledby="about-platform-title">
        <div className="about-platform-heading">
          <p className="page-kicker">{t('about.kicker')}</p>
          <h2 id="about-platform-title">{t('common.appName')}</h2>
          <p>{t('about.copy')}</p>
        </div>

        <div className="about-platform-grid">
          <article>
            <span>01</span>
            <h3>{t('about.card1.title')}</h3>
            <p>{t('about.card1.copy')}</p>
          </article>

          <article>
            <span>02</span>
            <h3>{t('about.card2.title')}</h3>
            <p>{t('about.card2.copy')}</p>
          </article>

          <article>
            <span>03</span>
            <h3>{t('about.card3.title')}</h3>
            <p>{t('about.card3.copy')}</p>
          </article>

          <article>
            <span>04</span>
            <h3>{t('about.card4.title')}</h3>
            <p>{t('about.card4.copy')}</p>
          </article>
        </div>
      </section>
    </PageSection>
  );
}
