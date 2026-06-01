import { createContext, ReactNode, useEffect, useState } from 'react';
import {
  login as loginRequest,
  logout as clearStoredAuth,
  readStoredAuth,
  register as registerRequest,
  requestCurrentUser,
  storeAuthSession,
} from '../services/api/authService';
import { AuthSession, LoginPayload, RegisterPayload, UserProfile, UserRole } from '../types/app';

type AppContextValue = {
  currentUser: UserProfile | null;
  currentRole: UserRole | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  isLoadingAuth: boolean;
  login: (payload: LoginPayload) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<void>;
  updateCurrentUser: (profile: UserProfile) => void;
  logout: () => void;
};

export const AppContext = createContext<AppContextValue | undefined>(undefined);

type AppContextProviderProps = {
  children: ReactNode;
};

export function AppContextProvider({ children }: AppContextProviderProps) {
  const [currentUser, setCurrentUser] = useState<UserProfile | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [isLoadingAuth, setIsLoadingAuth] = useState(true);

  useEffect(() => {
    const restoreSession = async () => {
      const storedSession = readStoredAuth();

      if (!storedSession) {
        setIsLoadingAuth(false);
        return;
      }

      setAccessToken(storedSession.accessToken);
      setCurrentUser(storedSession.user);

      try {
        const profile = await requestCurrentUser(storedSession.accessToken);
        const refreshedSession: AuthSession = {
          accessToken: storedSession.accessToken,
          user: profile,
        };

        storeAuthSession(refreshedSession);
        setCurrentUser(profile);
      } catch {
        clearStoredAuth();
        setAccessToken(null);
        setCurrentUser(null);
      } finally {
        setIsLoadingAuth(false);
      }
    };

    void restoreSession();
  }, []);

  const login = async (payload: LoginPayload) => {
    const response = await loginRequest(payload);
    const session: AuthSession = {
      accessToken: response.accessToken,
      user: {
        id: response.userId,
        fullName: response.fullName,
        email: response.email,
        phoneNumber: response.phoneNumber,
        bio: response.bio,
        profilePictureUrl: response.profilePictureUrl,
        role: normalizeRole(response.role),
      },
    };

    storeAuthSession(session);
    setAccessToken(session.accessToken);
    setCurrentUser(session.user);
  };

  const register = async (payload: RegisterPayload) => {
    await registerRequest(payload);
  };

  const logout = () => {
    clearStoredAuth();
    setAccessToken(null);
    setCurrentUser(null);
  };

  const updateCurrentUser = (profile: UserProfile) => {
    if (!accessToken) {
      return;
    }

    const session: AuthSession = {
      accessToken,
      user: profile,
    };

    storeAuthSession(session);
    setCurrentUser(profile);
  };

  const value = {
    currentUser,
    currentRole: currentUser?.role ?? null,
    accessToken,
    isAuthenticated: Boolean(currentUser && accessToken),
    isLoadingAuth,
    login,
    register,
    updateCurrentUser,
    logout,
  };

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
}

function normalizeRole(role: string): UserRole {
  const normalizedRole = role.toUpperCase();

  if (normalizedRole === 'ADMINISTRATOR' || normalizedRole === 'ADMIN') {
    return 'administrator';
  }

  if (normalizedRole === 'SYSTEM_ADMIN') {
    return 'system_admin';
  }

  return 'citizen';
}
