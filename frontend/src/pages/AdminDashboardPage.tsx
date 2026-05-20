import { ChangeEvent, MouseEvent, useEffect, useMemo, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { PageSection } from '../components/common/PageSection';
import { useAppContext } from '../hooks/useAppContext';
import { useLanguage } from '../hooks/useLanguage';
import {
  deleteAdminIssue,
  listAdminIssues,
  updateAdminIssueSeverity,
  updateAdminIssueStatus,
} from '../services/api/adminService';
import { ApiError } from '../services/api/apiClient';
import { getIssueById } from '../services/api/issueService';
import {
  AdminManageableStatus,
  AdminSeverity,
  IssueDetails,
  IssueSortOption,
  IssueStatus,
  IssueSummary,
} from '../types/app';

const CATEGORY_OPTIONS = [
  'Roads',
  'Street Lighting',
  'Waste Management',
  'Water Supply',
  'Public Safety',
  'Parks and Green Areas',
  'Other',
];

const STATUS_FILTER_OPTIONS: Array<{ value: IssueStatus; label: string }> = [
  { value: 'REPORTED', label: 'Open' },
  { value: 'CONFIRMED', label: 'Confirmed' },
  { value: 'IN_PROGRESS', label: 'In progress' },
  { value: 'RESOLVED', label: 'Resolved' },
  { value: 'REJECTED', label: 'Rejected' },
];

const STATUS_UPDATE_OPTIONS: Array<{ value: AdminManageableStatus; label: string }> = [
  { value: 'REPORTED', label: 'Open' },
  { value: 'CONFIRMED', label: 'Confirmed' },
  { value: 'IN_PROGRESS', label: 'In progress' },
  { value: 'RESOLVED', label: 'Resolved' },
  { value: 'REJECTED', label: 'Rejected' },
];

const SEVERITY_OPTIONS: Array<{ value: AdminSeverity; label: string }> = [
  { value: 'LOW', label: 'Low' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'HIGH', label: 'High' },
];

const SORT_OPTIONS: Array<{ value: IssueSortOption; label: string }> = [
  { value: 'HIGHEST_PRIORITY', label: 'Highest priority' },
  { value: 'NEWEST', label: 'Newest' },
  { value: 'MOST_VOTED', label: 'Most voted' },
];

const ACCEPTED_RESOLUTION_IMAGE_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];

export function AdminDashboardPage() {
  const { accessToken } = useAppContext();
  const { t } = useLanguage();
  const [searchParams] = useSearchParams();
  const [issues, setIssues] = useState<IssueSummary[]>([]);
  const [category, setCategory] = useState('');
  const [status, setStatus] = useState<IssueStatus | ''>('');
  const [sort, setSort] = useState<IssueSortOption>('HIGHEST_PRIORITY');
  const [selectedIssue, setSelectedIssue] = useState<IssueDetails | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingDetails, setIsLoadingDetails] = useState(false);
  const [updatingIssueId, setUpdatingIssueId] = useState<string | null>(null);
  const [deletingIssueId, setDeletingIssueId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [detailsError, setDetailsError] = useState<string | null>(null);
  const [resolutionProofIssue, setResolutionProofIssue] = useState<IssueSummary | null>(null);
  const [resolutionProofFile, setResolutionProofFile] = useState<File | null>(null);
  const [resolutionProofPreview, setResolutionProofPreview] = useState('');
  const [resolutionProofError, setResolutionProofError] = useState('');
  const [isResolutionCameraOpen, setIsResolutionCameraOpen] = useState(false);
  const resolutionVideoRef = useRef<HTMLVideoElement | null>(null);
  const resolutionCameraStreamRef = useRef<MediaStream | null>(null);
  const detailsPanelRef = useRef<HTMLElement | null>(null);

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    const controller = new AbortController();

    const loadIssues = async () => {
      setIsLoading(true);
      setError(null);

      try {
        const response = await listAdminIssues(
          {
            category: category || undefined,
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
              : 'Unable to load admin issues right now.',
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
  }, [accessToken, category, sort, status]);

  const dashboardStats = useMemo(() => {
    const openCount = issues.filter((issue) => issue.status === 'REPORTED').length;
    const activeCount = issues.filter((issue) => issue.status === 'CONFIRMED' || issue.status === 'IN_PROGRESS').length;
    const closedCount = issues.filter((issue) => issue.status === 'RESOLVED' || issue.status === 'REJECTED').length;

    return { openCount, activeCount, closedCount };
  }, [issues]);
  const shouldShowDetailsPanel = isLoadingDetails || Boolean(detailsError) || Boolean(selectedIssue);

  useEffect(() => {
    const issueId = searchParams.get('issueId');

    if (!issueId) {
      return;
    }

    void openIssueDetails(issueId);
  }, [searchParams, accessToken]);

  useEffect(() => {
    if (!shouldShowDetailsPanel) {
      return;
    }

    const handlePointerDown = (event: PointerEvent) => {
      const detailsPanel = detailsPanelRef.current;

      if (!detailsPanel || !(event.target instanceof Node) || detailsPanel.contains(event.target)) {
        return;
      }

      setSelectedIssue(null);
      setDetailsError(null);
    };

    document.addEventListener('pointerdown', handlePointerDown);

    return () => {
      document.removeEventListener('pointerdown', handlePointerDown);
    };
  }, [shouldShowDetailsPanel]);

  const handleOpenDetails = async (issueId: string, event: MouseEvent<HTMLButtonElement>) => {
    event.stopPropagation();

    await openIssueDetails(issueId);
  };

  const openIssueDetails = async (issueId: string) => {
    if (!accessToken) {
      return;
    }

    setIsLoadingDetails(true);
    setDetailsError(null);

    try {
      const response = await getIssueById(issueId, accessToken);
      setSelectedIssue(response);
    } catch (caughtError) {
      setDetailsError(
        caughtError instanceof ApiError
          ? caughtError.message
          : 'Unable to load issue details right now.',
      );
    } finally {
      setIsLoadingDetails(false);
    }
  };

  const handleStatusChange = async (issue: IssueSummary, nextStatus: AdminManageableStatus) => {
    if (!accessToken || nextStatus === issue.status) {
      return;
    }

    if (nextStatus === 'RESOLVED') {
      setResolutionProofIssue(issue);
      setResolutionProofFile(null);
      setResolutionProofPreview('');
      setResolutionProofError('');
      closeResolutionCamera();
      return;
    }

    await updateIssueStatus(issue, nextStatus);
  };

  const updateIssueStatus = async (
    issue: IssueSummary,
    nextStatus: AdminManageableStatus,
    resolutionImage?: File,
  ) => {
    if (!accessToken) {
      return;
    }

    setUpdatingIssueId(issue.issueId);
    setError(null);

    try {
      const updatedIssue = await updateAdminIssueStatus(
        issue.issueId,
        {
          status: nextStatus,
          note: `Administrator dashboard changed status to ${formatStatus(nextStatus)}`,
          resolutionImage,
        },
        accessToken,
      );

      setIssues((currentIssues) =>
        currentIssues.map((currentIssue) =>
          currentIssue.issueId === updatedIssue.issueId ? updatedIssue : currentIssue,
        ),
      );

      if (selectedIssue?.issueId === updatedIssue.issueId) {
        const refreshedIssue = await getIssueById(updatedIssue.issueId, accessToken);
        setSelectedIssue(refreshedIssue);
      }
    } catch (caughtError) {
      setError(
        caughtError instanceof ApiError ? caughtError.message : 'Unable to update issue status.',
      );
    } finally {
      setUpdatingIssueId(null);
    }
  };

  const handleResolutionProofFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];

    if (!file) {
      return;
    }

    setResolutionProofError('');

    if (!ACCEPTED_RESOLUTION_IMAGE_TYPES.includes(file.type)) {
      setResolutionProofError('Only JPEG, PNG, and WEBP images are allowed.');
      return;
    }

    setResolutionProofFile(file);
    setResolutionProofPreview(URL.createObjectURL(file));
  };

  const handleOpenResolutionCamera = async () => {
    if (!navigator.mediaDevices?.getUserMedia) {
      setResolutionProofError('Camera access is not supported in this browser.');
      return;
    }

    setResolutionProofError('');

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: { ideal: 'environment' } },
        audio: false,
      });

      resolutionCameraStreamRef.current = stream;
      setIsResolutionCameraOpen(true);

      window.setTimeout(() => {
        if (resolutionVideoRef.current) {
          resolutionVideoRef.current.srcObject = stream;
          void resolutionVideoRef.current.play();
        }
      }, 0);
    } catch {
      setResolutionProofError('Camera access was denied or the camera is unavailable.');
    }
  };

  const closeResolutionCamera = () => {
    resolutionCameraStreamRef.current?.getTracks().forEach((track) => track.stop());
    resolutionCameraStreamRef.current = null;

    if (resolutionVideoRef.current) {
      resolutionVideoRef.current.srcObject = null;
    }

    setIsResolutionCameraOpen(false);
  };

  const handleCaptureResolutionPhoto = async () => {
    const video = resolutionVideoRef.current;

    if (!video || video.videoWidth === 0 || video.videoHeight === 0) {
      setResolutionProofError('Camera preview is not ready yet.');
      return;
    }

    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const context = canvas.getContext('2d');

    if (!context) {
      setResolutionProofError('Unable to capture the photo.');
      return;
    }

    context.drawImage(video, 0, 0, canvas.width, canvas.height);
    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/png'));

    if (!blob) {
      setResolutionProofError('Unable to save the captured photo.');
      return;
    }

    const file = new File([blob], `resolution-photo-${Date.now()}.png`, { type: 'image/png' });
    setResolutionProofFile(file);
    setResolutionProofPreview(URL.createObjectURL(file));
    closeResolutionCamera();
  };

  const handleCancelResolutionProof = () => {
    closeResolutionCamera();
    setResolutionProofIssue(null);
    setResolutionProofFile(null);
    setResolutionProofPreview('');
    setResolutionProofError('');
  };

  const handleSubmitResolutionProof = async () => {
    if (!resolutionProofIssue || !resolutionProofFile) {
      setResolutionProofError('Add a solved issue photo before marking this issue as resolved.');
      return;
    }

    await updateIssueStatus(resolutionProofIssue, 'RESOLVED', resolutionProofFile);
    handleCancelResolutionProof();
  };

  const handleSeverityChange = async (issue: IssueSummary, nextSeverity: AdminSeverity) => {
    if (!accessToken || nextSeverity === issue.severity) {
      return;
    }

    setUpdatingIssueId(issue.issueId);
    setError(null);

    try {
      const severityResponse = await updateAdminIssueSeverity(issue.issueId, nextSeverity, accessToken);

      setIssues((currentIssues) =>
        currentIssues.map((currentIssue) =>
          currentIssue.issueId === issue.issueId
            ? {
                ...currentIssue,
                severity: severityResponse.effectiveSeverity,
                priorityScore: severityResponse.priorityScore,
              }
            : currentIssue,
        ),
      );

      if (selectedIssue?.issueId === issue.issueId) {
        setSelectedIssue({
          ...selectedIssue,
          severity: severityResponse.effectiveSeverity,
          priorityScore: severityResponse.priorityScore,
        });
      }
    } catch (caughtError) {
      setError(
        caughtError instanceof ApiError ? caughtError.message : 'Unable to update issue severity.',
      );
    } finally {
      setUpdatingIssueId(null);
    }
  };

  const handleDeleteIssue = async (issue: IssueSummary) => {
    if (!accessToken) {
      return;
    }

    const shouldDelete = window.confirm(`Delete "${issue.title}"? This removes the issue and its related records.`);
    if (!shouldDelete) {
      return;
    }

    setDeletingIssueId(issue.issueId);
    setError(null);

    try {
      await deleteAdminIssue(issue.issueId, accessToken);
      setIssues((currentIssues) => currentIssues.filter((currentIssue) => currentIssue.issueId !== issue.issueId));

      if (selectedIssue?.issueId === issue.issueId) {
        setSelectedIssue(null);
      }
    } catch (caughtError) {
      setError(caughtError instanceof ApiError ? caughtError.message : 'Unable to delete issue.');
    } finally {
      setDeletingIssueId(null);
    }
  };

  return (
    <PageSection
      title={t('admin.title')}
      description={t('admin.description')}
    >
      <div className="admin-dashboard">
        <section className="admin-summary-grid">
          <div className="admin-summary-tile">
            <span>{t('admin.totalPrimary')}</span>
            <strong>{issues.length}</strong>
          </div>
          <div className="admin-summary-tile">
            <span>{t('admin.open')}</span>
            <strong>{dashboardStats.openCount}</strong>
          </div>
          <div className="admin-summary-tile">
            <span>{t('common.active')}</span>
            <strong>{dashboardStats.activeCount}</strong>
          </div>
          <div className="admin-summary-tile">
            <span>{t('admin.closed')}</span>
            <strong>{dashboardStats.closedCount}</strong>
          </div>
        </section>

        <section className={`admin-management-layout ${shouldShowDetailsPanel ? 'details-open' : 'details-hidden'}`}>
          <div className="admin-issue-management">
            <div className="issues-toolbar">
              <div className="issues-filters">
                <label className="form-field">
                  {t('common.category')}
                  <select value={category} onChange={(event) => setCategory(event.target.value)}>
                    <option value="">{t('issues.allCategories')}</option>
                    {CATEGORY_OPTIONS.map((option) => (
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
                    {STATUS_FILTER_OPTIONS.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.value === 'REPORTED' ? t('admin.open') : t(`status.${option.value}`)}
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
                      <option key={option.value} value={option.value}>
                        {t(`sort.${option.value}`)}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
            </div>

            <div className="issues-results-bar">
              <p>{isLoading ? t('admin.loadingList') : t('admin.primaryShown', { count: issues.length, plural: issues.length === 1 ? '' : 's' })}</p>
            </div>

            {error ? <div className="form-alert error">{error}</div> : null}

            {resolutionProofIssue ? (
              <div className="resolution-proof-panel">
                <div className="detail-panel-header">
                  <div>
                    <h3>Resolution photo required</h3>
                    <span>{resolutionProofIssue.title}</span>
                  </div>
                </div>

                <p className="detail-muted">
                  Add a photo from the gallery or take a live photo before marking this issue as resolved.
                </p>

                {resolutionProofPreview ? (
                  <div className="resolution-proof-preview">
                    <img src={resolutionProofPreview} alt="Solved issue preview" />
                  </div>
                ) : null}

                {isResolutionCameraOpen ? (
                  <div className="camera-preview">
                    <video ref={resolutionVideoRef} playsInline muted />
                  </div>
                ) : null}

                <div className="camera-actions">
                  <label className="secondary-button resolution-file-button">
                    Choose photo
                    <input
                      type="file"
                      accept={ACCEPTED_RESOLUTION_IMAGE_TYPES.join(',')}
                      onChange={handleResolutionProofFileChange}
                    />
                  </label>

                  {!isResolutionCameraOpen ? (
                    <button type="button" className="secondary-button" onClick={() => void handleOpenResolutionCamera()}>
                      Take live photo
                    </button>
                  ) : (
                    <>
                      <button type="button" className="primary-button" onClick={() => void handleCaptureResolutionPhoto()}>
                        Capture photo
                      </button>
                      <button type="button" className="secondary-button" onClick={closeResolutionCamera}>
                        Close camera
                      </button>
                    </>
                  )}
                </div>

                {resolutionProofError ? <div className="form-alert error">{resolutionProofError}</div> : null}

                <div className="camera-actions">
                  <button
                    type="button"
                    className="primary-button"
                    disabled={!resolutionProofFile || updatingIssueId === resolutionProofIssue.issueId}
                    onClick={() => void handleSubmitResolutionProof()}
                  >
                    {updatingIssueId === resolutionProofIssue.issueId ? 'Resolving...' : 'Mark as resolved'}
                  </button>
                  <button type="button" className="secondary-button" onClick={handleCancelResolutionProof}>
                    Cancel
                  </button>
                </div>
              </div>
            ) : null}

            <div className="admin-issue-table">
              <div className="admin-issue-row admin-issue-header">
                <span>{t('admin.issue')}</span>
                <span>{t('common.zone')}</span>
                <span>{t('common.status')}</span>
                <span>Severity</span>
                <span>{t('common.priority')}</span>
                <span>{t('common.actions')}</span>
              </div>

              {!isLoading && !error && issues.length === 0 ? (
                <div className="issues-empty-state">
                  <h2>{t('admin.noPrimary')}</h2>
                  <p>{t('admin.noPrimaryCopy')}</p>
                </div>
              ) : null}

              {issues.map((issue) => (
                <article key={issue.issueId} className="admin-issue-row">
                  <div className="admin-issue-title-cell">
                    <p className="issue-category">{issue.category}</p>
                    <h2>{issue.title}</h2>
                    <span>{issue.locationText}</span>
                  </div>

                  <span className="issue-status-badge status-confirmed">
                    {formatZone(issue.zone, t)}
                  </span>

                  <label className="admin-status-select">
                    <span className={`issue-status-badge status-${issue.status.toLowerCase()}`}>
                      {t(`status.${issue.status}`)}
                    </span>
                    <select
                      value={toManageableStatus(issue.status)}
                      disabled={updatingIssueId === issue.issueId}
                      onChange={(event: ChangeEvent<HTMLSelectElement>) =>
                        void handleStatusChange(issue, event.target.value as AdminManageableStatus)
                      }
                    >
                      {STATUS_UPDATE_OPTIONS.map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.value === 'REPORTED' ? t('admin.open') : t(`status.${option.value}`)}
                        </option>
                      ))}
                    </select>
                  </label>

                  <label className="admin-status-select">
                    <span>{t(`common.${issue.severity.toLowerCase()}`)}</span>
                    <select
                      value={issue.severity as AdminSeverity}
                      disabled={updatingIssueId === issue.issueId}
                      onChange={(event: ChangeEvent<HTMLSelectElement>) =>
                        void handleSeverityChange(issue, event.target.value as AdminSeverity)
                      }
                    >
                      {SEVERITY_OPTIONS.map((option) => (
                        <option key={option.value} value={option.value}>
                          {t(`common.${option.value.toLowerCase()}`)}
                        </option>
                      ))}
                    </select>
                  </label>

                  <div className="admin-priority-cell">
                    <strong>{formatPriority(issue.priorityScore)}</strong>
                    <span>{priorityLabel(issue.priorityScore)}</span>
                  </div>

                  <div className="admin-row-actions">
                    <button
                      type="button"
                      className="secondary-button"
                      onClick={(event) => void handleOpenDetails(issue.issueId, event)}
                      disabled={isLoadingDetails}
                    >
                      {t('admin.openDetails')}
                    </button>
                    <button
                      type="button"
                      className="danger-button"
                      onClick={() => void handleDeleteIssue(issue)}
                      disabled={deletingIssueId === issue.issueId}
                    >
                      {deletingIssueId === issue.issueId ? t('admin.deleting') : t('admin.delete')}
                    </button>
                  </div>
                </article>
              ))}
            </div>
          </div>

          {shouldShowDetailsPanel ? (
            <aside className="admin-details-panel" ref={detailsPanelRef}>
              <div className="detail-panel-header">
                <h3>{t('admin.issueDetails')}</h3>
                {selectedIssue ? <span>{formatDate(selectedIssue.createdAt)}</span> : null}
              </div>

              {isLoadingDetails ? <div className="route-status">{t('admin.loadingDetails')}</div> : null}
              {detailsError ? <div className="form-alert error">{detailsError}</div> : null}

              {!isLoadingDetails && selectedIssue ? (
                <div className="admin-selected-issue">
                  <div>
                    <p className="issue-category">{selectedIssue.category}</p>
                    <h2>{selectedIssue.title}</h2>
                    <p>{selectedIssue.description}</p>
                  </div>

                  <div className="issue-meta-grid">
                    <div>
                      <span className="issue-meta-label">Severity</span>
                      <strong>{t(`common.${selectedIssue.severity.toLowerCase()}`)}</strong>
                    </div>
                    <div>
                      <span className="issue-meta-label">{t('common.votes')}</span>
                      <strong>{selectedIssue.voteCount}</strong>
                    </div>
                    <div>
                      <span className="issue-meta-label">{t('common.priority')}</span>
                      <strong>{formatPriority(selectedIssue.priorityScore)}</strong>
                    </div>
                    <div>
                      <span className="issue-meta-label">{t('common.zone')}</span>
                      <strong>{formatZone(selectedIssue.zone, t)}</strong>
                    </div>
                    <div>
                      <span className="issue-meta-label">{t('admin.duplicates')}</span>
                      <strong>{selectedIssue.linkedDuplicateCount}</strong>
                    </div>
                  </div>

                  <div className="detail-stack">
                    <p><strong>{t('common.status')}:</strong> {t(`status.${selectedIssue.status}`)}</p>
                    <p><strong>{t('common.location')}:</strong> {selectedIssue.locationText}</p>
                    <p><strong>Reporter:</strong> {selectedIssue.reporterName}</p>
                  </div>

                  <div>
                    <div className="detail-panel-header">
                      <h3>Reported evidence</h3>
                      <span>{selectedIssue.images.length} attached</span>
                    </div>

                    {selectedIssue.images.length > 0 ? (
                      <div className="admin-image-review-grid">
                        {selectedIssue.images.map((image) => (
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
                      <p className="detail-muted">No images are attached to this issue.</p>
                    )}
                  </div>

                  <div>
                    <div className="detail-panel-header">
                      <h3>Resolution evidence</h3>
                      <span>{selectedIssue.resolutionImages.length} attached</span>
                    </div>

                    {selectedIssue.resolutionImages.length > 0 ? (
                      <div className="admin-image-review-grid">
                        {selectedIssue.resolutionImages.map((image) => (
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
                    ) : (
                      <p className="detail-muted">No resolution photo has been added yet.</p>
                    )}
                  </div>
                </div>
              ) : null}
            </aside>
          ) : null}
        </section>
      </div>
    </PageSection>
  );
}

function toManageableStatus(status: IssueStatus): AdminManageableStatus {
  if (status === 'UNDER_REVIEW') {
    return 'REPORTED';
  }

  return status;
}

function formatStatus(status: string) {
  return status
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

function formatPriority(priorityScore: number | null) {
  return priorityScore === null ? 'Pending' : priorityScore.toFixed(2);
}

function priorityLabel(priorityScore: number | null) {
  if (priorityScore === null) {
    return 'Not scored';
  }

  if (priorityScore >= 80) {
    return 'Urgent';
  }

  if (priorityScore >= 50) {
    return 'High';
  }

  if (priorityScore >= 25) {
    return 'Medium';
  }

  return 'Low';
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(new Date(value));
}

function formatZone(zone: IssueSummary['zone'], t: (key: string) => string) {
  return zone ? t(`zone.${zone}`) : t('common.notApplicable');
}
