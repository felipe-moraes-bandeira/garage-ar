package com.ifpr.androidapptemplatear.game

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.json.JSONArray
import org.json.JSONObject

/** Uma linha do placar: modelo encontrado (+) ou carro repetido (−). */
data class ScoreEvent(
    val type: Type,
    val label: String,
    val delta: Int,
    val time: Long
) {
    enum class Type { FOUND, PENALTY }
}

/**
 * Estado do jogo, persistido em SharedPreferences.
 *
 * Regras:
 *  - começa com [START_SCORE] pontos;
 *  - encontrar um modelo NOVO em um ponto turístico soma os pontos do local;
 *  - cadastrar um carro repetido tira [DUPLICATE_PENALTY] pontos e mostra a tela de derrota;
 *  - pontuação 0 = game over;
 *  - "encerrar contagem" congela a pontuação (nada soma nem tira);
 *  - "reiniciar" apaga o histórico e começa de novo.
 * Chamar sempre da thread principal.
 */
class GameManager private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _score = MutableLiveData(prefs.getInt(KEY_SCORE, START_SCORE))
    val score: LiveData<Int> = _score

    private val _finished = MutableLiveData(prefs.getBoolean(KEY_FINISHED, false))
    val finished: LiveData<Boolean> = _finished

    private val _events = MutableLiveData(loadEvents())
    /** Histórico, do mais recente para o mais antigo. */
    val events: LiveData<List<ScoreEvent>> = _events

    val currentScore: Int get() = _score.value ?: START_SCORE
    val isFinished: Boolean get() = _finished.value == true
    val isGameOver: Boolean get() = currentScore <= 0

    val foundModels: Set<String>
        get() = prefs.getStringSet(KEY_FOUND, emptySet())?.toSet() ?: emptySet()

    /** Sorteia, entre os modelos disponíveis, um que o jogador ainda não encontrou. */
    fun pickNewModel(available: List<String>): String? {
        val found = foundModels
        return available.filter { it !in found }.randomOrNull()
    }

    /** Registra o modelo como encontrado e soma pontos. Falso se não pontuou. */
    fun markFound(model: String, points: Int, label: String): Boolean {
        if (isGameOver || isFinished || model in foundModels) return false
        prefs.edit().putStringSet(KEY_FOUND, foundModels + model).apply()
        addEvent(ScoreEvent(ScoreEvent.Type.FOUND, label, points, System.currentTimeMillis()))
        setScore(currentScore + points)
        return true
    }

    /** Aplica a penalidade de carro repetido e devolve a pontuação atual. */
    fun penalizeDuplicate(label: String): Int {
        if (isFinished) return currentScore
        val newScore = (currentScore - DUPLICATE_PENALTY).coerceAtLeast(0)
        addEvent(
            ScoreEvent(
                ScoreEvent.Type.PENALTY, label,
                newScore - currentScore, System.currentTimeMillis()
            )
        )
        setScore(newScore)
        return currentScore
    }

    /** Congela a pontuação: ela vira a pontuação final. */
    fun finish() {
        prefs.edit().putBoolean(KEY_FINISHED, true).apply()
        _finished.value = true
    }

    /** Reinício total: histórico, modelos encontrados, pontuação e encerramento. */
    fun reset() {
        prefs.edit()
            .remove(KEY_FOUND)
            .remove(KEY_EVENTS)
            .putBoolean(KEY_FINISHED, false)
            .putInt(KEY_SCORE, START_SCORE)
            .apply()
        _finished.value = false
        _events.value = emptyList()
        _score.value = START_SCORE
    }

    private fun setScore(value: Int) {
        prefs.edit().putInt(KEY_SCORE, value).apply()
        _score.value = value
    }

    private fun addEvent(event: ScoreEvent) {
        val list = (listOf(event) + (_events.value ?: emptyList())).take(MAX_EVENTS)
        _events.value = list
        val json = JSONArray()
        list.forEach {
            json.put(
                JSONObject()
                    .put("t", it.type.name).put("l", it.label)
                    .put("d", it.delta).put("ts", it.time)
            )
        }
        prefs.edit().putString(KEY_EVENTS, json.toString()).apply()
    }

    private fun loadEvents(): List<ScoreEvent> = try {
        val json = JSONArray(prefs.getString(KEY_EVENTS, "[]"))
        (0 until json.length()).map { i ->
            val o = json.getJSONObject(i)
            ScoreEvent(
                ScoreEvent.Type.valueOf(o.getString("t")),
                o.getString("l"), o.getInt("d"), o.getLong("ts")
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    companion object {
        const val START_SCORE = 100
        const val DUPLICATE_PENALTY = 50
        private const val MAX_EVENTS = 200

        private const val PREFS = "garage_game"
        private const val KEY_SCORE = "score"
        private const val KEY_FOUND = "found_models"
        private const val KEY_EVENTS = "events"
        private const val KEY_FINISHED = "finished"

        @Volatile
        private var instance: GameManager? = null

        fun get(context: Context): GameManager =
            instance ?: synchronized(this) {
                instance ?: GameManager(context.applicationContext).also { instance = it }
            }
    }
}
