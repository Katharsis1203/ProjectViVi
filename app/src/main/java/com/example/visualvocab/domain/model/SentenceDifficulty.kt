package com.example.visualvocab.domain.model

enum class SentenceDifficulty {
    EASY,
    MEDIUM,
    HARD;

    fun easier(): SentenceDifficulty = when (this) {
        EASY -> EASY
        MEDIUM -> EASY
        HARD -> MEDIUM
    }

    fun harder(): SentenceDifficulty = when (this) {
        EASY -> MEDIUM
        MEDIUM -> HARD
        HARD -> HARD
    }
}
