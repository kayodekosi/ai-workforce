# Architecture

AI-Workforce is a Spring Boot application with a clean layered structure.

## Layers

```
controller/   REST endpoints (staff, company, chat, auth)
service/      business logic (ChatService, AuthService)
connector/    pluggable AI backend abstraction (the key extension point)
repository/   Spring Data JPA repositories
model/        JPA entities (Company, Department, Level, Staff, Chat*, AdminUser)
config/       security
bootstrap/    first-run data seeding
```

## Domain model

- **Company** — the org profile (name, description, url, address, logo, industry)
- **Department** / **Level** — org structure (configurable)
- **Staff** — AI or human; carries identity, role, reporting line, contact details,
  and (for AI) prompt, model, connector, and voice config
- **ChatChannel** / **ChatMessage** — direct, group, and conference chat
- **AdminUser** — human login accounts (AI staff never authenticate)

`Staff.reportsTo` is self-referential, modelling the org chart.

## The connector abstraction (key design)

AI staff are decoupled from any specific AI provider via `AiConnector`:

```java
public interface AiConnector {
    ConnectorType type();
    AiReply generateReply(Staff staff, String message);
}
```

- **SimulationConnector** (`ConnectorType.NONE`) — local, no external calls, so the
  platform runs and demos with zero setup.
- **WebhookConnector** — handles n8n, Dify, Flowise, Langflow, and custom webhooks,
  which all share a JSON-POST shape. The staff member's `connectorEndpoint` is called.

`AiConnectorRegistry.forStaff(staff)` routes each AI staff member to the right
connector based on its configured `ConnectorType`. The rest of the platform never
knows which backend powers a given agent.

### Adding a bespoke connector
1. Implement `AiConnector` for your backend.
2. Route its `ConnectorType` in `AiConnectorRegistry`.
That's it — any staff member configured with that type now uses it.

## Chat & AI auto-reply

When a message is posted to a channel (`ChatService.postMessage`), every **AI**
staff member in that channel (except the sender, and only if `ACTIVE`) generates
a reply through its connector. This is how:
- AI staff "talk to each other" in group/conference channels, and
- an admin **simulates** conversations to test staff behaviour.

## Security & auth

- Passwords hashed with BCrypt.
- The seeded `admin/admin` account has `mustChangePassword=true`; `AuthService`
  enforces a first-login change (min length, must differ from current).
- The starter `SecurityConfig` leaves endpoints open for easy exploration —
  tightening per-role access and adding JWT/OAuth is a documented production step.

## Persistence

Defaults to in-memory **H2** so it runs with zero setup. For production, point
`spring.datasource.*` at PostgreSQL — no code changes needed (standard JPA).
