package com.knatware.aiworkforce.connector;

import com.knatware.aiworkforce.model.Staff;

/**
 * Abstraction over an external AI/automation backend that produces an AI staff
 * member's reply. Implementations call out to n8n, Langflow, Dify, Flowise, etc.
 *
 * This is the key extension point: to support a new backend, implement this
 * interface and register it in {@link AiConnectorRegistry}. The rest of the
 * platform is unaware of which backend powers any given AI staff member.
 */
public interface AiConnector {

    /** The connector type this implementation handles. */
    com.knatware.aiworkforce.model.ConnectorType type();

    /**
     * Produce a reply for the given AI staff member to a user/peer message.
     *
     * @param staff   the AI staff member (carries prompt, model, endpoint, etc.)
     * @param message the incoming message to respond to
     * @return the generated reply text
     */
    AiReply generateReply(Staff staff, String message);

    /** A reply plus optional voice clip URL. */
    record AiReply(String text, String voiceClipUrl) {
        public static AiReply text(String t) { return new AiReply(t, null); }
    }
}
