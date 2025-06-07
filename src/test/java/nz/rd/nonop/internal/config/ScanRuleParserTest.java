// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.internal.config;

import nz.rd.nonop.internal.logging.ConsoleNonopLogger;
import nz.rd.nonop.internal.logging.NonopLogger;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

class ScanRuleParserTest {

    private final ScanRuleParser scanRuleParser = new ScanRuleParser(new ConsoleNonopLogger(NonopLogger.Level.OFF));

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "\t", "\n"})
    @DisplayName("parseSingleRule: Empty or whitespace rule string should return null (no matcher)")
    void testParseSingleRule_withEmptyOrWhitespaceRuleString(String ruleString) {
        ScanMatcher result = scanRuleParser.parseSingleRule(ruleString);
        MatcherAssert.assertThat(result, Matchers.nullValue());
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

        MatcherAssert.assertThat(result, Matchers.instanceOf(ScanMatcher.NotMatcher.class));
        ScanMatcher expectedSubMatcher = scanRuleParser.parseSingleRule(expectedSubRuleString);
        MatcherAssert.assertThat(((ScanMatcher.NotMatcher) result).getInnerMatcher(), Matchers.equalTo(expectedSubMatcher));
    }

    @ParameterizedTest
    @ValueSource(strings = {"!", "! ", "!\t"})
    @DisplayName("parseSingleRule: Negation of empty or whitespace rule should throw IllegalArgumentException")
    void testParseSingleRule_negationOfEmptyRule(String ruleString) {
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> scanRuleParser.parseSingleRule(ruleString));
        MatcherAssert.assertThat(ex.getMessage(), Matchers.containsString("Negation cannot be applied to an empty or null rule."));
        MatcherAssert.assertThat(ex.getMessage(), Matchers.containsString("'" + ruleString + "'"));
    }

    @Test
    @DisplayName("parseSingleRule: Negation of an invalid rule (e.g., '!123invalid') should throw IllegalArgumentException")
    void testParseSingleRule_negationOfInvalidRule() {
        String ruleString = "!123invalid"; // "123invalid" is an invalid rule
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> scanRuleParser.parseSingleRule(ruleString));
        // The exception message should originate from the parsing attempt of "123invalid"
        MatcherAssert.assertThat(ex.getMessage(), Matchers.containsString("Invalid rule string: '123invalid'"));
        MatcherAssert.assertThat(ex.getMessage(), Matchers.not(Matchers.containsString("Negation cannot be applied"))); // Make sure it's not the empty negation error
    }

    @Test
    @DisplayName("parseSingleRule: Negated rule with leading/trailing whitespace in sub-rule (e.g. '!  MyClass  ')")
    void testParseSingleRule_negatedRuleWithInnerWhitespace() {
        // The overall rule "!  com.example.MyClass  " is trimmed by the caller of parseSingleRule (parseLine)
        // or if parseSingleRule is called directly, its internal trim will handle it.
        // Let's test the case where the "!" is followed by spaces before the actual rule.
        ScanMatcher result = scanRuleParser.parseSingleRule("!  com.example.MyClass  ");
        MatcherAssert.assertThat(result, Matchers.instanceOf(ScanMatcher.NotMatcher.class));
        ScanMatcher.NotMatcher notMatcher = (ScanMatcher.NotMatcher) result;

        // The originalPattern for the NotMatcher is the string given to parseSingleRule, after its initial trim.
        // If " !  com.example.MyClass   " was given, it'd be "!  com.example.MyClass"
        Assertions.assertEquals("!  com.example.MyClass", notMatcher.getPatternString());

        ScanMatcher innerMatcher = notMatcher.getInnerMatcher();
        MatcherAssert.assertThat(innerMatcher, Matchers.instanceOf(ScanMatcher.FqcnMatcher.class));
        // The inner matcher is parsed from "  com.example.MyClass  ", which parseSingleRule will trim.
        Assertions.assertEquals(new ScanMatcher.FqcnMatcher("com.example.MyClass", "com.example.MyClass"), innerMatcher);
    }

    @Test
    @DisplayName("parseLine: Multiple rules including negations, with varied spacing")
    void testParseLine_multipleRulesWithNegations() {
        // Rules: "pkg.A", "!pkg.B", "C", "!D.**", "!.E"
        List<ScanMatcher> matchers = scanRuleParser.parseLine("pkg.A, !pkg.B  ;  C   !D.** \t !.E");
        MatcherAssert.assertThat(matchers, Matchers.hasSize(5));

        Assertions.assertEquals(new ScanMatcher.FqcnMatcher("pkg.A", "pkg.A"), matchers.get(0));

        MatcherAssert.assertThat(matchers.get(1), Matchers.instanceOf(ScanMatcher.NotMatcher.class));
        ScanMatcher.NotMatcher notB = (ScanMatcher.NotMatcher) matchers.get(1);
        Assertions.assertEquals("!pkg.B", notB.getPatternString());
        Assertions.assertEquals(new ScanMatcher.FqcnMatcher("pkg.B", "pkg.B"), notB.getInnerMatcher());

        Assertions.assertEquals(new ScanMatcher.ClassNameSuffixMatcher("C", "C"), matchers.get(2));

        MatcherAssert.assertThat(matchers.get(3), Matchers.instanceOf(ScanMatcher.NotMatcher.class));
        ScanMatcher.NotMatcher notD = (ScanMatcher.NotMatcher) matchers.get(3);
        Assertions.assertEquals("!D.**", notD.getPatternString());
        Assertions.assertEquals(new ScanMatcher.PackagePrefixMatcher("D", "D.**"), notD.getInnerMatcher());

        MatcherAssert.assertThat(matchers.get(4), Matchers.instanceOf(ScanMatcher.NotMatcher.class));
        ScanMatcher.NotMatcher notE = (ScanMatcher.NotMatcher) matchers.get(4);
        Assertions.assertEquals("!.E", notE.getPatternString());
        Assertions.assertEquals(new ScanMatcher.ClassNameSuffixMatcher("E", ".E"), notE.getInnerMatcher());
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
        MatcherAssert.assertThat(matchers, Matchers.hasSize(6)); // 2 + 1 + 1 + 2

        // Line 1
        Assertions.assertEquals(new ScanMatcher.FqcnMatcher("com.example.ServiceA", "com.example.ServiceA"), matchers.get(0));

        MatcherAssert.assertThat(matchers.get(1), Matchers.instanceOf(ScanMatcher.NotMatcher.class));
        ScanMatcher.NotMatcher notServiceB = (ScanMatcher.NotMatcher) matchers.get(1);
        Assertions.assertEquals("!com.example.ServiceB", notServiceB.getPatternString());
        Assertions.assertEquals(new ScanMatcher.FqcnMatcher("com.example.ServiceB", "com.example.ServiceB"), notServiceB.getInnerMatcher());

        // Line 2
        MatcherAssert.assertThat(matchers.get(2), Matchers.instanceOf(ScanMatcher.NotMatcher.class));
        ScanMatcher.NotMatcher notExcluded = (ScanMatcher.NotMatcher) matchers.get(2);
        Assertions.assertEquals("!org.excluded.*", notExcluded.getPatternString());
        Assertions.assertEquals(new ScanMatcher.PackagePrefixMatcher("org.excluded", "org.excluded.*"), notExcluded.getInnerMatcher());

        // Line 3
        Assertions.assertEquals(new ScanMatcher.ClassNameSuffixMatcher("MyStandaloneClass", "MyStandaloneClass"), matchers.get(3));

        // Line 4
        MatcherAssert.assertThat(matchers.get(4), Matchers.instanceOf(ScanMatcher.NotMatcher.class));
        ScanMatcher.NotMatcher notAnotherClass = (ScanMatcher.NotMatcher) matchers.get(4);
        Assertions.assertEquals("!AnotherClass", notAnotherClass.getPatternString());
        Assertions.assertEquals(new ScanMatcher.ClassNameSuffixMatcher("AnotherClass", "AnotherClass"), notAnotherClass.getInnerMatcher());

        MatcherAssert.assertThat(matchers.get(5), Matchers.instanceOf(ScanMatcher.NotMatcher.class));
        ScanMatcher.NotMatcher notYetAnotherUtil = (ScanMatcher.NotMatcher) matchers.get(5);
        Assertions.assertEquals("!*.YetAnotherUtil", notYetAnotherUtil.getPatternString());
        Assertions.assertEquals(new ScanMatcher.ClassNameSuffixMatcher("YetAnotherUtil", "*.YetAnotherUtil"), notYetAnotherUtil.getInnerMatcher());
    }

    @ParameterizedTest
    @ValueSource(strings = {"*", "**", "***"})
    @DisplayName("parseSingleRule: Asterisks rule string should create MatchAllMatcher")
    void testParseSingleRule_withMatchAllRule(String ruleString) {
        ScanMatcher result = scanRuleParser.parseSingleRule(ruleString);
        MatcherAssert.assertThat(result, Matchers.instanceOf(ScanMatcher.MatchAllMatcher.class));
        Assertions.assertEquals(new ScanMatcher.MatchAllMatcher(ruleString), result);
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
        MatcherAssert.assertThat(result, Matchers.instanceOf(ScanMatcher.PackagePrefixMatcher.class));
        Assertions.assertEquals(new ScanMatcher.PackagePrefixMatcher(expectedPackage, ruleString), result);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "pkg", "pkg1.pkg2", "com.example", "util", "java.lang", // All lowercase
            "org.MyCompany.utils", "MyCo.services" // Mixed case (testing the fix for implicit package names)
    })
    @DisplayName("parseSingleRule: Implicit package prefix rule string should create PackagePrefixMatcher")
    void testParseSingleRule_withImplicitPackagePrefixRule(String ruleString) {
        ScanMatcher result = scanRuleParser.parseSingleRule(ruleString);
        MatcherAssert.assertThat(result, Matchers.instanceOf(ScanMatcher.PackagePrefixMatcher.class));
        ScanMatcher.PackagePrefixMatcher matcher = (ScanMatcher.PackagePrefixMatcher) result;
        Assertions.assertEquals(ruleString, matcher.getPatternString()); // Verifying the extracted package name
        Assertions.assertEquals(new ScanMatcher.PackagePrefixMatcher(ruleString, ruleString), result);
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
        MatcherAssert.assertThat(result, Matchers.instanceOf(ScanMatcher.ClassNameSuffixMatcher.class));
        Assertions.assertEquals(new ScanMatcher.ClassNameSuffixMatcher(expectedClassName, ruleString), result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MyClass", "Test", "SomeClass", "Class$Inner", "A"})
    @DisplayName("parseSingleRule: Implicit class name rule string should create ClassNameSuffixMatcher")
    void testParseSingleRule_withImplicitClassNameRule(String ruleString) {
        ScanMatcher result = scanRuleParser.parseSingleRule(ruleString);
        MatcherAssert.assertThat(result, Matchers.instanceOf(ScanMatcher.ClassNameSuffixMatcher.class));
        Assertions.assertEquals(new ScanMatcher.ClassNameSuffixMatcher(ruleString, ruleString), result);
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
        MatcherAssert.assertThat(result, Matchers.instanceOf(ScanMatcher.FqcnMatcher.class));
        Assertions.assertEquals(new ScanMatcher.FqcnMatcher(ruleString, ruleString), result);
    }

    @Test
    @DisplayName("parseSingleRule: Precedence - FQCN ('com.MyClass') vs Package ('com.myclass')")
    void testParseSingleRule_precedenceBetweenFqcnAndPackage() {
        // This should be FQCN because "MyClass" starts with an uppercase letter.
        ScanMatcher fqcnResult = scanRuleParser.parseSingleRule("com.MyClass");
        MatcherAssert.assertThat(fqcnResult, Matchers.instanceOf(ScanMatcher.FqcnMatcher.class));
        Assertions.assertEquals(new ScanMatcher.FqcnMatcher("com.MyClass", "com.MyClass"), fqcnResult);

        // This should be PackagePrefixMatcher (implicit package) because "myclass" is all lowercase.
        // (or, with new rule, because it's a valid identifier not matching FQCN/ImplicitClass)
        ScanMatcher packageResult = scanRuleParser.parseSingleRule("com.myclass");
        MatcherAssert.assertThat(packageResult, Matchers.instanceOf(ScanMatcher.PackagePrefixMatcher.class));
        Assertions.assertEquals(new ScanMatcher.PackagePrefixMatcher("com.myclass", "com.myclass"), packageResult);

        // This should be FQCN because "MyOtherClass" starts with an uppercase letter, even if "myMixedPkg" is mixed case.
        ScanMatcher mixedPkgFqcnResult = scanRuleParser.parseSingleRule("myMixedPkg.MyOtherClass");
        MatcherAssert.assertThat(mixedPkgFqcnResult, Matchers.instanceOf(ScanMatcher.FqcnMatcher.class));
        Assertions.assertEquals(new ScanMatcher.FqcnMatcher("myMixedPkg.MyOtherClass", "myMixedPkg.MyOtherClass"), mixedPkgFqcnResult);

        // This should be PackagePrefixMatcher (implicit package) because "anotherClass" does not start uppercase.
        ScanMatcher mixedPkgNonFqcnResult = scanRuleParser.parseSingleRule("myMixedPkg.anotherClass");
        MatcherAssert.assertThat(mixedPkgNonFqcnResult, Matchers.instanceOf(ScanMatcher.PackagePrefixMatcher.class));
        Assertions.assertEquals(new ScanMatcher.PackagePrefixMatcher("myMixedPkg.anotherClass", "myMixedPkg.anotherClass"), mixedPkgNonFqcnResult);
    }

    @Test
    @DisplayName("parseSingleRule: Valid Java identifiers with underscores and dollar signs")
    void testParseSingleRule_withValidJavaIdentifiers() {
        ScanMatcher result1 = scanRuleParser.parseSingleRule("My_Class$Inner");
        MatcherAssert.assertThat(result1, Matchers.instanceOf(ScanMatcher.ClassNameSuffixMatcher.class));
        Assertions.assertEquals(new ScanMatcher.ClassNameSuffixMatcher("My_Class$Inner", "My_Class$Inner"), result1);

        // Test for package with underscores (and fix for mixed case packages)
        ScanMatcher result2 = scanRuleParser.parseSingleRule("my_package._util"); // _util does not start with uppercase
        MatcherAssert.assertThat(result2, Matchers.instanceOf(ScanMatcher.PackagePrefixMatcher.class));
        Assertions.assertEquals(new ScanMatcher.PackagePrefixMatcher("my_package._util", "my_package._util"), result2);

        ScanMatcher result3 = scanRuleParser.parseSingleRule("my_pkg.My$Class_Name");
        MatcherAssert.assertThat(result3, Matchers.instanceOf(ScanMatcher.FqcnMatcher.class));
        Assertions.assertEquals(new ScanMatcher.FqcnMatcher("my_pkg.My$Class_Name", "my_pkg.My$Class_Name"), result3);

        ScanMatcher result4 = scanRuleParser.parseSingleRule("org.My$_Company.utilPackage");
        MatcherAssert.assertThat(result4, Matchers.instanceOf(ScanMatcher.PackagePrefixMatcher.class));
        Assertions.assertEquals(new ScanMatcher.PackagePrefixMatcher("org.My$_Company.utilPackage", "org.My$_Company.utilPackage"), result4);

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
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> scanRuleParser.parseSingleRule(ruleString)
        );
        MatcherAssert.assertThat(exception.getMessage(), Matchers.containsString("Invalid rule string"));
        MatcherAssert.assertThat(exception.getMessage(), Matchers.containsString("'" + ruleString + "'"));
    }

    @Test
    @DisplayName("parseSingleRule: Whitespace trimming of rule string")
    void testParseSingleRule_withLeadingTrailingWhitespace() {
        ScanMatcher result = scanRuleParser.parseSingleRule("  pkg.MyClass  ");
        MatcherAssert.assertThat(result, Matchers.instanceOf(ScanMatcher.FqcnMatcher.class));
        // The FQCN matcher should store the trimmed version as its key, and original for display
        // current ScanMatcher.FqcnMatcher stores fqcn (trimmed) and original (trimmed)
        Assertions.assertEquals(new ScanMatcher.FqcnMatcher("pkg.MyClass", "pkg.MyClass"), result);
    }

    // Tests for parseLine

    @Test
    @DisplayName("parseLine: Empty line should return empty list")
    void testParseLine_emptyLine() {
        List<ScanMatcher> matchers = scanRuleParser.parseLine("");
        MatcherAssert.assertThat(matchers, Matchers.is(Matchers.empty()));
    }

    @Test
    @DisplayName("parseLine: Whitespace-only line should return empty list")
    void testParseLine_whitespaceOnlyLine() {
        List<ScanMatcher> matchers = scanRuleParser.parseLine("   \t  ");
        MatcherAssert.assertThat(matchers, Matchers.is(Matchers.empty()));
    }

    @Test
    @DisplayName("parseLine: Line with only a comment should return empty list")
    void testParseLine_commentOnlyLine() {
        List<ScanMatcher> matchers1 = scanRuleParser.parseLine("# This is a comment");
        MatcherAssert.assertThat(matchers1, Matchers.is(Matchers.empty()));
        List<ScanMatcher> matchers2 = scanRuleParser.parseLine("// This is another comment");
        MatcherAssert.assertThat(matchers2, Matchers.is(Matchers.empty()));
        List<ScanMatcher> matchers3 = scanRuleParser.parseLine("   # Whitespace before comment");
        MatcherAssert.assertThat(matchers3, Matchers.is(Matchers.empty()));
    }

    @Test
    @DisplayName("parseLine: Single rule on a line")
    void testParseLine_singleRule() {
        List<ScanMatcher> matchers = scanRuleParser.parseLine("  com.example.MyClass  ");
        MatcherAssert.assertThat(matchers, Matchers.hasSize(1));
        MatcherAssert.assertThat(matchers.get(0), Matchers.instanceOf(ScanMatcher.FqcnMatcher.class));
        Assertions.assertEquals(new ScanMatcher.FqcnMatcher("com.example.MyClass", "com.example.MyClass"), matchers.get(0));
    }

    @Test
    @DisplayName("parseLine: Multiple rules on a line with various separators")
    void testParseLine_multipleRules() {
        List<ScanMatcher> matchers = scanRuleParser.parseLine("com.example.Foo, bar.Baz; org.another.** \t next.Rule");
        MatcherAssert.assertThat(matchers, Matchers.hasSize(4));
        MatcherAssert.assertThat(matchers.get(0), Matchers.equalTo(new ScanMatcher.FqcnMatcher("com.example.Foo", "com.example.Foo")));
        MatcherAssert.assertThat(matchers.get(1), Matchers.equalTo(new ScanMatcher.FqcnMatcher("bar.Baz", "bar.Baz"))); // Assuming Baz implies class
        MatcherAssert.assertThat(matchers.get(2), Matchers.equalTo(new ScanMatcher.PackagePrefixMatcher("org.another", "org.another.**")));
        MatcherAssert.assertThat(matchers.get(3), Matchers.equalTo(new ScanMatcher.FqcnMatcher("next.Rule", "next.Rule")));
    }

    @Test
    @DisplayName("parseLine: Multiple rules with redundant separators")
    void testParseLine_multipleRulesWithRedundantSeparators() {
        List<ScanMatcher> matchers = scanRuleParser.parseLine("com.example.Foo ,,  bar.Baz;;org.another.**");
        MatcherAssert.assertThat(matchers, Matchers.hasSize(3));
        MatcherAssert.assertThat(matchers.get(0), Matchers.equalTo(new ScanMatcher.FqcnMatcher("com.example.Foo", "com.example.Foo")));
        MatcherAssert.assertThat(matchers.get(1), Matchers.equalTo(new ScanMatcher.FqcnMatcher("bar.Baz", "bar.Baz")));
        MatcherAssert.assertThat(matchers.get(2), Matchers.equalTo(new ScanMatcher.PackagePrefixMatcher("org.another", "org.another.**")));
    }


    @Test
    @DisplayName("parseLine: Rule with inline comment should parse rule and ignore comment")
    void testParseLine_ruleWithInlineComment() {
        List<ScanMatcher> matchers1 = scanRuleParser.parseLine("pkg.AClass # comment here");
        MatcherAssert.assertThat(matchers1, Matchers.hasSize(1));
        Assertions.assertEquals(new ScanMatcher.FqcnMatcher("pkg.AClass", "pkg.AClass"), matchers1.get(0));

        List<ScanMatcher> matchers2 = scanRuleParser.parseLine("  another.pkg.* // another comment  ");
        MatcherAssert.assertThat(matchers2, Matchers.hasSize(1));
        Assertions.assertEquals(new ScanMatcher.PackagePrefixMatcher("another.pkg", "another.pkg.*"), matchers2.get(0));

        List<ScanMatcher> matchers3 = scanRuleParser.parseLine("Rule1; Rule2 # comment after Rule2");
        MatcherAssert.assertThat(matchers3, Matchers.hasSize(2));
        Assertions.assertEquals(new ScanMatcher.ClassNameSuffixMatcher("Rule1", "Rule1"), matchers3.get(0));
        Assertions.assertEquals(new ScanMatcher.ClassNameSuffixMatcher("Rule2", "Rule2"), matchers3.get(1));
    }

    // Tests for parse (main method for multi-line input)

    @Test
    @DisplayName("parse: Null input string should return empty list")
    void testParse_nullInput() {
        List<ScanMatcher> matchers = scanRuleParser.parse(null);
        MatcherAssert.assertThat(matchers, Matchers.is(Matchers.empty()));
    }

    @Test
    @DisplayName("parse: Empty input string should return empty list")
    void testParse_emptyInput() {
        List<ScanMatcher> matchers = scanRuleParser.parse("");
        MatcherAssert.assertThat(matchers, Matchers.is(Matchers.empty()));
    }

    @Test
    @DisplayName("parse: Whitespace-only input string should return empty list")
    void testParse_whitespaceOnlyInput() {
        List<ScanMatcher> matchers = scanRuleParser.parse("  \n\t  \n  ");
        MatcherAssert.assertThat(matchers, Matchers.is(Matchers.empty()));
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
        MatcherAssert.assertThat(matchers, Matchers.hasSize(6));

        // Verify matchers (order matters)
        // Line 1
        Assertions.assertEquals(new ScanMatcher.FqcnMatcher("com.example.service.ServiceA", "com.example.service.ServiceA"), matchers.get(0));
        Assertions.assertEquals(new ScanMatcher.FqcnMatcher("com.example.service.ServiceB", "com.example.service.ServiceB"), matchers.get(1));
        // Line 2
        Assertions.assertEquals(new ScanMatcher.PackagePrefixMatcher("org.common.utils", "org.common.utils.*"), matchers.get(2));
        // Line 3 (empty) - no matcher
        // Line 4 (comment) - no matcher
        // Line 5
        Assertions.assertEquals(new ScanMatcher.ClassNameSuffixMatcher("MyStandaloneClass", "MyStandaloneClass"), matchers.get(3));
        // Line 6 (comment) - no matcher
        // Line 7
        Assertions.assertEquals(new ScanMatcher.PackagePrefixMatcher("another.package", "another.package.**"), matchers.get(4));
        Assertions.assertEquals(new ScanMatcher.PackagePrefixMatcher("implicit.pkgName", "implicit.pkgName"), matchers.get(5));
    }
}