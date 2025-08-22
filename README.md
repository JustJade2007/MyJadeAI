# AI Chat Application

TODO: ADD SCREENSHOT/BANNER OF FINISHED PRODUCT

A full-stack, real-time chat application designed to provide a user interface for interacting with a custom AI model. This project serves as a comprehensive portfolio piece demonstrating modern Android development practices, backend integration with Firebase, and complex feature implementation.

---

## ✨ Core Features

-   **Dual Authentication:** Users can sign up and log in using either Email/Password or their Google account.
-   **Admin Approval System:** New user accounts are placed in a 'pending' state and must be manually approved by a developer via an in-app admin panel before they can access the application.
-   **Real-Time Chat:** A clean, modern chat interface for communicating with the AI.
-   **Typing Indicators:** See when the AI is "typing" a response.
-   **AI Status Display:** Shows the current status of the AI (e.g., Online, Maintenance).
-   **Admin Panel:** A secure, in-app panel accessible only to 'dev' roles for approving or declining new user accounts.
-   **Image & Video Sharing:** (Planned) Functionality for users to send and receive media.
-   **GitHub-Based Changelogs:** (Planned) A screen to display the latest updates pulled from the repository's releases.

---

## 🛠️ Tech Stack & Architecture

This project uses a modern, scalable tech stack leveraging Google's cloud infrastructure.

-   **Frontend (Android App):**
    -   **Language:** [Kotlin](https://kotlinlang.org/)
    -   **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
    -   **Architecture:** MVVM (Model-View-ViewModel)
    -   **Asynchronicity:** Kotlin Coroutines & Flow
    -   **Networking:** [Retrofit](https://square.github.io/retrofit/) (for future API calls)
    -   **Navigation:** [Jetpack Navigation Compose](https://developer.android.com/jetpack/compose/navigation)

-   **Backend & Database (BaaS):**
    -   **Platform:** [Firebase](https://firebase.google.com/)
    -   **Authentication:** Firebase Authentication (Email/Password & Google Sign-In)
    -   **Database:** Cloud Firestore (for real-time data, user info, and messages)
    -   **Storage:** Firebase Cloud Storage (for images and videos)

---

## 🚀 Getting Started

To get a local copy up and running, follow these simple steps.

### Prerequisites

-   Android Studio (latest version)
-   A Google Account for Firebase

### Setup Instructions

1.  **Clone the repository:**
    ```sh
    git clone https://github.com/your_username/your_repository_name.git
    ```

2.  **Firebase Project Setup:**
    -   Go to the [Firebase Console](https://console.firebase.google.com/) and create a new project.
    -   Add an Android app to the project with the package name `com.yourname.aichat` (or match it to the one in the project).
    -   Download the `google-services.json` file and place it in the `app/` directory of the Android project.
    -   Enable **Authentication** methods: **Email/Password** and **Play Games**.
    -   Enable the **Cloud Firestore** database.

3.  **Google Cloud Credentials:**
    -   Follow the instructions in the project guide to create an **OAuth 2.0 Client ID** in the Google Cloud Console for your Firebase project.
    -   Copy the **Client ID** and **Client Secret** and add them to the **Play Games** sign-in provider in the Firebase Authentication settings.

4.  **Add Your SHA-1 Fingerprint:**
    -   In Android Studio, run the `./gradlew signingReport` command in the terminal.
    -   Copy the **SHA-1** debug key.
    -   In your Firebase project settings, add this SHA-1 key to your Android app configuration.

5.  **Run the App:**
    -   Open the project in Android Studio, let it sync, and run it on an emulator or a physical device.

---

## License

Distributed under the MIT License. See `LICENSE` for more information.
