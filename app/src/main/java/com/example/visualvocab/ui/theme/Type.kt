package com.example.visualvocab.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// this file defines the different font styles used for titles and normal text.

private val AppFont =
    FontFamily.Default

val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.35).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Bold,
        fontSize = 27.sp,
        lineHeight = 33.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 27.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 23.sp
    ),
    titleSmall = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodySmall = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 19.sp
    ),
    labelMedium = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.35.sp
    )
)
