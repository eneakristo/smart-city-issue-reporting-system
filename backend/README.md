# Backend

Spring Boot backend base for the Smart City Issue Reporting and Prioritization System.

## Package Structure

- `config`: application properties and infrastructure configuration
- `controller`: REST endpoints
- `service`: business services
- `repository`: Spring Data repositories
- `model.entity`: JPA entities and shared persistence base classes
- `dto`: request and response data transfer objects
- `security`: Spring Security and JWT-related setup
- `exception`: centralized exception handling and custom exceptions

## Configuration

- `application.yml`: shared defaults
- `application-local.yml`: local development profile
- `application-prod.yml`: production profile baseline

## Issue Image Storage

Issue report images are uploaded through the Spring Boot backend and stored in Supabase Storage.

Required environment variables:

- `SUPABASE_URL`: project base URL, for example `https://your-project-id.supabase.co`
- `SUPABASE_SERVICE_ROLE_KEY`: server-side key used only by the backend to upload files
- `SUPABASE_STORAGE_BUCKET`: bucket name for issue images, defaults to `issue-image` and should be public for the current URL strategy
- `APP_ISSUE_IMAGE_OBJECT_PREFIX`: object path prefix inside the bucket, defaults to `issues`
- `APP_MAX_IMAGE_FILE_SIZE_BYTES`: max size per image, defaults to `5242880` (5 MB)
- `APP_ALLOWED_IMAGE_CONTENT_TYPES`: comma-separated list, defaults to `image/jpeg,image/png,image/webp`

Optional geocoding variables for manual text locations:

- `APP_GEOCODING_ENABLED`: enables backend geocoding for typed location text, defaults to `true`
- `APP_GEOCODING_BASE_URL`: geocoding provider base URL, defaults to OpenStreetMap Nominatim
- `APP_GEOCODING_SEARCH_PATH`: geocoding search path, defaults to `/search`
- `APP_GEOCODING_USER_AGENT`: HTTP user-agent sent to the provider
- `APP_GEOCODING_DEFAULT_QUERY_SUFFIX`: suffix appended to typed locations, defaults to `Tirana, Albania`
- `APP_GEOCODING_COUNTRY_CODE`: country filter, defaults to `al`
- `APP_GEOCODING_RESULT_LIMIT`: max returned matches, defaults to `1`

Expected upload flow:

- Frontend sends `multipart/form-data` to `POST /api/issues`
- `payload` contains the JSON issue request
- `images` contains one or more image files
- Backend validates auth, file count, type, and size
- If coordinates are missing, backend geocodes `locationText` and stores the resolved coordinates when possible
- Backend uploads each image to Supabase Storage
- Backend stores image metadata and public URLs in the database linked to the issue

## Notes

- Java 17 is used.
- Maven manages dependencies and build configuration.

## Prioritization Algorithm

The backend now uses a deterministic weighted scoring model for primary issues only. The score is always computed from the same input values, so two identical issues evaluated at the same time produce the same result.

Selected factors and weights:

- Severity: 35%
- Number of votes: 25%
- Location importance: 15%
- Recency: 15%
- Category importance: 5%
- Duplicate count: 5%

Why these weights were chosen:

- Severity carries the highest weight because physical risk and service disruption should dominate queue ordering.
- Votes are second because citizen demand matters, but it should not outweigh urgent safety problems.
- Location importance reflects that issues nearer the city center can affect more residents, traffic, and public activity.
- Recency increases over time so older unresolved issues gradually rise without introducing randomness.
- Category importance adds a small domain-policy signal.
- Duplicate count adds a small crowd-confirmation signal that rewards issues reported by multiple users without letting duplicate traffic overpower severity.

Factor rules:

- Severity importance: `HIGH = 1.0`, `MEDIUM = 0.75`, `LOW = 0.5`
- Vote importance: normalized to full weight at `20` active votes
- Location importance: `<= 1 km = 1.0`, `> 1 km and <= 3 km = 0.75`, `> 3 km = 0.5`
- Recency importance: `>= 2 days old = 1.0`, `>= 1 day old = 0.75`, `< 1 day old = 0.5`
- Category importance: mapped by category name, with public safety, water supply, and roads ranked highest
- Duplicate importance: normalized to full weight at `5` linked duplicates

Why the model still changes over time:

- The model is deterministic, but `recency` is evaluated against the current time.
- An issue therefore gets a higher score when it crosses the 1-day and 2-day thresholds.
- This keeps the method explainable while still preventing older unresolved issues from remaining buried.

Baseline method for later evaluation:

- A `votes-only` baseline is implemented in `IssuePrioritizationService`.
- This supports later thesis evaluation against a simpler ranking strategy such as popularity-only or first-come-first-served.
