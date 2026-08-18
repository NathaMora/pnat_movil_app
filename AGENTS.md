# AGENTS.md — PNAT Mobile

## 1. Purpose

This repository contains the native Android application for RaySharkApp / PNAT.

Repository:

`pnat_mobile`

The mobile application is a client of the existing `pnat_backend` REST API.

Its initial scope is intentionally limited to:

1. Project presentation.
2. Submit sighting reports.
3. My Reports.
4. Authentication only when required for user-specific functionality.

Do not expand the application beyond this scope unless explicitly requested.

---

## 2. Repository boundaries

Work only inside the `pnat_mobile` repository.

Do not modify:

- `pnat_backend`
- `pnat_front`
- PostgreSQL
- server infrastructure
- external repositories
- files outside this repository

If a mobile feature requires a backend change:

1. do not edit the backend from this repository;
2. document the exact requirement;
3. explain why the current API is insufficient;
4. identify the endpoint or contract involved;
5. stop that part of the implementation until explicitly instructed otherwise.

Do not invent backend behavior to work around a missing API capability.

---

## 3. Mandatory technical stack

Use:

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- Android ViewModel
- Kotlin Coroutines
- StateFlow
- Retrofit
- OkHttp
- Room
- WorkManager
- Coil where image loading is required
- Android Photo Picker or current Android media-selection APIs

Prefer modern Android APIs.

Do not introduce XML layouts for new screens unless there is a concrete technical reason and it is documented.

Do not use WebView to embed the Angular application.

---

## 4. Design rules

The mobile application must visually belong to the same product as the existing web application.

The mandatory design reference is:

`docs/design-guide.md`

Follow it before implementing or changing UI.

Do not create a new visual identity.

In particular:

- use the PNAT institutional blue `#2FA4DC`;
- use Material 3;
- keep light backgrounds;
- preserve the hierarchy and visual language of the web app;
- use Montserrat for headings and Inter for body text when approved font assets are available;
- use Material icons;
- use restrained elevation and shadows;
- use approximately 4–8 dp radii for regular controls and 12 dp for visual report/KPI cards;
- do not introduce decorative gradients, glassmorphism, neumorphism, neon colors, or unrelated design systems;
- do not imitate desktop hover behavior on Android;
- adapt desktop layouts to mobile instead of copying them literally.

The initial app uses a light theme only unless explicitly requested otherwise.

---

## 5. Internationalization

The app must be prepared for English and Spanish.

Do not hardcode user-visible strings inside composables, ViewModels, repositories, workers, or services.

Use Android string resources:

- `res/values/strings.xml`
- `res/values-es/strings.xml`

This includes:

- titles;
- buttons;
- labels;
- validation messages;
- network messages;
- synchronization states;
- accessibility descriptions;
- empty states;
- error messages intended for users.

Internal technical constants are not user-visible strings and may remain in code where appropriate.

---

## 6. Offline-first is mandatory

The application is offline-first.

A finished report must always be persisted locally before any attempt to send it to the backend.

Do not implement separate logic such as:

`if online -> send directly`

`if offline -> save locally`

The required flow is always:

`form -> local persistence -> synchronization queue -> backend`

Room is the local source of truth while a report is unsynchronized.

A report must survive:

- loss of connectivity;
- app closure;
- process death;
- device restart where supported by Android scheduling;
- partial upload failure;
- temporary backend failure.

Never discard a report because a network request failed.

---

## 7. Required synchronization states

Use persistent synchronization states with clear semantics.

At minimum:

- `DRAFT`
- `PENDING`
- `SYNCING`
- `SYNCED`
- `ERROR`

Do not confuse technical synchronization state with scientific verification state.

For example:

`SYNCED + pending scientific verification`

is valid.

A successful upload does not mean a scientist verified the observation.

---

## 8. Local persistence rules

Use Room for structured local data.

Keep separate local models for, at minimum:

- reports;
- media metadata;
- observations;
- observation-media relationships;
- cached catalogs;
- cached My Reports data where appropriate.

Do not store large photo or video binaries inside Room.

Room should store metadata and stable local file references.

Do not use Room entities directly as Retrofit DTOs.

Maintain clear separation between:

- API DTOs;
- Room entities;
- UI/domain models;
- mapping code.

---

## 9. Media handling

Photos and videos may need to remain on the device for a long period before synchronization.

When a user selects media:

1. obtain it through an appropriate Android API;
2. ensure the app retains durable access or creates an app-controlled local copy when needed;
3. persist the local reference;
4. persist the original filename when available;
5. persist MIME/type metadata;
6. calculate SHA-512;
7. associate the media with the correct local report and observations.

Do not depend solely on a temporary content URI that may become inaccessible later.

Do not delete local pending media before the backend has confirmed the relevant upload.

Do not load an entire large video into memory merely to hash or upload it.

Use streaming.

---

## 10. SHA-512

The backend uses SHA-512 for media duplicate detection and associations.

The Android implementation must produce a hexadecimal SHA-512 string compatible with the backend.

Requirements:

- process files as streams;
- produce 128 hexadecimal characters;
- persist the result locally;
- reuse the persisted hash during retries;
- do not recalculate unnecessarily.

Do not replace SHA-512 with another hash algorithm.

---

## 11. WorkManager

Use WorkManager for background synchronization.

Workers that send reports must require network connectivity with:

`NetworkType.CONNECTED`

Do not implement continuous manual polling.

Avoid multiple workers synchronizing the same report simultaneously.

Use unique work names or another reliable mutual-exclusion strategy.

When pending work exists, the app must ensure appropriate synchronization work is scheduled.

WorkManager must be able to continue after the user closes the application.

---

## 12. Resumable synchronization

Synchronization must be resumable.

Do not treat the report as one indivisible network operation.

Persist progress after each confirmed server step.

Typical sequence:

1. create remote report;
2. store returned `serverReportId`;
3. process pending media;
4. mark each confirmed media upload;
5. submit observations;
6. mark observations/step confirmation as needed;
7. set report to `SYNCED` only after the whole workflow is confirmed.

If the connection disappears after step 3, the next run must continue from the last confirmed state.

Do not blindly restart from step 1.

---

## 13. Duplicate prevention

Use all available client-side protections:

- locally generated UUIDs;
- persisted `serverReportId`;
- SHA-512 for media;
- per-step synchronization state;
- per-media upload state;
- durable local progress.

Never intentionally resend already confirmed media.

Never intentionally recreate a report when a valid `serverReportId` is already stored.

However, do not claim absolute report-creation idempotency if the backend does not support an idempotency key or equivalent unique client submission identifier.

The known limitation must be documented in:

`docs/backend-idempotency.md`

Do not modify the backend from this repository to solve it.

---

## 14. Authentication

The current backend authenticates using JWT stored in an HTTP cookie.

The Android app must treat the cookie as an HTTP-layer concern.

Do not extract and manually persist the JWT.

Do not store JWT values in:

- Room;
- DataStore;
- SharedPreferences;
- files;
- logs;
- source code.

Implement session-compatible HTTP cookie handling with OkHttp.

Authentication-related functionality should support the existing backend contracts for:

- login;
- session check;
- logout.

`My Reports` requires authentication.

If authentication expires during a pending authenticated synchronization:

- keep the local report;
- do not delete or corrupt its state;
- expose a recoverable authentication-required state;
- resume after the user successfully authenticates again.

---

## 15. API contracts

The backend is the source of truth for network contracts.

Do not invent endpoints.

Do not silently rename fields.

Do not assume fields are optional or required without checking the current backend contract.

Reuse the existing backend concepts for:

- public reports;
- authenticated reports;
- media uploads;
- observations;
- locations;
- taxonomy;
- behaviors;
- authentication;
- My Reports.

Keep endpoint definitions centralized in the remote data layer.

Do not build URLs manually inside UI code.

---

## 16. Base URLs and environments

Do not hardcode production URLs throughout the codebase.

Use an environment-aware mechanism such as:

- `BuildConfig`;
- Gradle properties;
- another centralized Android configuration mechanism.

No composable, ViewModel, Worker, or repository should contain ad-hoc server URLs.

Never commit secrets.

---

## 17. Security

Never commit:

- passwords;
- database credentials;
- JWT secrets;
- mail credentials;
- API secrets;
- production tokens;
- private keys;
- signing secrets.

Do not log:

- passwords;
- JWT/cookies;
- full authentication headers;
- sensitive personal information;
- full request bodies containing personal data unless explicitly sanitized for a safe local test.

Do not weaken TLS validation.

Do not add "trust all certificates" code.

Do not disable hostname verification.

Do not add insecure production fallbacks to make a local test pass.

---

## 18. Catalogs and offline forms

Cache the catalogs required to create reports offline.

At minimum where supported by the backend:

- oceans;
- destinations;
- sighting spots;
- genera;
- species;
- behaviors.

Keep the last valid local catalog when an update fails.

The report form should remain usable offline if catalogs were successfully downloaded previously.

If the application has never downloaded required catalogs, it may require an initial Internet connection and must communicate that clearly to the user.

Do not fabricate catalog entries.

---

## 19. Scope of the report form

Implement only fields supported by the current backend and requested mobile scope.

The report flow may include:

- participant identity when required;
- date;
- ocean;
- destination;
- sighting spot;
- photo/video evidence;
- genus;
- species;
- behavior;
- individual count;
- participant message;
- consent/contact fields where required for guest submissions.

Do not force taxonomic identification where the backend permits unidentified records.

Do not invent new mandatory fields.

Preserve media-to-observation relationships.

---

## 20. My Reports

My Reports should combine appropriate remote user contributions with local reports.

Local unsynchronized reports must appear immediately.

Possible local states include:

- draft;
- pending;
- syncing;
- error;
- synced.

Do not duplicate a local report in the UI when it has a known `serverReportId` and the same report is present in the remote result.

When offline:

- show local reports;
- show the latest valid cached remote information where implemented;
- make it clear when remote information may be stale.

---

## 21. Error handling

Classify errors.

### Retryable

Examples:

- no connectivity;
- timeout;
- temporary network failure;
- suitable HTTP 5xx responses.

Action:

- preserve data;
- keep/recover synchronization state;
- retry through WorkManager when appropriate.

### Authentication

Examples:

- HTTP 401;
- HTTP 403 where caused by session state.

Action:

- preserve local data;
- require authentication where appropriate;
- do not retry endlessly without user action.

### Validation/business error

Examples:

- HTTP 400 caused by an invalid request.

Action:

- preserve local data;
- stop infinite retries;
- expose a meaningful recoverable error;
- allow correction when possible.

Do not map every error to "No Internet".

Do not expose raw server exceptions directly to end users.

---

## 22. UI state requirements

Important screens must explicitly handle relevant states:

- loading;
- content;
- empty;
- error;
- offline;
- authentication required;
- synchronization state where relevant.

Do not create screens that only render the happy path.

Use StateFlow or another appropriate observable state mechanism from ViewModels.

Keep network and database operations out of composables.

---

## 23. Compose rules

Prefer small, focused composables.

Hoist state where appropriate.

Do not put business logic inside UI composables.

Do not make Retrofit or Room calls directly from composables.

Use previews where useful.

Reuse components when there is real repeated behavior, for example:

- `PnatScreenTitle`
- `PnatPrimaryButton`
- `PnatOfflineBanner`
- `ReportCard`
- `SyncStatusIndicator`
- `MediaThumbnail`
- `FormSection`
- `EmptyState`

Do not build an unnecessarily abstract design system.

---

## 24. Accessibility

Respect Android accessibility practices.

At minimum:

- 48 dp touch targets;
- content descriptions where needed;
- sufficient contrast;
- no state communicated by color alone;
- support system font scaling;
- visible labels for form controls;
- layouts that do not clip text at larger font sizes.

Do not use emojis as primary functional icons.

---

## 25. Dependency policy

Do not add dependencies merely for convenience.

Before introducing a new library:

1. check whether Android/Kotlin/Compose already provides the capability;
2. verify the library has a concrete use;
3. avoid overlapping libraries for the same job;
4. keep the dependency compatible with the project's existing versions.

Do not perform broad Kotlin, Gradle, Android Gradle Plugin, Compose, or dependency upgrades unless the task explicitly requires them.

If an upgrade is necessary, keep it minimal and explain why.

---

## 26. Existing code protection

Before editing:

1. inspect the relevant files;
2. inspect `git status`;
3. understand the existing implementation;
4. identify the smallest coherent change.

Do not rewrite large working areas because a different pattern is personally preferable.

Do not rename packages, modules, public models, or major directories without a concrete need.

Do not delete working functionality unrelated to the requested task.

Prefer incremental changes.

---

## 27. Git safety

Do not run destructive Git commands without explicit authorization.

Do not use:

- `git reset --hard`
- destructive checkout operations
- mass clean operations
- history rewrites
- force pushes

Do not:

- commit;
- push;
- create branches;
- change remotes;
- tag releases

unless explicitly instructed.

Always inspect `git status` before and after meaningful work.

Preserve uncommitted user changes.

---

## 28. Filesystem safety

Do not edit files outside the repository.

Do not recursively delete directories unless the task explicitly requires it and the target has been inspected.

Do not clean caches or external directories as a generic troubleshooting technique.

Do not overwrite user assets without checking their purpose.

---

## 29. Sandbox behavior

The sandbox may lack:

- Android SDK components;
- an emulator;
- Internet access;
- cached Gradle dependencies;
- signing configuration;
- external services.

Distinguish environment failures from code failures.

If a command cannot execute because the environment is incomplete:

- report the exact limitation;
- continue with safe work that does not require the missing capability;
- do not claim the skipped validation passed.

Never fabricate test results.

---

## 30. Validation after changes

Run the most relevant checks available for the change.

When the sandbox supports them, use:

`./gradlew test`

`./gradlew lint`

`./gradlew assembleDebug`

Run narrower tests during development when appropriate.

Do not wait until the end of a large implementation to discover compilation errors.

After UI changes, use Compose Preview, emulator screenshots, or another available visual validation mechanism when possible.

Do not claim visual validation if only compilation was performed.

---

## 31. Tests

Critical offline and synchronization behavior must have automated coverage where practical.

Prioritize tests for:

- SHA-512;
- DTO/entity mappers;
- local persistence;
- synchronization state transitions;
- retry decisions;
- partial synchronization recovery;
- duplicate prevention logic;
- authenticated synchronization failure;
- API payload construction.

Use MockWebServer or an equivalent isolated test mechanism for HTTP behavior.

Do not run automated tests against production.

---

## 32. Mandatory offline acceptance behavior

The implementation is not considered complete unless this behavior is supported:

1. user opens the app;
2. Internet is unavailable;
3. user completes a report;
4. user attaches media;
5. user finalizes the report;
6. report is persisted locally as pending;
7. app is closed;
8. app is reopened;
9. report still exists;
10. Internet returns;
11. background synchronization is eligible to run;
12. report is sent;
13. media is sent;
14. observations are sent;
15. report becomes synchronized;
16. repeating the worker does not intentionally repeat confirmed steps.

A solution that only works while the app remains open is incomplete.

---

## 33. Mandatory partial-failure behavior

The synchronization design must support this scenario:

1. remote report is created;
2. `serverReportId` is persisted;
3. first media file uploads successfully;
4. connectivity is lost;
5. worker stops/retries;
6. next execution does not recreate the report;
7. next execution does not re-upload already confirmed media;
8. remaining work continues;
9. final state becomes `SYNCED`.

Persist enough state to make this possible.

---

## 34. Documentation

Keep project documentation consistent with the implementation.

Important files:

- `README.md`
- `docs/design-guide.md`
- `docs/architecture.md`
- `docs/offline-sync.md`
- `docs/backend-idempotency.md`

When architecture or synchronization behavior changes materially, update the corresponding documentation in the same task.

Do not leave documentation describing behavior that no longer exists.

---

## 35. Communication when completing a task

At the end of each task, report concisely:

1. what was changed;
2. files created;
3. files modified;
4. tests/commands actually executed;
5. actual results;
6. remaining limitations or blockers.

For code changes, always identify the file path and relevant class/function/section.

Do not say:

- "everything works";
- "fully tested";
- "production ready";
- "verified";

unless the evidence produced during the task supports that claim.

---

## 36. Prohibited scope unless explicitly requested

Do not implement these modules in the first mobile version:

- Scientists Desk;
- administrator dashboard;
- user administration;
- affiliation administration;
- audit-log administration;
- Photo-ID management;
- scientific observation editing;
- rejected-record management;
- public analytics dashboard;
- trivia;
- scientific exports;
- desktop-only features.

The mobile app is intentionally focused.

---

## 37. Decision priority

When requirements conflict, follow this priority:

1. explicit current user instruction;
2. this `AGENTS.md`;
3. `docs/design-guide.md`;
4. `docs/offline-sync.md`;
5. `docs/architecture.md`;
6. established code patterns in the repository;
7. general Android conventions.

If a conflict remains ambiguous, do not make a destructive assumption.

Choose the safest reversible implementation and document the uncertainty.

---

## 38. Core principle

The application must be dependable in the field.

A diver or participant may create a report where connectivity is absent or unstable.

The application must prioritize:

- preservation of user data;
- clear status;
- resumable synchronization;
- compatibility with the existing backend;
- visual consistency with RaySharkApp;
- maintainable native Android code.

Never sacrifice data preservation merely to simplify implementation.
