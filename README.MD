# SMaRT Project Documentation

## Overview
SMaRT (Sensation Mapping and Recording Tool) is a comprehensive Android application designed for mapping and recording bodily sensations. The application provides an intuitive interface for users to draw sensations on body diagrams, record intensity levels, and manage multiple subjects and their sensation records. It supports various body schemes, sensation types, and provides tools for both real-time sensation mapping and retrospective analysis of recorded data.

## Demo Video

https://github.com/user-attachments/assets/06bb6db9-8d5a-4401-8da0-1de0ce76a2ac

## Setup & Installation

### Prerequisites
- **Android Studio**: Arctic Fox or later
- **Android SDK**: API Level 33 or later
- **Java**: JDK 8 or later
- **Android Device**: Android 13 (API 33) or later for testing

### Installation
1. Clone the repository
2. Open the project in Android Studio
3. Sync the project with Gradle files
4. Build and run the project on a connected Android device or emulator


## System Architecture

### Core Components
#### Data Model
- **Subject**: Represents individual subjects with unique identifiers, names, and configuration settings.
- **Record**: Contains sensation recordings with timestamp, subject association, and drawing data.
- **Step**: Individual drawing steps with brush properties, paths, and intensity markings.

### Device Support
- **Android Devices**: Optimized for tablets and large-screen devices for better drawing experience.
- **Stylus Support**: Enhanced drawing experience with stylus input detection and pressure sensitivity.

### Data Processing Pipeline
- Real-time drawing capture and processing
- Intensity mapping and color coding
- Multi-layer body view management (front/back views)
- Persistent data storage using SQLite database

## User Interface

### Tab-based Navigation System
- **Subject Management**: Create, select, and manage multiple subjects
- **Sensation Drawing**: Interactive body diagram drawing with intensity mapping

## Major Features

### Subject Management (SubjectFragment)
The **SubjectFragment** provides comprehensive subject management capabilities:
- Create new subjects with custom names
- Select and switch between different subjects
- View subject-specific records and statistics
- Manage subject configurations and body schemes
- Database integration for persistent subject storage

### Sensation Drawing (DrawFragment)
The **DrawFragment** is the core drawing interface featuring:
- Multi-tab body view system (front and back views)
- Real-time drawing with stylus and touch input
- Intensity scale with color-coded mapping
- Multiple brush tools (line, region, erase)
- Sensation tagging and categorization
- Undo/redo functionality
- Automatic data persistence

### Canvas Drawing (CanvasFragment)
The **CanvasFragment** provides the drawing canvas functionality:
- Interactive body diagram rendering
- Zoom and pan capabilities for detailed drawing
- Brush customization (thickness, color, type)
- Intensity mapping with visual feedback
- Multi-layer drawing support
- Real-time drawing preview and validation

### Body Drawing View (BodyDrawingView)
The **BodyDrawingView** handles the core drawing mechanics:
- Touch and stylus input processing
- Path creation and manipulation
- Matrix transformations for zoom/pan
- Drawing step management
- Color intensity mapping
- Outside drawing boundary controls

## Data Processing Framework

### Drawing Processing
The application implements sophisticated drawing processing:
#### Drawing Tools:
- **Line Brush**: Free-form drawing with customizable thickness
- **Region Brush**: Area-based drawing for larger sensation areas
- **Eraser**: Selective removal of drawn elements

#### Intensity Mapping:
- **Color-coded intensity scale**: Visual representation of sensation intensity
- **Dynamic color calculation**: Real-time color updates based on intensity
- **Persistent intensity marking**: Maintains intensity data across drawing sessions

## Technical Implementation

### Framework Usage
- **Android View Binding**: Type-safe view references
- **Navigation Components**: Fragment-based navigation system
- **SQLite Database**: Local data persistence
- **Material Design**: Modern UI components and theming
- **Custom Views**: Specialized drawing and UI components

### Data Storage
The application manages data through multiple storage mechanisms:
- **SQLite Database**: Core data persistence for subjects and records
- **SharedPreferences**: User settings and configuration storage
- **File System**: External storage for configuration files and exports
- **Serializable Objects**: Drawing data serialization for complex data structures

### User Settings
The application stores various user preferences:
- Subject selection and management
- Body scheme configuration
- Drawing tool preferences
- Intensity scale settings
- UI theme preferences (light/dark mode)
- Settings stored using **SharedPreferences** for persistence across app launches

### External Configuration Management
The application supports flexible configuration through external INI files:
- **Configuration Files**: Uses INI4J library for parsing external configuration files
- **Default Configuration**: Automatically creates default `config.ini` files in app storage and external Documents/SMaRT folder
- **Dynamic Loading**: Configuration is loaded at runtime from both internal app storage and external storage locations
- **Configurable Elements**: Body schemes, sensation types, color mappings, intensity scales, and UI text labels
- **Hot Reloading**: Configuration changes can be applied without app restart
- **Fallback System**: Graceful fallback to default values if configuration files are missing or corrupted

### Design Patterns
- **MVVM**: Clear separation of View and ViewModel components
- **Repository Pattern**: Data access abstraction
- **Observer Pattern**: Reactive updates to UI from data changes
- **Factory Pattern**: Dynamic creation of drawing tools and components
- **Singleton**: Shared configuration and database management

## Summary
SMaRT is a highly configurable Android application for mapping and recording bodily sensations. The application provides drawing interfaces for body diagrams, data storage capabilities, and analysis tools. It supports real-time sensation mapping and retrospective data analysis.

The application uses a modular architecture with extensive configuration options through external INI files. It implements touch-based drawing techniques for Android devices and stores sensation data in a local SQLite database.

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

## License

Copyright © 2023 SMaRT was developed by the Insula Institute (Florian Beissner) in cooperation with Bielefeld University (Evgenii Pustozerov).

SMaRT is scientific research software. It is not a medical device and should not be considered as such. It does not provide medical advice or diagnoses and is not intended to replace professional medical advice, diagnosis, or treatment.

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

SMaRT uses the Hannover Body Template from Till-Ansgar Neubert & Florian Beissner (2019). Hannover Body Template. figshare. Dataset. https://doi.org/10.6084/m9.figshare.7637387 under CC BY 4.0 (https://creativecommons.org/licenses/by/4.0/)
