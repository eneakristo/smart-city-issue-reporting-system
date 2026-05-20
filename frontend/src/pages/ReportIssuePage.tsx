import { ChangeEvent, FormEvent, useEffect, useMemo, useRef, useState } from 'react';
import { PageSection } from '../components/common/PageSection';
import { useAppContext } from '../hooks/useAppContext';
import { useLanguage } from '../hooks/useLanguage';
import { ApiError } from '../services/api/apiClient';
import { createIssue } from '../services/api/issueService';
import { CreateIssuePayload, CreateIssueResponse } from '../types/app';

const ACCEPTED_IMAGE_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
const MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;
const LOCATION_SEARCH_MIN_LENGTH = 3;
const LOCATION_SEARCH_ENDPOINT = 'https://nominatim.openstreetmap.org/search';
const CATEGORY_OPTIONS = ['', 'Roads', 'Street Lighting', 'Waste Management', 'Water Supply', 'Public Safety', 'Parks and Green Areas', 'Other'];

const INITIAL_FORM: CreateIssuePayload = {
  title: '',
  description: '',
  category: '',
  severity: 'MEDIUM',
  locationText: '',
  latitude: null,
  longitude: null,
};

type LocationSuggestion = {
  id: string;
  label: string;
  latitude: number;
  longitude: number;
};

export function ReportIssuePage() {
  const { accessToken } = useAppContext();
  const { t } = useLanguage();
  const [formValues, setFormValues] = useState<CreateIssuePayload>(INITIAL_FORM);
  const [images, setImages] = useState<File[]>([]);
  const [isCameraOpen, setIsCameraOpen] = useState(false);
  const [cameraError, setCameraError] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [apiError, setApiError] = useState('');
  const [success, setSuccess] = useState<CreateIssueResponse | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isGettingLocation, setIsGettingLocation] = useState(false);
  const [locationSuggestions, setLocationSuggestions] = useState<LocationSuggestion[]>([]);
  const [isSearchingLocation, setIsSearchingLocation] = useState(false);
  const [locationSearchError, setLocationSearchError] = useState('');
  const [hasSelectedSuggestedLocation, setHasSelectedSuggestedLocation] = useState(false);
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const cameraStreamRef = useRef<MediaStream | null>(null);
  const statusMessage = isSubmitting
    ? t('report.statusSubmitting')
    : success
      ? t('report.created', { id: success.issueId, count: success.images.length })
      : apiError;

  const helperCopy = useMemo(
    () => [
      t('report.rule1'),
      t('report.rule2'),
      t('report.rule3'),
    ],
    [t],
  );

  const handleChange =
    (field: keyof CreateIssuePayload) =>
    (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
      const value = event.target.value;

      setFormValues((current) => ({
        ...current,
        [field]: value,
        ...(field === 'locationText' ? { latitude: null, longitude: null } : {}),
      }));

      if (field === 'locationText') {
        setHasSelectedSuggestedLocation(false);
        setLocationSearchError('');
      }
    };

  const handleImageChange = (event: ChangeEvent<HTMLInputElement>) => {
    const selectedFiles = Array.from(event.target.files ?? []);
    setImages((currentImages) => [...currentImages, ...selectedFiles]);
  };

  const handleCoordinateChange =
    (field: 'latitude' | 'longitude') =>
    (event: ChangeEvent<HTMLInputElement>) => {
      const value = event.target.value;

      setFormValues((current) => ({
        ...current,
        [field]: value === '' ? null : Number(value),
      }));
      setHasSelectedSuggestedLocation(false);
      setLocationSuggestions([]);
    };

  const handleOpenCamera = async () => {
    if (!navigator.mediaDevices?.getUserMedia) {
      setCameraError('Camera access is not supported in this browser.');
      return;
    }

    setCameraError('');

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: {
          facingMode: { ideal: 'environment' },
        },
        audio: false,
      });

      cameraStreamRef.current = stream;
      setIsCameraOpen(true);

      window.setTimeout(() => {
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          void videoRef.current.play();
        }
      }, 0);
    } catch {
      setCameraError('Camera access was denied or the camera is unavailable.');
    }
  };

  const handleCloseCamera = () => {
    cameraStreamRef.current?.getTracks().forEach((track) => track.stop());
    cameraStreamRef.current = null;

    if (videoRef.current) {
      videoRef.current.srcObject = null;
    }

    setIsCameraOpen(false);
  };

  const handleCapturePhoto = async () => {
    const video = videoRef.current;

    if (!video || video.videoWidth === 0 || video.videoHeight === 0) {
      setCameraError('Camera preview is not ready yet.');
      return;
    }

    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;

    const context = canvas.getContext('2d');
    if (!context) {
      setCameraError('Unable to capture the photo.');
      return;
    }

    context.drawImage(video, 0, 0, canvas.width, canvas.height);

    const blob = await new Promise<Blob | null>((resolve) => {
      canvas.toBlob(resolve, 'image/png');
    });

    if (!blob) {
      setCameraError('Unable to save the captured photo.');
      return;
    }

    const photo = new File([blob], `camera-photo-${Date.now()}.png`, { type: 'image/png' });
    setImages((currentImages) => [...currentImages, photo]);
    setCameraError('');
    handleCloseCamera();
  };

  const handleUseCurrentLocation = () => {
    if (!navigator.geolocation) {
      setApiError('Geolocation is not supported in this browser.');
      return;
    }

    setApiError('');
    setIsGettingLocation(true);

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const latitude = Number(position.coords.latitude.toFixed(6));
        const longitude = Number(position.coords.longitude.toFixed(6));

        setFormValues((current) => ({
          ...current,
          latitude,
          longitude,
          locationText:
            current.locationText.trim() ||
            `Current location (${latitude.toFixed(6)}, ${longitude.toFixed(6)})`,
        }));
        setHasSelectedSuggestedLocation(false);
        setLocationSuggestions([]);
        setIsGettingLocation(false);
      },
      (error) => {
        const message = (() => {
          switch (error.code) {
            case error.PERMISSION_DENIED:
              return 'Location access was denied. Allow browser location permission and try again.';
            case error.POSITION_UNAVAILABLE:
              return 'Your location could not be determined right now.';
            case error.TIMEOUT:
              return 'Location request timed out. Try again.';
            default:
              return 'Unable to get your current location.';
          }
        })();

        setApiError(message);
        setIsGettingLocation(false);
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0,
      },
    );
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const validationErrors = validateForm(formValues, images);
    setErrors(validationErrors);
    setApiError('');
    setSuccess(null);

    if (Object.keys(validationErrors).length > 0) {
      return;
    }

    if (!accessToken) {
      setApiError('You must be logged in to submit an issue.');
      return;
    }

    try {
      setIsSubmitting(true);
      const response = await createIssue(formValues, images, accessToken);
      setSuccess(response);
      setFormValues(INITIAL_FORM);
      setImages([]);
      handleCloseCamera();
      setErrors({});
      setLocationSuggestions([]);
      setLocationSearchError('');
      setHasSelectedSuggestedLocation(false);
    } catch (error) {
      setApiError(error instanceof ApiError ? error.message : 'Issue submission failed. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  useEffect(() => {
    const query = formValues.locationText.trim();

    if (hasSelectedSuggestedLocation || query.length < LOCATION_SEARCH_MIN_LENGTH) {
      setLocationSuggestions([]);
      setIsSearchingLocation(false);
      setLocationSearchError('');
      return;
    }

    const controller = new AbortController();
    const timeoutId = window.setTimeout(async () => {
      setIsSearchingLocation(true);
      setLocationSearchError('');

      try {
        const url = new URL(LOCATION_SEARCH_ENDPOINT);
        url.searchParams.set('format', 'jsonv2');
        url.searchParams.set('limit', '5');
        url.searchParams.set('countrycodes', 'al');
        url.searchParams.set('q', `${query}, Tirana, Albania`);

        const response = await fetch(url.toString(), {
          signal: controller.signal,
          headers: {
            Accept: 'application/json',
          },
        });

        if (!response.ok) {
          throw new Error(`Search failed with status ${response.status}`);
        }

        const data = (await response.json()) as Array<{
          place_id: number;
          display_name: string;
          lat: string;
          lon: string;
        }>;

        setLocationSuggestions(
          data.map((item) => ({
            id: String(item.place_id),
            label: item.display_name,
            latitude: Number(item.lat),
            longitude: Number(item.lon),
          })),
        );
      } catch (error) {
        if (controller.signal.aborted) {
          return;
        }

        setLocationSuggestions([]);
        setLocationSearchError('Location suggestions are unavailable right now. You can still type the location manually.');
      } finally {
        if (!controller.signal.aborted) {
          setIsSearchingLocation(false);
        }
      }
    }, 350);

    return () => {
      controller.abort();
      window.clearTimeout(timeoutId);
    };
  }, [formValues.locationText, hasSelectedSuggestedLocation]);

  useEffect(() => {
    return () => {
      cameraStreamRef.current?.getTracks().forEach((track) => track.stop());
    };
  }, []);

  const handleSelectLocationSuggestion = (suggestion: LocationSuggestion) => {
    setFormValues((current) => ({
      ...current,
      locationText: suggestion.label,
      latitude: Number(suggestion.latitude.toFixed(6)),
      longitude: Number(suggestion.longitude.toFixed(6)),
    }));
    setLocationSuggestions([]);
    setLocationSearchError('');
    setHasSelectedSuggestedLocation(true);
  };

  return (
    <PageSection
      title={t('report.title')}
      description=""
    >
      <div className="report-layout">
        <form className="report-form" onSubmit={handleSubmit} noValidate>
          <label className="form-field">
            <span>{t('report.issueTitle')}</span>
            <input value={formValues.title} onChange={handleChange('title')} placeholder={t('report.titlePlaceholder')} />
            {errors.title ? <small className="form-error">{errors.title}</small> : null}
          </label>

          <label className="form-field">
            <span>{t('report.description')}</span>
            <textarea
              value={formValues.description}
              onChange={handleChange('description')}
              rows={5}
              placeholder={t('report.descriptionPlaceholder')}
            />
            {errors.description ? <small className="form-error">{errors.description}</small> : null}
          </label>

          <div className="report-form-grid">
            <label className="form-field">
              <span>{t('common.category')}</span>
              <select value={formValues.category} onChange={handleChange('category')}>
                {CATEGORY_OPTIONS.map((option) => (
                  <option key={option || 'empty'} value={option}>
                    {option || t('report.selectCategory')}
                  </option>
                ))}
              </select>
              {errors.category ? <small className="form-error">{errors.category}</small> : null}
            </label>

            <label className="form-field">
              <span>Severity</span>
              <select value={formValues.severity} onChange={handleChange('severity')}>
                <option value="LOW">{t('common.low')}</option>
                <option value="MEDIUM">{t('common.medium')}</option>
                <option value="HIGH">{t('common.high')}</option>
              </select>
              {errors.severity ? <small className="form-error">{errors.severity}</small> : null}
            </label>
          </div>

          <label className="form-field">
            <span>{t('common.location')}</span>
            <input
              value={formValues.locationText}
              onChange={handleChange('locationText')}
              placeholder={t('report.locationPlaceholder')}
            />
            <small className="form-hint">
              {t('report.locationHint')}
            </small>
            {errors.locationText ? <small className="form-error">{errors.locationText}</small> : null}
            {isSearchingLocation ? <small className="form-hint">{t('report.searchingLocations')}</small> : null}
            {locationSearchError ? <small className="form-error">{locationSearchError}</small> : null}
          </label>

          <div className="report-form-grid">
            <label className="form-field">
              <span>{t('report.latitude')}</span>
              <input
                type="number"
                step="0.000001"
                min="-90"
                max="90"
                value={formValues.latitude ?? ''}
                onChange={handleCoordinateChange('latitude')}
                placeholder="41.316000"
              />
              {errors.latitude ? <small className="form-error">{errors.latitude}</small> : null}
            </label>

            <label className="form-field">
              <span>{t('report.longitude')}</span>
              <input
                type="number"
                step="0.000001"
                min="-180"
                max="180"
                value={formValues.longitude ?? ''}
                onChange={handleCoordinateChange('longitude')}
                placeholder="19.700000"
              />
              {errors.longitude ? <small className="form-error">{errors.longitude}</small> : null}
            </label>
          </div>

          {locationSuggestions.length > 0 ? (
            <div className="location-suggestion-panel">
              <div className="detail-panel-header">
                <h3>{t('report.suggestedLocations')}</h3>
                <span>{t('report.selectLocationHint')}</span>
              </div>

              <div className="location-suggestion-list">
                {locationSuggestions.map((suggestion) => (
                  <button
                    key={suggestion.id}
                    type="button"
                    className="location-suggestion-item"
                    onClick={() => handleSelectLocationSuggestion(suggestion)}
                  >
                    <strong>{suggestion.label.split(',')[0]}</strong>
                    <span>{suggestion.label}</span>
                  </button>
                ))}
              </div>
            </div>
          ) : null}

          <div className="location-tools">
            <button
              type="button"
              className="secondary-button"
              onClick={handleUseCurrentLocation}
              disabled={isGettingLocation}
            >
              {isGettingLocation ? t('report.gettingLocation') : t('report.getLocation')}
            </button>

            <p className="location-help">
              {t('report.locationHelp')}
            </p>
          </div>

          {formValues.latitude !== null && formValues.longitude !== null ? (
            <>
              <div className="location-coordinates">
                <div>
                  <span className="issue-meta-label">{t('report.latitude')}</span>
                  <strong>{formValues.latitude.toFixed(6)}</strong>
                </div>
                <div>
                  <span className="issue-meta-label">{t('report.longitude')}</span>
                  <strong>{formValues.longitude.toFixed(6)}</strong>
                </div>
              </div>

              <div className="location-map-card">
                <div className="detail-panel-header">
                  <h3>{t('report.locationPreview')}</h3>
                  <span>{hasSelectedSuggestedLocation ? t('report.selectedFromSearch') : 'OpenStreetMap'}</span>
                </div>

                <div className="location-map-frame">
                  <iframe
                    title="Selected issue location"
                    src={buildMapEmbedUrl(formValues.latitude, formValues.longitude)}
                    loading="lazy"
                    referrerPolicy="no-referrer-when-downgrade"
                  />
                </div>
              </div>
            </>
          ) : null}

          <label className="form-field">
            <span>{t('common.images')}</span>
            <input type="file" accept={ACCEPTED_IMAGE_TYPES.join(',')} multiple onChange={handleImageChange} />
            {errors.images ? <small className="form-error">{errors.images}</small> : null}
          </label>

          <div className="camera-capture-card">
            <div className="detail-panel-header">
              <h3>{t('report.cameraPhoto')}</h3>
              <span>{isCameraOpen ? t('report.cameraActive') : t('common.optional')}</span>
            </div>

            {isCameraOpen ? (
              <div className="camera-preview">
                <video ref={videoRef} playsInline muted />
              </div>
            ) : null}

            {cameraError ? <small className="form-error">{cameraError}</small> : null}

            <div className="camera-actions">
              {!isCameraOpen ? (
                <button type="button" className="secondary-button" onClick={() => void handleOpenCamera()}>
                  {t('report.takePhoto')}
                </button>
              ) : (
                <>
                  <button type="button" className="primary-button" onClick={() => void handleCapturePhoto()}>
                    {t('report.capturePhoto')}
                  </button>
                  <button type="button" className="secondary-button" onClick={handleCloseCamera}>
                    {t('report.closeCamera')}
                  </button>
                </>
              )}
            </div>
          </div>

          {images.length > 0 ? (
            <div className="upload-list">
              {images.map((image) => (
                <div className="upload-item" key={`${image.name}-${image.lastModified}`}>
                  <strong>{image.name}</strong>
                  <span>{formatFileSize(image.size)}</span>
                </div>
              ))}
            </div>
          ) : null}

          {statusMessage ? (
            <div
              className={`form-alert ${apiError ? 'error' : isSubmitting || success ? 'success' : ''}`}
              role="status"
              aria-live="polite"
            >
              {statusMessage}
            </div>
          ) : null}

          <button className="primary-button" type="submit" disabled={isSubmitting}>
            {isSubmitting ? t('report.submitting') : t('report.submit')}
          </button>
        </form>

        <aside className="panel report-side-panel">
          <h2>{t('report.uploadRules')}</h2>
          <ul className="panel-list">
            {helperCopy.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </aside>
      </div>
    </PageSection>
  );
}

function validateForm(values: CreateIssuePayload, images: File[]) {
  const errors: Record<string, string> = {};

  if (!values.title.trim()) {
    errors.title = 'Title is required.';
  }

  if (!values.description.trim()) {
    errors.description = 'Description is required.';
  }

  if (!values.category.trim()) {
    errors.category = 'Category is required.';
  }

  if (!values.locationText.trim()) {
    errors.locationText = 'Location is required.';
  }

  if (
    values.latitude !== null
    && (!Number.isFinite(values.latitude) || values.latitude < -90 || values.latitude > 90)
  ) {
    errors.latitude = 'Latitude must be between -90 and 90.';
  }

  if (
    values.longitude !== null
    && (!Number.isFinite(values.longitude) || values.longitude < -180 || values.longitude > 180)
  ) {
    errors.longitude = 'Longitude must be between -180 and 180.';
  }

  if (images.length === 0) {
    errors.images = 'At least one image is required.';
  } else if (images.some((image) => !ACCEPTED_IMAGE_TYPES.includes(image.type))) {
    errors.images = 'Only JPEG, PNG, and WEBP images are allowed.';
  } else if (images.some((image) => image.size > MAX_IMAGE_SIZE_BYTES)) {
    errors.images = 'Each image must be 5 MB or smaller.';
  }

  return errors;
}

function formatFileSize(size: number) {
  return `${(size / (1024 * 1024)).toFixed(2)} MB`;
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
