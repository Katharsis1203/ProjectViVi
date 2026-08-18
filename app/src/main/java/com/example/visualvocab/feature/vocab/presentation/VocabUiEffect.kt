package com.example.visualvocab.feature.vocab.presentation

// these are one-time things the UI should show, like an error popup.
sealed interface VocabUiEffect {
    data class ShowError(val message: String) : VocabUiEffect
}
