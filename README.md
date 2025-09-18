# CampaApp - Automotive Workshop Vehicle Management System

![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-7F52FF?style=for-the-badge&logo=kotlin)
![Firebase](https://img.shields.io/badge/Firebase-Realtime_Database-FFCA28?style=for-the-badge&logo=firebase)
![MVVM](https://img.shields.io/badge/Architecture-MVVM-blue?style=for-the-badge)

ParqueaderoApp (also known as CampaApp) is a comprehensive, native Android application designed to solve a real-world business problem: digitizing the vehicle management process for a busy automotive workshop. The app replaces an inefficient, paper-based system, providing a robust, multi-user, real-time solution.

This project showcases a complete development cycle, from understanding client needs to architecting and delivering a stable, functional, and modern Android application.

---

## 📸 Screenshots & Demo

*(Consejo para ti: Haz capturas de pantalla de alta calidad de las 4 pestañas principales de la app. Para un efecto aún más profesional, usa un programa gratuito como 'ScreenToGif' para grabar un GIF corto de 5-10 segundos mostrando la navegación entre pestañas y el scroll en la lista de historial. Sube las imágenes y el GIF al repositorio y luego enlaza a ellos aquí.)*

## 📸 Screenshots & Demo

<table align="center">
 <tr>
    <td align="center"><b>Login Screen</b></td>
    <td align="center"><b>Home / Dashboard</b></td>
    <td align="center"><b>History View</b></td>
    <td align="center"><b>Sync</b></td>
    <td align="center"><b>User Management</b></td>
 </tr>
 <tr>
    <td><img src="https://github.com/Demmosful/VehicleManagementApp-Android/blob/main/screenshots/login-screen.jpeg?raw=true" width="150" alt="Login Screen"></td>
    <td><img src="https://github.com/Demmosful/VehicleManagementApp-Android/blob/main/screenshots/home-dashboard.jpeg?raw=true" width="150" alt="Home Screen"></td>
    <td><img src="https://github.com/Demmosful/VehicleManagementApp-Android/blob/main/screenshots/history-view.jpeg?raw=true" width="150" alt="History Screen"></td>
    <td><img src="https://github.com/Demmosful/VehicleManagementApp-Android/blob/main/screenshots/sync-import-export.jpeg?raw=true" width="150" alt="Sync Screen"></td>
    <td><img src="https://github.com/Demmosful/VehicleManagementApp-Android/blob/main/screenshots/user-management.jpeg?raw=true" width="150" alt="User Management Screen"></td>
 </tr>
</table>

---

## ✨ Features

*   **Secure User Authentication:** Role-based login system (Admin vs. User) powered by Firebase Authentication.
*   **Real-time Vehicle Tracking:** Live updates on vehicle status (active, departed) and location within the workshop, synchronized across all devices using Firebase Realtime Database.
*   **Complete Vehicle Lifecycle Management:** Register new vehicles, update details, and mark departures, with a full audit trail of which user performed each action.
*   **Advanced History & Search:** A comprehensive history log with a powerful search function to quickly find vehicles by license plate.
*   **Multi-Select & Bulk Actions:** Admins can select multiple records in the history view to perform bulk deletions.
*   **Data Management:** Functionality to import/export vehicle data via CSV files, allowing for backups and integration with other systems.
*   **Dynamic Brand & Model Management:** Admins can add, edit, and manage the list of vehicle brands and models directly within the app.

---

## 🛠️ Tech Stack & Architecture

This application was built with a focus on modern, stable, and scalable technologies, following industry best practices.

*   **Language:** **Kotlin** (100%)
*   **Architecture:** **MVVM (Model-View-ViewModel)**, ensuring a clean separation of concerns and a testable codebase.
*   **UI:** Built with **XML Layouts** and **ViewBinding** for type-safe view access.
*   **Navigation:** A modern **ViewPager2** implementation synchronized with a **BottomNavigationView** for a fluid, up-to-date user experience.
*   **Asynchronous Programming:** **Kotlin Coroutines** and **Flow** for managing background tasks and handling data streams from Firebase.
*   **Cloud Database:** **Firebase Realtime Database** for real-time data storage and synchronization.
*   **Authentication:** **Firebase Authentication** for secure email/password login.
*   **Dependency Management:** A shared **ViewModel (`MainViewModel`)** scoped to the Activity provides a single source of truth for the fragments, simplifying state management.

---

## 🚀 Setup & Installation

To run this project locally, you will need to provide your own Firebase configuration file.

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/Demmosful/VehicleManagementApp-Android
    ```
2.  **Open in Android Studio:** Open the project with the latest stable version of Android Studio.
3.  **Firebase Setup:**
    *   Go to your Firebase Console and create a new project.
    *   Enable **Authentication** (Email/Password method) and the **Realtime Database**.
    *   Download the `google-services.json` file provided by Firebase.
    *   Place this file in the `app/` directory of the project.
    *   *Note: This file is intentionally excluded from the repository via `.gitignore` for security reasons.*
4.  **Build and Run:** The project should now build and run on your device or emulator.
