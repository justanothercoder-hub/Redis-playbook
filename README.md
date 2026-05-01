# Enterprise Redis Architecture: Spring Boot E-Commerce API

## 🎯 Project Aim

This project is an enterprise-grade backend architecture built to demonstrate how to solve complex scaling, performance, and data integrity problems using **Spring Boot** and **Redis**.

Rather than just using Redis as a simple key-value cache, this API utilizes advanced Redis data structures (Lists, Sorted Sets, atomic commands) to implement 5 core enterprise patterns: **Caching, Rate Limiting, Real-time Analytics, Asynchronous Queues, and Distributed Locks.**

This project acts as a blueprint for handling high-traffic events (like Black Friday sales) without database bottlenecking or data corruption.

---

## 🚀 Core Features Demonstrated

1. **High-Speed Caching (Strings)**
    - Bypasses the primary database for frequent read requests.
    - Utilizes Spring's `@Cacheable`, `@CachePut`, and `@CacheEvict` annotations to ensure data consistency between the DB and Redis.

2. **API Security / Rate Limiting (TTL & INCR)**
    - Protects the API from spam and DDoS attacks.
    - Uses Redis `INCR` and `EXPIRE` commands to track requests per user. Blocks users with an `HTTP 429 Too Many Requests` if they exceed the limit (e.g., 5 requests per minute).

3. **Real-Time Analytics (ZSets & Lists)**
    - **Most Popular Leaderboard:** Uses Redis Sorted Sets (`ZINCRBY`) to track the most viewed products globally in O(log(N)) time.
    - **Recently Viewed History:** Uses Redis Lists (`LPUSH`, `LTRIM`) to maintain a capped list of the last 10 items a specific user viewed.

4. **Asynchronous Background Processing (Lists as Queues)**
    - Decouples slow tasks (like sending promotional emails) from the main API thread.
    - The API pushes jobs to a Redis List. A dedicated `@Scheduled` background worker (`NotificationWorker`) safely pops and processes these jobs, ensuring the user gets a `200 OK` instantly.

5. **Distributed Locks / Race Condition Prevention (SET NX PX)**
    - Prevents "Double Spending" and inventory overselling during concurrent checkout requests.
    - Implements a custom lock using pure Redis atomic commands (`SET key value NX PX timeout`) with a spin-lock retry mechanism and safe `finally` block releases.

---

## 🛠️ Prerequisites & Requirements

To run and test this project locally, you will need:

- **Java 17+**
- **Maven**
- **Docker Desktop** — to run the Redis container
- **Postman** — for API testing
- **Git Bash** — required for concurrent stress-testing the Distributed Lock

---

## ⚙️ Installation & Setup

**1. Clone the repository:**

```bash
git clone https://github.com/YOUR_USERNAME/spring-boot-redis-architecture.git
cd spring-boot-redis-architecture
```

**2. Start the Redis Server:**

Ensure Docker is running, then use the provided `docker-compose.yml` file to spin up Redis:

```bash
docker-compose up -d
```

**3. Run the Spring Boot Application:**

Run the project via your IDE (IntelliJ / Eclipse) or via Maven:

```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`.

---

## 🧪 How to Test the Features

### 1. Test Caching & The Async Queue

**Action:** Create a new product.

```
POST http://localhost:8080/products
```

**Request Body:**

```json
{
    "name": "Bluetooth Headphones",
    "price": 120.00,
    "stockQuantity": 50
}
```

**Result:** You will get a `200 OK` response instantly. Look at your IDE terminal — within 3 seconds, the background `NotificationWorker` will wake up and print:

```
[PROCESSING JOB] EMAIL_JOB: Send promo for new Bluetooth Headphones.
```

---

### 2. Test Rate Limiting & Analytics

**Action:** Rapidly fetch a product multiple times.

```
GET http://localhost:8080/products/1
```

**Result:** The first few requests will instantly return the product JSON (and quietly update the Redis Analytics ZSets/Lists in the background). If you spam the request more than 5 times within a single minute, the Rate Limiter will intercept it and return:

```
HTTP 429: Too Many Requests. You are blocked for the rest of the minute.
```

---

### 3. The Ultimate "Black Friday" Stress Test (Distributed Locks)

This proves the architecture can handle race conditions without overselling inventory.

**Step 1 — Create a scarce item:**

```
POST http://localhost:8080/products
```

```json
{
    "name": "RTX 4090 Graphics Card",
    "price": 1500.00,
    "stockQuantity": 1
}
```

> Note the `id` returned (e.g., `id: 5`).

**Step 2 — The Concurrent Attack (using Git Bash):**

Open Git Bash and paste the following command to fire two exact-millisecond concurrent purchase requests (replace `5` with your actual product ID):

```bash
curl -s -X POST http://localhost:8080/products/5/buy & \
curl -s -X POST http://localhost:8080/products/5/buy & \
wait
```

**Result:** Your terminal will output two different responses:

- ✅ `Successfully purchased! Remaining stock: 0` — Thread 1 acquired the lock and bought the item.
- ❌ `Sorry, out of stock!` — Thread 2 was blocked by the lock, forced to wait in the spin-loop, and safely rejected once the stock hit 0.

> **Data Integrity Maintained:** The database stock accurately stops at `0` and never drops to `-1`.

---

## 🧠 Architectural Insights & Caveats

**Queue Reality (At-Most-Once Delivery)**

The asynchronous queue uses the Redis `rightPop()` command. The exact millisecond the worker grabs the job, it is deleted from Redis memory. If the server crashes during processing, the job is permanently lost. This is an acceptable tradeoff for promotional emails, but financial transactions would require an "At-Least-Once" queue architecture (e.g., RabbitMQ, Kafka, or Redis Streams).

**Distributed Lock Safety**

The `RedisLockService` relies on a strict TTL (`Duration.ofMillis(5000)`) via the `PX` parameter as deadlock prevention. If the API crashes or loses power while holding the key, Redis will automatically destroy the lock after 5 seconds, allowing the system to recover autonomously.

**Lock Releasing**

Locks are strictly released inside a `finally` block within the controller to guarantee the key is returned even if the database throws a fatal exception during the critical read/write section.