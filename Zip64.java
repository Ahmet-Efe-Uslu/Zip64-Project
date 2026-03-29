/**
 * Zip64.java
 *
 * High-Performance Arbitrary Precision Decimal Library
 * ----------------------------------------------------
 * Zip64 is a professional-grade, fast, and precise decimal arithmetic
 * library for Java 25+. It handles extremely large numbers with arbitrary
 * precision, solves floating-point precision issues, and supports both
 * synchronous and asynchronous computations using virtual threads.
 *
 * Features included in this library (v1.0):
 * - Arbitrary precision arithmetic with BigInteger backing
 * - Addition, subtraction, multiplication, division with configurable rounding
 * - Efficient RLE-based string serialization (Zip64 format)
 * - Virtual thread asynchronous operations
 * - Sum/Product reduction over lists with parallelism
 * - Immutable design for thread safety
 * - Normalization of trailing zeros
 * - Caching for frequently used values (0, 1, 10)
 * - Conversion to BigDecimal and plain strings
 * - Sign handling and zero checks
 * - Customizable precision arithmetic
 * - Professional error handling and validation
 * - Designed for game engines, scientific computation, and finance
 * - Ready for multi-threaded high-performance applications
 *
 * @author ChatGPT 5.X (Vibe Coded)
 * @version 1.0
 * @since 2026-03-29
 */

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BinaryOperator;

public final class Zip64 implements Comparable<Zip64>, Serializable {
    private static final long serialVersionUID = 1L;

    private static final BigInteger BI_ZERO = BigInteger.ZERO;
    private static final BigInteger BI_ONE = BigInteger.ONE;
    private static final BigInteger BI_TEN = BigInteger.TEN;

    private static final ConcurrentHashMap<Integer, BigInteger> POW10_CACHE = new ConcurrentHashMap<>();

    public static final Zip64 ZERO = new Zip64(BI_ZERO, 0, false);
    public static final Zip64 ONE = new Zip64(BI_ONE, 0, false);
    public static final Zip64 TEN = new Zip64(BI_TEN, 0, false);

    private final BigInteger unscaled;
    private final int scale;

    private transient volatile String plainCache;
    private transient volatile String zipCache;

    // -----------------------------
    // Constructors
    // -----------------------------

    public Zip64(String text) {
        this(parse(text), true);
    }

    public Zip64(long value) {
        this(BigInteger.valueOf(value), 0, true);
    }

    public Zip64(BigInteger integer) {
        this(integer, 0, true);
    }

    public Zip64(BigDecimal value) {
        this(Objects.requireNonNull(value, "value").unscaledValue(), value.scale(), true);
    }

    // -----------------------------
    // Factory Methods
    // -----------------------------

    public static Zip64 valueOf(String text) {
        return new Zip64(text);
    }

    public static Zip64 valueOf(long value) {
        return new Zip64(value);
    }

    public static Zip64 valueOf(BigInteger integer) {
        return new Zip64(integer);
    }

    public static Zip64 valueOf(BigDecimal value) {
        return new Zip64(value);
    }

    public static Zip64 fromUnscaled(BigInteger unscaled, int scale) {
        return new Zip64(unscaled, scale, true);
    }

    // -----------------------------
    // Internal Parsing and Normalization
    // -----------------------------

    private Zip64(Parsed parsed, boolean normalize) {
        this(parsed.unscaled, parsed.scale, normalize);
    }

    private Zip64(BigInteger unscaled, int scale, boolean normalize) {
        Objects.requireNonNull(unscaled, "unscaled");
        if (normalize) {
            Normalized normalized = normalize(unscaled, scale);
            this.unscaled = normalized.unscaled;
            this.scale = normalized.scale;
        } else {
            this.unscaled = unscaled;
            this.scale = scale;
        }
    }

    private static Parsed parse(String text) {
        Objects.requireNonNull(text, "text");
        String input = text.trim();
        if (input.isEmpty()) {
            throw new IllegalArgumentException("Empty number");
        }
        BigDecimal parsed = new BigDecimal(input).stripTrailingZeros();
        return new Parsed(parsed.unscaledValue(), parsed.scale());
    }

    private static Normalized normalize(BigInteger value, int scale) {
        if (value.signum() == 0) {
            return new Normalized(BI_ZERO, 0);
        }

        BigInteger current = value;
        int currentScale = scale;

        while (true) {
            BigInteger[] qr = current.divideAndRemainder(BI_TEN);
            if (qr[1].signum() != 0) {
                break;
            }
            current = qr[0];
            currentScale = Math.subtractExact(currentScale, 1);
        }

        return new Normalized(current, currentScale);
    }

    // -----------------------------
    // Getters & Core Methods
    // -----------------------------

    public BigInteger unscaledValue() {
        return unscaled;
    }

    public int scale() {
        return scale;
    }

    public int signum() {
        return unscaled.signum();
    }

    public boolean isZero() {
        return unscaled.signum() == 0;
    }

    public Zip64 negate() {
        if (isZero()) {
            return ZERO;
        }
        return new Zip64(unscaled.negate(), scale, false);
    }

    public Zip64 abs() {
        return unscaled.signum() < 0 ? negate() : this;
    }

    public BigDecimal toBigDecimal() {
        return new BigDecimal(unscaled, scale);
    }

    public String toPlainString() {
        String cached = plainCache;
        if (cached != null) {
            return cached;
        }
        String result = toBigDecimal().toPlainString();
        plainCache = result;
        return result;
    }
    /**
 * Zip64.java - Part 2/2
 *
 * Continuation of the professional high-performance Zip64 library.
 * This part includes arithmetic operations, parallel reductions,
 * asynchronous virtual-thread operations, and RLE encoding/decoding.
 */

    // -----------------------------
    // Arithmetic Operations
    // -----------------------------

    public Zip64 add(Zip64 other) {
        Objects.requireNonNull(other, "other");
        if (this.isZero()) return other;
        if (other.isZero()) return this;

        if (this.scale == other.scale) {
            return new Zip64(this.unscaled.add(other.unscaled), this.scale, true);
        }

        int targetScale = Math.max(this.scale, other.scale);
        BigInteger left = rescaleUnscaled(this.unscaled, this.scale, targetScale);
        BigInteger right = rescaleUnscaled(other.unscaled, other.scale, targetScale);
        return new Zip64(left.add(right), targetScale, true);
    }

    public Zip64 subtract(Zip64 other) {
        Objects.requireNonNull(other, "other");
        return this.add(other.negate());
    }

    public Zip64 multiply(Zip64 other) {
        Objects.requireNonNull(other, "other");
        if (this.isZero() || other.isZero()) return ZERO;

        BigInteger resultUnscaled = this.unscaled.multiply(other.unscaled);
        int resultScale = Math.addExact(this.scale, other.scale);
        return new Zip64(resultUnscaled, resultScale, true);
    }

    public Zip64 divide(Zip64 other, int precision) {
        return divide(other, precision, RoundingMode.HALF_UP);
    }

    public Zip64 divide(Zip64 other, int precision, RoundingMode roundingMode) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(roundingMode, "roundingMode");
        if (precision < 0) throw new IllegalArgumentException("precision must be >= 0");
        if (other.isZero()) throw new ArithmeticException("Division by zero");
        if (this.isZero()) return ZERO;

        long exponentLong = (long) precision + (long) other.scale - (long) this.scale;
        if (exponentLong > Integer.MAX_VALUE || exponentLong < Integer.MIN_VALUE)
            throw new ArithmeticException("Scale overflow");

        int exponent = (int) exponentLong;
        BigInteger numerator = this.unscaled;
        BigInteger denominator = other.unscaled;

        if (exponent > 0) numerator = numerator.multiply(pow10(exponent));
        else if (exponent < 0) denominator = denominator.multiply(pow10(-exponent));

        int sign = numerator.signum() * denominator.signum();
        BigInteger numAbs = numerator.abs();
        BigInteger denAbs = denominator.abs();

        BigInteger[] qr = numAbs.divideAndRemainder(denAbs);
        BigInteger qAbs = qr[0];
        BigInteger rAbs = qr[1];

        if (rAbs.signum() != 0) qAbs = roundQuotient(qAbs, rAbs, denAbs, sign, roundingMode);

        BigInteger signedQuotient = sign < 0 ? qAbs.negate() : qAbs;
        return new Zip64(signedQuotient, precision, true);
    }

    @Override
    public int compareTo(Zip64 other) {
        Objects.requireNonNull(other, "other");
        if (this == other) return 0;

        if (this.scale == other.scale) return this.unscaled.compareTo(other.unscaled);

        int signA = this.signum();
        int signB = other.signum();
        if (signA != signB) return Integer.compare(signA, signB);

        int targetScale = Math.max(this.scale, other.scale);
        BigInteger left = rescaleUnscaled(this.unscaled.abs(), this.scale, targetScale);
        BigInteger right = rescaleUnscaled(other.unscaled.abs(), other.scale, targetScale);

        int cmp = left.compareTo(right);
        return signA < 0 ? -cmp : cmp;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Zip64)) return false;
        Zip64 other = (Zip64) obj;
        return this.scale == other.scale && this.unscaled.equals(other.unscaled);
    }

    @Override
    public int hashCode() {
        return 31 * unscaled.hashCode() + scale;
    }

    @Override
    public String toString() {
        return toPlainString();
    }

    // -----------------------------
    // Zip64 RLE Serialization
    // -----------------------------

    public String toZipString() {
        String cached = zipCache;
        if (cached != null) return cached;

        String digits = unscaled.abs().toString();
        StringBuilder sb = new StringBuilder(digits.length() * 2 + 24);
        sb.append(unscaled.signum() < 0 ? '-' : '+')
          .append('|')
          .append(scale)
          .append('|');

        appendRle(sb, digits);

        String result = sb.toString();
        zipCache = result;
        return result;
    }

    public static Zip64 fromZipString(String text) {
        Objects.requireNonNull(text, "text");
        String input = text.trim();
        if (input.isEmpty()) throw new IllegalArgumentException("Empty Zip64 string");

        int first = input.indexOf('|');
        int second = first < 0 ? -1 : input.indexOf('|', first + 1);
        if (first <= 0 || second <= first + 1 || second == input.length() - 1)
            throw new IllegalArgumentException("Invalid Zip64 string format");

        char sign = input.charAt(0);
        if (sign != '+' && sign != '-') throw new IllegalArgumentException("Invalid Zip64 sign");

        int scale = Integer.parseInt(input.substring(first + 1, second));
        String rle = input.substring(second + 1);
        String digits = decodeRle(rle);
        BigInteger unscaled = new BigInteger((sign == '-' ? "-" : "") + digits);
        return new Zip64(unscaled, scale, true);
    }

    // -----------------------------
    // Static Utility & Async Features
    // -----------------------------

    public static Zip64 sum(List<Zip64> values) {
        Objects.requireNonNull(values, "values");
        if (values.isEmpty()) return ZERO;
        return parallelReduceRange(values, 0, values.size(), Executors.newVirtualThreadPerTaskExecutor(), Zip64::add, ZERO);
    }

    public static Zip64 product(List<Zip64> values) {
        Objects.requireNonNull(values, "values");
        if (values.isEmpty()) return ONE;
        return parallelReduceRange(values, 0, values.size(), Executors.newVirtualThreadPerTaskExecutor(), Zip64::multiply, ONE);
    }

    public static Async async() {
        return new Async();
    }

    public static final class Async implements AutoCloseable {
        private final ExecutorService executor;

        private Async() {
            this.executor = Executors.newVirtualThreadPerTaskExecutor();
        }

        public CompletableFuture<Zip64> add(Zip64 left, Zip64 right) {
            return CompletableFuture.supplyAsync(() -> left.add(right), executor);
        }

        public CompletableFuture<Zip64> multiply(Zip64 left, Zip64 right) {
            return CompletableFuture.supplyAsync(() -> left.multiply(right), executor);
        }

        public CompletableFuture<Zip64> sumAsync(List<Zip64> values) {
            return CompletableFuture.supplyAsync(() -> Zip64.sum(values), executor);
        }

        public CompletableFuture<Zip64> productAsync(List<Zip64> values) {
            return CompletableFuture.supplyAsync(() -> Zip64.product(values), executor);
        }

        @Override
        public void close() {
            executor.shutdown();
        }
    }

    // -----------------------------
    // Private Helpers
    // -----------------------------

    private static BigInteger rescaleUnscaled(BigInteger value, int currentScale, int targetScale) {
        if (value.signum() == 0) return BI_ZERO;
        if (currentScale == targetScale) return value;

        long diffLong = (long) targetScale - (long) currentScale;
        if (diffLong < 0 || diffLong > Integer.MAX_VALUE)
            throw new ArithmeticException("Scale overflow");

        return value.multiply(pow10((int) diffLong));
    }

    private static BigInteger pow10(int exp) {
        if (exp < 0) throw new IllegalArgumentException("exp must be >= 0");
        if (exp == 0) return BI_ONE;
        return POW10_CACHE.computeIfAbsent(exp, k -> BI_TEN.pow(k));
    }

    private static BigInteger roundQuotient(BigInteger qAbs, BigInteger rAbs, BigInteger divisorAbs, int sign, RoundingMode mode) {
        switch (mode) {
            case DOWN: return qAbs;
            case UP: return qAbs.add(BI_ONE);
            case CEILING: return sign > 0 ? qAbs.add(BI_ONE) : qAbs;
            case FLOOR: return sign < 0 ? qAbs.add(BI_ONE) : qAbs;
            case HALF_UP: return rAbs.shiftLeft(1).compareTo(divisorAbs) >= 0 ? qAbs.add(BI_ONE) : qAbs;
            case HALF_DOWN: return rAbs.shiftLeft(1).compareTo(divisorAbs) > 0 ? qAbs.add(BI_ONE) : qAbs;
            case HALF_EVEN: {
                int cmp = rAbs.shiftLeft(1).compareTo(divisorAbs);
                if (cmp > 0) return qAbs.add(BI_ONE);
                if (cmp < 0) return qAbs;
                return qAbs.testBit(0) ? qAbs.add(BI_ONE) : qAbs;
            }
            case UNNECESSARY: throw new ArithmeticException("Rounding necessary");
            default: throw new AssertionError("Unsupported rounding mode: " + mode);
        }
    }

    private static void appendRle(StringBuilder out, String digits) {
        int i = 0;
        while (i < digits.length()) {
            char digit = digits.charAt(i);
            int j = i + 1;
            while (j < digits.length() && digits.charAt(j) == digit) j++;
            out.append(j - i).append('x').append(digit);
            if (j < digits.length()) out.append(',');
            i = j;
        }
    }

    private static String decodeRle(String encoded) {
        StringBuilder digits = new StringBuilder();
        int i = 0;
        while (i < encoded.length()) {
            int comma = encoded.indexOf(',', i);
            int end = comma < 0 ? encoded.length() : comma;
            int xPos = encoded.indexOf('x', i);
            int count = Integer.parseInt(encoded.substring(i, xPos));
            char digit = encoded.charAt(xPos + 1);
            for (int k = 0; k < count; k++) digits.append(digit);
            if (comma < 0) break;
            i = comma + 1;
        }
        return digits.toString();
    }

    private static Zip64 parallelReduceRange(List<Zip64> values, int fromInclusive, int toExclusive, ExecutorService executor, BinaryOperator<Zip64> op, Zip64 identity) {
        int size = toExclusive - fromInclusive;
        if (size <= 256) {
            Zip64 total = identity;
            for (int i = fromInclusive; i < toExclusive; i++) total = op.apply(total, values.get(i));
            return total;
        }

        int mid = fromInclusive + (size >>> 1);
        CompletableFuture<Zip64> left = CompletableFuture.supplyAsync(() -> parallelReduceRange(values, fromInclusive, mid, executor, op, identity), executor);
        Zip64 right = parallelReduceRange(values, mid, toExclusive, executor, op, identity);
        return op.apply(left.join(), right);
    }

    // -----------------------------
    // Internal Data Classes
    // -----------------------------

    private static final class Parsed {
        private final BigInteger unscaled;
        private final int scale;

        private Parsed(BigInteger unscaled, int scale) {
            this.unscaled = unscaled;
            this.scale = scale;
        }
    }

    private static final class Normalized {
        private final BigInteger unscaled;
        private final int scale;

        private Normalized(BigInteger unscaled, int scale) {
            this.unscaled = unscaled;
            this.scale = scale;
        }
    }
}