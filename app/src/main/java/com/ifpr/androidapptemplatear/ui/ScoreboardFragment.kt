package com.ifpr.androidapptemplatear.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ifpr.androidapptemplatear.R
import com.ifpr.androidapptemplatear.databinding.FragmentScoreboardBinding
import com.ifpr.androidapptemplatear.databinding.ItemScoreEventBinding
import com.ifpr.androidapptemplatear.game.GameManager
import com.ifpr.androidapptemplatear.game.ScoreEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Tabela de pontos, com "Encerrar contagem" e "Reiniciar tudo". */
class ScoreboardFragment : Fragment() {

    private var _binding: FragmentScoreboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var game: GameManager
    private val adapter = EventAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScoreboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        game = GameManager.get(requireContext())

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.eventList.layoutManager = LinearLayoutManager(requireContext())
        binding.eventList.adapter = adapter

        game.score.observe(viewLifecycleOwner) { render() }
        game.finished.observe(viewLifecycleOwner) { render() }
        game.events.observe(viewLifecycleOwner) { render() }

        binding.finishButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.finish_title)
                .setMessage(R.string.finish_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.finish_count) { _, _ -> game.finish() }
                .show()
        }

        binding.restartButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.restart_title)
                .setMessage(R.string.restart_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.restart_all) { _, _ -> game.reset() }
                .show()
        }
    }

    private fun render() {
        if (_binding == null) return
        val score = game.currentScore
        val finished = game.isFinished
        val events = game.events.value ?: emptyList()

        binding.status.setText(
            when {
                finished -> R.string.scoreboard_status_finished
                game.isGameOver -> R.string.scoreboard_status_game_over
                else -> R.string.scoreboard_status_running
            }
        )
        binding.scoreLabel.setText(
            if (finished) R.string.scoreboard_final else R.string.scoreboard_current
        )
        binding.scoreValue.text = score.toString()

        val totalModels = requireContext().assets.list("")
            ?.count { it.endsWith(".glb", ignoreCase = true) } ?: 0
        binding.foundText.text =
            getString(R.string.scoreboard_found_fmt, game.foundModels.size, totalModels)
        binding.dupesText.text = getString(
            R.string.scoreboard_dupes_fmt,
            events.count { it.type == ScoreEvent.Type.PENALTY }
        )

        adapter.submitList(events)
        binding.emptyText.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE

        // Depois de encerrada, só resta reiniciar.
        binding.finishButton.isEnabled = !finished
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.eventList.adapter = null
        _binding = null
    }

    private class EventAdapter :
        ListAdapter<ScoreEvent, EventAdapter.VH>(Diff) {

        class VH(val binding: ItemScoreEventBinding) : RecyclerView.ViewHolder(binding.root)

        private val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemScoreEventBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val e = getItem(position)
            val ctx = holder.binding.root.context
            with(holder.binding) {
                eventLabel.text = ctx.getString(
                    if (e.type == ScoreEvent.Type.FOUND) R.string.event_found_fmt
                    else R.string.event_penalty_fmt,
                    e.label
                )
                eventTime.text = format.format(Date(e.time))
                eventPoints.text = if (e.delta >= 0) "+${e.delta}" else e.delta.toString()
                eventPoints.setTextColor(
                    if (e.delta >= 0) Color.parseColor("#FF8700") else Color.parseColor("#FF5252")
                )
            }
        }

        private object Diff : DiffUtil.ItemCallback<ScoreEvent>() {
            override fun areItemsTheSame(a: ScoreEvent, b: ScoreEvent) = a.time == b.time && a.label == b.label
            override fun areContentsTheSame(a: ScoreEvent, b: ScoreEvent) = a == b
        }
    }
}
