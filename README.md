# JavaInternals

A hands-on learning repo for understanding JVM and Java internals — not just theory, but working MVPs that force the JVM to show its internals. Each module builds on the previous one.

---

## Repository structure

```
JavaInternals/
│
├── phase-1-jvm-classloading/
│   ├── 01-classloader-hierarchy/
│   ├── 02-bytecode-javap/
│   └── 03-memory-layout/
│
├── phase-2-garbage-collection/
│   ├── 01-gc-algorithms/
│   ├── 02-reference-types/
│   └── 03-memory-leaks/
│
├── phase-3-concurrency/
│   ├── 01-java-memory-model/
│   ├── 02-thread-pool-scratch/
│   ├── 03-locks-deadlocks/
│   └── 04-cas-atomics/
│
├── phase-4-io-reflection-runtime/
│   ├── 01-nio-vs-bio/
│   ├── 02-reflection-di/
│   ├── 03-java-agents/
│   └── 04-jit-compilation/
│
└── README.md
```

---

## Progress

### Phase 1 — JVM & class loading

> Goal: understand how the JVM finds, loads, and represents your code before a single line of your logic runs.

- [x] **01 — Class loader hierarchy**
  - Bootstrap → Platform → Application loader chain
  - Custom `ClassLoader` that reads `.class` files from an arbitrary directory
  - Class loader isolation: same bytecode, two loaders, two incompatible types
  - Key insight: `loadClass()` owns delegation; `findClass()` is your only required override

- [ ] **02 — Bytecode & javap**
  - Disassemble `.class` files with `javap -c -verbose`
  - Read opcodes: `invokevirtual`, `invokespecial`, `ldc`, `iload`, operand stack mechanics
  - Rewrite bytecode at runtime using the ASM library
  - MVP: method timing agent that injects entry/exit timestamps via ASM

- [ ] **03 — Memory layout**
  - Heap regions: eden, survivor (S0/S1), old gen
  - Thread stacks, stack frames, and metaspace (where class metadata lives)
  - Object header layout: mark word + class pointer
  - Tools: `jmap`, `jcmd`, VisualVM
  - MVP: heap explorer — allocate objects, inspect sizes, observe region promotion

---

### Phase 2 — Garbage collection

> Goal: understand how the JVM reclaims memory, how to read what it is doing, and how to stop it from surprising you in production.

- [ ] **01 — GC algorithms**
  - Serial, Parallel, G1, ZGC — when each activates and what it trades off
  - Read GC logs: `-Xlog:gc*`, pause times, throughput vs latency
  - MVP: GC log analyser that flags pauses above a configurable threshold

- [ ] **02 — Reference types**
  - Strong, soft, weak, phantom — how each affects reachability
  - `ReferenceQueue` and how caches (e.g. `WeakHashMap`) use it
  - MVP: demo that observes each reference type survive or die across GC cycles

- [ ] **03 — Memory leaks**
  - Classic leak patterns: static collections, classloader leaks, unregistered listeners
  - Detect with `jmap -histo`, heap dumps, and Eclipse MAT / VisualVM
  - Fix and verify with a second heap snapshot
  - MVP: intentional leak program + step-by-step diagnosis walkthrough

---

### Phase 3 — Concurrency internals

> Goal: understand what the JVM and CPU actually do with your threads — beyond what `synchronized` looks like syntactically.

- [ ] **01 — Java memory model**
  - Happens-before, visibility guarantees, instruction reordering
  - Why code that looks correct can break without `volatile`
  - MVP: visibility bug demo — prove the bug exists, then fix it

- [ ] **02 — Thread pool from scratch**
  - Implement a fixed-size pool using `BlockingQueue` and `Runnable` — no `Executors`
  - Worker lifecycle, graceful shutdown, task rejection
  - MVP: mini thread pool with a configurable queue and shutdown hook

- [ ] **03 — Locks & deadlocks**
  - `synchronized` vs `ReentrantLock` vs `ReadWriteLock` vs `StampedLock`
  - Create a deadlock intentionally; detect it with `jstack`
  - MVP: lock profiler that measures contention across strategies

- [ ] **04 — CAS & atomics**
  - How `AtomicInteger` works under the hood: compare-and-swap at the CPU level
  - ABA problem and `AtomicStampedReference`
  - MVP: lock-free stack using `AtomicReference`

---

### Phase 4 — I/O, reflection & runtime power tools

> Goal: understand the runtime machinery that frameworks like Spring Boot use — so none of it is magic anymore.

- [ ] **01 — NIO vs BIO**
  - Blocking I/O model vs non-blocking channels and `Selector`
  - How Netty and Spring WebFlux build on NIO
  - MVP: echo server in both models, benchmarked under concurrent connections

- [ ] **02 — Reflection & dependency injection**
  - `Class`, `Field`, `Method`, `Constructor` API
  - Dynamic proxies: `java.lang.reflect.Proxy` and what Spring does with `@Transactional`
  - MVP: mini IoC container — component scan, constructor injection, lifecycle callbacks

- [ ] **03 — Java agents**
  - `java.lang.instrument`, `premain`, `agentmain`
  - Attach to a running JVM without restart (`VirtualMachine.attach`)
  - MVP: method timing agent — inject entry/exit timestamps without touching source code

- [ ] **04 — JIT compilation**
  - How HotSpot identifies hot methods and compiles them to native code
  - Tiered compilation (C1 → C2), inlining, escape analysis
  - Read `-XX:+PrintCompilation` output
  - MVP: JIT log reader that shows which methods got compiled and when

---

## Tools used across the repo

| Tool | Purpose |
|---|---|
| `javap -c -verbose` | Disassemble bytecode |
| `jmap -histo <pid>` | Histogram of live objects |
| `jmap -dump:format=b,file=heap.hprof <pid>` | Full heap dump |
| `jstack <pid>` | Thread dump — deadlock detection |
| `jcmd <pid> VM.flags` | All active JVM flags |
| `-Xlog:gc*` | GC log (Java 9+) |
| `-verbose:class` | Watch every class being loaded |
| `-XX:+PrintCompilation` | Watch JIT compilation in real time |
| VisualVM | GUI for heap, threads, GC, profiling |
| Eclipse MAT | Deep heap dump analysis |
| ASM library | Bytecode manipulation |

---

## How to run each module

Every subdirectory is self-contained. Each has its own `README.md` with:
- what the MVP demonstrates
- compile and run commands
- what to observe in the output
- suggested experiments to try

```bash
cd phase-1-jvm-classloading/01-classloader-hierarchy
javac src/*.java -d out/
java -cp out Stage1Inspector
```

---

## Key mental model

Each phase answers one question:

| Phase | Question |
|---|---|
| 1 — Class loading | How does a `.class` file become a live object in the JVM? |
| 2 — GC | How does the JVM decide what memory to reclaim and when? |
| 3 — Concurrency | What do threads, locks, and visibility actually mean at the CPU level? |
| 4 — Runtime tools | How do frameworks like Spring instrument and rewire your code at runtime? |