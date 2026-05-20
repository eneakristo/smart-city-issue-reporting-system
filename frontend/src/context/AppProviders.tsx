import { ReactNode } from 'react';
import { BrowserRouter } from 'react-router-dom';
import { AppContextProvider } from './AppContext';
import { LanguageProvider } from './LanguageContext';

type AppProvidersProps = {
  children: ReactNode;
};

export function AppProviders({ children }: AppProvidersProps) {
  return (
    <LanguageProvider>
      <BrowserRouter>
        <AppContextProvider>{children}</AppContextProvider>
      </BrowserRouter>
    </LanguageProvider>
  );
}
