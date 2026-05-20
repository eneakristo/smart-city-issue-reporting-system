import { createContext, ReactNode, useMemo, useState } from 'react';
import { Language, translations } from '../i18n/translations';

const LANGUAGE_STORAGE_KEY = 'smart-city-language';

type TranslationParams = Record<string, string | number>;

type LanguageContextValue = {
  language: Language;
  setLanguage: (language: Language) => void;
  t: (key: string, params?: TranslationParams) => string;
};

export const LanguageContext = createContext<LanguageContextValue | undefined>(undefined);

type LanguageProviderProps = {
  children: ReactNode;
};

export function LanguageProvider({ children }: LanguageProviderProps) {
  const [language, setLanguageState] = useState<Language>(() => {
    const storedLanguage = localStorage.getItem(LANGUAGE_STORAGE_KEY);
    return storedLanguage === 'sq' ? 'sq' : 'en';
  });

  const setLanguage = (nextLanguage: Language) => {
    localStorage.setItem(LANGUAGE_STORAGE_KEY, nextLanguage);
    setLanguageState(nextLanguage);
  };

  const value = useMemo<LanguageContextValue>(() => {
    const t = (key: string, params?: TranslationParams) => {
      const dictionary = translations[language] as Record<string, string>;
      const fallbackDictionary = translations.en as Record<string, string>;
      const template = dictionary[key] ?? fallbackDictionary[key] ?? key;

      if (!params) {
        return template;
      }

      return Object.entries(params).reduce(
        (text, [paramKey, paramValue]) => text.split(`{{${paramKey}}}`).join(String(paramValue)),
        template,
      );
    };

    return { language, setLanguage, t };
  }, [language]);

  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>;
}
