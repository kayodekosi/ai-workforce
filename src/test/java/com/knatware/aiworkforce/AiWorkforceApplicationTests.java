package com.knatware.aiworkforce;

import com.knatware.aiworkforce.connector.SimulationConnector;
import com.knatware.aiworkforce.model.Staff;
import com.knatware.aiworkforce.model.StaffType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Lightweight unit tests that don't require the full Spring context. */
class AiWorkforceApplicationTests {

    @Test
    void simulationConnectorRepliesWithPersona() {
        Staff s = new Staff();
        s.setFullName("Ada");
        s.setFunction("Support Lead");
        s.setType(StaffType.AI);
        var reply = new SimulationConnector().generateReply(s, "hello");
        assertNotNull(reply.text());
        assertTrue(reply.text().contains("Ada"));
        assertTrue(reply.text().contains("Support Lead"));
    }

    @Test
    void staffDefaultsAreSensible() {
        Staff s = new Staff();
        assertEquals(StaffType.AI, s.getType());
        assertTrue(s.isAlwaysOnline());
    }
}
