package hangman.api;

import hangman.service.GameDetail;
import hangman.service.HangmanService;
import hangman.service.InvalidGameException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/hangman")
public class ApiHandler {

    @Autowired
    private HangmanService service;

    @RequestMapping(value = "/games/{gameId}", method = RequestMethod.GET)
    public ResponseEntity<GameResponse> getGameState(@PathVariable String gameId) {
        try {
            return ResponseEntity.ok(buildResponse(service.getGameState(gameId)));
        } catch (InvalidGameException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(value = "/games", method = RequestMethod.POST)
    public ResponseEntity<GameResponse> newGame() {
        GameDetail newGame = service.createNewGame();
        String newResource = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/").path(newGame.getGameId())
                .build().toString();
        return ResponseEntity.created(URI.create(newResource)).body(buildResponse(newGame));
    }

    @RequestMapping(value = "/games/{gameId}", method = RequestMethod.PUT)
    public ResponseEntity<GameResponse> guess(@PathVariable String gameId, @RequestParam Character guess, @RequestParam int guessId) {
        try {
            return ResponseEntity.ok(buildResponse(service.applyGuess(gameId, guess, guessId)));
        } catch (InvalidGameException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private GameResponse buildResponse(GameDetail game) {
        ImmutableGameState state = ImmutableGameState.builder()
                .guessesRemaining(game.getGuessesRemaining())
                .nextGuessId(game.getNextGuessId())
                .failedGuesses(buildFailedGuesses(game.getSecretWord(), game.getGuesses()))
                .matchingLetters(buildMatchString(game.getSecretWord(), game.getGuesses()))
                .status(game.getStatus())
                .build();
        return ImmutableGameResponse.builder()
                .gameId(game.getGameId())
                .numberOfLetters(game.getSecretWord().length())
                .state(state)
                .build();
    }

    private String buildMatchString(String secretWord, List<Character> guesses) {
        Set<Character> guessSet = new HashSet<>(guesses);
        StringBuilder sb = new StringBuilder();
        for(int loop = 0; loop < secretWord.length(); loop++) {
            if (guessSet.contains(secretWord.charAt(loop))) {
                sb.append(secretWord.charAt(loop));
            } else {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    private List<Character> buildFailedGuesses(String secretWord, List<Character> guesses) {
        return guesses.stream().filter(s -> secretWord.indexOf(s) == -1).collect(Collectors.toList());
    }

}
