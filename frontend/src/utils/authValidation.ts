import { AuthFormValues } from '../types/app';

type Translator = (key: string) => string;

const fallbackTranslate: Translator = (key) => {
  const messages: Record<string, string> = {
    'validation.emailRequired': 'Email is required.',
    'validation.emailInvalid': 'Enter a valid email address.',
    'validation.passwordRequired': 'Password is required.',
    'validation.fullNameRequired': 'Full name is required.',
    'validation.passwordMin': 'Password must be at least 8 characters.',
    'validation.confirmPasswordRequired': 'Please confirm your password.',
    'validation.passwordMismatch': 'Passwords do not match.',
  };

  return messages[key] ?? key;
};

export function validateLogin(values: AuthFormValues, t: Translator = fallbackTranslate) {
  const errors: Record<string, string> = {};

  if (!values.email.trim()) {
    errors.email = t('validation.emailRequired');
  } else if (!isValidEmail(values.email)) {
    errors.email = t('validation.emailInvalid');
  }

  if (!values.password) {
    errors.password = t('validation.passwordRequired');
  }

  return errors;
}

export function validateRegister(values: AuthFormValues, t: Translator = fallbackTranslate) {
  const errors = validateLogin(values, t);

  if (!values.fullName?.trim()) {
    errors.fullName = t('validation.fullNameRequired');
  }

  if (!values.password) {
    errors.password = t('validation.passwordRequired');
  } else if (values.password.length < 8) {
    errors.password = t('validation.passwordMin');
  }

  if (!values.confirmPassword) {
    errors.confirmPassword = t('validation.confirmPasswordRequired');
  } else if (values.password !== values.confirmPassword) {
    errors.confirmPassword = t('validation.passwordMismatch');
  }

  return errors;
}

export function validateForgotPassword(email: string, t: Translator = fallbackTranslate) {
  const errors: Record<string, string> = {};

  if (!email.trim()) {
    errors.email = t('validation.emailRequired');
  } else if (!isValidEmail(email)) {
    errors.email = t('validation.emailInvalid');
  }

  return errors;
}

export function validateResetPassword(password: string, confirmPassword: string, t: Translator = fallbackTranslate) {
  const errors: Record<string, string> = {};

  if (!password) {
    errors.password = t('validation.passwordRequired');
  } else if (password.length < 8) {
    errors.password = t('validation.passwordMin');
  }

  if (!confirmPassword) {
    errors.confirmPassword = t('validation.confirmPasswordRequired');
  } else if (password !== confirmPassword) {
    errors.confirmPassword = t('validation.passwordMismatch');
  }

  return errors;
}

function isValidEmail(email: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}
