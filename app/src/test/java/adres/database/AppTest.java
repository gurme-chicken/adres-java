package adres.database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppTest {

    @Test
    void escapeNull() {
        assertEquals("\\N", SqlDumpGenerator.escape(null));
    }

    @Test
    void escapeControlChars() {
        assertEquals("a\\tb\\nc\\\\d", SqlDumpGenerator.escape("a\tb\nc\\d"));
    }

    @Test
    void escapeTurkishChars() {
        assertEquals("İĞÜŞÖÇığüşöç", SqlDumpGenerator.escape("İĞÜŞÖÇığüşöç"));
    }
}
