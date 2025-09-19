## Overview

SMaRT (Sensation Mapping and Reporting Tool) is a scientific app for Android-based tablet PCs. It is used for the objective documentation of bodily or somatic sensations, such as tingling, fullness, warmth, etc., with the help of a digitally annotatable human body outline.
Patients or clinical personnel mark the quality, location, extent, and intensity of the sensation on the body outline. This information is stored locally on the mobile device and can be exported if desired.

## Demo Video

https://github.com/user-attachments/assets/06bb6db9-8d5a-4401-8da0-1de0ce76a2ac

## Prerequisites
- **Android Studio**: Arctic Fox or later
- **Android SDK**: API Level 33 or later
- **Java**: JDK 8 or later
- **Android Device**: Android 13 (API 33) or later for testing

## Device Support
- **Android Devices**: Optimized for tablets and large-screen devices for better drawing experience.
- **Stylus Support**: Enhanced drawing experience with stylus input detection and pressure sensitivity.

## User Interface
- **Subject Management**: Create, select, and manage multiple subjects
- **Sensation Drawing**: Interactive body diagram drawing with intensity mapping

## Architecture Documentation

For detailed information about the application's architecture, major features, and technical implementation, see [ARCHITECTURE.md](ARCHITECTURE.MD).

## Dependencies

Key dependencies (see `app/build.gradle` for complete list):
- **AndroidX AppCompat** ~= 1.7.0 (Backward compatibility support)
- **Material Design Components** ~= 1.12.0 (Modern UI components)
- **Navigation Components** ~= 2.7.7 (Fragment navigation)
- **ViewPager2** ~= 1.1.0 (Tab-based navigation)
- **ColorSeekBar** ~= 2.0.3 (Intensity scale component)
- **ColorPicker** ~= 0.0.15 (Color selection tools)
- **INI4J** ~= 0.5.4 (Configuration file parsing)
- **JUnit** ~= 4.13.2 (Unit testing framework)
- **Espresso** ~= 3.6.1 (UI testing framework)

## Project Structure

```
SMaRT/
├── app/                           # Main Android application
│   ├── build.gradle              # App-level build configuration
│   ├── libs/                     # External libraries
│   │   ├── pen-v4.1.0_light.aar  # Stylus input library
│   │   └── sdk-v1.0.0.jar        # SDK components
│   ├── proguard-rules.pro        # Code obfuscation rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml    # App configuration
│       │   ├── java/com/dm/smart/     # Main source code
│       │   │   ├── MainActivity.java           # Main activity
│       │   │   ├── Configuration.java           # Configuration management
│       │   │   ├── DBAdapter.java              # Database operations
│       │   │   ├── SharedViewModel.java         # Shared data model
│       │   │   ├── DrawFragment.java            # Drawing interface
│       │   │   ├── SubjectFragment.java         # Subject management
│       │   │   ├── CanvasFragment.java          # Drawing canvas
│       │   │   ├── BodyDrawingView.java         # Custom drawing view
│       │   │   ├── ViewPagerAdapter.java        # Tab management
│       │   │   ├── RecyclerViewAdapter*.java    # List adapters
│       │   │   ├── items/                       # Data models
│       │   │   │   ├── Subject.java             # Subject data model
│       │   │   │   ├── Record.java              # Record data model
│       │   │   │   ├── Brush.java               # Drawing brush
│       │   │   │   ├── Step.java                # Drawing step
│       │   │   │   ├── SerializablePath.java   # Drawing path
│       │   │   │   └── SerializablePaint.java   # Drawing paint
│       │   │   └── ui/elements/                 # UI components
│       │   │       ├── CustomAlertDialogs.java  # Custom dialogs
│       │   │       ├── CustomThumbDrawer.java   # Thumbnail drawing
│       │   │       ├── CustomToasts.java        # Custom notifications
│       │   │       └── VerticalButton.java      # Custom button
│       │   └── res/                            # Resources
│       │       ├── layout/                     # UI layouts
│       │       ├── values/                     # String resources
│       │       ├── drawable/                   # Images and icons
│       │       ├── navigation/                 # Navigation graphs
│       │       └── xml/                        # Configuration files
│       ├── test/                               # Unit tests
│       └── androidTest/                       # Instrumented tests
├── build.gradle                               # Project-level build configuration
├── gradle.properties                          # Gradle properties
├── settings.gradle                            # Project settings
├── gradlew                                    # Gradle wrapper (Unix)
├── gradlew.bat                                # Gradle wrapper (Windows)
├── resources/                                 # Demo resources
│   └── demo.mp4                               # Demo video
└── local.properties                           # Local configuration
```

## Contact / Maintainer

For questions about this project, contact **Evgenii Pustozerov** or the AG Digitale Medizin team.

## User Manual

Detailed instructions and use cases can be found on the official website: [https://www.insula-institut.org/software](https://www.insula-institut.org/software)

## License

Copyright © 2023 SMaRT was developed by the Insula Institute (Florian Beissner) in cooperation with Bielefeld University (Evgenii Pustozerov).

SMaRT is scientific research software. It is not a medical device and should not be considered as such. It does not provide medical advice or diagnoses and is not intended to replace professional medical advice, diagnosis, or treatment.

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

SMaRT uses the Hannover Body Template from Till-Ansgar Neubert & Florian Beissner (2019). Hannover Body Template. figshare. Dataset. https://doi.org/10.6084/m9.figshare.7637387 under CC BY 4.0 (https://creativecommons.org/licenses/by/4.0/)
