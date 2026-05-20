import { ChangeEvent, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { PageSection } from '../components/common/PageSection';
import { useAppContext } from '../hooks/useAppContext';
import { useLanguage } from '../hooks/useLanguage';
import { ApiError } from '../services/api/apiClient';
import { listCurrentUserIssues } from '../services/api/issueService';
import { IssueSortOption, IssueStatus, IssueSummary } from '../types/app';

const SORT_OPTIONS: IssueSortOption[] = ['NEWEST', 'MOST_VOTED', 'HIGHEST_PRIORITY'];
const STATUS_OPTIONS: IssueStatus[] = ['REPORTED', 'UNDER_REVIEW', 'CONFIRMED', 'IN_PROGRESS', 'RESOLVED', 'REJECTED'];

export function MyReportsPage() {
  const { accessToken } = useAppContext();
  const { t, language } = useLanguage();
  const [issues, setIssues] = useState<IssueSummary[]>([]);
  const [status, setStatus] = useState<IssueStatus | ''>('');
  const [sort, setSort] = useState<IssueSortOption>('NEWEST');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!accessToken) {
      setIssues([]);
      setIsLoading(false);
      setError(t('myReports.loginRequired'));
      return;
    }

    const controller = new AbortController();

    const loadIssues = async () => {
      setIsLoading(true);
      setError(null);

      try {
        const response = await listCurrentUserIssues(
          {
            status: status || undefined,
            sort,
          },
          accessToken,
        );

        if (!controller.signal.aborted) {
          setIssues(response);
        }
      } catch (caughtError) {
        if (!controller.signal.aborted) {
          setError(
            caughtError instanceof ApiError
              ? caughtError.message
              : 'Unable to load your reports right now.',
          );
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false);
        }
      }
    };

    void loadIssues();

    return () => {
      controller.abort();
    };
  }, [accessToken, sort, status]);

  const resultLabel = isLoading
    ? t('myReports.loading')
    : t('myReports.found', { count: issues.length, plural: issues.length === 1 ? '' : 's' });

  return (
    <PageSection
      title={t('myReports.title')}
      description={t('myReports.description')}
    >
      <div className="issues-toolbar">
        <div className="issues-filters">
          <label className="form-field">
            {t('common.status')}
            <select
              value={status}
              onChange={(event: ChangeEvent<HTMLSelectElement>) =>
                setStatus(event.target.value as IssueStatus | '')
              }
            >
              <option value="">{t('issues.allStatuses')}</option>
              {STATUS_OPTIONS.map((option) => (
                <option key={option} value={option}>
                  {t(`status.${option}`)}
                </option>
              ))}
            </select>
          </label>

          <label className="form-field">
            {t('common.sortBy')}
            <select
              value={sort}
              onChange={(event: ChangeEvent<HTMLSelectElement>) =>
                setSort(event.target.value as IssueSortOption)
              }
            >
              {SORT_OPTIONS.map((option) => (
                <option key={option} value={option}>
                  {t(`sort.${option}`)}
                </option>
              ))}
            </select>
          </label>
        </div>
      </div>

      <div className="issues-results-bar">
        <p>{resultLabel}</p>
      </div>

      {error ? <div className="form-alert error">{error}</div> : null}

      <div className="issues-list">
        {!isLoading && !error && issues.length === 0 ? (
          <div className="issues-empty-state">
            <h2>{t('myReports.emptyTitle')}</h2>
            <p>{t('myReports.emptyCopy')}</p>
          </div>
        ) : null}

        {issues.map((issue) => (
          <article key={issue.issueId} className="issue-card my-report-card">
              <div className="issue-thumbnail">
                <IssueThumbnail thumbnailUrl={issue.thumbnailUrl} title={issue.title} />
              </div>

              <div className="issue-card-content">
                <div className="issue-card-head">
                  <div>
                    <p className="issue-category">{issue.category}</p>
                    <h2>{issue.title}</h2>
                  </div>
                  <span className={`issue-status-badge status-${issue.status.toLowerCase()}`}>
                    {t(`status.${issue.status}`)}
                  </span>
                </div>

                <p className="issue-location">{issue.locationText}</p>
                <div className={issue.issueType === 'DUPLICATE' ? 'duplicate-report-note' : 'primary-report-note'}>
                  <strong>{issue.issueType === 'DUPLICATE' ? t('myReports.markedDuplicate') : t('myReports.primaryReport')}</strong>
                  {issue.issueType === 'DUPLICATE' ? (
                    issue.parentIssueId ? (
                      <span>
                        {t('myReports.linkedOriginal')}{' '}
                        <Link to={`/issues/${issue.parentIssueId}`}>
                          {issue.parentIssueTitle ?? `Issue #${issue.parentIssueId}`}
                        </Link>
                      </span>
                    ) : (
                      <span>{t('myReports.originalMissing')}</span>
                    )
                  ) : (
                    <span>{t('myReports.primaryTracked')}</span>
                  )}
                </div>

                <div className="issue-meta-grid">
                  <div>
                    <span className="issue-meta-label">{t('common.type')}</span>
                    <strong>{issue.issueType === 'DUPLICATE' ? t('common.duplicate') : t('common.primary')}</strong>
                  </div>
                  <div>
                    <span className="issue-meta-label">{t('common.votes')}</span>
                    <strong>{issue.voteCount}</strong>
                  </div>
                  <div>
                    <span className="issue-meta-label">{t('common.priority')}</span>
                    <strong>{formatPriority(issue.priorityScore, t)}</strong>
                  </div>
                  <div>
                    <span className="issue-meta-label">{t('common.reported')}</span>
                    <strong>{formatDate(issue.createdAt, language)}</strong>
                  </div>
                </div>

                <div className="my-report-actions">
                  <Link to={`/issues/${issue.issueId}`} className="secondary-button detail-link-button">
                    {t('myReports.openReport')}
                  </Link>
                  {issue.issueType === 'DUPLICATE' && issue.parentIssueId ? (
                    <Link to={`/issues/${issue.parentIssueId}`} className="secondary-button detail-link-button">
                      {t('myReports.openOriginal')}
                    </Link>
                  ) : null}
                </div>
              </div>
            </article>
        ))}
      </div>
    </PageSection>
  );
}

function IssueThumbnail({
  thumbnailUrl,
  title,
}: {
  thumbnailUrl: string | null;
  title: string;
}) {
  if (thumbnailUrl) {
    return <img src={thumbnailUrl} alt={title} className="issue-thumbnail-image" />;
  }

  return <div className="issue-thumbnail-fallback">{title.slice(0, 1).toUpperCase()}</div>;
}

function formatPriority(priorityScore: number | null, t: (key: string) => string) {
  return priorityScore === null ? t('common.pending') : priorityScore.toFixed(2);
}

function formatDate(value: string, language: string) {
  return new Intl.DateTimeFormat(language === 'sq' ? 'sq-AL' : undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(new Date(value));
}
