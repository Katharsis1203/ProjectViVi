package com.example.visualvocab.domain.model

// this sets how hard the sentences should be for the user.
enum class SentenceDifficulty {
    EASY,
    MEDIUM,
    HARD;

    // makes the sentences a bit easier.
    fun easier(): SentenceDifficulty = when (this) {
        EASY -> EASY
        MEDIUM -> EASY
        HARD -> MEDIUM
    }

    // makes the sentences a bit harder.
    fun harder(): SentenceDifficulty = when (this) {
        EASY -> MEDIUM
        MEDIUM -> HARD
        HARD -> HARD
    }
}
