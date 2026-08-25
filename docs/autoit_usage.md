# How to Use AutoIT Actions

The framework now supports running AutoIT scripts (compiled `.exe` files) to handle operating system level dialogs, such as file upload windows, authentication popups, or print dialogs.

## Prerequisites

1.  **Install AutoIT**: Download and install AutoIT on your machine.
2.  **Write Script**: Write your AutoIT script (e.g., `FileUpload.au3`) to handle the window.
3.  **Compile to EXE**: Right-click your `.au3` file and select **Compile Script (x64/x86)** to generate an `.exe` file (e.g., `FileUpload.exe`).

## Test Script Usage

You can trigger these scripts from your Excel/CSV test cases using the `autoit` action.

### Columns Mapping

| Column | Value to Enter | Description |
| :--- | :--- | :--- |
| **Action** | `autoit` | The command to tell the framework to run an external program. |
| **Value** | `path/to/script.exe` | The **absolute path** to your compiled `.exe` file. |
| **XPath / Context** | `"arguments"` | (Optional) Any arguments to pass to the script. Put them in quotes (e.g. `"C:\file.jpg"`). |

---

## Examples

### Example 1: Simple Script (No Arguments)
Running a script that just hits specific keys or handles a static window.

| Action | Value | XPath |
| :--- | :--- | :--- |
| `autoit` | `C:\Automation\Scripts\HandleLoginPopup.exe` | *(leave empty)* |

### Example 2: File Upload (With Arguments)
Running a script that expects a file path as an argument.

**Your AutoIT Script (`upload.au3`) might look like this:**
```autoit
ControlFocus("Open", "", "Edit1")
ControlSetText("Open", "", "Edit1", $CmdLine[1]) ; $CmdLine[1] is the first argument
ControlClick("Open", "", "Button1")
```

**Your Test Step in Excel/CSV:**

| Action | Value | XPath |
| :--- | :--- | :--- |
| `click` | | `//button[@id='upload']` | (Click button to open dialog first) |
| `autoit` | `C:\Automation\Scripts\upload_file.exe` | `C:\Images\profile_pic.jpg` | (Run script to handle dialog) |

## Notes

*   **Paths**: Always use **Absolute Paths** (e.g., `C:\...`) for both the script and the arguments to avoid "File not found" errors.
*   **Timing**: It is often good practice to add a small `wait` step before the `autoit` step to ensure the OS dialog has appeared.
*   **Arguments**: The `XPath` column is repurposed to hold the arguments string. If you need multiple arguments, pass them as a single string separated by spaces (ensure your script parses them correctly).
