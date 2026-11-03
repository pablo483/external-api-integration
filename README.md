# External API Integration Service

An event-driven **Spring Boot 3.5** backend service demonstrating production integration patterns: non-blocking third-party HTTP clients (**WebClient**), **Redis** TTL caching with connection pooling, asynchronous **Apache Kafka** audit event pipelines, and **Prometheus / Actuator** health observability.

## Key Engineering Highlights

- **Reactive HTTP Clients (`WebClient`):** Integrates external Weather (OpenWeatherMap) and Currency Exchange APIs with configurable timeouts and isolated exception handling (`ExternalApiException`).
- **Redis Read-Through Caching:** Caches external weather responses (`@Cacheable` with 10-minute TTL, `integration::` key prefix, null-caching disabled, and Lettuce pool tuning) to minimize third-party API latency and rate-limit consumption.
- **Non-Blocking Kafka Audit Pipeline:** Every external API call asynchronously publishes an `AuditEvent` via `KafkaAuditEventProducer` (`acks=all`, retries enabled) and processes it in `KafkaAuditEventConsumer` using `manual_immediate` offset acknowledgments. Audit failures are isolated so downstream Kafka issues never block user API responses.
- **Observability Ready:** Exposes `/actuator/health`, `/actuator/metrics`, and `/actuator/prometheus` with Redis and Kafka health indicators enabled.

## Tech Stack

- **Language & Framework:** Java 17, Spring Boot 3.5.9, Spring WebFlux (`WebClient`)
- **Caching:** Redis 7 (`Spring Data Redis`, Lettuce connection pool)
- **Messaging:** Apache Kafka (`Spring for Apache Kafka`)
- **Monitoring:** Spring Boot Actuator, Micrometer / Prometheus
- **Containerization:** Docker & Docker Compose

## API Endpoints

| Method | Endpoint | Query Params | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/weather` | `city` *(required, e.g. `Kathmandu`)* | Fetch current weather (cached in Redis for 10 min + async Kafka audit event) |
| `GET` | `/api/currency/latest` | `base` *(default `USD`)*, `symbols` *(optional)* | Fetch latest currency exchange rates |
| `GET` | `/actuator/health` | — | Application, Redis, and Kafka health status |

## Quick Start (Docker Compose)

1. **Clone the repository**
   ```bash
   git clone https://github.com/pablo483/external-api-integration.git
   cd external-api-integration
   ```

2. **Set your OpenWeatherMap API key**
   ```bash
   export WEATHER_API_KEY="your_openweather_api_key"
   ```

3. **Start Kafka, Redis, and the Spring Boot service**
   ```bash
   docker compose up --build -d
   ```
   The API will be available at `http://localhost:8081` (mapped to container port `8080`).

4. **Test an endpoint**
   ```bash
   curl "http://localhost:8081/api/weather?city=Kathmandu"
   ```

## Running Locally Without Docker

Ensure local **Redis** (`localhost:6379`) and **Kafka** (`localhost:9092`) instances are running, then execute:

```bash
WEATHER_API_KEY=your_key ./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--spring.data.redis.host=localhost --spring.kafka.bootstrap-servers=localhost:9092"
```

## Author

**Sudarshan GC** — [GitHub](https://github.com/pablo483) • [sudarshangc580@gmail.com](mailto:sudarshangc580@gmail.com)
