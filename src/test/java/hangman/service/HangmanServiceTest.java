package hangman.service;

import hangman.api.GameStatus;
import hangman.store.HangmanStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ConcurrentModificationException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {HangmanStore.class, HangmanService.class})
class HangmanServiceTest {

    @Autowired
    private HangmanService service;

    @Autowired
    private HangmanStore store;

    @Test
    void createNewGame() {
        GameDetail game = service.createNewGame();
        assertNotNull(game.getGameId());
        assertNotNull(game.getSecretWord());
        assertEquals(10, game.getGuessesRemaining());
        assertEquals(0, game.getNextGuessId());
        assertEquals(GameStatus.NEW, game.getStatus());

        assertEquals(service.getGameState(game.getGameId()), game);
    }


    @Test
    void applyGuess() {
        ImmutableGameDetail game = ImmutableGameDetail.builder()
                .secretWord("foobar")
                .status(GameStatus.NEW)
                .nextGuessId(0)
                .guessesRemaining(10)
                .gameId("id")
                .build();
        store.storeGame(game);

        GameDetail updatedDetail = service.applyGuess("id", 'a', 0);
        assertEquals(1, updatedDetail.getNextGuessId());
        assertEquals("[a]", updatedDetail.getGuesses().toString());
        assertEquals(10, updatedDetail.getGuessesRemaining());
        assertEquals(GameStatus.IN_PROGRESS, updatedDetail.getStatus());

        updatedDetail = service.applyGuess("id", 'o', 1);
        assertEquals(2, updatedDetail.getNextGuessId());
        assertEquals("[a, o]", updatedDetail.getGuesses().toString());
        assertEquals(10, updatedDetail.getGuessesRemaining());
        assertEquals(GameStatus.IN_PROGRESS, updatedDetail.getStatus());

        updatedDetail = service.applyGuess("id", 'g', 2);
        updatedDetail = service.applyGuess("id", 'j', 3);
        updatedDetail = service.applyGuess("id", 'k', 4);
        assertEquals(5, updatedDetail.getNextGuessId());
        assertEquals("[a, o, g, j, k]", updatedDetail.getGuesses().toString());
        assertEquals(7, updatedDetail.getGuessesRemaining());
        assertEquals(GameStatus.IN_PROGRESS, updatedDetail.getStatus());
    }

    @Test
    void testSuccess() {
        ImmutableGameDetail game = ImmutableGameDetail.builder()
                .secretWord("xyz")
                .status(GameStatus.NEW)
                .nextGuessId(0)
                .guessesRemaining(3)
                .gameId("id")
                .build();
        store.storeGame(game);

        GameDetail updatedDetail = service.applyGuess("id", 'x', 0);
        updatedDetail = service.applyGuess("id", 'y', 1);
        updatedDetail = service.applyGuess("id", 'z', 2);

        assertEquals("[x, y, z]", updatedDetail.getGuesses().toString());
        assertEquals(3, updatedDetail.getGuessesRemaining());
        assertEquals(GameStatus.WON, updatedDetail.getStatus());
    }

    @Test
    void testFailing() {
        ImmutableGameDetail game = ImmutableGameDetail.builder()
                .secretWord("foobar")
                .status(GameStatus.NEW)
                .nextGuessId(0)
                .guessesRemaining(3)
                .gameId("id")
                .build();
        store.storeGame(game);

        GameDetail updatedDetail = service.applyGuess("id", 'x', 0);
        updatedDetail = service.applyGuess("id", 'y', 1);
        updatedDetail = service.applyGuess("id", 'z', 2);

        assertEquals("[x, y, z]", updatedDetail.getGuesses().toString());
        assertEquals(0, updatedDetail.getGuessesRemaining());
        assertEquals(GameStatus.LOST, updatedDetail.getStatus());

        assertThrows(IllegalStateException.class,
                () -> service.applyGuess("id", 'y', 2));

    }

    @Test
    void testGuessingSameLetter() {
        ImmutableGameDetail game = ImmutableGameDetail.builder()
                .secretWord("foobar")
                .status(GameStatus.NEW)
                .nextGuessId(0)
                .guessesRemaining(3)
                .gameId("id")
                .build();
        store.storeGame(game);

        GameDetail updatedDetail = service.applyGuess("id", 'x', 0);
        updatedDetail = service.applyGuess("id", 'X', 1);
        updatedDetail = service.applyGuess("id", 'x', 1);

        assertEquals("[x]", updatedDetail.getGuesses().toString());
        assertEquals(2, updatedDetail.getGuessesRemaining());
    }

    @Test
    void testOverlappingUpdates() {
        ImmutableGameDetail game = ImmutableGameDetail.builder()
                .secretWord("foobar")
                .status(GameStatus.NEW)
                .nextGuessId(0)
                .guessesRemaining(3)
                .gameId("id")
                .build();
        store.storeGame(game);

        service.applyGuess("id", 'x', 0);
        assertThrows(ConcurrentModificationException.class,
                () -> service.applyGuess("id", 'y', 0));
    }

}