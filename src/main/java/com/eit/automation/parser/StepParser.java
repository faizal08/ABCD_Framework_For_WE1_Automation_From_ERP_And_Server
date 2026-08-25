package com.eit.automation.parser;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;

public class StepParser {

	// Logging configuration
	private static boolean detailedLogging = false; // Reduced verbosity

	private static Map<String, String> savedValues = new HashMap<>();

	public static void saveRuntimeValue(String key, String value) {
		if (key != null && value != null) {
			savedValues.put(key.trim(), value.trim());
			System.out.println("💾 [StepParser Saved] Key: " + key.trim() + " | Value: " + value.trim());
		}
	}

	public static String getRuntimeValue(String key) {
		if (key == null) {
			return null;
		}
		// Using .trim() here to match how keys are saved into the map
		return savedValues.get(key.trim());
	}
	private static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

	// Initializing Regex Patterns
	private static class StepPattern {
		final Pattern pattern;
		final String action;

		StepPattern(String regex, String action) {
			this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
			this.action = action;
		}
	}

	private static final List<StepPattern> ACTION_PATTERNS = new ArrayList<>();
	private static final List<StepPattern> VERIFY_PATTERNS = new ArrayList<>();

	static {

		// verify element present or not present in an given page
		ACTION_PATTERNS.add(new StepPattern(".*\\b(element_present|exists)\\b.*", "element_present"));
		ACTION_PATTERNS.add(new StepPattern(".*\\b(element_absent|not_exists)\\b.*", "element_absent"));

		// ========================================
		// MOBILE SPECIFIC ACTIONS (Prioritized to prevent generic click capture)
		// ========================================

		// Reload App (Wipes data cache completely and opens from onboarding)
		ACTION_PATTERNS.add(new StepPattern(".*\\b(reload_app|reloadapp|restart_app)\\b.*", "reload_app"));
		// Swipe / Scroll (e.g., "swipe up", "scroll down")
		ACTION_PATTERNS.add(new StepPattern(".*\\b(swipe|scroll_mobile)\\b.*", "swipe"));

		// Long Press (e.g., "long press on the logo")
		ACTION_PATTERNS.add(new StepPattern(".*\\b(long_press|longpress)\\b.*", "long_press"));

		// Keyboard Management (e.g., "hide keyboard", "close keypad")
		ACTION_PATTERNS.add(new StepPattern(".*\\b(hide\\s+keyboard|close\\s+keyboard|hide_keyboard)\\b.*", "hide_keyboard"));

		// Device Back Button (e.g., "press back", "mobile back")
		ACTION_PATTERNS.add(new StepPattern(".*\\b(mobile_back|device_back)\\b.*", "mobile_back"));

		ACTION_PATTERNS.add(new StepPattern(".*\\b(tap_if_present|tapifpresent)\\b.*", "tap_if_present"));
		// Tap (Alternative to click for mobile clarity)
		ACTION_PATTERNS.add(new StepPattern(".*\\b(tap)\\b.*", "tap"));

		// Change the single dots (.) to dot-stars (.*)
		ACTION_PATTERNS.add(new StepPattern(".*\\b(tap_coordinate|click_coordinate|tap_xy)\\b.*", "tap_coordinate"));

		ACTION_PATTERNS.add(new StepPattern(".*\\b(set_location|setlocation|spoof_gps|inject_gps)\\b.*", "set_location"));
		// ========================================
		// 5. FILE UPLOAD
		// ========================================
		ACTION_PATTERNS.add(new StepPattern(".*\\bwait\\s+for\\s+upload\\b.*", "waitforupload"));
		ACTION_PATTERNS.add(new StepPattern(".*\\b(upload|attach)\\b.*", "uploadfile"));
		ACTION_PATTERNS.add(new StepPattern(".*\\b(select|choose)\\s+file\\b.*", "uploadfile"));

		// ========================================
		// 5. SET DATE
		// ========================================
		// Add to your action keyword mapping blocks
		ACTION_PATTERNS.add(new StepPattern(".*\\b(set_date|type_date)\\b.*", "set_date"));

		// ========================================
		// 1. VERIFICATION (Highest Priority - Assertions often start sentence)
		// ========================================
		ACTION_PATTERNS.add(new StepPattern(".*\\b(verify|check|assert|ensure)\\b.*", "verify"));
		// For comparing the runtime text content of two stored framework variables
		ACTION_PATTERNS.add(new StepPattern(".*\\b(verify_variables_match|compare_variables|match_variables)\\b.*", "verify_variables_match"));

		// ========================================
		// 2. NAVIGATION (High Priority)
		// ========================================
		ACTION_PATTERNS.add(new StepPattern(".*\\b(navigate\\s+to|open.*url|go\\s+to\\s+url)\\b.*", "openurl"));
		ACTION_PATTERNS.add(new StepPattern(".*\\b(navigate|go\\s+to)\\b.*", "openurl"));

		// Map Polygon (Specific click type)
		ACTION_PATTERNS.add(new StepPattern(".*\\bdraw\\s*(?:the\\s+)?polygon\\b.*", "drawpolygon"));

		// ========================================
		// 3. SPECIFIC INTERACTIONS (Select, Type take precedence over Click)
		// ========================================

		// --- NEW KEYBOARD PATTERNS ---
		ACTION_PATTERNS.add(new StepPattern(".*\\b(arrow_down|arrow-down)\\b.*", "arrow_down"));
		ACTION_PATTERNS.add(new StepPattern(".*\\b(arrow_up|arrow-up)\\b.*", "arrow_up"));
		ACTION_PATTERNS.add(new StepPattern(".*\\b(press_enter|enter_key)\\b.*", "press_enter"));
		ACTION_PATTERNS.add(new StepPattern(".*\\b(tab_key|tab)\\b.*", "tab"));
		// --------------------------------------------

		// Clear Field (NEW)
		ACTION_PATTERNS.add(new StepPattern(".*\\b(clear|empty|remove)\\s+(text|field|value|content)\\b.*", "clear"));
		ACTION_PATTERNS.add(new StepPattern(".*\\bclear\\b.*", "clear"));

		// Select (Moved before Click to handle "Select X" properly)
		// Generic Select (often means Click) if not file/upload/radio

		ACTION_PATTERNS.add(new StepPattern(".*\\bselect\\b(?!.*(file|upload|radio)).*", "select"));

		// Input / Type (Prioritized over generic Click for "Type X")
		ACTION_PATTERNS.add(new StepPattern("^(?!.*\\bverify\\b).*\\benter\\s+(the|a|an)\\b.*", "type"));
		ACTION_PATTERNS
				.add(new StepPattern("^(?!.*\\bverify\\b).*\\benter\\b(?!.*(press\\s+enter|hit\\s+enter)).*", "type"));
		ACTION_PATTERNS.add(new StepPattern("^(?!.*\\bverify\\b).*\\btype\\b.*", "type"));
		ACTION_PATTERNS.add(new StepPattern("^(?!.*\\bverify\\b).*\\b(input|fill)\\b.*", "type"));

		// ========================================
		// 4. CLICK (Generic fallback)
		// ========================================
		// Hover / Mouse Movements (For Tailwind menus and hover dropdowns)
		ACTION_PATTERNS.add(new StepPattern(".*\\b(hover|movetoelement|mouse_over|mouseover)\\b.*", "hover"));
		ACTION_PATTERNS.add(new StepPattern(".*\\bclick\\b.*", "click"));
		ACTION_PATTERNS.add(new StepPattern(".*\\bpress\\b(?!.*(enter|tab|key|escape)).*", "click")); // Press button
		ACTION_PATTERNS.add(new StepPattern(".*\\b(click_if_present|clickifpresent)\\b.*", "click_if_present"));

		// For Extracting Data From Xpath/Locators and store in an variable
		ACTION_PATTERNS.add(new StepPattern(".*\\b(store_text|capture_value)\\b.*", "store_text"));

		// For Test Data Cleanup and Accessing Database
		ACTION_PATTERNS.add(new StepPattern(".*\\b(sql delete|db cleanup|execute sql)\\b.*", "sql_cleanup"));
		// For fetching runtime values directly from database records
		ACTION_PATTERNS.add(new StepPattern(".*\\b(fetch_db_value|get_db_value|query_db)\\b.*", "fetch_db_value"));

		// ========================================
		// 5.5 AUTOIT (System Interactions)
		// ========================================
		ACTION_PATTERNS.add(new StepPattern(".*\\b(autoit|runautoit|executeautoit)\\b.*", "autoit"));
		ACTION_PATTERNS.add(new StepPattern(".*\\b(robot|robotupload|uploadrobot)\\b.*", "robotupload"));

		// ========================================
		// 6. TOAST / ALERT ACTIONS
		// ========================================
		ACTION_PATTERNS.add(new StepPattern(".*\\bwait\\s+for\\s+toast\\b.*", "waitfortoast"));
		ACTION_PATTERNS.add(new StepPattern(".*\\btoast\\b.*success.*", "verifysuccesstoast"));
		ACTION_PATTERNS.add(new StepPattern(".*\\btoast\\b.*error.*", "verifyerrortoast"));
		ACTION_PATTERNS.add(new StepPattern(".*\\btoast\\b.*", "verifytoast"));

		ACTION_PATTERNS.add(new StepPattern(".*\\bverify\\s+notification\\b.*", "verifytoast"));
		ACTION_PATTERNS.add(new StepPattern(".*\\bverify\\s+alert\\b.*", "verifyalert"));

		// ========================================
		// 7. EXPLICIT WAITS
		// ========================================
		// --- NEW STRUCTURAL CHECKPOINT PATTERNS ---
		ACTION_PATTERNS.add(new StepPattern(".*\\b(wait_if_present|waitifpresent)\\b.*", "wait_if_present")); // 🚀 ADDED
		ACTION_PATTERNS.add(new StepPattern(".*\\b(wait_until_visible|wait_visible)\\b.*", "wait_until_visible"));
		ACTION_PATTERNS.add(new StepPattern(".*\\bwait\\s+until\\s+visible\\b.*", "wait_until_visible"));
		ACTION_PATTERNS.add(new StepPattern(".*\\bwait\\s+(for|until)\\s+visible\\b.*", "waitforvisible"));
		ACTION_PATTERNS.add(new StepPattern(".*\\bwait\\s+(for|until)\\s+clickable\\b.*", "waitforclickable"));
		ACTION_PATTERNS.add(new StepPattern(".*\\bwait\\s+(for|until)\\s+(present|element)\\b.*", "waitforpresent"));
		ACTION_PATTERNS.add(new StepPattern(".*\\bwait\\s+(for|until)\\s+disappear\\b.*", "waitfordisappear"));
		ACTION_PATTERNS.add(new StepPattern(".*\\bwait\\s+for\\s+(page|load)\\b.*", "waitforpageload"));
		ACTION_PATTERNS.add(new StepPattern(".*\\bwait\\s+for\\s+text\\b.*", "waitfortext"));
		ACTION_PATTERNS.add(new StepPattern(".*\\bwait\\b.*", "wait"));

		// ========================================
		// 8. SCROLL
		// ========================================
		ACTION_PATTERNS.add(new StepPattern(".*\\bscroll\\s+.*to\\s+.*element\\b.*", "scrolltoelement"));
		ACTION_PATTERNS.add(new StepPattern(".*\\bscroll\\s+.*(to|at)\\s+.*(top|up)\\b.*", "scrolltotop"));
		ACTION_PATTERNS.add(new StepPattern(".*\\bscroll\\s+.*(to|at)\\s+.*(bottom|down)\\b.*", "scrolltobottom")); // Matches

		ACTION_PATTERNS.add(new StepPattern(".*\\bscroll\\s+.*up\\b.*", "scrolltotop"));
		ACTION_PATTERNS.add(new StepPattern(".*\\bscroll\\s+.*down\\b.*", "scrolltobottom"));
		ACTION_PATTERNS.add(new StepPattern(".*\\bscroll\\s+by\\b.*", "scrollby"));
		ACTION_PATTERNS.add(new StepPattern(".*\\bscroll\\s+to\\b.*", "scrolltoelement"));

		// ========================================
		// 9. SWITCH / FRAMES / WINDOWS
		// ========================================
		ACTION_PATTERNS.add(new StepPattern(".*\\bswitch\\s+(to\\s+)?frame\\b.*", "switchtoframe"));
		ACTION_PATTERNS.add(
				new StepPattern(".*\\bswitch\\s+to\\s+(default|parent\\s+content)\\b.*", "switchtodefaultcontent"));
		ACTION_PATTERNS.add(new StepPattern(".*\\bswitch\\s+to\\s+parent\\s+frame\\b.*", "switchtoparentframe"));

		ACTION_PATTERNS.add(new StepPattern(".*\\bswitch\\s+to\\s+(window|tab)\\b.*", "switchtowindow"));
		ACTION_PATTERNS.add(new StepPattern(".*\\bclose\\s+(window|tab)\\b.*", "closewindow"));
		ACTION_PATTERNS.add(new StepPattern(".*\\bswitch\\s+to\\s+new\\s+window\\b.*", "switchtonewwindow"));

		ACTION_PATTERNS.add(new StepPattern(".*\\bmaximize\\b.*", "maximizewindow"));
		ACTION_PATTERNS.add(new StepPattern(".*\\bminimize\\b.*", "minimizewindow"));

		// ========================================
		// 10. SCREENSHOT
		// ========================================
		ACTION_PATTERNS.add(new StepPattern(".*\\bscreenshot\\b.*", "takescreenshot"));

		// ========================================
		// 14. BACK/FORWARD
		// ========================================
		ACTION_PATTERNS.add(new StepPattern("^\\d*[\\.\\)\\-]?\\s*back\\b.*", "back"));
		ACTION_PATTERNS.add(new StepPattern("^\\d*[\\.\\)\\-]?\\s*forward\\b.*", "forward"));
		// ========================================
		// VERIFICATION PATTERNS (Refinement)
		// ========================================

		// 1. TOAST / ALERT
		VERIFY_PATTERNS.add(new StepPattern(".*\\btoast\\b.*success.*", "verifysuccesstoast"));
		VERIFY_PATTERNS.add(new StepPattern(".*\\btoast\\b.*error.*", "verifyerrortoast"));
		VERIFY_PATTERNS.add(new StepPattern(".*\\btoast\\b.*", "verifytoast"));
		VERIFY_PATTERNS.add(new StepPattern(".*\\b(alert|notification)\\b.*", "verifyalert"));

		// 2. PAGE / TITLE / URL
		VERIFY_PATTERNS.add(new StepPattern(".*\\b(page\\s+title|title\\s+of)\\b.*contains.*", "verifytitlecontains"));
		VERIFY_PATTERNS.add(new StepPattern(".*\\b(page\\s+title|title\\s+of)\\b.*", "verifypagetitle"));
		VERIFY_PATTERNS.add(new StepPattern(".*\\b(current\\s+url|url)\\b.*contains.*", "verifyurlcontains"));
		VERIFY_PATTERNS.add(new StepPattern(".*\\b(current\\s+url|url)\\b.*", "verifyurl"));

		// 3. ELEMENT STATE
		ACTION_PATTERNS.add(new StepPattern(".*\\benabled\\b.*", "verifyenabled"));
		ACTION_PATTERNS.add(new StepPattern(".*\\bdisabled\\b.*", "verifydisabled"));
		ACTION_PATTERNS.add(new StepPattern(".*\\b(selected|checked)\\b.*", "verifyselected"));

		// 4. VISIBILITY / EXISTENCE (Negative first)
		// Complex negative visibility regex (matches "not displayed", "should not
		// appear", "hidden") - reused logic
		VERIFY_PATTERNS.add(new StepPattern(
				".*\\bnot\\s+(be\\s+)?(display|displayed|displaying|visible|appear|appears|appeared|appearing|show|shown|showing)\\b.*",
				"verifyhidden"));
		VERIFY_PATTERNS.add(new StepPattern(
				".*\\b(does|do|did|should|must|will|can)\\s+not\\s+(be\\s+)?(display|displayed|displaying|visible|appear|show)\\b.*",
				"verifyhidden"));
		VERIFY_PATTERNS.add(new StepPattern(".*\\bhidden\\b.*", "verifyhidden"));

		// Positive Visibility
		VERIFY_PATTERNS.add(new StepPattern(".*\\b(displayed|visible|appear|shown)\\b.*", "verifyvisible"));
		VERIFY_PATTERNS.add(new StepPattern(".*\\b(exists|present)\\b.*", "verifyexists"));

		// 5. TEXT / VALUE
		VERIFY_PATTERNS.add(new StepPattern(".*\\bexact\\s+text\\b.*", "verifyexacttext"));
		VERIFY_PATTERNS.add(new StepPattern(".*\\b(text|label)\\b.*contains.*", "verifycontains"));
		VERIFY_PATTERNS.add(new StepPattern(".*\\b(text|label)\\b.*", "verifytext"));
		VERIFY_PATTERNS.add(new StepPattern(".*\\bcontains\\b.*", "verifycontains"));
		VERIFY_PATTERNS.add(new StepPattern(".*\\bvalue\\b.*", "verifyvalue"));

		// 6. DATE
		VERIFY_PATTERNS.add(new StepPattern(".*\\bdate\\b.*(today|current|now).*", "verifycurrentdate"));
		VERIFY_PATTERNS.add(new StepPattern(".*\\bdate\\b.*", "verifydate"));

		// 7. MISC
		VERIFY_PATTERNS.add(new StepPattern(".*\\b(count|number\\s+of)\\b.*", "verifycount"));
		VERIFY_PATTERNS.add(new StepPattern(".*\\battribute\\b.*", "verifyattribute"));

		// 8. MAP
		VERIFY_PATTERNS.add(new StepPattern(".*\\b(polygon|map\\s+shape|map\\s+element)\\b.*", "verifypolygon"));

		// 9. GRID
		VERIFY_PATTERNS.add(new StepPattern(".*\\bverify\\s+grid\\b.*", "verifygridvalue"));

		// ========================================
		// 9. SWITCH / FRAMES / WINDOWS
		// ========================================
		// --- ADD THESE TWO LINES FOR MULTI-SESSION SUPPORT ---
		ACTION_PATTERNS.add(new StepPattern(".*\\b(switch_to|switchsession|switch_session|change_session)\\b.*", "switch_to"));
		ACTION_PATTERNS.add(new StepPattern(".*\\b(switch_to_user|switch_to_admin)\\b.*", "switch_to"));
		// -----------------------------------------------------

		ACTION_PATTERNS.add(new StepPattern(".*\\bswitch\\s+(to\\s+)?frame\\b.*", "switchtoframe"));
	}
	/**
	 * Parse test steps from text block - HANDLES ALL 75+ ACTION KEYWORDS
	 */
	public static List<TestStep> parseSteps(String textBlock) {
		logInfo("═".repeat(80));
		logInfo("STEP PARSER: Starting to parse test steps");
		logInfo("═".repeat(80));

		List<TestStep> steps = new ArrayList<>();
		String[] lines = textBlock.split("\\r?\\n");

		logInfo("Total lines to parse: " + lines.length);
		logInfo("");

		int lineNumber = 0;
		for (String line : lines) {
			lineNumber++;
			String originalLine = line;
			line = line.trim();

			if (line.isEmpty()) {
				logDebug("Line " + lineNumber + ": EMPTY - Skipping");
				continue;
			}

			logInfo("─".repeat(80));
			logInfo("LINE " + lineNumber + ": " + originalLine);
			logInfo("─".repeat(80));

			// Remove numbering (e.g., "1.", "2.") at the beginning
			String beforeNumberRemoval = line;
			line = line.replaceFirst("^\\d+\\.?\\s*", "");
			if (!beforeNumberRemoval.equals(line)) {
				logDebug("  ► Removed numbering: '" + beforeNumberRemoval + "' → '" + line + "'");
			}

			String action = "";
			String value = "";
			String xpath = "";
			String context = "";

			// CHECK FOR PIPE-DELIMITED FORMAT FIRST (Used by CSV Reader)
			// Format: Action | Value | XPath | Context
			if (line.contains("|")) {
				String[] parts = line.split("\\|");
				if (parts.length >= 1)
					action = parts[0].trim();
				if (parts.length >= 2)
					value = parts[1].trim();
				if (parts.length >= 3)
					xpath = parts[2].trim();
				if (parts.length >= 4)
					context = parts[3].trim();

				// Determine specific action type if generic "click" or "verify" etc is passed
				if (!action.isEmpty()) {
					logInfo("  ► Pipe-delimited action found: " + action);
					// Run detection again to normalize action names if needed
					String detected = detectAction(action);
					if (!detected.isEmpty()) {
						action = detected;
					}
				}

				// Create step immediately if valid
				if (!action.isEmpty()) {
					value = processPlaceholders(value);
					xpath = processPlaceholders(xpath);
					TestStep step = new TestStep(lineNumber, action, value, xpath);
					step.setOriginalSentence(originalLine);
					if (!context.isEmpty()) {
						step.setContext(context);
					}
					steps.add(step);
					continue; // Skip the rest of the loop
				}
			}

			// CHECK FOR COMMA-DELIMITED FORMAT (Excel/Custom)
			if (line.contains(",") && !line.contains("|")) {
				String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1); // Split by comma respecting quotes

				int bestActionIndex = -1;
				String bestAction = "";

				for (int i = 0; i < parts.length; i++) {
					String part = parts[i].trim();
					if (part.isEmpty())
						continue;

					String detected = detectAction(part);
					if (!detected.isEmpty()) {
						boolean isStrongMatch = part.equalsIgnoreCase(detected)
								|| part.toLowerCase().matches("^[a-z]+$") // Single word
								|| detected.equals("robotupload")
								|| detected.equals("autoit");

						if (isStrongMatch) {
							bestActionIndex = i;
							bestAction = detected;
							break; // Found a strong command, stop looking
						}
					}
				}

				if (bestActionIndex != -1) {
					logInfo("  ► Comma-delimited action found at index " + bestActionIndex + ": " + bestAction);

					value = (parts.length > bestActionIndex + 1) ? parts[bestActionIndex + 1].trim() : "";
					xpath = (parts.length > bestActionIndex + 2) ? parts[bestActionIndex + 2].trim() : "";

					if (value.isEmpty()) {
						String actionPart = parts[bestActionIndex];
						List<String> internalQuotes = extractQuotedParts(actionPart);
						if (!internalQuotes.isEmpty()) {
							value = internalQuotes.get(0);
							logInfo("  ► Extracted value from action part: " + value);
						}
					}

					// Context is everything before the action
					if (bestActionIndex > 0) {
						StringBuilder contextBuilder = new StringBuilder();
						for (int j = 0; j < bestActionIndex; j++) {
							if (j > 0)
								contextBuilder.append(", ");
							contextBuilder.append(parts[j].trim());
						}
						context = contextBuilder.toString();
					}

					// Handle Quotes in Value/XPath if present
					value = value.replaceAll("^\"|\"$", "").replace("\"\"", "\"");
					xpath = xpath.replaceAll("^\"|\"$", "").replace("\"\"", "\"");
					value = processPlaceholders(value);
					xpath = processPlaceholders(xpath);
					TestStep step = new TestStep(lineNumber, bestAction, value, xpath);
					step.setOriginalSentence(originalLine);
					if (!context.isEmpty()) {
						step.setContext(context);
					}
					steps.add(step);
					continue;
				}
			}

			// Detect action from line
			logInfo("  ► Detecting action...");
			action = detectAction(line);
			if (!action.isEmpty()) {
				logInfo("  ✓ Action detected: " + action.toUpperCase());
			} else {
				logWarning("  ✗ No action detected - skipping line");
				continue;
			}

			// Extract all quoted parts (text in "quotes")
			logInfo("  ► Extracting quoted parts...");
			List<String> quotedParts = extractQuotedParts(line);
			if (!quotedParts.isEmpty()) {
				logInfo("  ✓ Found " + quotedParts.size() + " quoted part(s):");
				for (int i = 0; i < quotedParts.size(); i++) {
					logInfo("    [" + (i + 1) + "] \"" + quotedParts.get(i) + "\"");
				}
			} else {
				logDebug("  ⚠ No quoted parts found");
			}

			// Assign value, xpath, and context from extracted parts
			for (String part : quotedParts) {
				// UPDATED: Support extraction of both traditional web XPaths AND native mobile locator prefixes
				if (part.startsWith("//") || part.startsWith("(//") ||
						part.startsWith("accessibility=") || part.startsWith("id=") || part.startsWith("automator=")) {
					xpath = part;
					logInfo("  ✓ Locator identified: " + xpath);
				} else if (value.isEmpty()) {
					value = part;
					logInfo("  ✓ Value identified: " + (value.length() > 50 ? value.substring(0, 50) + "..." : value));
				} else if (context.isEmpty()) {
					context = part;
					logInfo("  ✓ Context identified: " + (context.length() > 50 ? context.substring(0, 50) + "..." : context));
				}
			}

			// Fallback: extract xpath/locator without quotes
			if (xpath.isEmpty()) {
				logDebug("  ► Trying fallback locator extraction (without quotes)...");
				// UPDATED: Patterns updated to fetch unquoted strings for mobile strategies as well
				Matcher locatorMatcher = Pattern.compile("(//[^\\s\"]+|\\b(?:accessibility|id|automator)=[^\\s\"]+)").matcher(line);
				if (locatorMatcher.find()) {
					xpath = locatorMatcher.group(1);
					logInfo("  ✓ Locator found (unquoted): " + xpath);
				}
			}

			// Handle special cases
			if (action.equals("openurl") || action.equals("navigate") || action.equals("goto")) {
				xpath = ""; // URL goes in value, no xpath needed
				logDebug("  ► Navigation action - clearing XPath");
			}

			// For verify actions, determine specific verification type
			if (action.startsWith("verify")) {
				String originalAction = action;
				action = determineVerifyAction(line, action, value);
				if (!originalAction.equals(action)) {
					logInfo("  ✓ Refined verification type: " + originalAction + " → " + action.toUpperCase());
				}
			}

			// Fallback for hover if xpath missing
			if (action.equals("hover") && xpath.isEmpty() && !value.isEmpty()) {
				xpath = "//*[contains(text(), '" + value + "')]";
				logInfo("  ✓ Auto-generated hover XPath: " + xpath);
			}

			// Skip invalid lines
			if (action.isEmpty()) {
				logWarning("  ✗ Action is empty after processing - skipping");
				continue;
			}

			value = processPlaceholders(value);
			xpath = processPlaceholders(xpath);

			// Create test step
			TestStep step = new TestStep(lineNumber, action, value, xpath);
			step.setOriginalSentence(originalLine);
			if (!context.isEmpty()) {
				step.setContext(context);
			}
			steps.add(step);

			logInfo("  ✓ TEST STEP CREATED:");
			logInfo("    Action: " + (action != null ? action : "(null)"));
			logInfo("    Value:  " + (value != null && !value.isEmpty()
					? (value.length() > 50 ? value.substring(0, 50) + "..." : value)
					: "(empty)"));
			logInfo("    Locator: " + (xpath != null && !xpath.isEmpty()
					? (xpath.length() > 60 ? xpath.substring(0, 60) + "..." : xpath)
					: "(empty)"));
			logInfo("");
		}

		logInfo("═".repeat(80));
		logInfo("STEP PARSER: Completed");
		logInfo("Total steps parsed: " + steps.size() + " out of " + lines.length + " lines");
		logInfo("═".repeat(80));
		logInfo("");

		return steps;
	}
	/**
	 * Detect action keyword from line - COMPREHENSIVE DETECTION FOR ALL 75+ ACTIONS
	 * ORDER MATTERS: More specific actions must be checked BEFORE generic ones
	 */
	private static String detectAction(String line) {
		String cleanedLine = removeQuotedText(line);
		logDebug("    Analyzing: '" + line + "'");

		// Check full line to ensure keywords stuck inside quotes are caught properly
		String lowerLine = line.toLowerCase();

		for (StepPattern stepPattern : ACTION_PATTERNS) {
			if (stepPattern.pattern.matcher(lowerLine).matches()) {
				logDebug("    Match: " + stepPattern.action.toUpperCase() + " (Regex: " + stepPattern.pattern + ")");
				return stepPattern.action;
			}
		}

		logDebug("    ✗ No pattern matched");
		return "";
	}

	private static String removeQuotedText(String line) {
		return line.replaceAll("\"[^\"]*\"", "");
	}

	/**
	 * Determine specific verification action type - HANDLES ALL 30+ VERIFICATION
	 * TYPES
	 */
	private static String determineVerifyAction(String line, String baseAction, String value) {
		logDebug("    Refining verification type for: '" + line + "'");

		// Lowercase full line to inspect both descriptive words and locators safely
		String lowerLine = line.toLowerCase();

		for (StepPattern stepPattern : VERIFY_PATTERNS) {
			if (stepPattern.pattern.matcher(lowerLine).matches()) {
				logDebug("      → " + stepPattern.action.toUpperCase() + " (Regex: " + stepPattern.pattern + ")");
				return stepPattern.action;
			}
		}

		logDebug("      → verifyvisible (default)");
		return "verifyvisible";
	}

	/**
	 * Extract all quoted parts from line
	 */
	private static List<String> extractQuotedParts(String line) {
		List<String> parts = new ArrayList<>();
		Matcher quotedMatcher = Pattern.compile("\"([^\"]*)\"").matcher(line);

		while (quotedMatcher.find()) {
			String raw = quotedMatcher.group(1).trim();
			if (!raw.isEmpty()) {
				parts.add(raw);
			}
		}

		return parts;
	}

	/**
	 * Generate auto XPath from label/text
	 */
	public static By getAutoXpath(String label) {
		String xpathExpression = String.format(
				"//*[normalize-space()='%1$s' or @placeholder='%1$s' or @name='%1$s' or @value='%1$s' or @id='%1$s' or contains(text(), '%1$s') or @aria-label='%1$s' or @title='%1$s']",
				label);
		return By.xpath(xpathExpression);
	}

	/**
	 * Generate XPath for button elements
	 */
	public static By getButtonXpath(String label) {
		String xpathExpression = String.format(
				"//button[normalize-space()='%1$s' or @value='%1$s' or contains(text(), '%1$s')] | //input[@type='button' or @type='submit'][@value='%1$s']",
				label);
		return By.xpath(xpathExpression);
	}

	/**
	 * Generate XPath for input fields
	 */
	public static By getInputXpath(String label) {
		String xpathExpression = String.format(
				"//input[@placeholder='%1$s' or @name='%1$s' or @id='%1$s'] | //textarea[@placeholder='%1$s' or @name='%1$s' or @id='%1$s'] | //label[contains(text(), '%1$s')]/following::input[1]",
				label);
		return By.xpath(xpathExpression);
	}

	/**
	 * Generate XPath for links
	 */
	public static By getLinkXpath(String label) {
		String xpathExpression = String
				.format("//a[normalize-space()='%1$s' or contains(text(), '%1$s') or @title='%1$s']", label);
		return By.xpath(xpathExpression);
	}

	/**
	 * Generate XPath for toast/notification messages
	 */
	public static By getToastXpath(String message) {
		String xpathExpression = String.format(
				"//*[(contains(@class,'toast') or contains(@class,'notification') or contains(@class,'alert') or contains(@class,'message')) and contains(text(), '%1$s')]",
				message);
		return By.xpath(xpathExpression);
	}
	/**
	 * Parse a single test step line
	 */
	public static TestStep parseSingleStep(String line) {
		List<TestStep> steps = parseSteps(line);
		return steps.isEmpty() ? null : steps.get(0);
	}

	/**
	 * Validate if a line contains a valid action
	 */
	public static boolean isValidStep(String line) {
		if (line == null || line.trim().isEmpty()) {
			return false;
		}
		line = line.trim().replaceFirst("^\\d+\\.?\\s*", "");
		String action = detectAction(line);
		return !action.isEmpty();
	}

	/**
	 * Extract action keyword from line
	 */
	public static String extractAction(String line) {
		if (line == null || line.trim().isEmpty()) {
			return "";
		}
		line = line.trim().replaceFirst("^\\d+\\.?\\s*", "");
		return detectAction(line);
	}

	/**
	 * Extract value from line
	 */
	public static String extractValue(String line) {
		List<String> parts = extractQuotedParts(line);
		for (String part : parts) {
			// UPDATED: Skip over mobile locators as well so they don't get misidentified as a text value
			if (!part.startsWith("//") && !part.startsWith("(//") &&
					!part.startsWith("accessibility=") && !part.startsWith("id=") && !part.startsWith("automator=")) {
				return part;
			}
		}
		return "";
	}

	/**
	 * Extract locator (xpath/id/accessibility) from line
	 */
	public static String extractXpath(String line) {
		List<String> parts = extractQuotedParts(line);
		for (String part : parts) {
			// UPDATED: Instantly return if the quoted part matches a web or native mobile strategy
			if (part.startsWith("//") || part.startsWith("(//") ||
					part.startsWith("accessibility=") || part.startsWith("id=") || part.startsWith("automator=")) {
				return part;
			}
		}

		// UPDATED: Fallback matcher tracking unquoted web XPaths or native mobile identifiers
		Matcher locatorMatcher = Pattern.compile("(//[^\\s\"]+|\\b(?:accessibility|id|automator)=[^\\s\"]+)").matcher(line);
		if (locatorMatcher.find()) {
			return locatorMatcher.group(1);
		}
		return "";
	}
	// ========================================
	// LOGGING HELPER METHODS
	// ========================================

	private static void logInfo(String message) {
		if (detailedLogging) {
			System.out.println("[PARSER " + getCurrentTime() + "] " + message);
		}
	}

	private static void logDebug(String message) {
		if (detailedLogging) {
			System.out.println("[PARSER " + getCurrentTime() + "] " + message);
		}
	}

	private static void logWarning(String message) {
		if (detailedLogging) {
			System.out.println("[PARSER " + getCurrentTime() + "] ⚠ WARNING: " + message);
		}
	}

	private static String getCurrentTime() {
		return LocalDateTime.now().format(timeFormatter);
	}

	/**
	 * Enable or disable detailed logging
	 */
	public static void setDetailedLogging(boolean enabled) {
		detailedLogging = enabled;
		if (enabled) {
			logInfo("Detailed logging enabled");
		}
	}
	/**
	 * Process placeholders and handle Save/Reuse logic
	 */
	public static String processPlaceholders(String value) {
		if (value == null || value.isEmpty()) return value;

		// --- 1. SAVE LOGIC (e.g., "plumber_{randomAlpha} >> savedName") ---
		if (value.contains(">>")) {
			String[] parts = value.split(">>");
			String template = parts[0].trim();
			String variableName = parts[1].trim();

			// We call the helper method here
			String generatedValue = replaceAllPlaceholders(template);

			savedValues.put(variableName, generatedValue);
			return generatedValue;
		}

		// --- 2. REUSE LOGIC (Check if the value is a saved variable) ---
		for (String key : savedValues.keySet()) {
			if (value.contains("{" + key + "}")) {
				value = value.replace("{" + key + "}", savedValues.get(key));
			}
		}

		// --- 3. STANDARD LOGIC (One-time use) ---
		return replaceAllPlaceholders(value);
	}

	/**
	 * HELPER METHOD: This is the central brain for all tags.
	 * If you want to add a new tag, you ONLY add it here.
	 */
	private static String replaceAllPlaceholders(String value) {
		if (value == null) return null;

		// Fix for Plumber issue: Generate random LETTERS only
		if (value.contains("{randomAlpha}")) {
			value = value.replace("{randomAlpha}", generateRandomString(6));
		}

		// Standard Timestamp (Numbers)
		if (value.contains("{timestamp}")) {
			value = value.replace("{timestamp}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddHHmmss")));
		}

		// Random Phone logic
		if (value.contains("{randomPhone}")) {
			long firstDigit = (long) (Math.random() * 3) + 7;
			long remainingNine = (long) (Math.floor(Math.random() * 900_000_000L) + 100_000_000L);
			String phone = String.valueOf(firstDigit) + String.valueOf(remainingNine);
			value = value.replace("{randomPhone}", phone);
		}

		return value;
	}

	/**
	 * Internal method to generate random uppercase letters
	 */
	private static String generateRandomString(int length) {
		String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		StringBuilder sb = new StringBuilder();
		Random random = new Random();
		for (int i = 0; i < length; i++) {
			sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
		}
		return sb.toString();
	}

	/**
	 * 🚀 LIVE EXTRACTED VARIABLE INJECTOR FOR TARGET XPATHS
	 * Scans resolved property file strings and injects runtime saved values
	 */
	public static String replaceSavedVariablesOnly(String xpath) {
		if (xpath == null || xpath.isEmpty()) return xpath;

		// Iterates over all runtime values memorized via the ">>" syntax
		for (Map.Entry<String, String> entry : savedValues.entrySet()) {
			String placeholder = "{" + entry.getKey() + "}";
			if (xpath.contains(placeholder)) {
				xpath = xpath.replace(placeholder, entry.getValue());
			}
		}
		return xpath;
	}
}