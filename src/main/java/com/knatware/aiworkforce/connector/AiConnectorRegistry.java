package com.knatware.aiworkforce.connector;

import com.knatware.aiworkforce.model.ConnectorType;
import com.knatware.aiworkforce.model.Staff;
import org.springframework.stereotype.Component;

/**
 * Routes each AI staff member to the right connector based on its configured
 * {@link ConnectorType}. Webhook-style backends (n8n, Dify, Flowise, Langflow,
 * custom) all share the {@link WebhookConnector}; NONE uses the local
 * {@link SimulationConnector}.
 *
 * To add a bespoke connector for a backend that needs special handling, create
 * a new {@link AiConnector} and route its type here.
 */
@Component
public class AiConnectorRegistry {

    private final WebhookConnector webhook;
    private final SimulationConnector simulation;

    public AiConnectorRegistry(WebhookConnector webhook, SimulationConnector simulation) {
        this.webhook = webhook;
        this.simulation = simulation;
    }

    public AiConnector forStaff(Staff staff) {
        ConnectorType t = staff.getConnector() == null ? ConnectorType.NONE : staff.getConnector();
        return switch (t) {
            case NONE -> simulation;
            case N8N, LANGFLOW, DIFY, FLOWISE, CUSTOM_WEBHOOK -> webhook;
        };
    }
}
