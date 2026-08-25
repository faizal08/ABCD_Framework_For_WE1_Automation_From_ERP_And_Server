package com.eit.automation.core;

import com.eit.automation.parser.StepParser;
import com.eit.automation.parser.TestStep;
import java.util.List;

public class ParserCheck {
    public static void main(String[] args) {
        System.out.println("Running Comprehensive Parser Verification...");

        // 1. Verify "Verify" vs "Type" (Fixed earlier)
        check("12.Verify the Issue Type dropdown is displayed.", "verifyvisible");

        // 2. Verify Grid Value (New implementation)
        check("Verify grid value \"Status=Active\" in \"//table\"", "verifygridvalue");
        check("Verify grid \"Status=Active\"", "verifygridvalue");

        // 3. Verify 'Select' (User reported issue)
        check("Select the City from the dropdown", "click");
        check("Select \"Option\" from dropdown", "click");

        // 4. Control/Negative Cases
        check("Type \"hello\"", "type");
        check("Enter \"hello\"", "type");

        // 5. Scroll Actions
        check("Scroll down to the bottom of the page", "scrolltobottom");
        check("Scroll up to the top of the page", "scrolltotop");
        check("Scroll down", "scrolltobottom");
        check("Scroll to element \"submit\"", "scrolltoelement");
    }

    private static void check(String line, String expected) {
        List<TestStep> steps = StepParser.parseSteps(line);
        String actual = steps.isEmpty() ? "NONE" : steps.get(0).getAction();
        String result = actual.equals(expected) ? "PASS" : "FAIL";
        System.out.printf("[%s] Input: '%s' -> Got: '%s' (Expected: '%s')\n", result, line, actual, expected);
    }
}
