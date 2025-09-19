# SMaRT Architecture Documentation

## Core Components
- **Subject**: Represents individual subjects with unique identifiers, names, and configuration settings.
- **Record**: Contains sensation recordings with timestamp, subject association, and drawing data.
- **Step**: Individual drawing steps with brush properties, paths, and intensity markings.

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

## Drawing Tools:
- **Line Brush**: Free-form drawing with customizable thickness
- **Region Brush**: Area-based drawing for larger sensation areas
- **Eraser**: Selective removal of drawn elements

## External Configuration Management
The application supports flexible configuration through external INI files:
- **Configuration Files**: Uses INI4J library for parsing external configuration files
- **Default Configuration**: Automatically creates default `config.ini` files in app storage and external Documents/SMaRT folder
- **Dynamic Loading**: Configuration is loaded at runtime from both internal app storage and external storage locations
- **Configurable Elements**: Body schemes, sensation types, color mappings, intensity scales, and UI text labels
- **Hot Reloading**: Configuration changes can be applied without app restart
- **Fallback System**: Graceful fallback to default values if configuration files are missing or corrupted