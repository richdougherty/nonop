package nz.rd.nonop.internal.config;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;

/**
 * Represents a rule for matching class names.
 * Implementations determine if a fully qualified class name (FQCN)
 * should be included or excluded based on the rule's logic.
 */
public interface ScanMatcher {

    /**
     * Evaluates this matcher against the given fully qualified class name.
     *
     * @param fqClassName The fully qualified class name (e.g., "java.lang.String", "com.example.MyClass$Inner").
     * @return {@link Boolean#TRUE} if this rule positively matches and implies inclusion,
     *         {@link Boolean#FALSE} if this rule positively matches and implies exclusion,
     *         {@code null} if this rule does not apply to the given class name.
     */
    Boolean eval(String fqClassName);

    /**
     * Indicates the fundamental intent of this rule if it were to match something.
     * For example, "com.example.*" is effectively an include rule, while "!com.example.*"
     * is effectively an exclude rule.
     *
     * @return {@code true} if the rule's intent is to include, {@code false} if to exclude.
     */
    boolean isEffectivelyInclude();

    /**
     * @return A string representation of the original pattern this matcher was created from.
     */
    String getPatternString();

    abstract class AbstractScanMatcher implements ScanMatcher {
        protected final String pattern;
        protected final String originalPatternString; // For toString/debugging

        public AbstractScanMatcher(String pattern, String originalPatternString) {
            this.pattern = pattern;
            this.originalPatternString = originalPatternString;
        }

        protected String normalizeClassName(String fqClassName) {
            // Match against the main class name, removing inner class suffixes like $Inner
            int innerClassMarker = fqClassName.indexOf('$');
            if (innerClassMarker != -1) {
                return fqClassName.substring(0, innerClassMarker);
            }
            return fqClassName;
        }

        @Override
        public String getPatternString() {
            return originalPatternString;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AbstractScanMatcher)) return false; // Check specific type in concrete classes if needed
            AbstractScanMatcher that = (AbstractScanMatcher) o;
            // Equality based on original pattern and effective inclusion (handled by NotMatcher potentially)
            return isEffectivelyInclude() == that.isEffectivelyInclude() &&
                    Objects.equals(originalPatternString, that.originalPatternString);
        }

        @Override
        public int hashCode() {
            return Objects.hash(originalPatternString, isEffectivelyInclude());
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" + pattern + "}";
        }
    }

    class NotMatcher extends AbstractScanMatcher {
        private final @NonNull ScanMatcher innerMatcher;

        public NotMatcher(@NonNull ScanMatcher innerMatcher, String originalPatternString) {
            super("!"+ innerMatcher.getPatternString(), originalPatternString);
            this.innerMatcher = innerMatcher;
        }

        @Override
        public Boolean eval(String fqClassName) {
            Boolean innerEval = innerMatcher.eval(fqClassName);
            return (innerEval == null) ? null : !innerEval;
        }

        @Override
        public boolean isEffectivelyInclude() {
            return !innerMatcher.isEffectivelyInclude();
        }

        public @NonNull ScanMatcher getInnerMatcher() {
            return innerMatcher;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o instanceof NotMatcher && super.equals(o);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" + innerMatcher + "}";
        }
    }

    class MatchAllMatcher extends AbstractScanMatcher {
        public MatchAllMatcher(String originalPatternString) {
            super("*", originalPatternString);
        }

        @Override
        public Boolean eval(String fqClassName) {
            return Boolean.TRUE; // Always matches and implies inclusion by default
        }

        @Override
        public boolean isEffectivelyInclude() {
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o instanceof MatchAllMatcher && super.equals(o);
        }
    }

    class PackagePrefixMatcher extends AbstractScanMatcher {
        public PackagePrefixMatcher(String pattern, String originalPatternString) {
            super(pattern, originalPatternString);
        }

        @Override
        public Boolean eval(String fqClassName) {
            String mainClassName = normalizeClassName(fqClassName);
            if (mainClassName.equals(this.pattern)) { // Exact match for package name itself (e.g. "com.foo" matches "com.foo")
                return Boolean.TRUE;
            }
            if (mainClassName.startsWith(this.pattern + ".")) { // Standard package prefix (e.g. "com.foo" matches "com.foo.Bar")
                return Boolean.TRUE;
            }
            if (this.pattern.isEmpty() && !mainClassName.contains(".")) { // Match default package classes if pattern is "" (from "*")
                return Boolean.TRUE;
            }
            return null; // No match
        }

        @Override
        public boolean isEffectivelyInclude() {
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o instanceof PackagePrefixMatcher && super.equals(o);
        }
    }

    class FqcnMatcher extends AbstractScanMatcher {
        public FqcnMatcher(String pattern, String originalPatternString) {
            super(pattern, originalPatternString);
        }

        @Override
        public Boolean eval(String fqClassName) {
            String mainClassName = normalizeClassName(fqClassName);
            return mainClassName.equals(this.pattern) ? Boolean.TRUE : null;
        }

        @Override
        public boolean isEffectivelyInclude() {
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o instanceof FqcnMatcher && super.equals(o);
        }
    }

    class ClassNameSuffixMatcher extends AbstractScanMatcher {
        public ClassNameSuffixMatcher(String pattern, String originalPatternString) {
            super(pattern, originalPatternString); // pattern is the simple class name
        }

        @Override
        public Boolean eval(String fqClassName) {
            String mainClassName = normalizeClassName(fqClassName);
            int lastDot = mainClassName.lastIndexOf('.');
            String simpleName = (lastDot == -1) ? mainClassName : mainClassName.substring(lastDot + 1);
            return simpleName.equals(this.pattern) ? Boolean.TRUE : null;
        }

        @Override
        public boolean isEffectivelyInclude() {
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o instanceof ClassNameSuffixMatcher && super.equals(o);
        }
    }
}