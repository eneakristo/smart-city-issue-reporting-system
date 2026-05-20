import { ReactNode } from 'react';

type AuthFormShellProps = {
  title: string;
  description: string;
  footer: ReactNode;
  children: ReactNode;
};

export function AuthFormShell({ title, description, footer, children }: AuthFormShellProps) {
  return (
    <section className="auth-shell">
      <div className="auth-card">
        <p className="page-kicker">Smart City Platform</p>
        <h1>{title}</h1>
        <p className="auth-description">{description}</p>
        {children}
        <div className="auth-footer">{footer}</div>
      </div>
    </section>
  );
}
