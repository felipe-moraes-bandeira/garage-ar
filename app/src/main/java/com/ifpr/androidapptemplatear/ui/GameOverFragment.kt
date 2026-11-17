package com.ifpr.androidapptemplatear.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ifpr.androidapptemplatear.R
import com.ifpr.androidapptemplatear.databinding.FragmentGameOverBinding
import com.ifpr.androidapptemplatear.game.GameManager

/** Tela de derrota (carro repetido). Com pontuação 0 vira GAME OVER e exige novo jogo. */
class GameOverFragment : Fragment() {

    private var _binding: FragmentGameOverBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameOverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val game = GameManager.get(requireContext())
        val score = requireArguments().getInt("score")
        val vehicleName = requireArguments().getString("vehicleName").orEmpty()
        val gameOver = score <= 0

        binding.title.setText(if (gameOver) R.string.game_over_banner else R.string.defeat_title)
        binding.message.text = if (gameOver) {
            getString(R.string.game_over_message)
        } else {
            getString(R.string.defeat_message, vehicleName, GameManager.DUPLICATE_PENALTY)
        }
        binding.score.text = getString(R.string.final_score_fmt, score)
        binding.actionButton.setText(if (gameOver) R.string.new_game else R.string.continue_game)

        binding.actionButton.setOnClickListener {
            if (gameOver) game.reset()
            findNavController().popBackStack()
        }

        // Voltar fecha a tela de derrota; em game over só o botão "Novo jogo" sai daqui.
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!gameOver) findNavController().popBackStack()
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
