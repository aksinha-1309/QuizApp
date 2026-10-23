package com.ankurapps.quizapp.Model

data class QuestionModel(
    val category: String,
    val difficulty: String,
    val questionEn: String,
    val questionHi: String,
    val optionAEn: String,
    val optionAHi: String,
    val optionBEn: String,
    val optionBHi: String,
    val optionCEn: String,
    val optionCHi: String,
    val optionDEn: String,
    val optionDHi: String,
    val correctAnswerEn: String,
    val correctAnswerHi: String
)