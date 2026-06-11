# Walkthrough

A hands-on tour. Start the app first:

```bash
mvn spring-boot:run
```

It seeds a sample company, AI staff, and the default admin, then listens on
`http://localhost:8080`.

## 1. First login & password change

```bash
# Log in with the seeded default credentials
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
# -> { "ok": true, "mustChangePassword": true, "message": "...must change your password..." }

# Change it (required before normal use)
curl -X POST http://localhost:8080/api/auth/change-password \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","oldPassword":"admin","newPassword":"newSecret123"}'
```

## 2. Configure the company

```bash
curl http://localhost:8080/api/company           # view
curl -X PUT http://localhost:8080/api/company/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"My AI Co","description":"...","url":"https://my.co","address":"...","industry":"Sales"}'
```

## 3. Departments & levels

```bash
curl -X POST http://localhost:8080/api/company/departments \
  -H "Content-Type: application/json" -d '{"name":"Engineering"}'

curl -X POST http://localhost:8080/api/company/levels \
  -H "Content-Type: application/json" -d '{"name":"Director","rank":5}'
```

## 4. Onboard an AI staff member (full config)

```bash
curl -X POST http://localhost:8080/api/staff \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Nova (Sales SDR)",
    "email": "nova@my.co",
    "phoneExtension": "2001",
    "position": "Sales Development Rep",
    "function": "SDR",
    "duties": "Qualifies inbound leads and books demos.",
    "type": "AI",
    "alwaysOnline": true,
    "systemPrompt": "You are Nova, an upbeat SDR. Qualify leads using BANT.",
    "model": "gpt-4o",
    "connector": "N8N",
    "connectorEndpoint": "https://my-n8n/webhook/nova",
    "voiceEnabled": true,
    "voiceProvider": "elevenlabs",
    "voiceId": "nova-voice"
  }'
```

Set `"connector": "NONE"` to run a staff member in **simulation mode** (no
external backend needed) — handy for trying the platform immediately.

## 5. Put a staff member on leave / reactivate

```bash
curl -X POST http://localhost:8080/api/staff/2/leave
curl -X POST http://localhost:8080/api/staff/2/activate
```

On-leave AI staff stop replying in chat until reactivated.

## 6. Create a conference channel & simulate a conversation

```bash
# A conference with AI staff ids 1 and 2
curl -X POST http://localhost:8080/api/chat/channels \
  -H "Content-Type: application/json" \
  -d '{"name":"Standup","type":"CONFERENCE","memberIds":[1,2]}'

# Post a message as staff 1 — every AI member replies via its connector
curl -X POST http://localhost:8080/api/chat/channels/1/messages \
  -H "Content-Type: application/json" \
  -d '{"senderId":1,"content":"Any blockers today?"}'
# -> returns the original message PLUS auto-generated AI replies

# Read full history
curl http://localhost:8080/api/chat/channels/1/messages
```

This is how an admin **simulates** how AI staff behave and converse — exactly the
behaviour you'd wire to a real n8n/Dify/Langflow flow in production by setting the
staff member's connector + endpoint.

## 7. Inspecting data (H2 console)

Visit `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:aiworkforce`,
user `sa`, no password) to browse the tables directly.
