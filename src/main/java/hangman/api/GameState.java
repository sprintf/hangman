package hangman.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@Value.Style(jdkOnly=true)
@JsonSerialize(as = ImmutableGameState.class)
@JsonDeserialize(as = ImmutableGameState.class)
public interface GameState {

    int getGuessesRemaining();

    int getNextGuessId();

    List<Character> getFailedGuesses();

    String getMatchingLetters();

    GameStatus getStatus();

}
