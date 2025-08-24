## Getting Started

- Ensure Firebase is configured in the project and `google-services.json` is present.
- Launch `LoginActivity` which initializes Firebase and Google Sign-In.
- On successful authentication, the app navigates to `MainActivity` with bottom navigation for Counter, Items, Today, Reports, and More.

Entry points:
- `MyPOSApplication` initializes Firebase on app start.
- `LoginActivity` handles email/password and Google Sign-In.
- `MainActivity` hosts Fragments and the drawer.