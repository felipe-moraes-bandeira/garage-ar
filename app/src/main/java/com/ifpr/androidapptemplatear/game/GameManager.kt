package com.ifpr.androidapptemplatear.game

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Estado do jogo: pontuação e modelos já encontrados (persistidos em SharedPreferences).
 *
 * Regras:
 *  - começa com [START_SCORE] pontos;
 *  - encontrar um modelo NOVO em um ponto turístico soma os pontos do local;
 *  - cadastrar um carro repetido na garagem tira [DUPLICATE_PENALTY] pontos e mostra a tela de derrota;
 *  - pontuação 0 = game over (só um novo jogo reinicia).
 * Chamar sempre da thread principal.
 */
class GameManager private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _score = MutableLiveData(prefs.getInt(KEY_SCORE, START_SCORE))
    val score: LiveData<Int> = _score

    val currentScore: Int get() = _score.value ?: START_SCORE

    val foundModels: Set<String>
        get() = prefs.getStringSet(KEY_FOUND, emptySet())?.toSet() ?: emptySet()

    val isGameOver: Boolean get() = currentScore <= 0

    /** Sorteia, entre os modelos disponíveis, um que o jogador ainda não encontrou. */
    fun pickNewModel(available: List<String>): String? {
        val found = foundModels
        return available.filter { it !in found }.randomOrNull()
    }

    /** Registra o modelo como encontrado e soma pontos. Falso se já era conhecido. */
    fun markFound(model: String, points: Int): Boolean {
        if (isGameOver) return false
        val found = foundModels
        if (model in found) return false
        prefs.edit().putStringSet(KEY_FOUND, found + model).apply()
        setScore(currentScore + points)
        return true
    }

    /** Aplica a penalidade de carro repetido e devolve a nova pontuação. */
    fun penalizeDuplicate(): Int {
        setScore((currentScore - DUPLICATE_PENALTY).coerceAtLeast(0))
        return currentScore
    }

    /** Novo jogo: zera os modelos encontrados e volta à pontuação inicial. */
    fun reset() {
        prefs.edit().remove(KEY_FOUND).apply()
        setScore(START_SCORE)
    }

    private fun setScore(value: Int) {
        prefs.edit().putInt(KEY_SCORE, value).apply()
        _score.value = value
    }

    companion object {
        const val START_SCORE = 100
        const val DUPLICATE_PENALTY = 50

        private const val PREFS = "garage_game"
        private const val KEY_SCORE = "score"
        private const val KEY_FOUND = "found_models"

        @Volatile
        private var instance: GameManager? = null

        fun get(context: Context): GameManager =
            instance ?: synchronized(this) {
                instance ?: GameManager(context.applicationContext).also { instance = it }
            }
    }
}
