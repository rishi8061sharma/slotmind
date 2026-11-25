# SlotMind

SlotMind is a reactive clinic appointment re-scheduling system for CareBridge front-desk coordinators.

When a provider goes offline or a booked slot is cancelled, the backend identifies affected appointments, generates a replacement recommendation, and presents it to staff for confirmation.

## Project structure

```text
project - rishi/
├── backend/              Spring Boot 3.4 + Java 17 + MySQL API
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/slotmind/
│       │   ├── controller/       REST endpoints
│       │   ├── service/          Business logic and strategies
│       │   ├── repository/       Spring Data JPA repositories
│       │   ├── model/            JPA entities and enums
│       │   ├── dto/              API request/response records
│       │   ├── exception/        API error handling
│       │   └── ai/               Python gateway integration
│       └── resources/
│           ├── application.properties
│           └── db/
│               ├── slotmind_schema_seed.sql
│               └── demo_ui_seed.sql
├── frontend/              React 18 + Vite front-desk dashboard
└── python-ai-gateway/     FastAPI recommendation service
```

## Architecture

```text
React dashboard :5173
        |
        | REST / SSE
        v
Spring Boot API :8080/slotmind
        |
        ├── MySQL database: ai_rescheduling
        └── Python AI gateway :8001
```

Spring Boot owns the domain model, transactions, scheduling strategies, async events, and database persistence. The Python service is a separate HTTP service used by the AI scheduling strategy.

## Prerequisites

Install:

- Java 17 or later
- Maven 3.9+
- Node.js 18+
- npm
- Python 3.10+
- MySQL 8.0+

## 1. Create the MySQL database

```sql
CREATE DATABASE ai_rescheduling;
```

The application connects to:

```text
jdbc:mysql://localhost:3306/ai_rescheduling
```

The current local credentials are configured through environment variables. Prefer environment variables instead of committing passwords:

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
```

## 2. Load schema and initial data

Use MySQL Workbench or the MySQL command line.

Full schema and seed data:

```text
backend/src/main/resources/db/slotmind_schema_seed.sql
```

The script creates:

- `providers`
- `slots`
- `appointments`
- `reschedule_suggestions`

It also creates the generated `pending_key` and unique index used to prevent duplicate pending suggestions.

To load it from a MySQL client:

```bash
mysql -u root -p ai_rescheduling < backend/src/main/resources/db/slotmind_schema_seed.sql
```

### Load UI demo data

To display multiple scenarios in the React dashboard, run:

```text
backend/src/main/resources/db/demo_ui_seed.sql
```

This adds unique `DEMO-*` records for:

- Provider offline
- Slot cancelled
- Urgent appointment
- Manual suggestion
- Appointment with no suggestion
- Different confidence values and patient-friendly explanations

The demo script does not delete normal records.

## 3. Configure Spring Boot

Configuration file:

```text
backend/src/main/resources/application.properties
```

Important settings:

```properties
server.address=0.0.0.0
server.port=8080
server.servlet.context-path=/slotmind

spring.datasource.url=jdbc:mysql://localhost:3306/ai_rescheduling
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:your_mysql_password}

slotmind.scheduling.strategy=${SCHEDULING_STRATEGY:rule}
slotmind.ai.gateway-url=${AI_GATEWAY_URL:http://localhost:8001}
```

Available scheduling strategies:

```text
rule
ai
```

Do not set `spring.sql.init.mode=always`; the schema seed script resets tables and should be run manually.

## 4. Start the Python AI gateway

Open a terminal:

```powershell
cd python-ai-gateway
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
python -m uvicorn app:app --host 0.0.0.0 --port 8001
```

Health check:

```text
http://localhost:8001/health
```

Recommendation endpoint:

```text
POST http://localhost:8001/api/v1/schedule/recommend
```

The gateway excludes unavailable slots, excludes offline providers, validates appointment duration, and applies trigger-specific selection behavior.

## 5. Start Spring Boot

Open another terminal:

```powershell
cd backend
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
mvn spring-boot:run
```

Spring Boot base URL:

```text
http://localhost:8080/slotmind
```

Health check:

```text
http://localhost:8080/slotmind/actuator/health
```

## 6. Start React

Open another terminal:

```powershell
cd frontend
npm install
npm run dev -- --host 0.0.0.0
```

Open the dashboard:

```text
http://localhost:5173
```

Frontend API configuration is controlled by:

```text
frontend/.env.example
```

Default API URL:

```text
VITE_API_BASE_URL=http://localhost:8080/slotmind
```

## API reference

### Appointments

```http
POST /slotmind/appointments
GET  /slotmind/appointments?status=RESCHEDULE_PENDING
POST /slotmind/appointments/{id}/suggest
POST /slotmind/appointments/{id}/suggest/stream
POST /slotmind/appointments/{id}/complete
```

### Providers

```http
GET   /slotmind/providers
PATCH /slotmind/providers/{id}/status
```

Example:

```json
{
  "status": "OFFLINE"
}
```

### Slots

```http
GET   /slotmind/slots
PATCH /slotmind/slots/{id}/status
```

Example:

```json
{
  "status": "CANCELLED"
}
```

### Suggestions

```http
GET   /slotmind/suggestions?status=PENDING
PATCH /slotmind/suggestions/{id}
```

Accept:

```json
{
  "status": "ACCEPTED"
}
```

Reject:

```json
{
  "status": "REJECTED"
}
```

## Important API behavior

- Invalid provider or slot status returns `400 Bad Request`.
- Unknown resources return `404 Not Found`.
- Invalid suggestion transitions return `409 Conflict`.
- Accepting a suggestion is transactional.
- The old slot, new slot, appointment, and suggestion are updated atomically.
- Pessimistic database locks prevent concurrent double-booking.
- Duplicate pending suggestions are prevented by application logic and the database generated-column unique index.
- Provider-offline and slot-cancelled events are processed asynchronously after transaction commit.
- AI failures fall back to the rule-based strategy.

## SSE event flow

Endpoint:

```text
POST /slotmind/appointments/{id}/suggest/stream
```

Normal response:

```text
event:reasoning
event:recommendation
event:complete
```

AI fallback response:

```text
event:reasoning
event:error
event:recommendation
event:complete
```

## Domain state machines

Appointment:

```text
CONFIRMED -> RESCHEDULE_PENDING -> RESCHEDULED -> COMPLETED
```

Provider:

```text
AVAILABLE <-> BUSY
AVAILABLE/BUSY -> OFFLINE
```

Slot:

```text
OPEN -> BOOKED
OPEN/BOOKED -> CANCELLED
```

Suggestion:

```text
PENDING -> ACCEPTED
PENDING -> REJECTED
```

## Scheduling strategies

### Rule-based strategy

The rule strategy:

1. Removes booked and cancelled slots.
2. Removes slots from offline providers.
3. Removes slots shorter than the required duration.
4. Prefers the same provider.
5. Prefers matching specialty.
6. Orders candidates by nearest start time.

### AI strategy

The AI strategy sends appointment details, slots, providers, urgency/type, duration, and trigger reason to the Python gateway.

The response is validated against actual database data. Invalid slot IDs, provider IDs, confidence values, unavailable slots, or gateway failures use the rule-based fallback.

## Design patterns used

- Singleton: Spring-managed services and components
- Factory: appointment creation boundary
- Builder: domain construction extension point
- Prototype: suggestion template extension point
- Registry: runtime scheduling strategy lookup
- Observer: provider-offline and slot-cancelled events
- Strategy: rule-based and AI scheduling strategies
- Decorator: availability checking around a scheduling strategy

## Test and verification commands

Backend compile/package:

```bash
cd backend
mvn clean package -DskipTests
```

Frontend build:

```bash
cd frontend
npm install
npm run build
```

Python syntax check:

```bash
cd python-ai-gateway
python -m py_compile app.py
```

Health checks:

```text
http://localhost:8080/slotmind/actuator/health
http://localhost:8001/health
http://localhost:5173
```

## Troubleshooting

### MySQL access denied

Check the configured environment variables:

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
```

Then restart Spring Boot.

### React cannot call Spring Boot

Confirm:

- Spring Boot is running on port `8080`.
- React is running on port `5173`.
- The frontend API URL is `http://localhost:8080/slotmind`.
- CORS allows `http://localhost:5173`.

### Python gateway unavailable

Start it with:

```powershell
cd python-ai-gateway
python -m uvicorn app:app --host 0.0.0.0 --port 8001
```

The AI strategy should still return a rule-based recommendation when the Python gateway is unavailable.

### Stale frontend or backend process

Stop existing processes using ports `5173` and `8080`, then restart both applications.

### Schema mismatch after model changes

For development, either run the schema script against a clean database or use the existing Hibernate update behavior. Do not run the reset seed script against data that must be preserved.