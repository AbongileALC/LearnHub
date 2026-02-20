# 🗺️ LearnHub – Map Subsystem

> **PRJ262S – Project 2** | Abongile Phandle (230670849) | Cape Peninsula University of Technology

A Java Swing desktop subsystem for the **LearnHub** student platform. It displays nearby study groups on an interactive OpenStreetMap, powered by JXMapViewer and Apache Derby, allowing CPUT students to find, filter, and navigate to study groups based on their real-time campus location.

---

## 📋 Table of Contents

- [About the Project](#about-the-project)
- [Features](#features)
- [Database Design](#database-design)
- [UI Design](#ui-design)
- [Business Rules](#business-rules)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Design Patterns Used](#design-patterns-used)
- [Lessons Learned](#lessons-learned)

---

## About the Project

LearnHub is a group desktop application built to help CPUT students connect with nearby study groups, find safe study spaces, and communicate with coursemates. This subsystem — the **Map / Location Module** — was designed and developed by Abongile Phandle as part of Group 02's Term 2–4 project deliverables.

The module enables students to:
- View study groups pinned on a live OpenStreetMap
- Set their own GPS pin on the map
- Calculate and display real distances between their location and each group
- Filter and sort groups by distance, name, or member count

---

## Features

| Feature | Description |
|--------|-------------|
| 🗺️ Interactive Map | Live OpenStreetMap rendered via JXMapViewer with pan, zoom, and click support |
| 📍 Set My Location | Click anywhere on the map to update your location; saved to Apache Derby DB |
| 📏 Distance Calculation | Haversine formula calculates real-world distances (meters/km) from user to each group |
| 🔍 Filter & Sort | Sort groups by distance, name, or member count; filter by 100m / 500m / 1km radius |
| 🗂️ List & Grid View | Toggle between list view and grid card view for study groups |
| 🧭 Waypoint Markers | Custom painted markers: red for user position, blue for study group locations |
| 🏫 DB Integration | Loads study groups and user location from Apache Derby; falls back to sample data |
| 👤 Student Info Header | Displays logged-in student's name, student number, and email dynamically |

---

## Database Design

The full LearnHub database consists of the following entities:

### Core Entities

**User** — `UserID (PK)`, FirstName, LastName, Email, Password, StudentNumber, `CourseID (FK)`, `ResidenceID (FK)`, YearOfStudy, ProfilePicture, DateRegistered, Status

**Course** — `CourseID (PK)`, CourseName, CourseCode, Department, Faculty

**Residence** — `ResidenceID (PK)`, ResidenceName, Address, Latitude, Longitude, ResidenceType, Capacity

**StudyGroup** — `GroupID (PK)`, GroupName, `CreatorID (FK)`, `CourseID (FK)`, Description, MaxCapacity, DateCreated, Status

**GroupMembership** — `MembershipID (PK)`, `GroupID (FK)`, `UserID (FK)`, JoinDate, Role, Status

**StudyLocation** — `LocationID (PK)`, LocationName, Address, Latitude, Longitude, LocationType, Capacity, OpeningHours, ClosingHours, SafetyRating, NoiseLevel

**Message** — `MessageID (PK)`, `GroupID (FK)`, `SenderID (FK)`, Content, Timestamp, Status

**StudySession** — `SessionID (PK)`, `GroupID (FK)`, `LocationID (FK)`, StartTime, EndTime, Topic, Status

### Key Relationships

- Course → User: **1-to-many** (one course, many students)
- Course → StudyGroup: **1-to-many**
- User ↔ StudyGroup: **many-to-many** via `GroupMembership`
- StudyGroup → Message: **1-to-many**
- StudyGroup ↔ StudyLocation: **many-to-many** via `StudySession`

---

## UI Design

### Brand Guidelines

| Element | Value |
|--------|-------|
| Primary colour | Deep Navy `#0B2C4D` |
| Accent colour | Bright Blue `#1890FF` |
| Text colour | Charcoal `#333333` |
| Background | Off-White `#FAFAFA` |
| Heading font | Proxima Nova |
| Body font | Open Sans |
| Button font | Canva Sans |

### Screen Layout

The app follows a modular layout with these consistent components across all screens:

- **Header Bar** — LearnHub logo, student name/number/email, profile initial avatar
- **Navigation Bar** — Dashboard · Profile · Groups · Location · Messages · Study Points
- **Map Section** — Interactive JXMapViewer panel with expand/collapse and Set My Location
- **Groups Section** — Scrollable list or grid of nearby study groups with sort/filter controls
- **Footer** — Trademark bar

### Subsystem Screens Designed

1. **Find Study Groups** — search, list/grid toggle, sort by distance/activity/size, group cards with quick actions
2. **Profile Management** — profile picture upload, personal details, residence/course selection, privacy settings
3. **Study Group Creation** — group name, course, capacity, schedule, privacy
4. **Study Points Suggestion** — map/list view of safe locations, filters by type/noise/capacity, room booking
5. **Messaging System** — group chat, composition area, file attachments, read receipts

---

## Business Rules

### Location & Proximity
- Study groups are displayed based on proximity to the user's saved location
- Default search radius: **3 km**; maximum: **10 km**
- Distances calculated using **latitude/longitude (Haversine formula)**
- Groups with available capacity are prioritised over full groups
- More active groups receive higher suggestion ranking
- Newly created groups (within 15 days) receive a temporary visibility boost

### Study Groups
- Any registered user can create a study group
- Maximum group size: **8 members**
- Users may join up to **3 study groups**
- Users may only join groups linked to their enrolled courses
- Group names must be unique within the system
- Group creator is automatically assigned the Admin role

### User Accounts
- Registration requires a valid academic email (`2….@mycput.ac.za`)
- Passwords must have ≥ 8 characters (uppercase, lowercase, number, special character)
- After 5 failed logins, account is locked for 30 minutes

### Study Locations
- Must have a minimum safety rating of **3 / 5**
- Library rooms bookable up to **3 days** in advance, max **3 hours** per session
- Study sessions can be scheduled up to **30 days** ahead

### Data Privacy
- User contact details are only visible to members of shared groups
- Location data is used only for proximity calculations, never shared directly
- Users can opt out of location-based features at any time

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17+ |
| GUI Framework | Java Swing |
| Map Library | JXMapViewer2 |
| Tile Source | OpenStreetMap |
| Database | Apache Derby (embedded / client) |
| Build Tool | Apache Maven |
| IDE | Apache NetBeans |

---

## Getting Started

### Prerequisites

- Java JDK 17 or higher
- Apache Maven 3.x
- Apache Derby (included via Maven dependency)
- Internet connection (for OpenStreetMap tiles)

### Run the App

```bash
git clone https://github.com/your-username/learnhub-map.git
cd learnhub-map
mvn clean package
mvn exec:java -Dexec.mainClass="za.ac.cput.mapapp.MapApp"
```

### Database Setup

The app connects to an Apache Derby database at:
```
jdbc:derby://localhost:1527/LocationDB;user=app;password=app
```

If no database is found, the app automatically falls back to built-in sample data so the map still renders correctly.

---

## Project Structure

```
src/
└── main/java/za/ac/cput/mapapp/
    ├── MapApp.java                      # Main JFrame — entry point
    ├── Student.java                     # Student data model
    ├── StudyLocation.java               # Study location data model
    ├── StudyLocationWaypoint.java       # Custom JXMapViewer waypoint
    ├── StudyLocationWaypointPainter.java # Custom waypoint painter
    └── StudyLocationDBDemo.java         # Database helper (DAO)
resources/
    └── learnHub_Logo.png
pom.xml
```

---

## Design Patterns Used

| Pattern | Where Used |
|--------|-----------|
| **Singleton** | `SessionManager`, `UnifiedDBConnection` |
| **Factory** | `HeaderPanelCreator`, `NavigationPanelCreator` |
| **Observer** | Swing's event handling (`ActionListener`, `MouseListener`) |
| **MVC** | Domain models (Model), GUI classes (View), DAOs (Controller) |
| **DAO** | `StudyLocationDBDemo` separates data access from UI logic |

---

## Lessons Learned

- **SwingUtilities.invokeLater()** is essential — all GUI updates must happen on the Event Dispatch Thread to avoid race conditions
- **Custom component rendering** (overriding `paintComponent`) unlocks modern-looking UIs beyond Swing defaults
- **NavigationManager with proper `dispose()`** calls prevents memory leaks when switching screens
- **Data validation at load time** — study groups without valid coordinates caused (0.0, 0.0) map pins; fixed by validating before display
- **Accessibility** should be designed in from the start, not added later — keyboard navigation and colour contrast need early planning

---

## Author

**Abongile Phandle** — UI Designer & Front-End Developer, Group 02  
Student No: 230670849 | PRJ262S – Project 2  
Cape Peninsula University of Technology (CPUT)

---

*© 2025 LearnHub – All Rights Reserved*
