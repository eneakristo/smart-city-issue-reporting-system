import { ChangeEvent, FormEvent, useEffect, useState } from 'react';
import { PageSection } from '../components/common/PageSection';
import { useAppContext } from '../hooks/useAppContext';
import { useLanguage } from '../hooks/useLanguage';
import { ApiError } from '../services/api/apiClient';
import { changePassword, updateProfile } from '../services/api/authService';

const MAX_PROFILE_IMAGE_BYTES = 1_000_000;

export function ProfilePage() {
  const { accessToken, currentUser, updateCurrentUser } = useAppContext();
  const { t } = useLanguage();
  const [fullName, setFullName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [bio, setBio] = useState('');
  const [profilePictureUrl, setProfilePictureUrl] = useState('');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [profileError, setProfileError] = useState('');
  const [profileSuccess, setProfileSuccess] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [passwordSuccess, setPasswordSuccess] = useState('');
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [isChangingPassword, setIsChangingPassword] = useState(false);

  useEffect(() => {
    if (!currentUser) {
      return;
    }

    setFullName(currentUser.fullName);
    setPhoneNumber(currentUser.phoneNumber ?? '');
    setBio(currentUser.bio ?? '');
    setProfilePictureUrl(currentUser.profilePictureUrl ?? '');
  }, [currentUser]);

  const handleProfileImageChange = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];

    if (!file) {
      return;
    }

    setProfileError('');
    setProfileSuccess('');

    if (!file.type.startsWith('image/')) {
      setProfileError(t('profile.imageTypeError'));
      return;
    }

    if (file.size > MAX_PROFILE_IMAGE_BYTES) {
      setProfileError(t('profile.imageSizeError'));
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        setProfilePictureUrl(reader.result);
      }
    };
    reader.readAsDataURL(file);
  };

  const handleSaveProfile = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!accessToken) {
      return;
    }

    setProfileError('');
    setProfileSuccess('');

    if (!fullName.trim()) {
      setProfileError(t('validation.fullNameRequired'));
      return;
    }

    if (phoneNumber.length > 30) {
      setProfileError('Phone number must be at most 30 characters.');
      return;
    }

    if (bio.length > 600) {
      setProfileError('Bio must be at most 600 characters.');
      return;
    }

    setIsSavingProfile(true);

    try {
      const updatedProfile = await updateProfile(
        {
          fullName: fullName.trim(),
          phoneNumber: phoneNumber.trim(),
          bio: bio.trim(),
          profilePictureUrl,
        },
        accessToken,
      );
      updateCurrentUser(updatedProfile);
      setProfileSuccess(t('profile.updated'));
    } catch (error) {
      setProfileError(error instanceof ApiError ? error.message : 'Unable to update profile.');
    } finally {
      setIsSavingProfile(false);
    }
  };

  const handleChangePassword = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!accessToken) {
      return;
    }

    setPasswordError('');
    setPasswordSuccess('');

    if (!currentPassword) {
      setPasswordError(t('profile.currentPassword') + ' is required.');
      return;
    }

    if (newPassword.length < 8) {
      setPasswordError(t('validation.passwordMin'));
      return;
    }

    if (newPassword !== confirmPassword) {
      setPasswordError(t('validation.passwordMismatch'));
      return;
    }

    setIsChangingPassword(true);

    try {
      const response = await changePassword({ currentPassword, newPassword }, accessToken);
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setPasswordSuccess(response.message);
    } catch (error) {
      setPasswordError(error instanceof ApiError ? error.message : 'Unable to change password.');
    } finally {
      setIsChangingPassword(false);
    }
  };

  if (!currentUser) {
    return <div className="route-status">{t('profile.loading')}</div>;
  }

  return (
    <PageSection
      title={t('profile.title')}
      description={t('profile.description')}
    >
      <div className="profile-layout">
        <section className="profile-summary-panel">
          <div className="profile-picture-preview">
            {profilePictureUrl ? <img src={profilePictureUrl} alt="" /> : <span>{getInitials(fullName)}</span>}
          </div>
          <div>
            <h2>{fullName}</h2>
            <p>{currentUser.email}</p>
          </div>
          <span className="role-badge">{t(`role.${currentUser.role}`)}</span>
        </section>

        <form className="profile-form-panel" onSubmit={handleSaveProfile} noValidate>
          <div>
            <p className="issue-category">{t('profile.accountDetails')}</p>
            <h2>{t('profile.publicProfile')}</h2>
          </div>

          <label className="form-field">
            {t('common.fullName')}
            <input value={fullName} onChange={(event) => setFullName(event.target.value)} />
          </label>

          <label className="form-field">
            {t('common.email')}
            <input value={currentUser.email} disabled />
            <small className="form-hint">{t('profile.emailHint')}</small>
          </label>

          <label className="form-field">
            {t('common.phone')}
            <input
              type="tel"
              value={phoneNumber}
              onChange={(event) => setPhoneNumber(event.target.value)}
              placeholder="+355 ..."
            />
          </label>

          <label className="form-field">
            {t('common.bio')}
            <textarea
              rows={5}
              value={bio}
              onChange={(event) => setBio(event.target.value)}
              placeholder={t('profile.bioPlaceholder')}
            />
          </label>

          <label className="form-field">
            {t('profile.picture')}
            <input type="file" accept="image/*" onChange={handleProfileImageChange} />
          </label>

          {profilePictureUrl ? (
            <button className="secondary-button" type="button" onClick={() => setProfilePictureUrl('')}>
              {t('profile.removePicture')}
            </button>
          ) : null}

          {profileError ? <div className="form-alert error">{profileError}</div> : null}
          {profileSuccess ? <div className="form-alert success">{profileSuccess}</div> : null}

          <button className="primary-button" type="submit" disabled={isSavingProfile}>
            {isSavingProfile ? t('profile.saving') : t('profile.save')}
          </button>
        </form>

        <form className="profile-form-panel" onSubmit={handleChangePassword} noValidate>
          <div>
            <p className="issue-category">{t('profile.security')}</p>
            <h2>{t('profile.changePassword')}</h2>
          </div>

          <label className="form-field">
            {t('profile.currentPassword')}
            <input
              type="password"
              value={currentPassword}
              onChange={(event) => setCurrentPassword(event.target.value)}
            />
          </label>

          <label className="form-field">
            {t('profile.newPassword')}
            <input type="password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} />
          </label>

          <label className="form-field">
            {t('profile.confirmNewPassword')}
            <input
              type="password"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
            />
          </label>

          {passwordError ? <div className="form-alert error">{passwordError}</div> : null}
          {passwordSuccess ? <div className="form-alert success">{passwordSuccess}</div> : null}

          <button className="primary-button" type="submit" disabled={isChangingPassword}>
            {isChangingPassword ? t('profile.changingPassword') : t('profile.changePassword')}
          </button>
        </form>
      </div>
    </PageSection>
  );
}

function getInitials(fullName: string) {
  return fullName
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('');
}
