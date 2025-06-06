package nz.rd.nonop.config.scan;

import nz.rd.nonop.internal.util.NonopConsoleLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

class ScanRuleParserTest {

    private final ScanRuleParser scanRuleParser = new ScanRuleParser(new NonopConsoleLogger(true));

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "\t", "\n"})
    @DisplayName("parseSingleRule: Empty or whitespace rule string should return null (no matcher)")
    void testParseSingleRule_withEmptyOrWhitespaceRuleString(String ruleString) {
        ScanMatcher result = scanRuleParser.parseSingleRule(ruleString);
        assertThat(result, nullValue());
    }

    @ParameterizedTest
    @CsvSource({
            "!*, *",
            "!pkg, pkg",
            "!*.pkg, *.pkg",
            "!Class, Class",
            "!!*, !*"
    })
    @DisplayName("parseSingleRule: Explicit package prefix rule string should create PackagePrefixMatcher")
    void testNotRuleContentMatches(String ruleString, String expectedSubRuleString) {
        ScanMatcher result = scanRuleParser.parseSingleRule(ruleString);

        assertThat(result, instanceOf(ScanMatcher.NotMatcher.class));
        ScanMatcher expectedSubMatcher = scanRuleParser.parseSingleRule(expectedSubRuleString);
        assertThat(((ScanMatcher.NotMatcher) result).getInnerMatcher(), equalTo(expectedSubMatcher));
    }

    @ParameterizedTest
    @ValueSource(strings = {"!", "! ", "!\t"})
    @DisplayName("parseSingleRule: Negation of empty or whitespace rule should throw IllegalArgumentException")
    void testParseSingleRule_negationOfEmptyRule(String ruleString) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> scanRuleParser.parseSingleRule(ruleString));
        assertThat(ex.getMessage(), containsString("Negation cannot be applied to an empty or null rule."));
        assertThat(ex.getMessage(), containsString("'" + ruleString + "'"));
    }

    @Test
    @DisplayName("parseSingleRule: Negation of an invalid rule (e.g., '!123invalid') should throw IllegalArgumentException")
    void testParseSingleRule_negationOfInvalidRule() {
        String ruleString = "!123invalid"; // "123invalid" is an invalid rule
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> scanRuleParser.parseSingleRule(ruleString));
        // The exception message should originate from the parsing attempt of "123invalid"
        assertThat(ex.getMessage(), containsString("Invalid rule string: '123invalid'"));
        assertThat(ex.getMessage(), not(containsString("Negation cannot be applied"))); // Make sure it's not the empty negation error
    }

    @Test
    @DisplayName("parseSingleRule: Negated rule with leading/trailing whitespace in sub-rule (e.g. '!  MyClass  ')")
    void testParseSingleRule_negatedRuleWithInnerWhitespace() {
        // The overall rule "!  com.example.MyClass  " is trimmed by the caller of parseSingleRule (parseLine)
        // or if parseSingleRule is called directly, its internal trim will handle it.
        // Let's test the case where the "!" is followed by spaces before the actual rule.
        ScanMatcher result = scanRuleParser.parseSingleRule("!  com.example.MyClass  ");
        assertThat(result, instanceOf(ScanMatcher.NotMatcher.class));
        ScanMatcher.NotMatcher notMatcher = (ScanMatcher.NotMatcher) result;

        // The originalPattern for the NotMatcher is the string given to parseSingleRule, after its initial trim.
        // If " !  com.example.MyClass   " was given, it'd be "!  com.example.MyClass"
        assertEquals("!  com.example.MyClass", notMatcher.getPatternString());

        ScanMatcher innerMatcher = notMatcher.getInnerMatcher();
        assertThat(innerMatcher, instanceOf(ScanMatcher.FqcnMatcher.class));
        // The inner matcher is parsed from "  com.example.MyClass  ", which parseSingleRule will trim.
        assertEquals(new ScanMatcher.FqcnMatcher("com.example.MyClass", "com.example.MyClass"), innerMatcher);
    }

    @Test
    @DisplayName("parseLine: Multiple rules including negations, with varied spacing")
    void testParseLine_multipleRulesWithNegations() {
        // Rules: "pkg.A", "!pkg.B", "C", "!D.**", "!.E"
        List<ScanMatcher> matchers = scanRuleParser.parseLine("pkg.A, !pkg.B  ;  C   !D.** \t !.E");
        assertThat(matchers, hasSize(5));

        assertEquals(new ScanMatcher.FqcnMatcher("pkg.A", "pkg.A"), matchers.get(0));

        assertThat(matchers.get(1), instanceOf(ScanMatcher.NotMatcher.class));
        ScanMatcher.NotMatcher notB = (ScanMatcher.NotMatcher) matchers.get(1);
        assertEquals("!pkg.B", notB.getPatternString());
        assertEquals(new ScanMatcher.FqcnMatcher("pkg.B", "pkg.B"), notB.getInnerMatcher());

        assertEquals(new ScanMatcher.ClassNameSuffixMatcher("C", "C"), matchers.get(2));

        assertThat(matchers.get(3), instanceOf(ScanMatcher.NotMatcher.class));
        ScanMatcher.NotMatcher notD = (ScanMatcher.NotMatcher) matchers.get(3);
        assertEquals("!D.**", notD.getPatternString());
        assertEquals(new ScanMatcher.PackagePrefixMatcher("D", "D.**"), notD.getInnerMatcher());

        assertThat(matchers.get(4), instanceOf(ScanMatcher.NotMatcher.class));
        ScanMatcher.NotMatcher notE = (ScanMatcher.NotMatcher) matchers.get(4);
        assertEquals("!.E", notE.getPatternString());
        assertEquals(new ScanMatcher.ClassNameSuffixMatcher("E", ".E"), notE.getInnerMatcher());
    }

    @Test
    @DisplayName("parse: Multi-line input with negated rules and comments")
    void testParse_multilineInputWithNegations() {
        String multiLineRules = String.join("\n",
                "com.example.ServiceA, !com.example.ServiceB # Negate ServiceB",
                "  !org.excluded.* // Exclude this whole package",
                "MyStandaloneClass",
                "!AnotherClass ; !*.YetAnotherUtil"
        );

        List<ScanMatcher> matchers = scanRuleParser.parse(multiLineRules);
        assertThat(matchers, hasSize(6)); // 2 + 1 + 1 + 2

        // Line 1
        assertEquals(new ScanMatcher.FqcnMatcher("com.example.ServiceA", "com.example.ServiceA"), matchers.get(0));

        assertThat(matchers.get(1), instanceOf(ScanMatcher.NotMatcher.class));
        ScanMatcher.NotMatcher notServiceB = (ScanMatcher.NotMatcher) matchers.get(1);
        assertEquals("!com.example.ServiceB", notServiceB.getPatternString());
        assertEquals(new ScanMatcher.FqcnMatcher("com.example.ServiceB", "com.example.ServiceB"), notServiceB.getInnerMatcher());

        // Line 2
        assertThat(matchers.get(2), instanceOf(ScanMatcher.NotMatcher.class));
        ScanMatcher.NotMatcher notExcluded = (ScanMatcher.NotMatcher) matchers.get(2);
        assertEquals("!org.excluded.*", notExcluded.getPatternString());
        assertEquals(new ScanMatcher.PackagePrefixMatcher("org.excluded", "org.excluded.*"), notExcluded.getInnerMatcher());

        // Line 3
        assertEquals(new ScanMatcher.ClassNameSuffixMatcher("MyStandaloneClass", "MyStandaloneClass"), matchers.get(3));

        // Line 4
        assertThat(matchers.get(4), instanceOf(ScanMatcher.NotMatcher.class));
        ScanMatcher.NotMatcher notAnotherClass = (ScanMatcher.NotMatcher) matchers.get(4);
        assertEquals("!AnotherClass", notAnotherClass.getPatternString());
        assertEquals(new ScanMatcher.ClassNameSuffixMatcher("AnotherClass", "AnotherClass"), notAnotherClass.getInnerMatcher());

        assertThat(matchers.get(5), instanceOf(ScanMatcher.NotMatcher.class));
        ScanMatcher.NotMatcher notYetAnotherUtil = (ScanMatcher.NotMatcher) matchers.get(5);
        assertEquals("!*.YetAnotherUtil", notYetAnotherUtil.getPatternString());
        assertEquals(new ScanMatcher.ClassNameSuffixMatcher("YetAnotherUtil", "*.YetAnotherUtil"), notYetAnotherUtil.getInnerMatcher());
    }

    @ParameterizedTest
    @ValueSource(strings = {"*", "**", "***"})
    @DisplayName("parseSingleRule: Asterisks rule string should create MatchAllMatcher")
    void testParseSingleRule_withMatchAllRule(String ruleString) {
        ScanMatcher result = scanRuleParser.parseSingleRule(ruleString);
        assertThat(result, instanceOf(ScanMatcher.MatchAllMatcher.class));
        assertEquals(new ScanMatcher.MatchAllMatcher(ruleString), result);
    }

    @ParameterizedTest
    @CsvSource({
            "pkg., pkg",
            "pkg.*, pkg",
            "pkg.**, pkg",
            "pkg.sub., pkg.sub",
            "pkg.sub.*, pkg.sub",
            "pkg.sub.**, pkg.sub",
            "pkg.myPkg., pkg.myPkg", // Mixed case in package name part
            "pkg.myPkg.*, pkg.myPkg",
            "pkg.myPkg.**, pkg.myPkg",
            "com.example., com.example",
            "com.example.*, com.example",
            "com.example.**, com.example"
    })
    @DisplayName("parseSingleRule: Explicit package prefix rule string should create PackagePrefixMatcher")
    void testParseSingleRule_withExplicitPackagePrefixRule(String ruleString, String expectedPackage) {
        ScanMatcher result = scanRuleParser.parseSingleRule(ruleString);
        assertThat(result, instanceOf(ScanMatcher.PackagePrefixMatcher.class));
        assertEquals(new ScanMatcher.PackagePrefixMatcher(expectedPackage, ruleString), result);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "pkg", "pkg1.pkg2", "com.example", "util", "java.lang", // All lowercase
            "org.MyCompany.utils", "MyCo.services" // Mixed case (testing the fix for implicit package names)
    })
    @DisplayName("parseSingleRule: Implicit package prefix rule string should create PackagePrefixMatcher")
    void testParseSingleRule_withImplicitPackagePrefixRule(String ruleString) {
        ScanMatcher result = scanRuleParser.parseSingleRule(ruleString);
        assertThat(result, instanceOf(ScanMatcher.PackagePrefixMatcher.class));
        ScanMatcher.PackagePrefixMatcher matcher = (ScanMatcher.PackagePrefixMatcher) result;
        assertEquals(ruleString, matcher.getPatternString()); // Verifying the extracted package name
        assertEquals(new ScanMatcher.PackagePrefixMatcher(ruleString, ruleString), result);
    }

    @ParameterizedTest
    @CsvSource({
            ".MyClass, MyClass",
            "*.MyClass, MyClass",
            "**.MyClass, MyClass",
            ".myclass, myclass", // Class names can technically be lowercase if not following convention
            "*.myclass, myclass",
            "**.myclass, myclass",
            ".Test$Inner, Test$Inner",
            "*.Test$Inner, Test$Inner",
            "**.Test$Inner, Test$Inner"
    })
    @DisplayName("parseSingleRule: Explicit class suffix rule string should create ClassNameSuffixMatcher")
    void testParseSingleRule_withExplicitClassSuffixRule(String ruleString, String expectedClassName) {
        ScanMatcher result = scanRuleParser.parseSingleRule(ruleString);
        assertThat(result, instanceOf(ScanMatcher.ClassNameSuffixMatcher.class));
        assertEquals(new ScanMatcher.ClassNameSuffixMatcher(expectedClassName, ruleString), result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MyClass", "Test", "SomeClass", "Class$Inner", "A"})
    @DisplayName("parseSingleRule: Implicit class name rule string should create ClassNameSuffixMatcher")
    void testParseSingleRule_withImplicitClassNameRule(String ruleString) {
        ScanMatcher result = scanRuleParser.parseSingleRule(ruleString);
        assertThat(result, instanceOf(ScanMatcher.ClassNameSuffixMatcher.class));
        assertEquals(new ScanMatcher.ClassNameSuffixMatcher(ruleString, ruleString), result);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "pkg.MyClass",
            "com.example.Test",
            "pkg.sub.MyClass",
            "java.lang.String",
            "nz.rd.nonop.SomeClass",
            "MyPackage.MyClass" // Package part can be mixed case
    })
    @DisplayName("parseSingleRule: Fully qualified class name rule string should create FqcnMatcher")
    void testParseSingleRule_withFullyQualifiedClassNameRule(String ruleString) {
        ScanMatcher result = scanRuleParser.parseSingleRule(ruleString);
        assertThat(result, instanceOf(ScanMatcher.FqcnMatcher.class));
        assertEquals(new ScanMatcher.FqcnMatcher(ruleString, ruleString), result);
    }

    @Test
    @DisplayName("parseSingleRule: Precedence - FQCN ('com.MyClass') vs Package ('com.myclass')")
    void testParseSingleRule_precedenceBetweenFqcnAndPackage() {
        // This should be FQCN because "MyClass" starts with an uppercase letter.
        ScanMatcher fqcnResult = scanRuleParser.parseSingleRule("com.MyClass");
        assertThat(fqcnResult, instanceOf(ScanMatcher.FqcnMatcher.class));
        assertEquals(new ScanMatcher.FqcnMatcher("com.MyClass", "com.MyClass"), fqcnResult);

        // This should be PackagePrefixMatcher (implicit package) because "myclass" is all lowercase.
        // (or, with new rule, because it's a valid identifier not matching FQCN/ImplicitClass)
        ScanMatcher packageResult = scanRuleParser.parseSingleRule("com.myclass");
        assertThat(packageResult, instanceOf(ScanMatcher.PackagePrefixMatcher.class));
        assertEquals(new ScanMatcher.PackagePrefixMatcher("com.myclass", "com.myclass"), packageResult);

        // This should be FQCN because "MyOtherClass" starts with an uppercase letter, even if "myMixedPkg" is mixed case.
        ScanMatcher mixedPkgFqcnResult = scanRuleParser.parseSingleRule("myMixedPkg.MyOtherClass");
        assertThat(mixedPkgFqcnResult, instanceOf(ScanMatcher.FqcnMatcher.class));
        assertEquals(new ScanMatcher.FqcnMatcher("myMixedPkg.MyOtherClass", "myMixedPkg.MyOtherClass"), mixedPkgFqcnResult);

        // This should be PackagePrefixMatcher (implicit package) because "anotherClass" does not start uppercase.
        ScanMatcher mixedPkgNonFqcnResult = scanRuleParser.parseSingleRule("myMixedPkg.anotherClass");
        assertThat(mixedPkgNonFqcnResult, instanceOf(ScanMatcher.PackagePrefixMatcher.class));
        assertEquals(new ScanMatcher.PackagePrefixMatcher("myMixedPkg.anotherClass", "myMixedPkg.anotherClass"), mixedPkgNonFqcnResult);
    }

    @Test
    @DisplayName("parseSingleRule: Valid Java identifiers with underscores and dollar signs")
    void testParseSingleRule_withValidJavaIdentifiers() {
        ScanMatcher result1 = scanRuleParser.parseSingleRule("My_Class$Inner");
        assertThat(result1, instanceOf(ScanMatcher.ClassNameSuffixMatcher.class));
        assertEquals(new ScanMatcher.ClassNameSuffixMatcher("My_Class$Inner", "My_Class$Inner"), result1);

        // Test for package with underscores (and fix for mixed case packages)
        ScanMatcher result2 = scanRuleParser.parseSingleRule("my_package._util"); // _util does not start with uppercase
        assertThat(result2, instanceOf(ScanMatcher.PackagePrefixMatcher.class));
        assertEquals(new ScanMatcher.PackagePrefixMatcher("my_package._util", "my_package._util"), result2);

        ScanMatcher result3 = scanRuleParser.parseSingleRule("my_pkg.My$Class_Name");
        assertThat(result3, instanceOf(ScanMatcher.FqcnMatcher.class));
        assertEquals(new ScanMatcher.FqcnMatcher("my_pkg.My$Class_Name", "my_pkg.My$Class_Name"), result3);

        ScanMatcher result4 = scanRuleParser.parseSingleRule("org.My$_Company.utilPackage");
        assertThat(result4, instanceOf(ScanMatcher.PackagePrefixMatcher.class));
        assertEquals(new ScanMatcher.PackagePrefixMatcher("org.My$_Company.utilPackage", "org.My$_Company.utilPackage"), result4);

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "123invalid",      // Identifier can't start with number
            "pkg..MyClass",    // Double dots in package
            "*.*.MyClass",     // Multiple wildcards not in a supported structure for class suffix
            "pkg.*invalid",    // Invalid characters after package wildcard
            ".*.MyClass",      // Invalid wildcard usage for class suffix
            "a-b-c",           // Hyphens not allowed in identifiers
            "pkg.1Class",      // Class name part can't start with number
            "myRule!",         // Invalid character
            "."                // Just a dot
    })
    @DisplayName("parseSingleRule: Invalid rule strings should throw IllegalArgumentException")
    void testParseSingleRule_withInvalidRuleString(String ruleString) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> scanRuleParser.parseSingleRule(ruleString)
        );
        assertThat(exception.getMessage(), containsString("Invalid rule string"));
        assertThat(exception.getMessage(), containsString("'" + ruleString + "'"));
    }

    @Test
    @DisplayName("parseSingleRule: Whitespace trimming of rule string")
    void testParseSingleRule_withLeadingTrailingWhitespace() {
        ScanMatcher result = scanRuleParser.parseSingleRule("  pkg.MyClass  ");
        assertThat(result, instanceOf(ScanMatcher.FqcnMatcher.class));
        // The FQCN matcher should store the trimmed version as its key, and original for display
        // current ScanMatcher.FqcnMatcher stores fqcn (trimmed) and original (trimmed)
        assertEquals(new ScanMatcher.FqcnMatcher("pkg.MyClass", "pkg.MyClass"), result);
    }

    // Tests for parseLine

    @Test
    @DisplayName("parseLine: Empty line should return empty list")
    void testParseLine_emptyLine() {
        List<ScanMatcher> matchers = scanRuleParser.parseLine("");
        assertThat(matchers, is(empty()));
    }

    @Test
    @DisplayName("parseLine: Whitespace-only line should return empty list")
    void testParseLine_whitespaceOnlyLine() {
        List<ScanMatcher> matchers = scanRuleParser.parseLine("   \t  ");
        assertThat(matchers, is(empty()));
    }

    @Test
    @DisplayName("parseLine: Line with only a comment should return empty list")
    void testParseLine_commentOnlyLine() {
        List<ScanMatcher> matchers1 = scanRuleParser.parseLine("# This is a comment");
        assertThat(matchers1, is(empty()));
        List<ScanMatcher> matchers2 = scanRuleParser.parseLine("// This is another comment");
        assertThat(matchers2, is(empty()));
        List<ScanMatcher> matchers3 = scanRuleParser.parseLine("   # Whitespace before comment");
        assertThat(matchers3, is(empty()));
    }

    @Test
    @DisplayName("parseLine: Single rule on a line")
    void testParseLine_singleRule() {
        List<ScanMatcher> matchers = scanRuleParser.parseLine("  com.example.MyClass  ");
        assertThat(matchers, hasSize(1));
        assertThat(matchers.get(0), instanceOf(ScanMatcher.FqcnMatcher.class));
        assertEquals(new ScanMatcher.FqcnMatcher("com.example.MyClass", "com.example.MyClass"), matchers.get(0));
    }

    @Test
    @DisplayName("parseLine: Multiple rules on a line with various separators")
    void testParseLine_multipleRules() {
        List<ScanMatcher> matchers = scanRuleParser.parseLine("com.example.Foo, bar.Baz; org.another.** \t next.Rule");
        assertThat(matchers, hasSize(4));
        assertThat(matchers.get(0), equalTo(new ScanMatcher.FqcnMatcher("com.example.Foo", "com.example.Foo")));
        assertThat(matchers.get(1), equalTo(new ScanMatcher.FqcnMatcher("bar.Baz", "bar.Baz"))); // Assuming Baz implies class
        assertThat(matchers.get(2), equalTo(new ScanMatcher.PackagePrefixMatcher("org.another", "org.another.**")));
        assertThat(matchers.get(3), equalTo(new ScanMatcher.FqcnMatcher("next.Rule", "next.Rule")));
    }

    @Test
    @DisplayName("parseLine: Multiple rules with redundant separators")
    void testParseLine_multipleRulesWithRedundantSeparators() {
        List<ScanMatcher> matchers = scanRuleParser.parseLine("com.example.Foo ,,  bar.Baz;;org.another.**");
        assertThat(matchers, hasSize(3));
        assertThat(matchers.get(0), equalTo(new ScanMatcher.FqcnMatcher("com.example.Foo", "com.example.Foo")));
        assertThat(matchers.get(1), equalTo(new ScanMatcher.FqcnMatcher("bar.Baz", "bar.Baz")));
        assertThat(matchers.get(2), equalTo(new ScanMatcher.PackagePrefixMatcher("org.another", "org.another.**")));
    }


    @Test
    @DisplayName("parseLine: Rule with inline comment should parse rule and ignore comment")
    void testParseLine_ruleWithInlineComment() {
        List<ScanMatcher> matchers1 = scanRuleParser.parseLine("pkg.AClass # comment here");
        assertThat(matchers1, hasSize(1));
        assertEquals(new ScanMatcher.FqcnMatcher("pkg.AClass", "pkg.AClass"), matchers1.get(0));

        List<ScanMatcher> matchers2 = scanRuleParser.parseLine("  another.pkg.* // another comment  ");
        assertThat(matchers2, hasSize(1));
        assertEquals(new ScanMatcher.PackagePrefixMatcher("another.pkg", "another.pkg.*"), matchers2.get(0));

        List<ScanMatcher> matchers3 = scanRuleParser.parseLine("Rule1; Rule2 # comment after Rule2");
        assertThat(matchers3, hasSize(2));
        assertEquals(new ScanMatcher.ClassNameSuffixMatcher("Rule1", "Rule1"), matchers3.get(0));
        assertEquals(new ScanMatcher.ClassNameSuffixMatcher("Rule2", "Rule2"), matchers3.get(1));
    }

    // Tests for parse (main method for multi-line input)

    @Test
    @DisplayName("parse: Null input string should return empty list")
    void testParse_nullInput() {
        List<ScanMatcher> matchers = scanRuleParser.parse(null);
        assertThat(matchers, is(empty()));
    }

    @Test
    @DisplayName("parse: Empty input string should return empty list")
    void testParse_emptyInput() {
        List<ScanMatcher> matchers = scanRuleParser.parse("");
        assertThat(matchers, is(empty()));
    }

    @Test
    @DisplayName("parse: Whitespace-only input string should return empty list")
    void testParse_whitespaceOnlyInput() {
        List<ScanMatcher> matchers = scanRuleParser.parse("  \n\t  \n  ");
        assertThat(matchers, is(empty()));
    }

    @Test
    @DisplayName("parse: Multi-line input with various rules, comments, and empty lines")
    void testParse_multilineInput() {
        String multiLineRules = String.join("\n",
                "com.example.service.ServiceA, com.example.service.ServiceB",
                "  org.common.utils.*  # Match all utils",
                "", // Empty line
                "# Full comment line",
                "  MyStandaloneClass  ",
                "// Another full comment line",
                "  another.package.** ; implicit.pkgName"
        );

        List<ScanMatcher> matchers = scanRuleParser.parse(multiLineRules);
        assertThat(matchers, hasSize(6));

        // Verify matchers (order matters)
        // Line 1
        assertEquals(new ScanMatcher.FqcnMatcher("com.example.service.ServiceA", "com.example.service.ServiceA"), matchers.get(0));
        assertEquals(new ScanMatcher.FqcnMatcher("com.example.service.ServiceB", "com.example.service.ServiceB"), matchers.get(1));
        // Line 2
        assertEquals(new ScanMatcher.PackagePrefixMatcher("org.common.utils", "org.common.utils.*"), matchers.get(2));
        // Line 3 (empty) - no matcher
        // Line 4 (comment) - no matcher
        // Line 5
        assertEquals(new ScanMatcher.ClassNameSuffixMatcher("MyStandaloneClass", "MyStandaloneClass"), matchers.get(3));
        // Line 6 (comment) - no matcher
        // Line 7
        assertEquals(new ScanMatcher.PackagePrefixMatcher("another.package", "another.package.**"), matchers.get(4));
        assertEquals(new ScanMatcher.PackagePrefixMatcher("implicit.pkgName", "implicit.pkgName"), matchers.get(5));
    }
}