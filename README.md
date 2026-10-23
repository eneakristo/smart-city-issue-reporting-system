# Smart City Issue Reporting and Prioritization System

A full-stack web application designed to help citizens report urban infrastructure problems and enable local authorities to manage, prioritize, and resolve reported issues efficiently.

The platform provides a centralized system for submitting reports, tracking their progress, and managing municipal responses. It includes an automated prioritization algorithm and duplicate-report detection to support more efficient issue management.

Developed as a Software Engineering graduation project at the University of New York Tirana.

## Technology Stack

### Frontend

* React
* JavaScript
* HTML5
* CSS3
* Vite
* React Router
* REST API integration

### Backend

* Java 17
* Spring Boot 3
* Spring Security
* Spring Data JPA
* Hibernate
* JWT Authentication
* Maven

### Database

* PostgreSQL
* Supabase

### Deployment and Development Tools

* Vercel – Frontend hosting
* Render – Backend hosting
* Git and GitHub – Version control
* Postman – API testing
* IntelliJ IDEA
* Visual Studio Code

## Key Features

### 1. User Authentication and Authorization

The application implements secure authentication and role-based access control.

Features include:

* User registration and login
* JWT-based authentication
* Password reset functionality
* Protected API endpoints
* Role-based authorization
* User account management

The platform supports three primary roles:

**Citizen**

* Submit new issue reports
* Browse reported issues
* Track issue status
* Vote on existing reports

**Administrator**

* Review and manage reported issues
* Update issue statuses
* Review potential duplicate reports
* Manage issues within assigned zones

**System Administrator**

* Manage users and administrator permissions
* Oversee platform administration
* Manage system-level access

### 2. Issue Reporting

Citizens can submit reports about urban problems such as:

* Damaged roads
* Street lighting issues
* Waste management problems
* Water infrastructure issues
* Other public infrastructure concerns

Each report can include:

* Title and description
* Issue category
* Severity level
* Geographic location
* Supporting images

### 3. Issue Management Dashboard

The application provides interfaces for browsing and managing reported issues.

Features include:

* Issue lists and detailed views
* Filtering and sorting
* Status tracking
* Administrative review
* Issue prioritization
* User interaction through voting

### 4. Automated Issue Prioritization

One of the main features of the application is its prioritization algorithm.

The algorithm evaluates reported issues using several factors:

* Reported severity
* Issue category
* Community votes
* Report recency
* Location-related risk
* Duplicate reports
* Current issue status

These factors contribute to a prioritization score that helps administrators identify issues requiring attention.

### 5. Duplicate Report Detection

The system identifies potentially duplicate reports by comparing:

* Report titles
* Geographic locations
* Issue categories
* Descriptions
* Submission times

Potential duplicates are presented for administrative review.

This reduces repeated reports and improves issue management.

### 6. Issue Status Management

Administrators can manage issues throughout their lifecycle.

The system supports status updates and tracking, allowing citizens to follow the progress of their reports.

Resolution evidence can also be attached to completed issues.

## Frontend Development

The frontend was developed using React and Vite, with an emphasis on component-based development and communication with backend services.

### Component-Based Architecture

The application organizes frontend functionality into reusable components and pages.

This approach helps improve:

* Code organization
* Maintainability
* Reusability
* Consistency across interfaces

### REST API Integration

The React frontend communicates with the Spring Boot backend through REST APIs.

API integration supports:

* User authentication
* Issue submission
* Issue retrieval
* Filtering and sorting
* Voting
* Administrative operations
* Status updates

### Authentication and Protected Routes

The frontend integrates with JWT-based authentication to manage authenticated interactions with the backend.

User permissions determine access to specific application functionality.

### User Interfaces

The frontend includes interfaces for:

* Registration and login
* Citizen reporting
* Issue browsing
* Issue details
* Administrative issue management
* User management

## Project Architecture

The repository separates frontend, backend, and supporting documentation.

```text
smart-city-issue-reporting-system/
│
├── backend/
│   └── Spring Boot REST API
│
├── frontend/
│   └── React + Vite application
│
├── docs/
│   └── Architecture and thesis documentation
│
├── screenshots/
│   └── Application screenshots
│
└── README.md
```

### Backend Architecture

The backend follows a layered architecture:

```text
Controller
    |
    v
Service
    |
    v
Repository
    |
    v
PostgreSQL Database
```

**Controller Layer**

Handles incoming HTTP requests and exposes REST API endpoints.

**Service Layer**

Contains business logic, validation, prioritization, and issue management operations.

**Repository Layer**

Handles database access using Spring Data JPA.

## Screenshots

Application screenshots are stored in the `screenshots/` directory.

Screenshots can demonstrate:

* Authentication pages
* Citizen dashboard
* Issue reporting form
* Issue listing and filtering
* Issue details
* Administrator dashboard
* Issue prioritization

## Getting Started

### Prerequisites

To run the application locally, install:

* Java 17
* Maven
* Node.js and npm
* PostgreSQL
* Git

### 1. Clone the Repository

```bash
git clone https://github.com/eneakristo/smart-city-issue-reporting-system.git

cd smart-city-issue-reporting-system
```

### 2. Configure the Backend

Navigate to the backend directory:

```bash
cd backend
```

Configure the PostgreSQL connection and required application environment variables.

The backend requires database configuration and JWT authentication settings.

Example database environment variables:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/smartcity
SPRING_DATASOURCE_USERNAME=your_username
SPRING_DATASOURCE_PASSWORD=your_password
```

The exact configuration depends on the local development environment.

Start the backend:

```bash
mvn spring-boot:run
```

### 3. Configure the Frontend

Open another terminal and navigate to the frontend directory:

```bash
cd frontend
```

Install dependencies:

```bash
npm install
```

Start the development server:

```bash
npm run dev
```

Vite will display the local frontend address in the terminal.

Ensure that the frontend API configuration points to the running backend.

## Deployment

The application uses separate hosting services for frontend and backend.

**Frontend**

* Hosted on Vercel
* Built using React and Vite

**Backend**

* Hosted on Render
* Built using Spring Boot

**Database**

* PostgreSQL hosted on Supabase

This separation allows the frontend and backend to be deployed and maintained independently.

## Password Reset Email Configuration

The backend uses the Brevo transactional email API over HTTPS for password reset functionality.

Required environment variables:

```env
BREVO_API_KEY=your_brevo_api_key
APP_EMAIL_FROM_ADDRESS=verified_sender@example.com
APP_EMAIL_FROM_NAME=Smart City Issue Reporting
APP_PASSWORD_RESET_FRONTEND_URL=https://your-domain.com/reset-password
```

The Brevo API is used instead of SMTP to support deployment environments where SMTP ports may be restricted.

### PostgreSQL Configuration

The backend also requires PostgreSQL connection settings.

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/database
SPRING_DATASOURCE_USERNAME=your_database_username
SPRING_DATASOURCE_PASSWORD=your_database_password
```

For Supabase, connection settings should be copied from the project's database connection configuration.

Do not commit credentials or API keys to the repository.

## Development Practices

The project emphasizes:

* Separation of frontend and backend responsibilities
* Component-based frontend development
* Layered backend architecture
* RESTful API communication
* Authentication and authorization
* Database integration
* Maintainable application structure
* Version control with Git

## Future Improvements

Potential improvements include:

* Expanded automated frontend testing
* Additional accessibility improvements
* Further frontend performance optimization
* Enhanced reporting analytics
* Additional administrative dashboard features

## Academic Context

This project was developed as a graduation thesis for the Bachelor's Degree in Software Engineering at the University of New York Tirana.

The project combines full-stack software development with an algorithmic approach to urban issue prioritization.


University of New York Tirana

