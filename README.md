# 🖥️ EcoAdventure Desktop - Management System

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk)](https://openjdk.org)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-blue?style=for-the-badge&logo=java)](https://openjfx.io)
[![Maven](https://img.shields.io/badge/Maven-3.9+-C71A36?style=for-the-badge&logo=apache-maven)](https://maven.apache.org)
[![UI](https://img.shields.io/badge/UI-AtlantaFX-purple?style=for-the-badge)](https://github.com/mkpaz/atlantafx)

> **EcoAdventure Desktop** is the administrative powerhouse of the EcoAdventure ecosystem. It provides a high-performance, cross-platform interface for managers and coaches to oversee operations, process data with AI, and engage with users in real-time.

---

## ✨ Advanced Features

### 📊 Management Dashboard
- **Real-time Analytics**: Interactive charts and statistics for bookings and user engagement.
- **Reporting**: Export detailed reports in **PDF (iText)** and **Excel (Apache POI)** formats.
- **Data Visualization**: Premium UI powered by **AtlantaFX** and **Ikonli**.

### 🤖 Intelligent Integrations
- **OCR (Optical Character Recognition)**: Automate data entry by scanning documents using **Tess4J**.
- **Speech Recognition**: Voice-controlled navigation and data input powered by **Vosk**.
- **QR Code Engine**: Generate and scan QR codes for attendance tracking via **ZXing**.

### 🛠️ Administrative Tools
- **User Management**: Advanced search, filtering, and role management.
- **Support Center**: Manage reclamations and provide real-time assistance.
- **Messaging**: Integrated chat system for coach-to-user communication.

### 🔐 Security & Reliability
- **BCrypt Hashing**: Synchronized security with the Symfony web platform for unified authentication.
- **Google API Integration**: Sync events with **Google Calendar** and utilize OAuth2.
- **Mailing & SMS**: Automated notifications via **Jakarta Mail** and **Infobip**.

---

## 🛠️ Technology Stack

| Component | Technologies |
| :--- | :--- |
| **Language** | Java 17 |
| **Framework** | JavaFX 21 |
| **UI Library** | AtlantaFX (Premium Themes) |
| **Build System** | Maven |
| **Networking** | Spring Boot (Web/WebSocket Clients) |
| **Security** | jBCrypt |
| **Data Processing** | Apache POI, iText, PDFBox |
| **AI / Vision** | Tess4J (OCR), Vosk (Speech), JavaCV |

---

## 📦 Getting Started

### Prerequisites
- JDK 17 or higher
- Maven 3.9+
- MySQL Server

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/CMhedi/3A3-Les_Experts.git
   cd 3A3-Les_Experts
   ```

2. **Configure Database**
   Update the database connection strings in your configuration file (usually in `src/main/resources`).

3. **Build the project**
   ```bash
   mvn clean install
   ```

4. **Run the Application**
   ```bash
   mvn javafx:run
   ```

---

## 🏷️ Topics & Keywords

`#JavaFX` `#DesktopApp` `#Java17` `#AdminDashboard` `#OCR` `#SpeechToText` `#ManagementSystem` `#EcoAdventure` `#PIDEV` `#Esprit` `#SoftwareEngineering`

---

## 👨‍💻 Contributors

- **Les Experts Team**
- **Hedi** (Architect)

---
*Developed as part of the 3A3 Integrated Project (PIDEV) at Esprit.*