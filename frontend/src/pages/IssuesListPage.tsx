import { ChangeEvent, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { PageSection } from '../components/common/PageSection';
import { useAppContext } from '../hooks/useAppContext';
import { useLanguage } from '../hooks/useLanguage';
import { listIssues } from '../services/api/issueService';
import { ApiError } from '../services/api/apiClient';
import { IssueSortOption, IssueStatus, IssueSummary } from '../types/app';

const SORT_OPTIONS: IssueSortOption[] = ['NEWEST', 'MOST_VOTED', 'HIGHEST_PRIORITY'];
const STATUS_OPTIONS: IssueStatus[] = ['REPORTED', 'UNDER_REVIEW', 'CONFIRMED', 'IN_PROGRESS', 'RESOLVED', 'REJECTED'];

export function IssuesListPage() {
  const { accessToken, isAuthenticated } = useAppContext();
  const { t, language } = useLanguage();
  const [issues, setIssues] = useState<IssueSummary[]>([]);
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState('');
  const [status, setStatus] = useState<IssueStatus | ''>('');
  const [sort, setSort] = useState<IssueSortOption>('NEWEST');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    const loadIssues = async () => {
      setIsLoading(true);
      setError(null);

      try {
        const response = await listIssues(
          {
            keyword: search.trim() || undefined,
            category: category || undefined,
            status: status || undefined,
            sort,
          },
          accessToken,
        );

        if (!controller.signal.aborted) {
          setIssues(response.filter((issue) => issue.issueType === 'PRIMARY'));
        }
      } catch (caughtError) {
        if (!controller.signal.aborted) {
          setError(
            caughtError instanceof ApiError
              ? caughtError.message
              : 'Unable to load issues right now.',
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
  }, [accessToken, category, search, sort, status]);

  const categoryOptions = useMemo(() => {
    return Array.from(new Set(issues.map((issue) => issue.category))).sort((left, right) =>
      left.localeCompare(right),
    );
  }, [issues]);

  const resultLabel = isLoading
    ? t('issues.loading')
    : t('issues.found', { count: issues.length, plural: issues.length === 1 ? '' : 's' });

  return (
    <PageSection
      title={t('issues.title')}
      description={t('issues.description')}
    >
      <div className="issues-toolbar">
        <label className="form-field issues-search-field">
          {t('common.search')}
          <input
            type="search"
            placeholder={t('issues.searchPlaceholder')}
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </label>

        <div className="issues-filters">
          <label className="form-field">
            {t('common.category')}
            <select value={category} onChange={(event) => setCategory(event.target.value)}>
              <option value="">{t('issues.allCategories')}</option>
              {categoryOptions.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          </label>

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
            <h2>{t('issues.emptyTitle')}</h2>
            <p>{t('issues.emptyCopy')}</p>
          </div>
        ) : null}

        {issues.map((issue) => (
          <Link key={issue.issueId} to={`/issues/${issue.issueId}`} className="issue-card-link">
            <article className="issue-card">
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

                {isAuthenticated && issue.currentUserHasVoted ? (
                  <p className="issue-support-state">{t('issues.voted')}</p>
                ) : null}

                <div className="issue-meta-grid">
                  <div>
                    <span className="issue-meta-label">{t('common.votes')}</span>
                    <strong>{issue.voteCount}</strong>
                  </div>
                  <div>
                    <span className="issue-meta-label">{t('common.priority')}</span>
                    <strong>{formatPriority(issue.priorityScore, t)}</strong>
                  </div>
                  <div>
                    <span className="issue-meta-label">{t('common.priorityLabel')}</span>
                    <strong>{priorityLabel(issue.priorityScore, t)}</strong>
                  </div>
                  <div>
                    <span className="issue-meta-label">{t('common.reported')}</span>
                    <strong>{formatDate(issue.createdAt, language)}</strong>
                  </div>
                </div>
              </div>
            </article>
          </Link>
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

function priorityLabel(priorityScore: number | null, t: (key: string) => string) {
  if (priorityScore === null) {
    return t('common.notScored');
  }

  if (priorityScore >= 80) {
    return t('common.urgent');
  }

  if (priorityScore >= 50) {
    return t('common.high');
  }

  if (priorityScore >= 25) {
    return t('common.medium');
  }

  return t('common.low');
}

function formatDate(value: string, language: string) {
  return new Intl.DateTimeFormat(language === 'sq' ? 'sq-AL' : undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(new Date(value));
}
