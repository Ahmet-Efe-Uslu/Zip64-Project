# Zip64: High-Performance Arbitrary Precision Decimal Library

**Version:** 1.0  
**Author:** ChatGPT 5.X (Vibe Coded)  
**Date:** 2026-03-29  

---

## Overview

**Zip64** is a high-performance, arbitrary-precision decimal arithmetic library for Java 25+. It solves floating-point precision problems while remaining extremely fast, both synchronously and asynchronously using virtual threads. Zip64 is independent of any game engine or framework, making it ideal for scientific computation, finance, and high-precision mathematics in any Java environment.

---

## Key Features

- Arbitrary precision arithmetic using `BigInteger` backend.  
- Addition, subtraction, multiplication, division with configurable rounding modes.  
- Efficient RLE-based serialization and deserialization (`Zip64` string format).  
- Immutable and thread-safe design.  
- Virtual-thread asynchronous operations for scalable computations.  
- Parallel sum and product reductions over large lists.  
- Normalization of trailing zeros to reduce memory and computation overhead.  
- Caching for commonly used values (0, 1, 10).  
- Conversion to `BigDecimal` and plain strings.  
- Comprehensive sign and zero handling.  
- High-performance for both single-threaded and multi-threaded workloads.  
- Ready for very large numbers (100,000+ digits supported).  
- Fully tested with benchmarks for synchronous and asynchronous operations.  
- Robust exception handling and validation.  

---

## Installation

Copy the `Zip64.java` file into your project:

```bash
src/
└── main/
    └── java/
        └── your/package/
            └── Zip64.java

Compile with Java 25+:

javac Zip64.java


---

Usage

import java.math.BigInteger;

public class Example {
    public static void main(String[] args) {
        Zip64 a = new Zip64("12345678901234567890");
        Zip64 b = new Zip64("98765432109876543210");

        Zip64 sum = a.add(b);
        Zip64 product = a.multiply(b);

        System.out.println("Sum: " + sum);
        System.out.println("Product: " + product);

        // Asynchronous usage with virtual threads
        try (Zip64.Async async = Zip64.async()) {
            Zip64 result = async.add(a, b).join();
            System.out.println("Async Sum: " + result);
        }
    }
}


---

Benchmark Example

Included in Zip64Test.java, benchmarks test:

Addition and multiplication of extremely large numbers (50k+ digits).

Asynchronous sum and product using virtual threads.

Execution time and result digit count measurement.



---

Serialization Format

Zip64 uses a custom RLE-based string format for storage and transmission:

[sign]|[scale]|[RLE digits]

Example:

+|5|10x1,5x2,2x3

This allows extremely large numbers to be stored efficiently.


---

Contributing

Contributions are welcome! Please fork the repository and submit pull requests for:

New mathematical operations

Performance optimizations

Bug fixes

Documentation improvements



---

License

MIT License © 2026 PixelPhantom

Zip64 enables accurate, fast, and scalable high-precision mathematics for Java developers everywhere. Perfect for game engines, scientific simulations, finance, or any application where floating-point precision just isn’t enough.

---
