package hangman.service;

import com.google.common.collect.ImmutableList;
import hangman.api.GameStatus;
import hangman.store.HangmanStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class HangmanService {

    private static final int GUESSES_ALLOWED = 10;

    @Autowired
    private HangmanStore store;

    public GameDetail createNewGame() {
        ImmutableGameDetail newGame = ImmutableGameDetail.builder()
                .gameId(store.generateUniqueId())
                .secretWord(chooseWord())
                .status(GameStatus.NEW)
                .guessesRemaining(GUESSES_ALLOWED)
                .nextGuessId(0)
                .build();
        store.storeGame(newGame);
        return newGame;
    }

    public GameDetail getGameState(String gameId) throws InvalidGameException {
        return store.loadGame(gameId).orElseThrow(InvalidGameException::new);
    }

    public GameDetail applyGuess(String gameId, Character guess, int guessId) throws InvalidGameException {
        GameDetail gameDetail = store.loadGame(gameId).orElseThrow(InvalidGameException::new);

        if (gameDetail.getStatus() == GameStatus.WON || gameDetail.getStatus() == GameStatus.LOST) {
            throw new IllegalStateException("game has finished");
        }

        char lowercaseGuess = Character.toLowerCase(guess);
        if (gameDetail.getGuesses().contains(lowercaseGuess)) {
            // this has already been tried, no update is needed
            return gameDetail;
        }

        GameStatus nextState = GameStatus.IN_PROGRESS;
        ImmutableList<Character> guesses = ImmutableList.<Character>builder().addAll(gameDetail.getGuesses()).add(guess).build();

        int remainingGuesses = gameDetail.getGuessesRemaining();
        if (gameDetail.getSecretWord().indexOf(lowercaseGuess) >= 0) {
            // it's a good guess. See if we've got a complete match
            if (allMatched(gameDetail.getSecretWord(), guesses)) {
                nextState = GameStatus.WON;
            }
        } else {
            remainingGuesses--;
            if (remainingGuesses == 0) {
                nextState = GameStatus.LOST;
            }
        }

        ImmutableGameDetail updated = ImmutableGameDetail.copyOf(gameDetail)
                .withGuessesRemaining(remainingGuesses)
                .withNextGuessId(gameDetail.getNextGuessId() + 1)
                .withGuesses(guesses)
                .withStatus(nextState);

        // this can throw concurrentmodificationexception if the game was updated by someone else
        store.updateGame(updated, guessId);
        return updated;
    }

    private boolean allMatched(String secretWord, ImmutableList<Character> guesses) {
        // performance note : this is succint but not performant. If we wanted to tune
        // this for better performance we would increase the data structure to maintain a
        // set of characters that are needed to be typed. As successful guesses occur,
        // this set would have elements removed. Once the set was empty then the match
        // has occurred.
        Set<Integer> guessSet = guesses.stream().map(Integer::new).collect(Collectors.toSet());
        return secretWord.chars().allMatch(guessSet::contains);
    }

    private static List<String> words = new ArrayList<>();

    @PostConstruct
    private void loadWords() {
        InputStream inputStream = getClass()
                .getClassLoader().getResourceAsStream("words.txt");
        Scanner scanner = new Scanner(inputStream);
        while (scanner.hasNextLine()) {
            words.add(scanner.nextLine());
        }
    }

    private String chooseWord() {
        Random random = new Random();
        return words.get(random.nextInt(words.size()));
    }
}
