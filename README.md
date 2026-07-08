# 🌌 GlassFit — Glassmorphic Fitness Companion

GlassFit is a premium, beautifully crafted Android application designed with an **Elegant Dark Glassmorphic Theme** ("Frosted Glass" with deep cosmic indigo, royal violet, and rich amber neon accents). 

Leveraging **Jetpack Compose** and **Google Gemini AI**, GlassFit provides a seamless, immersive tracking ecosystem that handles custom workouts, AI-powered nutritional calorie scanning, offline database persistence with **Room**, interactive visual data dashboard analytics, and fully-customized PDF workout routine generation.

---

## 🎨 Visual Preview

| Home Dashboard (Elegant Dark) | AI Workout Generator | AI Calorie Scanner | Interactive Analytics |
| :---: | :---: | :---: | :---: |
| <img src="https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=400" width="220" alt="Dashboard"/> | <img src="https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=400" width="220" alt="Workouts"/> | <img src="https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=400" width="220" alt="Calorie Scanner"/> | <img src="https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=400" width="220" alt="Analytics"/> |

---

## ✨ Features & Functionality

*   **🌌 Elegant Dark Glassmorphic Theme:** Ambient glowing radial orbs underneath high-contrast translucent frosted glass sheets. Perfectly responsive to both gorgeous system-level Dark Mode and clean frosted light variations.
*   **🏋️ AI Custom Workout Routine Generator:** Powered by **Google Gemini Beta**, simply state your fitness goal (Build Muscle, Weight Loss, General Fitness), experience level, and training frequency, and get back a structured plan mapped instantly.
*   **📸 Gemini Calorie Scanner:** Snap or select photos of any food/meal. Gemini analyses the photo structure, estimates total calories, protein, carbs, fats, and appends nutritional suggestions.
*   **📊 Interactive Progress Analytics:** Live graphical charts rendered on Jetpack Compose Canvas displaying daily caloric trends, active exercise duration, and body weight fluctuations over time.
*   **📄 Free PDF Download:** Seamlessly export generated fitness routines into professionally designed, shareable, and printable PDF documents.
*   **🗄️ Offline-First Architecture:** Integrated Local SQLite caching via **Room Database** to keep all history, scans, logs, and routines accessible offline.

---

## 🚀 How to Run the App in Android Studio

To compile and launch the application on your physical device or streaming emulator:

1.  **Open Project:** Open Android Studio and select **File -> Open**, then navigate to the root directory containing this project.
2.  **Verify Configuration:** In the top toolbar, ensure that the active build target is set to `:app` and your preferred target device is selected.
3.  **Launch Entry Point:** The absolute main entry point of the application is **`MainActivity.kt`** (located at `/app/src/main/java/com/example/MainActivity.kt`). You do not run the file manually; instead:
    *   Click the green **Run (Play)** button `▶` in the top toolbar.
    *   Alternatively, use the keyboard shortcut `Shift + F10` (Windows/Linux) or `Control + R` (macOS).
4.  **Google Gemini API Setup:**
    *   To enable the AI capabilities (Meal scanner and custom routines), generate an API Key from the Google AI Studio console.
    *   Add `GEMINI_API_KEY=your_actual_key` in your environment configuration or a secure `.env` properties setup so the `BuildConfig` injects it securely.

---

## 🛠️ Tech Stack & Architecture

*   **Language:** Kotlin (100%)
*   **UI Framework:** Jetpack Compose (Material 3)
*   **Database:** Android Room (with Kotlin Coroutines & Flow)
*   **Networking:** Retrofit & OkHttp
*   **JSON Serialization:** Moshi (with reflective Kotlin codegen support)
*   **Image Loading:** Coil Compose
*   **Document Generator:** Android `PdfDocument` API
