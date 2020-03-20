package hangman.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableGameResponse.class)
@JsonDeserialize(as = ImmutableGameResponse.class)
public interface GameResponse {

    String getGameId();

    int getNumberOfLetters();

    GameState getState();

}
