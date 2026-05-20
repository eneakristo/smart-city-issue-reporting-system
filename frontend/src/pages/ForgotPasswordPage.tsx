import { FormEvent, useState } from 'react';
import { Link } from 'react-router-dom';
import { AuthFormShell } from '../components/auth/AuthFormShell';
import { useLanguage } from '../hooks/useLanguage';
import { ApiError } from '../services/api/apiClient';
import { requestPasswordReset } from '../services/api/authService';
import { validateForgotPassword } from '../utils/authValidation';

export function ForgotPasswordPage() {
  const { t } = useLanguage();
  const [email, setEmail] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [apiError, setApiError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const validationErrors = validateForgotPassword(email, t);
    setErrors(validationErrors);
    setApiError('');
    setSuccessMessage('');

    if (Object.keys(validationErrors).length > 0) {
      return;
    }

    try {
      setIsSubmitting(true);
      const response = await requestPasswordReset({ email: email.trim() });
      setSuccessMessage(response.message);
    } catch (error) {
      setApiError(error instanceof ApiError ? error.message : t('auth.forgot.failed'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AuthFormShell
      title={t('auth.forgot.title')}
      description={t('auth.forgot.description')}
      footer={
        <p>
          {t('auth.forgot.remembered')} <Link to="/login">{t('auth.forgot.backLogin')}</Link>
        </p>
      }
    >
      <form className="auth-form" onSubmit={handleSubmit} noValidate>
        <label className="form-field">
          <span>{t('common.email')}</span>
          <input
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder="you@example.com"
          />
          {errors.email ? <small className="form-error">{errors.email}</small> : null}
        </label>

        {apiError ? <div className="form-alert error">{apiError}</div> : null}
        {successMessage ? <div className="form-alert success">{successMessage}</div> : null}

        <button className="primary-button" type="submit" disabled={isSubmitting}>
          {isSubmitting ? t('auth.forgot.submitting') : t('auth.forgot.submit')}
        </button>
      </form>
    </AuthFormShell>
  );
}
