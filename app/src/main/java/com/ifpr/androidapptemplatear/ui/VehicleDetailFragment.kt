package com.ifpr.androidapptemplatear.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ifpr.androidapptemplatear.R
import com.ifpr.androidapptemplatear.data.Rarity
import com.ifpr.androidapptemplatear.data.Vehicle
import com.ifpr.androidapptemplatear.databinding.FragmentVehicleDetailBinding
import com.ifpr.androidapptemplatear.databinding.ItemSpecRowBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VehicleDetailFragment : Fragment() {

    private var _binding: FragmentVehicleDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GarageViewModel by activityViewModels()
    private var current: Vehicle? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVehicleDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val id = requireArguments().getLong("vehicleId")
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        binding.favorite.setOnClickListener { current?.let { viewModel.toggleFavorite(it) } }
        binding.editButton.setOnClickListener {
            current?.let {
                findNavController().navigate(
                    R.id.action_detail_to_form,
                    bundleOf("vehicleId" to it.id)
                )
            }
        }
        binding.mapButton.setOnClickListener {
            current?.let {
                findNavController().navigate(
                    R.id.action_detail_to_map,
                    bundleOf("focusVehicleId" to it.id)
                )
            }
        }
        binding.deleteButton.setOnClickListener { current?.let { confirmDelete(it) } }

        viewModel.vehicle(id).observe(viewLifecycleOwner) { v ->
            if (v != null) {
                current = v
                bind(v)
            }
        }
    }

    private fun bind(v: Vehicle) = with(binding) {
        photo.loadVehiclePhoto(v.photoPath)
        brand.text = v.brand
        fullName.text = "${v.model} (${v.year})"
        rarity.bindRarity(Rarity.fromLabel(v.rarity))
        favorite.setFavoriteIcon(v.isFavorite)

        val date = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))
            .format(Date(v.createdAt))
        registered.text = if (v.foundLocation.isNotBlank())
            "Encontrado em ${v.foundLocation} • $date" else "Registrado em $date"

        specsContainer.removeAllViews()
        addSpec(getString(R.string.field_brand), v.brand)
        addSpec(getString(R.string.field_model), v.model)
        addSpec(getString(R.string.field_year), v.year.toString())
        addSpec(getString(R.string.field_engine), v.engine)
        addSpec(getString(R.string.field_hp), if (v.horsepower > 0) "${v.horsepower} cv" else "")
        addSpec(getString(R.string.field_torque), v.torque)
        addSpec(getString(R.string.field_drivetrain), v.drivetrain)

        mapButton.visibility = if (v.hasLocation) View.VISIBLE else View.GONE

        notesCard.visibility = if (v.notes.isBlank()) View.GONE else View.VISIBLE
        notes.text = v.notes
    }

    private fun addSpec(label: String, value: String) {
        val row = ItemSpecRowBinding.inflate(layoutInflater, binding.specsContainer, false)
        row.specLabel.text = label
        row.specValue.text = value.ifBlank { getString(R.string.not_informed) }
        binding.specsContainer.addView(row.root)
    }

    private fun confirmDelete(v: Vehicle) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_title)
            .setMessage(getString(R.string.delete_message, v.fullName))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.delete(v)
                findNavController().popBackStack()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
