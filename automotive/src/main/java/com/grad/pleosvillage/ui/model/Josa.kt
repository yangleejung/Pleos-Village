package com.grad.pleosvillage.ui.model

/*
 * Korean particles after names. Chosen by the final consonant (batchim) of the last syllable (레이니가 / 웨이브는 / 우디도).
 * Non-Hangul characters are treated as having no batchim.
 */

private fun hasBatchim(word: String): Boolean {
    val c = word.trimEnd().lastOrNull() ?: return false
    if (c !in '가'..'힣') return false
    return (c - '가') % 28 != 0
}

/** Subject particle 이/가 */
fun String.iGa() = this + if (hasBatchim(this)) "이" else "가"

/** Topic particle 은/는 */
fun String.eunNeun() = this + if (hasBatchim(this)) "은" else "는"

/** Object particle 을/를 */
fun String.eulReul() = this + if (hasBatchim(this)) "을" else "를"
