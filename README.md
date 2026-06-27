# Securenet Auditor - Technical Documentation

## 1. Project Overview
Securenet Auditor is a professional-grade Android application designed for network security discovery, vulnerability assessment, and OSINT (Open Source Intelligence) gathering. It provides system administrators and security professionals with a mobile-first toolkit for auditing local and remote network infrastructures.

---

## 2. Architecture & Tech Stack
The application follows modern Android development best practices:
- **Architecture**: MVVM (Model-View-ViewModel) with a Repository pattern for clean data separation.
- **UI Framework**: 100% Jetpack Compose for a reactive and modern user interface.
- **Asynchrony**: Kotlin Coroutines and Flow for non-blocking network and database operations.
- **Dependency Management**: Manual Dependency Injection via a Centralized `AppContainer`.
- **Local Storage**: 
    - **Room DB**: For scan history, ARP tables, and speed test results.
    - **EncryptedSharedPreferences**: For sensitive data like API keys and user preferences.
- **Networking**: Retrofit 2 for REST APIs and standard Java/Kotlin Sockets/Datagrams for low-level network probing.

---

## 3. Core Application Flow

### A. Initialization Phase
1. **Application Class (`SecureNetApp`)**: On startup, it initializes the `AppContainer`.
2. **`AppContainer`**: Instantiates all singletons (Database, Repositories, API Services, Scanners) to be shared across the app.
3. **`MainActivity`**: 
    - Requests necessary runtime permissions (Location, Internet, Wi-Fi state).
    - Sets the Compose content with `SecureNetTheme`.
    - Launches the `NavGraph`.

### B. Navigation Flow (`NavGraph.kt`)
The application uses a `ModalNavigationDrawer` as the primary navigation hub, organized into four main pillars:
- **CORE**: Dashboard, Network Scanner, Security Report, Network Monitor.
- **TOOLS**: Ping, DNS, SSL, WoL, Subnet Calculator, Password Auditor, Hash Tool.
- **INTELLIGENCE**: OSINT, IP Geolocation, SNMP Inspector.
- **STORAGE**: Secure Vault.

---

## 4. Key Functional Modules

### 🛡️ Network Discovery & Mapping
- **Subnet Scanning**: Automatically detects the current Wi-Fi subnet and probes for active hosts using ICMP and ARP.
- **Port Scanning & Banner Grabbing**: Performs deep probing of discovered hosts. It identifies open ports and "grabs" service banners to identify software versions (e.g., SSH-2.0-OpenSSH_8.2p1).
- **Vendor Lookup**: Uses MAC address OUI prefixes to identify device manufacturers.

### 📊 Security Auditor & Reporting
- **Vulnerability Engine**: Analyzes scan data (open ports, service banners) to flag risks like insecure protocols (Telnet/FTP), outdated software versions, or unnecessary information exposure.
- **PDF Reporting**: Generates a professional security audit report including risk scores and detailed findings, which can be shared or archived.

### 🌐 OSINT Intelligence Engine
Integrates with multiple professional intelligence sources:
- **Have I Been Pwned**: Checks for email account breaches.
- **Hunter.io**: Domain-wide email harvesting.
- **Abuse.ch (URLHaus)**: Checks URLs against global malware/threat databases.
- **IP-API**: Detailed geolocation and ISP tracking for remote IP addresses.
- **WHOIS**: Real-time registration lookup for domains and IP ranges.

### 🔐 Secure Vault & Biometrics
- **Authentication**: Uses `BiometricPrompt` for secure hardware-backed access.
- **Storage**: Provides a protected area for scan history and sensitive network data.
- **Password Manager**: A local-only, encrypted manager for network credentials.

### 📡 background Monitoring
- **Surveillance Service**: A background `Service`/`WorkManager` that periodically scans the network for new or unauthorized devices ("Rogue APs").
- **Alerting System**: Notifies the user if a new MAC address appears on the network or if a previously known device goes offline.

---

## 5. Network Utilities Flow
1. **Ping/DNS/SSL**: Direct socket-level testing for connectivity and certificate validity.
2. **Subnet Calculator**: Logical bitwise operations to assist in IP planning.
3. **Wake-on-LAN**: Constructs and broadcasts UDP "Magic Packets" to target MAC addresses on Port 9.

---

## 6. Security Considerations
- **No Data Export**: Application data (except generated reports) remains local to the device.
- **Encrypted Storage**: Sensitive strings and keys are encrypted at rest using the Android Keystore.
- **Permission Scoping**: Fine-grained permissions are requested only when specific features (like Wi-Fi scanning) require them.
