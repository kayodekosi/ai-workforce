package com.knatware.aiworkforce.connector;

import com.knatware.aiworkforce.model.ConnectorType;
import com.knatware.aiworkforce.model.Staff;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Generic webhook connector covering n8n, Dify, Flowise and custom webhooks —
 * all of which accept a JSON POST and return a JSON reply. The staff member's
 * configured {@code connectorEndpoint} is called with the prompt + message.
 *
 * One class handles several backends because they share the webhook shape; the
 * differences (auth headers, field names) are configuration, kept minimal here
 * and documented as an extension point.
 */
@Component
public class WebhookConnector implements AiConnector {

    private final RestClient http = RestClient.create();

    @Override
    public ConnectorType type() {
        // This connector is selected for any webhook-style backend; the registry
        // maps several ConnectorTypes to it (see AiConnectorRegistry).
        return ConnectorType.CUSTOM_WEBHOOK;
    }

    @Override
    public AiReply generateReply(Staff staff, String message) {
        String endpoint = staff.getConnectorEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            // No endpoint configured yet — fall back to a clearly-labelled stub so
            // the platform still functions end-to-end during setup/demo.
            return AiReply.text("[" + staff.getFullName() + " — " + staff.getConnector()
                    + " endpoint not configured] (echo) " + message);
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = http.post()
                    .uri(endpoint)
                    .body(Map.of(
                            "systemPrompt", staff.getSystemPrompt() == null ? "" : staff.getSystemPrompt(),
                            "model", staff.getModel() == null ? "" : staff.getModel(),
                            "message", message,
                            "staffName", staff.getFullName()
                    ))
                    .retrieve()
                    .body(Map.class);
            Object reply = resp == null ? null : resp.getOrDefault("reply", resp.get("output"));
            String text = reply == null ? "(no reply field in backend response)" : reply.toString();
            return AiReply.text(text);
        } catch (Exception e) {
            return AiReply.text("[connector error contacting " + staff.getConnector()
                    + ": " + e.getMessage() + "]");
        }
    }
}
