package hangman.store;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HangmanStoreTest {

    @Test
    void testIdLength() {
        HangmanStore store = new HangmanStore();
        assertTrue(store.generateUniqueId().length() < 10);
    }

}