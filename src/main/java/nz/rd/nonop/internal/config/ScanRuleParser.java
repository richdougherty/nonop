// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.internal.config;

import nz.rd.nonop.internal.logging.NonopLogger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScanRuleParser {

    private final NonopLogger logger;

    private static final Pattern COMMENT_PATTERN = Pattern.compile("^(.*?)(?:\\s*(?:#|//).*)?$");

    // Pattern to split by common separators, tolerant of multiple spaces.
    private static final Pattern RULE_SEPARATOR_PATTERN = Pattern.compile("[\\s,;]+");

    // Java identifier patterns
    private static final String JAVA_IDENTIFIER = "[a-zA-Z_$][a-zA-Z0-9_$]*";
    private static final String PACKAGE_PART = JAVA_IDENTIFIER; // Used for segments of a package name
    private static final String CLASS_NAME_CONVENTION = "[A-Z][a-zA-Z0-9_$]*"; // Class names conventionally start with uppercase

    // Compiled regex patterns
    private static final Pattern MATCH_ALL = Pattern.compile("^(?:\\*)+$");

    private static final Pattern EXPLICIT_PACKAGE_PREFIX = Pattern.compile(
            // e.g., pkg.sub.** or com.example.
            "^(" + PACKAGE_PART + "(?:\\." + PACKAGE_PART + ")*)(?:\\.(?:\\**))$"
    );

    // This pattern matches any valid Java qualified identifier (e.g., "pkg.name", "MyClass", "org.MyCompany.util")
    // It should be used as a fallback to identify implicit package prefixes.
    private static final Pattern QUALIFIED_JAVA_IDENTIFIER = Pattern.compile(
            "^(?:" + JAVA_IDENTIFIER + ")(?:\\." + JAVA_IDENTIFIER + ")*$"
    );


    private static final Pattern EXPLICIT_CLASS_SUFFIX = Pattern.compile(
            // e.g., *.ClassName or .ClassName
            "^(?:(?:\\**)\\.|\\.)(" + JAVA_IDENTIFIER + ")$"
    );

    private static final Pattern IMPLICIT_CLASS_NAME = Pattern.compile(
            // e.g., ClassName (must start with uppercase, no dots)
            "^" + CLASS_NAME_CONVENTION + "$"
    );

    private static final Pattern FULLY_QUALIFIED_CLASS = Pattern.compile(
            // e.g., pkg.sub.ClassName (last part must follow CLASS_NAME_CONVENTION)
            "^(" + PACKAGE_PART + "(?:\\." + PACKAGE_PART + ")*)\\.(" + CLASS_NAME_CONVENTION + ")$"
    );

    public ScanRuleParser(NonopLogger logger) {
        this.logger = logger;
    }

    /**
     * Parses a string containing multiple scan rules, potentially over multiple lines.
     *
     * @param rulesInput The string containing scan rules.
     * @return A list of {@link ScanMatcher}s. Returns an empty list if rulesInput is null or empty.
     */
    public List<ScanMatcher> parse(@NonNull String rulesInput) {
        List<ScanMatcher> matchers = new ArrayList<>();
        if (rulesInput == null || rulesInput.trim().isEmpty()) {
            return matchers; // Return empty list for null or effectively empty input
        }

        String[] lines = rulesInput.split("\\r?\\n"); // Split by newline characters
        for (String line : lines) {
            matchers.addAll(parseLine(line));
        }
        return matchers;
    }

    /**
     * Parses a single line of text which may contain multiple comma or semicolon-separated rules.
     * Handles comments starting with '#' or '//'.
     *
     * @param lineString The line string to parse.
     * @return A list of {@link ScanMatcher}s found on the line.
     */
    @NonNull List<ScanMatcher> parseLine(@NonNull String lineString) {
        // Strip comments
        String processedLine = lineString;
        Matcher matcher = COMMENT_PATTERN.matcher(processedLine);
        if (matcher.matches()) {
            processedLine = matcher.group(1);
        }
        processedLine = processedLine.trim();

        if (processedLine.isEmpty()) { // No need to check for startsWith("#") or startsWith("//") if comments are stripped first
            return Collections.emptyList();
        }

        List<ScanMatcher> scanMatchers = new ArrayList<>();
        String[] ruleStrings = RULE_SEPARATOR_PATTERN.split(processedLine);
        for (String ruleString : ruleStrings) {
            String trimmedRuleString = ruleString.trim(); // Trim individual rule strings again, split might leave whitespace if not part of separator
            if (trimmedRuleString.isEmpty()) { // Skip empty strings that might result from multiple separators like ",,"
                continue;
            }
            @Nullable ScanMatcher sm = parseSingleRule(trimmedRuleString);
            if (sm != null) {
                scanMatchers.add(sm);
            }
        }
        return scanMatchers;
    }


    /**
     * Parses a single rule string into a {@link ScanMatcher}.
     *
     * @param ruleString The non-null, non-empty, trimmed rule string.
     * @return A {@link ScanMatcher} or null if the ruleString is effectively empty after trimming.
     * @throws IllegalArgumentException if the ruleString is invalid.
     */
    @Nullable
    ScanMatcher parseSingleRule(@NonNull String ruleString) {
        String trimmedRuleString = ruleString.trim(); // Although called with trimmed, an extra trim is safe.

        if (trimmedRuleString.isEmpty()) {
            return null; // No pattern = no matcher
        }

        if (trimmedRuleString.startsWith("!")) {
            @Nullable ScanMatcher subMatcher = parseSingleRule(trimmedRuleString.substring(1));
            if (subMatcher == null) {
                throw new IllegalArgumentException(
                        "[ScanRuleParser] Invalid rule string: '" + ruleString +
                                "'. Negation cannot be applied to an empty or null rule."
                );
            }
            return new ScanMatcher.NotMatcher(subMatcher, trimmedRuleString);
        }

        // Match all wildcard: "*" or "**" etc.
        if (MATCH_ALL.matcher(trimmedRuleString).matches()) {
            return new ScanMatcher.MatchAllMatcher(trimmedRuleString);
        }

        // Explicit package prefix: "pkg.", "pkg.**", "pkg.sub.**"
        Matcher explicitPackageMatcher = EXPLICIT_PACKAGE_PREFIX.matcher(trimmedRuleString);
        if (explicitPackageMatcher.matches()) {
            String packageName = explicitPackageMatcher.group(1);
            return new ScanMatcher.PackagePrefixMatcher(packageName, trimmedRuleString);
        }

        // Explicit class suffix: "*.ClassName", ".ClassName"
        Matcher explicitClassMatcher = EXPLICIT_CLASS_SUFFIX.matcher(trimmedRuleString);
        if (explicitClassMatcher.matches()) {
            String className = explicitClassMatcher.group(1);
            return new ScanMatcher.ClassNameSuffixMatcher(className, trimmedRuleString);
        }

        // Fully qualified class name: "pkg.ClassName", "pkg.sub.ClassName"
        Matcher fqcnMatcher = FULLY_QUALIFIED_CLASS.matcher(trimmedRuleString);
        if (fqcnMatcher.matches()) {
            // group(1) is package, group(2) is class. The full match is the FQCN string.
            return new ScanMatcher.FqcnMatcher(trimmedRuleString, trimmedRuleString);
        }

        // Implicit class name: "ClassName" (starts with uppercase, no dots)
        if (IMPLICIT_CLASS_NAME.matcher(trimmedRuleString).matches()) {
            return new ScanMatcher.ClassNameSuffixMatcher(trimmedRuleString, trimmedRuleString);
        }

        // Implicit package prefix
        if (QUALIFIED_JAVA_IDENTIFIER.matcher(trimmedRuleString).matches()) {
            // This rule string is a valid qualified Java identifier (e.g., "my.package", "org.MyCompany.utils", "simpleName")
            // and it wasn't matched as FQCN or Implicit Class Name, so treat as package prefix.
            return new ScanMatcher.PackagePrefixMatcher(trimmedRuleString, trimmedRuleString);
        }

        // If none of the patterns match, the rule string is considered invalid.
        // FIXME: Error message prefix should be consistent, e.g., [ScanRuleParser]
        throw new IllegalArgumentException(
                "[ScanRuleParser] Invalid rule string: '" + ruleString +
                        "'. Pattern does not match any recognized format."
        );
    }
}