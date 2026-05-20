import { ReactNode } from 'react';
import { useLanguage } from '../../hooks/useLanguage';

type PageSectionProps = {
  title: string;
  description: string;
  children?: ReactNode;
};

export function PageSection({ title, description, children }: PageSectionProps) {
  const { t } = useLanguage();

  return (
    <section className="page-card">
      <div className="page-header">
        <p className="page-kicker">{t('common.platform')}</p>
        <h1>{title}</h1>
        {description ? <p className="page-description">{description}</p> : null}
      </div>
      {children}
    </section>
  );
}
