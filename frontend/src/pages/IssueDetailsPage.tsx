import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { PageSection } from '../components/common/PageSection';
import { useAppContext } from '../hooks/useAppContext';
import { useLanguage } from '../hooks/useLanguage';
import { ApiError } from '../services/api/apiClient';
import { getIssueById, removeVote, voteForIssue } from '../services/api/issueService';
import { IssueDetails } from '../types/app';

export function IssueDetailsPage() {
  const { issueId } = useParams();
  const { accessToken, isAuthenticated } = useAppContext();
  const { t, language } = useLanguage();
  const [issue, setIssue] = useState<IssueDetails | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [voteError, setVoteError] = useState<string | null>(null);
  const [isVoting, setIsVoting] = useState(false);

  useEffect(() => {
    if (!issueId) {
      setError('Issue id is missing.');
      setIsLoading(false);
      return;
    }

    let isActive = true;

    const loadIssue = async () => {
      setIsLoading(true);
      setError(null);

      try {
        const response = await getIssueById(issueId, accessToken);

        if (isActive) {
          setIssue(response);
        }
      } catch (caughtError) {
        if (isActive) {
          setError(
            caughtError instanceof ApiError
              ? caughtError.message
              : 'Unable to load issue details right now.',
          );
        }
      } finally {
        if (isActive) {
          setIsLoading(false);
        }
      }
    };

    void loadIssue();

    return () => {
      isActive = false;
    };
  }, [accessToken, issueId]);

  const handleVote = async () => {
    if (!issueId || !accessToken || !issue) {
      return;
    }

    setIsVoting(true);
    setVoteError(null);

    try {
      const response = issue.currentUserHasVoted
        ? await removeVote(issueId, accessToken)
        : await voteForIssue(issueId, accessToken);

      setIssue({
        ...issue,
        voteCount: response.voteCount,
        currentUserHasVoted: response.currentUserHasVoted,
        priorityScore: response.priorityScore,
      });
    } catch (caughtError) {
      setVoteError(
        caughtError instanceof ApiError ? caughtError.message : 'Unable to update your vote.',
      );
    } finally {
      setIsVoting(false);
    }
  };

  const title = issue ? issue.title : `${t('details.title')}${issueId ? ` #${issueId}` : ''}`;

  const description = issue
    ? t('details.description')
    : t('details.loadingDescription');

  return (
    <PageSection
      title={title}
      description={description}
    >
      {isLoading ? <div className="route-status">{t('details.loading')}</div> : null}
      {error ? <div className="form-alert error">{error}</div> : null}

      {!isLoading && !error && issue ? (
        <div className="issue-details-layout">
          <div className="issue-details-main">
            <section className="detail-panel">
              <div className="issue-detail-heading">
                <div>
                  <p className="issue-category">{issue.category}</p>
                  <h2>{issue.title}</h2>
                </div>
                <span className={`issue-status-badge status-${issue.status.toLowerCase()}`}>
                  {t(`status.${issue.status}`)}
                </span>
              </div>

              <p className="issue-detail-description">{issue.description}</p>

              <div className="issue-meta-grid">
                <div>
                  <span className="issue-meta-label">{t('common.severity')}</span>
                  <strong>{t(`common.${issue.severity.toLowerCase()}`)}</strong>
                </div>
                <div>
                  <span className="issue-meta-label">{t('common.location')}</span>
                  <strong>{issue.locationText}</strong>
                </div>
                <div>
                  <span className="issue-meta-label">{t('common.zone')}</span>
                  <strong>{issue.zone ? t(`zone.${issue.zone}`) : t('common.notApplicable')}</strong>
                </div>
                <div>
                  <span className="issue-meta-label">{t('common.votes')}</span>
                  <strong>{issue.voteCount}</strong>
                </div>
                <div>
                  <span className="issue-meta-label">{t('details.priorityScore')}</span>
                  <strong>{formatPriority(issue.priorityScore, t)}</strong>
                </div>
              </div>
            </section>

            {issue.latitude !== null && issue.longitude !== null ? (
              <section className="detail-panel">
                <div className="detail-panel-header">
                  <h3>{t('details.map')}</h3>
                  <span>{issue.latitude.toFixed(6)}, {issue.longitude.toFixed(6)}</span>
                </div>

                <div className="location-map-frame">
                  <iframe
                    title="Issue location map"
                    src={buildMapEmbedUrl(issue.latitude, issue.longitude)}
                    loading="lazy"
                    referrerPolicy="no-referrer-when-downgrade"
                  />
                </div>
              </section>
            ) : null}

            <section className="detail-panel">
              <div className="detail-panel-header">
                <h3>Reported evidence</h3>
                <span>{t('details.attached', { count: issue.images.length })}</span>
              </div>

              {issue.images.length > 0 ? (
                <div className="issue-image-grid">
                  {issue.images.map((image) => (
                    <a
                      key={image.id}
                      href={image.publicUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="issue-image-card"
                    >
                      <img src={image.publicUrl} alt={image.originalFileName} />
                    </a>
                  ))}
                </div>
              ) : (
                <p className="detail-muted">{t('details.noImages')}</p>
              )}
            </section>

            {issue.resolutionImages.length > 0 ? (
              <section className="detail-panel resolution-evidence-panel">
                <div className="detail-panel-header">
                  <h3>Resolution evidence</h3>
                  <span>{t('details.attached', { count: issue.resolutionImages.length })}</span>
                </div>

                <div className="issue-image-grid">
                  {issue.resolutionImages.map((image) => (
                    <a
                      key={image.id}
                      href={image.publicUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="issue-image-card resolution-image-card"
                    >
                      <img src={image.publicUrl} alt={image.originalFileName} />
                    </a>
                  ))}
                </div>
              </section>
            ) : null}
          </div>

          <aside className="issue-details-sidebar">
            <section className="detail-panel">
              <div className="detail-panel-header">
                <h3>{t('details.support')}</h3>
              </div>

              <p className="detail-stat">{issue.voteCount}</p>
              <p className="detail-muted">
                {issue.currentUserHasVoted
                  ? t('details.supported')
                  : t('details.supportPrompt')}
              </p>

              {issue.issueType === 'PRIMARY' ? (
                isAuthenticated ? (
                  <button
                    type="button"
                    className="primary-button"
                    onClick={handleVote}
                    disabled={isVoting}
                  >
                    {isVoting
                      ? issue.currentUserHasVoted
                        ? 'Removing vote...'
                        : 'Submitting vote...'
                      : issue.currentUserHasVoted
                        ? t('details.removeVote')
                        : t('details.vote')}
                  </button>
                ) : (
                  <Link to="/login" className="secondary-button detail-link-button">
                    {t('details.loginToVote')}
                  </Link>
                )
              ) : (
                <p className="detail-muted">Voting is available on the original issue only.</p>
              )}

              {voteError ? <div className="form-alert error">{voteError}</div> : null}
            </section>

            <section className="detail-panel">
              <div className="detail-panel-header">
                <h3>{t('details.duplicateContext')}</h3>
              </div>

              {issue.issueType === 'PRIMARY' ? (
                <>
                  <p className="detail-stat">{issue.linkedDuplicateCount}</p>
                  <p className="detail-muted">
                    linked duplicate {issue.linkedDuplicateCount === 1 ? 'report' : 'reports'}
                  </p>
                </>
              ) : (
                <>
                  <p className="detail-muted">This report is marked as a duplicate of another issue.</p>
                  {issue.parentIssueId ? (
                    <Link to={`/issues/${issue.parentIssueId}`} className="secondary-button detail-link-button">
                      Open original issue
                    </Link>
                  ) : null}
                </>
              )}
            </section>

            <section className="detail-panel">
              <div className="detail-panel-header">
                <h3>{t('details.record')}</h3>
              </div>

              <div className="detail-stack">
                <p><strong>{t('details.reporter')}:</strong> {issue.reporterName}</p>
                <p><strong>{t('common.created')}:</strong> {formatDate(issue.createdAt, language)}</p>
                <p><strong>{t('details.updated')}:</strong> {formatDate(issue.updatedAt, language)}</p>
              </div>
            </section>

            <section className="detail-panel">
              <div className="detail-panel-header">
                <h3>{t('details.history')}</h3>
                <span>{t('details.events', { count: issue.statusHistory.length })}</span>
              </div>

              {issue.statusHistory.length > 0 ? (
                <div className="status-history-list">
                  {issue.statusHistory.map((entry) => (
                    <article key={entry.historyId} className="status-history-item">
                      <div>
                        <strong>
                          {entry.oldStatus ? t(`status.${entry.oldStatus}`) : t('details.created')} {t('details.to')}{' '}
                          {t(`status.${entry.newStatus}`)}
                        </strong>
                        <span>{formatDateTime(entry.changedAt, language)}</span>
                      </div>
                      <p>{entry.note ?? t('details.statusUpdated')}</p>
                      <small>{t('details.changedBy', { name: entry.changedByName })}</small>
                    </article>
                  ))}
                </div>
              ) : (
                <p className="detail-muted">{t('details.noHistory')}</p>
              )}
            </section>
          </aside>
        </div>
      ) : null}
    </PageSection>
  );
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

function formatDateTime(value: string, language: string) {
  return new Intl.DateTimeFormat(language === 'sq' ? 'sq-AL' : undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

function buildMapEmbedUrl(latitude: number, longitude: number) {
  const latOffset = 0.008;
  const lngOffset = 0.012;
  const bbox = [
    longitude - lngOffset,
    latitude - latOffset,
    longitude + lngOffset,
    latitude + latOffset,
  ]
    .map((value) => value.toFixed(6))
    .join('%2C');

  return `https://www.openstreetmap.org/export/embed.html?bbox=${bbox}&layer=mapnik&marker=${latitude.toFixed(6)}%2C${longitude.toFixed(6)}`;
}
