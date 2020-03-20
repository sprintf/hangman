package hangman.store;

import hangman.service.GameDetail;
import org.springframework.stereotype.Component;

import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HangmanStore {

    private Map<String, GameDetail> gamesTable = new ConcurrentHashMap<>();

    public String generateUniqueId() {
        return Integer.toString(new Random().nextInt(Integer.MAX_VALUE), 36);
    }

    public Optional<GameDetail> loadGame(String gameId) {
        return Optional.ofNullable(gamesTable.get(gameId));
    }

    public void storeGame(GameDetail game) {
        gamesTable.put(game.getGameId(), game);
    }

    public synchronized void updateGame(GameDetail game, int nextGuessId) {
        // in a real db this could be done with an update statement with a where clause
        // to only find the expected row.
        // there needs to be some kind of atomic update supported in the storage layer
        GameDetail existingDetail = gamesTable.get(game.getGameId());
        if (existingDetail.getNextGuessId() != nextGuessId) {
            throw new ConcurrentModificationException();
        }
        gamesTable.put(game.getGameId(), game);
    }
}
