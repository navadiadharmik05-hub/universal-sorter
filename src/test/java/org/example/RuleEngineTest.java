package org.example;

import org.junit.jupiter.api.Test;
import java.io.File;
import static org.junit.jupiter.api.Assertions.*;

class RuleEngineTest {

    @Test
    void testRuleEvaluationExtensions() {
        assertEquals("Installers", RuleEngine.evaluate(new File("setup.exe")));
        assertEquals("Images", RuleEngine.evaluate(new File("photo.png")));
        assertEquals("Videos", RuleEngine.evaluate(new File("movie.mp4")));
        assertEquals("Code_Projects", RuleEngine.evaluate(new File("Main.java")));
    }

    @Test
    void testRuleEvaluationKeywords() {
        assertEquals("Finance_Invoices", RuleEngine.evaluate(new File("invoice_2026.pdf")));
        assertEquals("Academics", RuleEngine.evaluate(new File("my_resume.pdf")));
    }
}
