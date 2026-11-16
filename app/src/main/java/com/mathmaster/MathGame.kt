package com.mathmaster

enum class Difficulty { EASY, MEDIUM, HARD }

data class Question(
    val questionText: String,
    val correctAnswer: Int,
    val options: List<Int>
)

/** Generates a random math question for the given difficulty. */
fun generateQuestion(difficulty: Difficulty): Question {
    val ops = listOf("+", "-", "×", "÷")
    val op = ops.random()

    val smallRange = when (difficulty) {
        Difficulty.EASY -> 2..10
        Difficulty.MEDIUM -> 2..15
        Difficulty.HARD -> 2..25
    }
    val bigRange = when (difficulty) {
        Difficulty.EASY -> 1..20
        Difficulty.MEDIUM -> 1..100
        Difficulty.HARD -> 1..200
    }

    val a: Int
    val b: Int
    val answer: Int
    val text: String

    when (op) {
        "+" -> {
            a = bigRange.random()
            b = bigRange.random()
            answer = a + b
            text = "$a + $b"
        }
        "-" -> {
            val x = bigRange.random()
            val y = bigRange.random()
            a = maxOf(x, y)
            b = minOf(x, y)
            answer = a - b
            text = "$a - $b"
        }
        "×" -> {
            a = smallRange.random()
            b = smallRange.random()
            answer = a * b
            text = "$a × $b"
        }
        else -> { // "÷" — always valid, no remainder, no division by zero
            b = smallRange.random()
            answer = smallRange.random()
            a = b * answer
            text = "$a ÷ $b"
        }
    }

    return Question(text, answer, generateOptions(answer))
}

private fun generateOptions(answer: Int): List<Int> {
    val set = mutableSetOf(answer)
    var guard = 0
    val spread = maxOf(5, answer / 3 + 3)

    while (set.size < 4 && guard < 200) {
        guard++
        val delta = (1..spread).random()
        val candidate = if ((0..1).random() == 0) answer - delta else answer + delta
        if (candidate >= 0 && candidate != answer) set.add(candidate)
    }
    var extra = 1
    while (set.size < 4) {
        set.add(answer + extra++)
    }
    return set.toList().shuffled()
}
