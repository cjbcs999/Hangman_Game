package com.example.hangmangame

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import kotlin.random.Random
import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import kotlinx.parcelize.Parcelize

// -------------------------
// 1. MainActivity
// -------------------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HangmanGameApp()
        }
    }
}

// -------------------------
// 2. App Entry: HangmanGameApp
// -------------------------
@Composable
fun HangmanGameApp() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            HangmanGame()
        }
    }
}

// -------------------------
// 3. Preview
// -------------------------
@Preview(showBackground = true, name = "HangmanGame Preview")
@Composable
fun HangmanGamePreview() {
    HangmanGameApp()
}

// -------------------------
// 4. Define Data Class, Word List, and Game Logic
// -------------------------

@Parcelize
data class WordWithHint(
    val word: String,
    val hint: String
) : Parcelable

val wordList = listOf(
    WordWithHint("APPLE", "A type of fruit"),
    WordWithHint("PIZZA", "Popular Italian food"),
    WordWithHint("ORANGE", "Color and a fruit"),
    WordWithHint("BANANA", "A yellow fruit"),
    WordWithHint("COFFEE", "A popular morning beverage"),
    WordWithHint("BURGER", "Fast food, often with cheese"),

    // Fruits
    WordWithHint("STRAWBERRY", "A small red fruit with seeds on the outside"),
    WordWithHint("GRAPE", "A small fruit, often used to make wine"),
    WordWithHint("WATERMELON", "A large fruit with green rind and red flesh"),
    WordWithHint("PINEAPPLE", "A tropical fruit with spiky skin and sweet flesh"),
    WordWithHint("PEACH", "A fuzzy fruit with a stone inside"),

    // Food
    WordWithHint("SUSHI", "A Japanese dish with rice and seafood"),
    WordWithHint("PASTA", "A staple Italian dish made from wheat"),
    WordWithHint("TACO", "A Mexican dish with a folded tortilla"),
    WordWithHint("SANDWICH", "A meal with fillings between bread slices"),
    WordWithHint("CHEESE", "A dairy product used in many dishes"),

    // Drinks
    WordWithHint("TEA", "A hot beverage made from leaves"),
    WordWithHint("MILK", "A white liquid produced by cows"),
    WordWithHint("JUICE", "A drink made from squeezed fruit"),
    WordWithHint("SODA", "A fizzy carbonated drink"),
    WordWithHint("WATER", "Essential for life, no color or taste"),

    // Animals
    WordWithHint("TIGER", "A large wild cat with orange and black stripes"),
    WordWithHint("ELEPHANT", "A large animal with a trunk"),
    WordWithHint("PANDA", "A black-and-white bear from China"),
    WordWithHint("DOLPHIN", "A smart marine animal"),
    WordWithHint("EAGLE", "A bird known for its sharp vision"),

    // Objects
    WordWithHint("LAPTOP", "A portable computer"),
    WordWithHint("GUITAR", "A string instrument used in music"),
    WordWithHint("TELEPHONE", "A device used for communication"),
    WordWithHint("UMBRELLA", "Used to protect from rain"),
    WordWithHint("WATCH", "A device worn on the wrist to tell time"),

    // Other common words
    WordWithHint("MOUNTAIN", "A large natural elevation of the earth"),
    WordWithHint("OCEAN", "A vast body of salt water"),
    WordWithHint("SUNFLOWER", "A tall plant with a large yellow flower"),
    WordWithHint("RAINBOW", "A colorful arc in the sky after rain"),
    WordWithHint("MOON", "Earth’s natural satellite that shines at night")
)

val alphabet = ('A'..'Z').toList()

@Composable
fun HangmanGame() {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current

    // State: Randomly select a word
    var currentWord by rememberSaveable { mutableStateOf(pickRandomWord()) }
    // Letters that have been guessed
    var guessedLetters by rememberSaveable { mutableStateOf(setOf<Char>()) }
    // Disabled letters (cannot be clicked again once selected)
    var disabledLetters by rememberSaveable { mutableStateOf(setOf<Char>()) }
    // Remaining chances
    var livesRemaining by rememberSaveable { mutableStateOf(6) }
    // Number of times the Hint button has been clicked (0~3)
    var hintClickCount by rememberSaveable { mutableStateOf(0) }
    // Text to display the hint, initially empty
    var hintText by rememberSaveable { mutableStateOf("") }

    val isGameOver = remember(guessedLetters, livesRemaining) {
        livesRemaining <= 0 || checkWin(currentWord.word, guessedLetters)
    }

    // Event: Guess a letter
    fun onLetterClick(letter: Char) {
        if (isGameOver) return
        // Disable the letter after clicking
        disabledLetters = disabledLetters + letter

        if (letter in currentWord.word) {
            // Correct guess
            guessedLetters = guessedLetters + letter
        } else {
            // Incorrect guess -> Lose one life
            livesRemaining--
        }
    }

    // Event：New Game
    fun onNewGameClick() {
        currentWord = pickRandomWord()
        guessedLetters = setOf()
        disabledLetters = setOf()
        livesRemaining = 6
        hintClickCount = 0
        hintText = ""
    }

    // Event：Hint
    fun onHintButtonClick() {
        if (hintClickCount >= 3) return
        // The second and third clicks deduct a life; if it would result in 0 lives, show a toast
        if (hintClickCount >= 1) {
            if (livesRemaining <= 1) {
                Toast.makeText(context, "Hint not available", Toast.LENGTH_SHORT).show()
                return
            }
        }

        hintClickCount++

        when (hintClickCount) {
            1 -> {
                // First click -> Directly update hintText
                hintText = "Hint: ${currentWord.hint}"
            }
            2 -> {
                // Second click -> Deduct a life and disable half of the letters not in the word
                livesRemaining--
                val notInWord = alphabet.filter {
                    it !in currentWord.word && it !in disabledLetters && it !in guessedLetters
                }
                if (notInWord.isNotEmpty()) {
                    val halfCount = notInWord.size / 2
                    val toDisable = notInWord.shuffled().take(halfCount).toSet()
                    disabledLetters = disabledLetters + toDisable
                }
            }
            3 -> {
                // Third click -> Deduct a life, disable all vowels, and mark guessed ones
                livesRemaining--
                val vowels = setOf('A', 'E', 'I', 'O', 'U')
                disabledLetters = disabledLetters + vowels
                guessedLetters = guessedLetters + (vowels intersect currentWord.word.toSet())
            }
        }
    }

    // UI
    if (isLandscape) {
        // Landscape mode: Row layout divided into three sections, with padding to prevent button clipping
        Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Column(modifier = Modifier.weight(1.5f).fillMaxSize()){
                // Panel 1：Letter selection
                Box(modifier = Modifier.weight(2f)) {
                    PanelChooseLetter(
                        onLetterClick = ::onLetterClick,
                        guessedLetters = guessedLetters,
                        disabledLetters = disabledLetters,
                        isGameOver = isGameOver
                    )
                }
                // Panel 2: Hint area, directly displaying hintText
                Box(modifier = Modifier.weight(1f)) {
                    PanelHintArea(
                        onHintClick = ::onHintButtonClick,
                        hintClickCount = hintClickCount,
                        hintText = hintText
                    )
                }
            }
            // Panel 3: Main game area
            Box(modifier = Modifier.weight(2f)) {
                PanelMainGame(
                    word = currentWord.word,
                    guessedLetters = guessedLetters,
                    livesRemaining = livesRemaining,
                    onNewGameClick = ::onNewGameClick
                )
            }
        }
    } else {
        // Portrait mode: Column layout, no Hint Button, height shared using weight
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                PanelMainGame(
                    word = currentWord.word,
                    guessedLetters = guessedLetters,
                    livesRemaining = livesRemaining,
                    onNewGameClick = ::onNewGameClick
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Choose A Letter",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    PanelChooseLetter(
                        onLetterClick = ::onLetterClick,
                        guessedLetters = guessedLetters,
                        disabledLetters = disabledLetters,
                        isGameOver = isGameOver
                    )
                }
            }
        }
    }


}

// -------------------------
// 5. Composable Panels
// -------------------------
@Composable
fun PanelChooseLetter(
    onLetterClick: (Char) -> Unit,
     guessedLetters: Set<Char>,
    disabledLetters: Set<Char>,
    isGameOver: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(alphabet.chunked(7)) { rowLetters ->
            Row {
                rowLetters.forEach { letter ->
                    val enabled = !isGameOver && letter !in disabledLetters
                    Button(
                        onClick = { onLetterClick(letter) },
                        enabled = enabled,
                        modifier = Modifier
                            .padding(2.dp)
                            .width(40.dp),
                        contentPadding = PaddingValues(0.dp) // Remove default padding
                    ) {
                        Text(letter.toString())
                    }
                }
            }
        }
    }
}

@Composable
fun PanelHintArea(
    onHintClick: () -> Unit,
    hintClickCount: Int,
    hintText: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Display hint text if available
        if (hintText.isNotBlank()) {
            Text(text = hintText, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onHintClick,
            enabled = hintClickCount < 3,
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Hint Button (used $hintClickCount / 3)")
        }
    }
}

@Composable
fun PanelMainGame(
    word: String,
    guessedLetters: Set<Char>,
    livesRemaining: Int,
    onNewGameClick: () -> Unit
) {
    val isWin = checkWin(word, guessedLetters)
    val isLose = livesRemaining <= 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Display Hangman ASCII drawing
        Text(text = drawHangman(6 - livesRemaining))

        Spacer(modifier = Modifier.height(16.dp))

        // Display guessed letters
        val displayedWord = word.map { ch ->
            if (ch in guessedLetters) ch else '_'
        }.joinToString(" ")
        Text(text = displayedWord, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        // Display game result
        when {
            isWin -> {
                Text("You Win!", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            isLose -> {
                Text("You Lose! The word was: $word", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNewGameClick,
            modifier = Modifier.padding(8.dp)
        ) {
            Text("New Game")
        }
    }
}

// -------------------------
// 6. Utility Functions
// -------------------------
fun pickRandomWord(): WordWithHint {
    return wordList[Random.nextInt(wordList.size)]
}

fun checkWin(word: String, guessedLetters: Set<Char>): Boolean {
    return word.all { it in guessedLetters }
}

// ASCII drawing for Hangman
fun drawHangman(bodyParts: Int): String {
    return when (bodyParts) {
        0 -> """
            +---+
            |   
            |   
            |   
            ===
        """.trimIndent()

        1 -> """
            +---+
            |   O
            |   
            |   
            ===
        """.trimIndent()

        2 -> """
            +---+
            |   O
            |   |
            |   
            ===
        """.trimIndent()

        3 -> """
            +---+
            |   O
            |  /|
            |   
            ===
        """.trimIndent()

        4 -> """
            +---+
            |   O
            |  /|\
            |   
            ===
        """.trimIndent()

        5 -> """
            +---+
            |   O
            |  /|\
            |  /
            ===
        """.trimIndent()

        else -> """
            +---+
            |   O
            |  /|\
            |  / \
            ===
        """.trimIndent()
    }
}
