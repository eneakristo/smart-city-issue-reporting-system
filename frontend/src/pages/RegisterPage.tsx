import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AuthFormShell } from '../components/auth/AuthFormShell';
import { useAppContext } from '../hooks/useAppContext';
import { useLanguage } from '../hooks/useLanguage';
import { ApiError } from '../services/api/apiClient';
import { validateRegister } from '../utils/authValidation';

export function RegisterPage() {
  const { register } = useAppContext();
  const { t } = useLanguage();
  const navigate = useNavigate();

  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [apiError, setApiError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const validationErrors = validateRegister({
      fullName,
      email,
      password,
      confirmPassword,
    }, t);

    setErrors(validationErrors);
    setApiError('');

    if (Object.keys(validationErrors).length > 0) {
      return;
    }

    try {
      setIsSubmitting(true);
      await register({ fullName: fullName.trim(), email: email.trim(), password });
      navigate('/login', { replace: true });
    } catch (error) {
      setApiError(error instanceof ApiError ? error.message : 'Registration failed. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AuthFormShell
      title={t('auth.register.title')}
      description={t('auth.register.description')}
      footer={
        <p>
          {t('auth.register.hasAccount')} <Link to="/login">{t('auth.register.loginHere')}</Link>
        </p>
      }
    >
      <form className="auth-form" onSubmit={handleSubmit} noValidate>
        <label className="form-field">
          <span>{t('common.fullName')}</span>
          <input
            type="text"
            value={fullName}
            onChange={(event) => setFullName(event.target.value)}
            placeholder="Your full name"
          />
          {errors.fullName ? <small className="form-error">{errors.fullName}</small> : null}
        </label>

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
          <span>{t('common.password')}</span>
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder={t('auth.register.passwordPlaceholder')}
          />
          {errors.password ? <small className="form-error">{errors.password}</small> : null}
        </label>

        <label className="form-field">
          <span>{t('auth.register.confirmPassword')}</span>
          <input
            type="password"
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
            placeholder={t('auth.register.confirmPlaceholder')}
          />
          {errors.confirmPassword ? <small className="form-error">{errors.confirmPassword}</small> : null}
        </label>

        {apiError ? <div className="form-alert error">{apiError}</div> : null}

        <button className="primary-button" type="submit" disabled={isSubmitting}>
          {isSubmitting ? t('auth.register.submitting') : t('auth.register.submit')}
        </button>
      </form>
    </AuthFormShell>
  );
}
