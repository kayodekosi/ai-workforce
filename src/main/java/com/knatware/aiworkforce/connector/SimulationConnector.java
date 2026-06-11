package com.knatware.aiworkforce.connector;

import com.knatware.aiworkforce.model.ConnectorType;
import com.knatware.aiworkforce.model.Staff;
import org.springframework.stereotype.Component;

/**
 * A local, no-external-call connector used when ConnectorType is NONE. It lets
 * the platform run and be demoed/simulated with zero external setup — the admin
 * can simulate AI staff replies immediately. Replies reflect the staff persona
 * so the simulation feels real without calling out anywhere.
 */
@Component
public class SimulationConnector implements AiConnector {

    @Override
    public ConnectorType type() { return ConnectorType.NONE; }

    @Override
    public AiReply generateReply(Staff staff, String message) {
        String persona = staff.getFunction() == null ? staff.getPosition() : staff.getFunction();
        String reply = "Hi, this is " + staff.getFullName()
                + (persona != null ? " (" + persona + ")" : "")
                + ". I've received your message: \"" + message + "\". "
                + "In a configured deployment I'd handle this via my "
                + "assigned workflow. (Simulated reply — no external AI backend attached.)";
        return AiReply.text(reply);
    }
}
