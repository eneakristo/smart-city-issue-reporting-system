import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { PageSection } from '../components/common/PageSection';
import { useAppContext } from '../hooks/useAppContext';
import { useLanguage } from '../hooks/useLanguage';
import { listDuplicateReviews, reviewDuplicate } from '../services/api/adminService';
import { AdminDuplicateReview, AdminDuplicateReviewDecision } from '../types/app';

type ReviewAction = Exclude<AdminDuplicateReviewDecision, 'PENDING'>;

export function DuplicateReportsPage() {
  const { accessToken } = useAppContext();
  const { t } = useLanguage();
  const [reviews, setReviews] = useState<AdminDuplicateReview[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [activeReviewId, setActiveReviewId] = useState<string | null>(null);

  useEffect(() => {
    const loadReviews = async () => {
      if (!accessToken) {
        return;
      }

      try {
        setIsLoading(true);
        setErrorMessage(null);
        const duplicateReviews = await listDuplicateReviews(accessToken);
        setReviews(duplicateReviews);
      } catch (error) {
        setErrorMessage(error instanceof Error ? error.message : 'Could not load duplicate reports.');
      } finally {
        setIsLoading(false);
      }
    };

    void loadReviews();
  }, [accessToken]);

  const handleReview = async (reviewId: string, decision: ReviewAction) => {
    if (!accessToken) {
      return;
    }

    try {
      setActiveReviewId(reviewId);
      setErrorMessage(null);
      const updatedReview = await reviewDuplicate(reviewId, decision, accessToken);
      setReviews((currentReviews) => {
        if (updatedReview.reviewDecision === 'REJECTED_DUPLICATE') {
          return currentReviews.filter((review) => review.reviewId !== reviewId);
        }

        return currentReviews.map((review) => (review.reviewId === reviewId ? updatedReview : review));
      });
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Could not update duplicate review.');
    } finally {
      setActiveReviewId(null);
    }
  };

  return (
    <PageSection
      title={t('duplicates.title')}
      description={t('duplicates.description')}
    >
      <div className="duplicate-review-toolbar">
        <p>{t('duplicates.queue', { count: reviews.length, plural: reviews.length === 1 ? '' : 's' })}</p>
      </div>

      {errorMessage ? <div className="form-alert error">{errorMessage}</div> : null}
      {isLoading ? <div className="route-status">{t('duplicates.loading')}</div> : null}

      {!isLoading && reviews.length === 0 ? (
        <div className="issues-empty-state">
          <h2>{t('duplicates.emptyTitle')}</h2>
          <p>{t('duplicates.emptyCopy')}</p>
        </div>
      ) : null}

      {!isLoading && reviews.length > 0 ? (
        <div className="duplicate-review-list">
          {reviews.map((review) => (
            <article className="duplicate-review-card" key={review.reviewId}>
              <div className="duplicate-review-main">
                <div className="duplicate-review-heading">
                  <div>
                    <p className="issue-category">{review.category}</p>
                    <h2>{review.reportedTitle}</h2>
                    <p className="issue-location">{review.locationText}</p>
                  </div>
                  <span className="issue-status-badge">{formatDecision(review.reviewDecision, t)}</span>
                </div>

                <div className="duplicate-review-images">
                  {review.images.length > 0 ? (
                    review.images.map((image) => (
                      <a
                        className="duplicate-review-image"
                        href={image.publicUrl}
                        key={image.id}
                        target="_blank"
                        rel="noreferrer"
                      >
                        <img src={image.publicUrl} alt={image.originalFileName} />
                      </a>
                    ))
                  ) : (
                    <div className="duplicate-review-image-fallback">{t('duplicates.noImages')}</div>
                  )}
                </div>

                <div className="duplicate-review-match">
                  <div>
                    <span className="issue-meta-label">{t('duplicates.confidence')}</span>
                    <strong>{formatConfidence(review.duplicateConfidence, t)}</strong>
                  </div>
                  <div>
                    <span className="issue-meta-label">{t('duplicates.matchReason')}</span>
                    <p>{review.matchReason ?? t('duplicates.noReason')}</p>
                  </div>
                </div>
              </div>

              <aside className="duplicate-review-side">
                <h3>{t('duplicates.linkedOriginal')}</h3>
                {review.linkedOriginalIssue ? (
                  <div className="duplicate-original-card">
                    <p className="issue-category">{review.linkedOriginalIssue.category}</p>
                    <Link to={`/issues/${review.linkedOriginalIssue.issueId}`}>
                      {review.linkedOriginalIssue.title}
                    </Link>
                    <p>{review.linkedOriginalIssue.locationText}</p>
                    <span className="issue-status-badge">{review.linkedOriginalIssue.status}</span>
                  </div>
                ) : (
                  <p className="detail-muted">{t('duplicates.noOriginal')}</p>
                )}

                <div className="duplicate-review-actions">
                  <button
                    className="primary-button"
                    type="button"
                    disabled={activeReviewId === review.reviewId}
                    onClick={() => void handleReview(review.reviewId, 'CONFIRMED_DUPLICATE')}
                  >
                    {t('duplicates.confirm')}
                  </button>
                  <button
                    className="secondary-button"
                    type="button"
                    disabled={activeReviewId === review.reviewId}
                    onClick={() => void handleReview(review.reviewId, 'REJECTED_DUPLICATE')}
                  >
                    {t('duplicates.reject')}
                  </button>
                </div>
              </aside>
            </article>
          ))}
        </div>
      ) : null}
    </PageSection>
  );
}

function formatConfidence(confidence: number | null, t: (key: string) => string): string {
  if (confidence === null) {
    return t('duplicates.notStored');
  }

  return `${Math.round(confidence * 100)}%`;
}

function formatDecision(decision: AdminDuplicateReviewDecision, t: (key: string) => string): string {
  if (decision === 'PENDING') {
    return t('common.pending');
  }

  if (decision === 'CONFIRMED_DUPLICATE') {
    return t('duplicates.confirm');
  }

  return t('duplicates.reject');
}
