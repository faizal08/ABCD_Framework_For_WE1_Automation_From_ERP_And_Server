<div align="center">

# ⚙️ ABCD HYBRID AUTOMATION FRAMEWORK FOR MOBILE AND WEB TESTING
### *Keyword-Driven Selenium & Appium Testing Engine*

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Selenium](https://img.shields.io/badge/Selenium-4.x-43B02A?style=for-the-badge&logo=selenium&logoColor=white)](https://www.selenium.dev/)
[![Appium](https://img.shields.io/badge/Appium-3.x-662d91?style=for-the-badge&logo=appium&logoColor=white)](http://appium.io/)
[![JCodec](https://img.shields.io/badge/Video-JCodec--0.2.5-blue?style=for-the-badge)](https://jcodec.org/)
[![Maven](https://img.shields.io/badge/Build-Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Android Studio](https://img.shields.io/badge/Android%20Studio-3DDC84?style=for-the-badge&logo=android-studio&logoColor=white)](https://developer.android.com/studio)
[![Appium Inspector](https://img.shields.io/badge/Appium%20Inspector-662D91?style=for-the-badge&logo=appium&logoColor=white)](https://github.com/appium/appium-inspector)
[![Cloudinary](https://img.shields.io/badge/Cloudinary-3448C5?style=for-the-badge&logo=cloudinary&logoColor=white)](https://cloudinary.com/)

**A Seamless Multi-Platform Testing Engine for Web, User Apps, and Driver Apps.**
</div>

---

## 📖 Overview
The **ABCD Hybrid Framework** is an advanced evolution of the keyword-driven approach, designed to handle complex ecosystems where **Web Portals** and **Mobile Applications** (Android/iOS) must work in sync. Whether you are validating an Admin dashboard on Chrome or a Driver's And Customers real-time response and interaction on an Emulator, this framework manages all sessions through a single Excel-based logic using selenium and appium .Also this framework can also run test cases stored in erp database through API Calls.
The core philosophy is driven by the company motive: **"Any Body Can Develop"**—allowing even non-technical to build bulletproof automation scripts without writing code.

![Framework Screenshot](framework_screenshot.png)
---

## 📸 Test Reporting Preview
The framework generates rich and interactive HTML reports with video recordings for each tests. Each test step includes automatic failure screenshots for rapid debugging.

![Test Report Screenshot](report_screenshot.png)

---

## 🏗️ Hybrid Architecture
This framework operates on a **Multi-Session Driver Pool** logic:
* **Parallel Driver Management:** Maintains simultaneous connections to ChromeDriver and multiple Appium sessions (e.g., `web`, `user`, and `driver`).
* **Dynamic Context Switching:** Automatically switches focus between the browser and mobile devices based on the test step.
* **Live Browser Overlay:** A custom-built JavaScript dashboard injected into the Web session that provides real-time status updates of mobile actions.

---

## ✨ Core Features

* **🚀 Unified Keyword Engine:** Use the same `click`, `type`, and `verify` keywords for both Web and Mobile.
* **🖥️ Live Command Center:** Real-time overlay on the browser showing the current Step Number, Action, and Active Role (USER/DRIVER/WEB).
* **📱 Multi-App Orchestration:** Seamlessly test flows that start on Web (Super Admin), move to a User App, and end on a Driver App.
* **🎲 Intelligent Data Generation:** Built-in placeholders for `{timestamp}`, `{randomAlpha}`, and `{randomPhone}`.
* **💾 Data Persistence:** Capture values from one platform (e.g., an Order ID from Mobile) and use it on another (e.g., searching on Web) using the `>>` operator.
* **🎥 Synchronized Media Logs:** Automated video recording (MP4) and failure screenshots for every session.
* **🗄️ Database Integration:** Direct PostgreSQL connectivity for cleaning up test data or verifying backend records.
* **📱 Native Multi-Locator Fallbacks:** Seamlessly accepts both standard web **XPaths** and native, high-speed Appium identifier patterns (**Accessibility IDs**, **Resource IDs**, and **Android UI Automator expression strings**) in the exact same sheet column.

---

## 💻 Tech Stack
| Component | Technology                   |
| :--- |:-----------------------------|
| **Language** | Java 17                      |
| **Web Engine** | Selenium WebDriver 4.27.0    |
| **Mobile Engine** | Appium Java Client 9.3.0     |
| **Automation Server** | Appium v3.4.2                |
| **IDE / Emulator Manager** | Android Studio Panda 4       |
| **Element Locator** | Appium Inspector             |
| **Reporting** | Extent Reports / Custom HTML |
| **Video Encoding** | JCodec 0.2.5                 |
| **Database** | PostgreSQL                   |

---

## 🚦 Getting Started

### 1. Prerequisites (Installation)
To run this framework, you must have the following installed on your local machine:
* **Java Development Kit (JDK) 17+**
* **Node.js & NPM** (For Appium)
* **Appium Server:** `npm install -g appium`
* **UiAutomator2 Driver:** `appium driver install uiautomator2`
* **Android SDK & Build Tools** (Set `ANDROID_HOME` environment variable)
* **Android Emulator** (Via Android Studio)
* **Appium Inspector** 

---

## 2. 📱 Recommended Emulator Configuration

| Setting | Selection |
| :--- | :--- |
| **Hardware** | Pixel 4 |
| **API Level** | API 33 ("Tiramisu"; Android 13.0) |
| **Services** | **Google APIs** *(Do not pick Google Play Store)* |
| **System Image** | Google APIs Intel x86_64 (or ARM64) Atom System Image |
| **Graphics (Advanced)** | Hardware - GLES 2.0 |
| **Boot Option (Advanced)** | Cold Boot *(Avoid Quick Boot to prevent memory leak retention)* |

> ⚠️ **WARNING:** Please ensure you use these exact emulator settings to guarantee optimized framework performance and prevent memory leak issues during extended test automation runs.

------

### 3. Framework Setup
1.  **Clone the Repository:**
    ```bash
    git clone [https://github.com/faizal08/ABCD_Hybrid_Framework.git](https://github.com/faizal08/ABCD_Hybrid_Framework.git)
    ```
2.  **Install Maven Dependencies:**
    ```bash
    mvn clean install
    ```

### 4. Running the Hybrid Suite
The framework uses system properties to point to specific configuration files:

| Environment          | CLI Command                          |
|:---------------------|:-------------------------------------|
| **WE1 Super Admin**  | `mvn exec:java -Denv=we1_superadmin` |
| **Hybrid Full Flow** | `mvn exec:java -Denv=we1_hybrid`     |

---

## ⚠️ CRITICAL: MONITOR SCREEN ALIGNMENT SETUP
> **🚨 MUST-READ BEFORE RUNNING HYBRID TESTS**
>
> The ABCD Framework uses an **Adaptive Initial Resizer Engine** for split-screen parallel execution.
> * When a hybrid test is detected, the **Web Browser automatically launches and locks itself to the LEFT side of the monitor** (Coordinates: `X=0, Width=960, Height=1080`).
> * **Action Required:** Before launching your tests, you **MUST manually move and align all Android Emulators (User, Driver, Store) to the RIGHT side of your monitor screen (Starting from X=960 onwards).**
>
> *Failure to arrange emulators to the right will cause the Chrome browser window to overlap your active mobile devices during the automation layout phase, blocking real-time visual tracking.*

---

## 📊 Live Dashboard Preview
The framework injects a high-visibility overlay into your browser during execution. It dynamically detects the active role:
* **💻 WEB:** Active when interacting with the Admin Portal.
* **📱 USER:** Active when performing actions on the Customer App.
* **📱 DRIVER:** Active when performing actions on the Driver App.

---

## 🏗️ Architecture & Logic
The framework is built on a modular "Plug-and-Play" design:

* **Action Keywords:** A library of reusable functions (e.g., `click`, `type`, `verify`) that handle browser interactions.
* **Execution Engine:** The core logic that reads the test steps and invokes the corresponding action keywords.
* **Data Controller:** Manages the input from Excel/Property files using Apache POI.
* **Locator Repository:** Centralized storage for element locators (XPath, CSS) to ensure easy maintenance.
* **Media Controller:** Handles **Screen Recording (AVI/MP4)** of every test session for audit trails.

---

## 🎲 Dynamic Data Generation
The framework features a built-in engine to generate unique values on-the-fly. This is essential for testing "Create" forms (like User Registration or Driver Onboarding) where unique names or IDs are required.

| Placeholder | logic | Output Example | Best Used For |
| :--- | :--- | :--- | :--- |
| **`{timestamp}`** | Current Time (Numeric) | `30174522` | Unique IDs, Tracking Numbers |
| **`{randomAlpha}`** | Random Letters (A-Z) | `KJHBTX` | **Plumber/Driver Names**, Usernames |
| **`{randomPhone}`** | 10-Digit Numeric | `9876543210` | Mobile Number fields |

---

### 💾 Data Persistence (Save & Reuse)
You can capture a generated value and reuse it in later steps using the `>>` operator:
1. **Save:** `NewUser{randomAlpha} >> myUser` (Generates `NewUserABC` and saves it).
2. **Reuse:** Use `{myUser}` in any following step to type the exact same name.

---
## 🚀 Advanced Execution (CLI)
You can switch environments without modifying any configuration files by using the `-Denv` system property:

| Target Environment | CLI Command |
| :--- | :--- |
| **Default Config** | `mvn exec:java -Denv=config` |
| **ERP System** | `mvn exec:java -Denv=erp` |
| **WE1 Super Admin** | `mvn exec:java -Denv=we1_superadmin` |
| **Hybrid Full Flow** | `mvn exec:java -Denv=we1_hybrid`     |
---

## ✨ Key Features
* **🚀 High Reusability:** Write a keyword once and use it across hundreds of test cases.
* **📊 External Configuration:** Manage test execution flows via Excel or Properties files without changing code.
* **🔍 Robust Logging:** Detailed console logs for every action performed during execution.
* **🛠️ Error Handling:** Built-in try-catch blocks and explicit waits to handle synchronization issues.
* **🎥 Automated Video Logs:** Every test execution is recorded and saved to `test-outputs/videos`. Perfect for reviewing "flaky" tests that pass locally but fail in CI.
* **🗄️ Automated DB Cleanup:** Direct PostgreSQL integration to delete/update test records after execution, keeping environments clean.
* **🌍 Multi-Environment Support:** Run tests against different sites (ERP, Admin, User,Hybrid) instantly using dynamic CLI arguments.

---

---

## 🌐 Frappe ERP API Integration & Execution

The **ABCD Framework** features direct integration with **Frappe ERP**. Instead of relying solely on static, local Excel files, the framework can fetch test cases directly from an ERP database via REST API calls at runtime, dynamically parse the test step JSON, build an in-memory POI Workbook, and execute the suite seamlessly.

---

### ⚙️ 1. ERP Configuration Setup

To enable ERP execution, configure the following properties inside your target `.properties` file (e.g., `EPIC_0001_Platform_Configuration.properties` in your project root):

```properties
# ==========================================
# FRAPPE ERP API CONFIGURATION
# ==========================================
# Base URL for the Frappe ERP instance
erp.base.url=https://erp.qa.ethicalintelligent.com

# Target Epic or Backlog details for dynamic automation retrieval
erp.target.type=Epic
erp.target.name=EPIC-0001

```

### 📩 2. ERP API Payload & JSON Schema

When querying the Frappe ERP endpoint (`/api/method/...`), the client sends a structured payload specifying the desired target `type` and `names` (Epics/Backlogs):

```json
{
    "type": "Epic",
    "names": [
        "EPIC-0001"
    ]
}
```
#### Expected API JSON Response Structure
The framework automatically parses the returned JSON payload into test sheets and step blocks:

```json
{
  "message": {
    "status": "success",
    "epic_id": "EPIC-0001",
    "sheets": [
      {
        "sheet_name": "UserDriverWebIntialilzer",
        "test_cases": [
          {
            "test_case_id": "TC_RG_01",
            "test_case_name": "TC_RG_01_Super_Admin_Login",
            "step_block": "SWITCH_TO: WEB\nTYPE: id=email -> super_admin@gmail.com\nTYPE: id=password -> Admin@123\nCLICK: xpath=//button[@type='submit']",
            "precondition": ""
          }
        ]
      }
    ]
  }
}
```
### 🔀 3. Data Source Toggling (`-Dsource`)

The framework dynamically routes test case loading depending on the source flag:

| Execution Mode | Parameter | Description |
| :--- | :--- | :--- |
| **Local Excel / CSV** | `-Dsource=excel` *(Default)* | Reads static files directly from local disk path (`excel.name`). |
| **Frappe ERP API** | `-Dsource=erp` | Connects to `erp.base.url`, pulls live JSON for `erp.target.name`, generates an in-memory POI Workbook, and executes directly. |

### 🚀 4. Running Tests via ERP API (Command Line)

Pass both the environment configuration file (`-Denv`) and the dynamic ERP source flag (`-Dsource=erp`):

#### 🔹 Run Specific Epic via ERP API:
```bash
mvn clean exec:java -Denv=EPIC_0001_Platform_Configuration -Dsource=erp
```
-----------

## ☁️ Cloudinary Cloud Storage & Auto-Cleanup Integration

The **ABCD Framework** features built-in Cloudinary integration to offload test artifacts (screenshots and failure execution videos) to the cloud. This ensures reports are lightweight, globally shareable, and accessible without requiring local file paths.

---

### 🔑 Cloudinary API Keys & Configuration

All Cloudinary API configuration and credentials are managed directly inside the `CloudinaryUploader` utility class:

📍 **Configuration Location:** `com.eit.automation.utils.CloudinaryUploader`

```java
public class CloudinaryUploader {

    // 🔑 Cloudinary Account Credentials
    private static final String CLOUD_NAME = "{add you cloudinary name here}";
    private static final String API_KEY    = "{add you cloudinary api key here}";
    private static final String API_SECRET = "{add you cloudinary api seceret here}";

    // 📁 Target Cloud Directory
    // All media uploaded by the framework is routed to this folder:
    // "automation-reports"
}
```

> ⚠️ **IMPORTANT CREDENTIAL MANAGEMENT NOTE:**  
> If you plan to rotate or update your Cloudinary credentials, modify the `CLOUD_NAME`, `API_KEY`, and `API_SECRET` fields inside `CloudinaryUploader.java`.

---

### ⚙️ How Cloud Artifact Upload Works

1. **Zero Execution Latency (Post-Suite Batch Upload):**  
   To prevent network delays from slowing down active Selenium and Appium test steps, screenshots and videos are saved locally during the run.
2. **High-Speed Parallel Upload:**  
   Once the entire test suite completes (`reportGenerator.endTestExecution()`), the framework triggers `CloudinaryUploader.uploadBatchInParallel(...)` using a **30-thread parallel executor pool** to upload all gathered artifacts simultaneously.
3. **HTML Report Synchronization:**  
   After uploads complete, local media paths in the generated HTML report are automatically transformed into hosted, secure Cloudinary HTTPS links (`https://res.cloudinary.com/...`).

> 🛑 **DO NOT TERMINATE PROCESS EARLY:**  
> Allow the console process to finish after tests complete. You will see the following confirmation message once batch uploading and report syncing are finalized:  
> `✅ HTML report updated with hosted Cloudinary links!`

---

### 🧹 Automated 2-Day Storage Cleanup

To prevent Cloudinary storage limits from filling up, `CloudinaryUploader` includes an automated, background-scheduled task that cleans up old test artifacts:

* **Cleanup Frequency:** Automatically triggers every **2 days**.
* **Retention Policy:** Searches the `automation-reports` folder for all `image` and `video` assets created more than 2 days ago and purges them from the Cloudinary repository via the Search API.

---

### 🗜️ Video & Image Compression Techniques

To maintain maximum upload speed and adhere to storage quotas, the framework applies lightweight compression prior to upload:

* **Image Screenshots:** Captures compressed JPEG files (~60 KB each) instead of heavy PNGs, allowing fast, parallel batch transfers over standard HTTP connections.
* **Video Screen Recordings:** Rendered using **JCodec (0.2.5)** with an optimized frame rate and standard dimensions to ensure video logs stay compact while maintaining crisp visual debugging clarity.

---

### 🚨 CRITICAL CLOUDINARY FILE SIZE WARNINGS

> ⚠️ **WARNING: STRICT 100 MB PER-FILE UPLOAD LIMIT**
>
> Cloudinary Free/Standard tier enforces a **strict 100 MB maximum size limit per single uploaded video or media file**.
>
> * **DO NOT** record continuous multi-hour test sessions without stopping between suites.
> * Keep individual test case executions under control so that the resulting JCodec MP4 file stays **well under 100 MB**.
> * Videos exceeding 100 MB will trigger an `HTTP 400 / File size limit exceeded` error from Cloudinary and will fail to sync with your HTML report.

> 💡 **PRO TIP FOR LONG-RUNNING TEST SUITES:**  
> If executing long-running test suites, keep video recordings scope-bound per test block or disable video recording for passing tests to save bandwidth and stay comfortably under Cloudinary limits.

## 💻 Tech Stack
| Component | Technology |
| :--- | :--- |
| **Language** | Java 17 |
| **Automation Engine** | Selenium WebDriver |
| **Data Management** | Apache POI / Properties |
| **Video Encoding** | **JCodec 0.2.5** (Native Java) |
| **Build Tool** | Maven |

---

- ## 📚 Documentation
- [Main Framework Guide](README.md)
- [Keywords & Excel Writing Guide](KEYWORDS_REFRENCE.md) <-- Click to go to keyword Refrence file here!

---

## 📧 Contact
- **Developer:** [Faizal](https://github.com/faizal08)
- **Email:** [reachfaizal08@gmail.com](mailto:reachfaizal08@gmail.com)
