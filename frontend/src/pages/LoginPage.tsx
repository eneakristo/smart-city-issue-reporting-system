import { FormEvent, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { AuthFormShell } from '../components/auth/AuthFormShell';
import { useAppContext } from '../hooks/useAppContext';
import { useLanguage } from '../hooks/useLanguage';
import { ApiError } from '../services/api/apiClient';
import { validateLogin } from '../utils/authValidation';

export function LoginPage() {
  const { login } = useAppContext();
  const { t } = useLanguage();
  const navigate = useNavigate();
  const location = useLocation();
  const redirectTo = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname ?? '/';

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [apiError, setApiError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const validationErrors = validateLogin({ email, password }, t);
    setErrors(validationErrors);
    setApiError('');

    if (Object.keys(validationErrors).length > 0) {
      return;
    }

    try {
      setIsSubmitting(true);
      await login({ email: email.trim(), password });
      navigate(redirectTo, { replace: true });
    } catch (error) {
      setApiError(error instanceof ApiError ? error.message : t('auth.login.failed'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AuthFormShell
      title={t('auth.login.title')}
      description={t('auth.login.description')}
      footer={
        <p className="login-register-prompt">
          {t('auth.login.noAccount')} <Link to="/register">{t('auth.login.createOne')}</Link>
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

        <label className="form-field">
          <span className="form-label-row">
            {t('common.password')} <Link to="/forgot-password">{t('auth.login.forgot')}</Link>
          </span>
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder="Enter your password"
          />
          {errors.password ? <small className="form-error">{errors.password}</small> : null}
        </label>

        {apiError ? <div className="form-alert error">{apiError}</div> : null}

        <button className="primary-button" type="submit" disabled={isSubmitting}>
          {isSubmitting ? t('auth.login.submitting') : t('auth.login.submit')}
        </button>
      </form>
    </AuthFormShell>
  );
}
