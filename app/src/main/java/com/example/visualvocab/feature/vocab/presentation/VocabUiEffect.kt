package com.example.visualvocab.feature.vocab.presentation

sealed interface VocabUiEffect {
    data class ShowError(val message: String) : VocabUiEffect
}
