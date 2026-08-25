package com.eit.automation.parser;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestStep {

	private String action;
	private String value;
	private String xpath;
	private Integer count;
	private String originalSentence;
	private int lineNumber;
	private String context;

	// Transient field to hold the actual element if found by PageFactory
	private transient Object cachedElement; // Stored as Object to avoid deep coupling in parser, handled in Executor

	public TestStep(int lineNumber, String action, String value, String xpath) {
		this.lineNumber = lineNumber;
		this.action = action;
		this.value = value;
		this.xpath = xpath;
	}
}
