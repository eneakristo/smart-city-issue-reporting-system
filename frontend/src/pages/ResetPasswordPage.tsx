import { FormEvent, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { AuthFormShell } from '../components/auth/AuthFormShell';
import { useLanguage } from '../hooks/useLanguage';
import { ApiError } from '../services/api/apiClient';
import { resetPassword } from '../services/api/authService';
import { validateResetPassword } from '../utils/authValidation';

export function ResetPasswordPage() {
  const { t } = useLanguage();
  const [searchParams] = useSearchParams();
  const token = useMemo(() => searchParams.get('token') ?? '', [searchParams]);

  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [apiError, setApiError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const validationErrors = validateResetPassword(password, confirmPassword, t);
    setErrors(validationErrors);
    setApiError('');
    setSuccessMessage('');

    if (!token) {
      setApiError(t('auth.reset.missingToken'));
      return;
    }

    if (Object.keys(validationErrors).length > 0) {
      return;
    }

    try {
      setIsSubmitting(true);
      const response = await resetPassword({ token, password });
      setSuccessMessage(response.message);
      setPassword('');
      setConfirmPassword('');
    } catch (error) {
      setApiError(error instanceof ApiError ? error.message : t('auth.reset.failed'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AuthFormShell
      title={t('auth.reset.title')}
      description={t('auth.reset.description')}
      footer={
        <p>
          {t('auth.reset.already')} <Link to="/login">{t('auth.reset.login')}</Link>
        </p>
      }
    >
      <form className="auth-form" onSubmit={handleSubmit} noValidate>
        <label className="form-field">
          <span>{t('auth.reset.newPassword')}</span>
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder="Create a new password"
          />
          {errors.password ? <small className="form-error">{errors.password}</small> : null}
        </label>

        <label className="form-field">
          <span>{t('auth.reset.confirmPassword')}</span>
          <input
            type="password"
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
            placeholder="Repeat your new password"
          />
          {errors.confirmPassword ? <small className="form-error">{errors.confirmPassword}</small> : null}
        </label>

        {apiError ? <div className="form-alert error">{apiError}</div> : null}
        {successMessage ? <div className="form-alert success">{successMessage}</div> : null}

        <button className="primary-button" type="submit" disabled={isSubmitting || Boolean(successMessage)}>
          {isSubmitting ? t('auth.reset.submitting') : t('auth.reset.submit')}
        </button>
      </form>
    </AuthFormShell>
  );
}
