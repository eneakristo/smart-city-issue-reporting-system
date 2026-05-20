import { Navigate, Route, Routes } from 'react-router-dom';
import { AppLayout } from '../app/AppLayout';
import { ProtectedRoute } from '../components/auth/ProtectedRoute';
import { PublicOnlyRoute } from '../components/auth/PublicOnlyRoute';
import { AboutPage } from '../pages/AboutPage';
import { AdminDashboardPage } from '../pages/AdminDashboardPage';
import { DuplicateReportsPage } from '../pages/DuplicateReportsPage';
import { ForgotPasswordPage } from '../pages/ForgotPasswordPage';
import { HomePage } from '../pages/HomePage';
import { IssueDetailsPage } from '../pages/IssueDetailsPage';
import { IssuesListPage } from '../pages/IssuesListPage';
import { LoginPage } from '../pages/LoginPage';
import { MyReportsPage } from '../pages/MyReportsPage';
import { ProfilePage } from '../pages/ProfilePage';
import { RegisterPage } from '../pages/RegisterPage';
import { ReportIssuePage } from '../pages/ReportIssuePage';
import { ResetPasswordPage } from '../pages/ResetPasswordPage';
import { SystemAdminDashboardPage } from '../pages/SystemAdminDashboardPage';

export function AppRouter() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/issues" element={<IssuesListPage />} />
        <Route path="/about" element={<AboutPage />} />
        <Route path="/issues/:issueId" element={<IssueDetailsPage />} />

        <Route element={<PublicOnlyRoute />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
        </Route>

        <Route element={<ProtectedRoute />}>
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/report-issue" element={<ReportIssuePage />} />
          <Route path="/my-reports" element={<MyReportsPage />} />
        </Route>

        <Route element={<ProtectedRoute administratorOnly />}>
          <Route path="/administrator" element={<AdminDashboardPage />} />
          <Route path="/duplicates" element={<DuplicateReportsPage />} />
        </Route>

        <Route element={<ProtectedRoute systemAdminOnly />}>
          <Route path="/admin" element={<SystemAdminDashboardPage />} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
