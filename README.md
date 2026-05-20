# Smart City Issue Reporting and Prioritization System

This repository is organized as a clean full-stack thesis project with a separate backend and frontend.

## Architecture

- `backend/`: Spring Boot application using a controller/service/repository structure
- `frontend/`: React application for the web client
- `docs/`: thesis support documents, architecture notes, API drafts, and design decisions
- `screenshots/`: UI screenshots for reports, presentations, and thesis material

## Planned Responsibilities

- Backend:
  - authentication and authorization
  - issue reporting and duplicate-report management
  - prioritization algorithm
  - database and storage integration
- Frontend:
  - authentication flows
  - citizen and admin user interfaces
  - issue browsing, reporting, and review screens

## Notes

- This initial structure intentionally avoids business feature implementation.
- The goal is to provide a clear, maintainable starting point for incremental development.

## Password Reset Email

The backend sends password reset links through the Brevo transactional email API over HTTPS, which works on Render Free because it does not use blocked SMTP ports.

Required Render environment variables:

- `BREVO_API_KEY`: API key from Brevo, not an SMTP key.
- `APP_EMAIL_FROM_ADDRESS`: verified Brevo sender email address.
- `APP_EMAIL_FROM_NAME`: sender display name, for example `Smart City Issue Reporting`.
- `APP_PASSWORD_RESET_FRONTEND_URL`: deployed frontend reset page, for example `https://your-frontend.onrender.com/reset-password`.

The backend also needs PostgreSQL connection variables before it can start:

- `SPRING_DATASOURCE_URL`: JDBC URL, for example `jdbc:postgresql://host:5432/database`.
- `SPRING_DATASOURCE_USERNAME`: database username.
- `SPRING_DATASOURCE_PASSWORD`: database password.
