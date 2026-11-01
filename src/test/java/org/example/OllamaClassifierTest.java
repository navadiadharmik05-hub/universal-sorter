package org.example;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OllamaClassifierTest {

    private static Classification parse(String raw) {
        JsonObject result = JsonRepair.parseAndRepair(raw);
        String category = result.has("category") ? result.get("category").getAsString() : "Misc";
        double confidence = result.has("confidence") ? result.get("confidence").getAsDouble() : 0;
        confidence = Math.max(0, Math.min(1, confidence));
        if (confidence < 0.75) {
            category = "Needs_Review";
        }
        return new Classification(category, confidence, false, "", "", null);
    }

    @Test
    void testParseValidJson() {
        String raw = "{\"category\": \"Finance_Invoices\", \"confidence\": 0.95}";
        Classification c = parse(raw);
        assertEquals("Finance_Invoices", c.category());
        assertEquals(0.95, c.confidence(), 0.001);
        assertFalse(c.ruleBased());
    }

    @Test
    void testParseWithThinkBlock() {
        String raw = "<think>\nThinking about categories...\n</think>\n{\"category\": \"Academics\", \"confidence\": 0.90}";
        Classification c = parse(raw);
        assertEquals("Academics", c.category());
        assertEquals(0.90, c.confidence(), 0.001);
    }

    @Test
    void testParseWithExtraText() {
        String raw = "Here is the categorization result: {\"category\": \"Code_Projects\", \"confidence\": 0.85} hope this helps!";
        Classification c = parse(raw);
        assertEquals("Code_Projects", c.category());
        assertEquals(0.85, c.confidence(), 0.001);
    }

    @Test
    void testParseLowConfidenceFallback() {
        String raw = "{\"category\": \"Images\", \"confidence\": 0.40}";
        Classification c = parse(raw);
        assertEquals("Needs_Review", c.category());
        assertEquals(0.40, c.confidence(), 0.001);
    }

    @Test
    void testParseInvalidJsonThrows() {
        String raw = "Not json at all";
        assertThrows(IllegalArgumentException.class, () -> parse(raw));
    }

    @Test
    void testOllamaIntegrationIfAvailable() {
        Config config = Config.load();
        Orchestrator orchestrator = new Orchestrator(config);
        Assumptions.assumeTrue(orchestrator.isAvailable(), "Ollama is not running; skipping integration test.");

        try {
            Classification c = orchestrator.classify("Electricity bill amount $120 due October 2026", "electricity_bill_october.pdf");
            assertNotNull(c);
            assertNotNull(c.category());
            assertTrue(c.confidence() >= 0 && c.confidence() <= 1);
            System.out.println("Integration classification result: " + c.category() + " (confidence: " + c.confidence() + ")");
        } catch (Exception e) {
            fail("Ollama classification failed: " + e.getMessage());
        }
    }
}
