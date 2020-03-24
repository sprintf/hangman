package hangman.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import hangman.service.HangmanService;
import hangman.store.HangmanStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = {ApiHandler.class, HangmanService.class, HangmanStore.class})
class ApiHandlerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private HangmanService service;

    @Autowired
    private HangmanStore store;

    @Test
    void getGameStateNoSuchGame() throws Exception {
        mvc.perform(get("/api/hangman/games/xyz")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void newGame() throws Exception {
        MvcResult result = mvc.perform(post("/api/hangman/games")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();
        GameResponse game = getGameResponse(result);
        assertNotNull(game.getGameId());
        assertEquals(10, game.getState().getGuessesRemaining());
        assertEquals(0, game.getState().getNextGuessId());

        String location = result.getResponse().getHeaderValue("Location").toString();
        assertTrue(location.endsWith("api/hangman/games/" + game.getGameId()));

        // make sure the secret word isn't anywhere in there. Go to the store to get the secret
        String secretWord = store.loadGame(game.getGameId()).get().getSecretWord();
        assertFalse(result.getResponse().getContentAsString().contains(secretWord));
    }

    @Test
    void testSharingGame() throws Exception {
        MvcResult result = mvc.perform(post("/api/hangman/games")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();
        GameResponse game = getGameResponse(result);

        MvcResult share1 = mvc.perform(get("/api/hangman/games/" + game.getGameId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        GameResponse sharedGame1 = getGameResponse(share1);

        MvcResult share2 = mvc.perform(get("/api/hangman/games/" + game.getGameId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        GameResponse sharedGame2 = getGameResponse(share2);

        assertEquals(game, sharedGame1);
        assertEquals(sharedGame1, sharedGame2);
    }

    @Test
    void testGuessing() throws Exception {
        MvcResult result = mvc.perform(post("/api/hangman/games")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();
        GameResponse game = getGameResponse(result);

        String secretWord = store.loadGame(game.getGameId()).get().getSecretWord();
        char goodGuess = secretWord.charAt(0);

        result = mvc.perform(put("/api/hangman/games/" + game.getGameId())
                .param("guess", String.valueOf(goodGuess))
                .param("guessId", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        game = getGameResponse(result);

        assertEquals(10, game.getState().getGuessesRemaining());
        assertEquals(1, game.getState().getNextGuessId());
        assertTrue(game.getState().getMatchingLetters().startsWith(goodGuess + ""));

        char badGuess = '0';
        result = mvc.perform(put("/api/hangman/games/" + game.getGameId())
                .param("guess", String.valueOf(badGuess))
                .param("guessId", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        game = getGameResponse(result);

        assertEquals(9, game.getState().getGuessesRemaining());
        assertEquals(2, game.getState().getNextGuessId());
        assertEquals("[0]", game.getState().getFailedGuesses().toString());
    }

    @Test
    void testCaseInsensitive() throws Exception {
        MvcResult result = mvc.perform(post("/api/hangman/games")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();
        GameResponse game = getGameResponse(result);

        String secretWord = store.loadGame(game.getGameId()).get().getSecretWord();
        char goodGuessCapitalized = Character.toUpperCase(secretWord.charAt(0));

        result = mvc.perform(put("/api/hangman/games/" + game.getGameId())
                .param("guess", String.valueOf(goodGuessCapitalized))
                .param("guessId", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        game = getGameResponse(result);

        assertEquals(10, game.getState().getGuessesRemaining());
        assertTrue(game.getState().getMatchingLetters().startsWith(Character.toLowerCase(goodGuessCapitalized) + ""));
    }

    @Test
    void testGuessingWithBadParams() throws Exception {
        MvcResult result = mvc.perform(post("/api/hangman/games")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();
        GameResponse game = getGameResponse(result);

        mvc.perform(put("/api/hangman/games/not-a-game")
                .param("guess", "a")
                .param("guessId", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        mvc.perform(put("/api/hangman/games/" + game.getGameId())
                .param("noguess", "whatever")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/hangman/games/" + game.getGameId())
                .param("guess", "not-a-single-character")
                .param("guessId", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/hangman/games/" + game.getGameId())
                .param("guess", "x")
                .param("guessId", "x")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/hangman/games/" + game.getGameId())
                .param("guess", "x")
                .param("guessId", "1") // should be zero
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(409));

    }

    private GameResponse getGameResponse(MvcResult result) throws com.fasterxml.jackson.core.JsonProcessingException, UnsupportedEncodingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        return mapper.readValue(result.getResponse().getContentAsString(), ImmutableGameResponse.class);
    }

}