package hangman.service;

import hangman.api.GameStatus;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
public interface GameDetail {

    String getGameId();

    String getSecretWord();

    int getGuessesRemaining();

    int getNextGuessId();

    List<Character> getGuesses();

    GameStatus getStatus();
}
