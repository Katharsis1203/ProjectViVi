package com.example.visualvocab.domain.model

// this class holds the words and sentences in both english and spanish.
data class Vocabulary(
    val englishWord: String,
    val spanishWord: String,
    val englishSentence: String,
    val spanishSentence: String
)
