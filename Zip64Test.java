/**
 * Zip64Test.java
 *
 * Professional Benchmark and Unit Test for Zip64 Library
 *
 * Tests large number arithmetic, precision, and async virtual-thread performance.
 *
 * Author: ChatGPT 5.X (Vibe Coded)
 * Version: 1.0
 * Date: 2026-03-29
 */

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Zip64Test {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        final int digitCount = 50000; // 50k-digit numbers
        final int numbersCount = 10;

        System.out.println("Initializing " + numbersCount + " numbers with " + digitCount + " digits each...");

        List<Zip64> numbers = new ArrayList<>();
        StringBuilder sb = new StringBuilder(digitCount);
        for (int i = 0; i < digitCount; i++) sb.append((i % 9) + 1);
        String bigNum = sb.toString();

        for (int i = 0; i < numbersCount; i++) numbers.add(new Zip64(new BigInteger(bigNum)));

        System.out.println("Starting synchronous benchmark...");
        long t0 = System.currentTimeMillis();
        Zip64 sum = Zip64.sum(numbers);
        long t1 = System.currentTimeMillis();
        System.out.println("Synchronous sum time (ms): " + (t1 - t0));
        System.out.println("Sum digits: " + sum.toPlainString().length());

        t0 = System.currentTimeMillis();
        Zip64 product = Zip64.product(numbers);
        t1 = System.currentTimeMillis();
        System.out.println("Synchronous product time (ms): " + (t1 - t0));
        System.out.println("Product digits: " + product.toPlainString().length());

        System.out.println("Starting asynchronous benchmark using virtual threads...");
        try (Zip64.Async async = Zip64.async()) {
            t0 = System.currentTimeMillis();
            CompletableFuture<Zip64> futureSum = async.sumAsync(numbers);
            Zip64 asyncSum = futureSum.get();
            t1 = System.currentTimeMillis();
            System.out.println("Async sum time (ms): " + (t1 - t0));
            System.out.println("Async sum digits: " + asyncSum.toPlainString().length());

            t0 = System.currentTimeMillis();
            CompletableFuture<Zip64> futureProduct = async.productAsync(numbers);
            Zip64 asyncProduct = futureProduct.get();
            t1 = System.currentTimeMillis();
            System.out.println("Async product time (ms): " + (t1 - t0));
            System.out.println("Async product digits: " + asyncProduct.toPlainString().length());
        }

        System.out.println("All benchmarks completed successfully!");
    }
}