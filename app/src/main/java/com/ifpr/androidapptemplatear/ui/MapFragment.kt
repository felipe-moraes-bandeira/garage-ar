package com.ifpr.androidapptemplatear.ui

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.ifpr.androidapptemplatear.R
import com.ifpr.androidapptemplatear.data.Vehicle
import com.ifpr.androidapptemplatear.databinding.FragmentMapBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import java.io.File

/** Mapa (OpenStreetMap, sem chave de API) com um marcador por carro cadastrado com localização. */
class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GarageViewModel by activityViewModels()
    private var selected: Vehicle? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Cache de tiles no armazenamento do app: não precisa de permissão de armazenamento.
        val ctx = requireContext().applicationContext
        Configuration.getInstance().apply {
            userAgentValue = ctx.packageName
            osmdroidBasePath = File(ctx.cacheDir, "osmdroid")
            osmdroidTileCache = File(ctx.cacheDir, "osmdroid/tiles")
        }
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        binding.map.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            controller.setZoom(13.0)
            controller.setCenter(GeoPoint(DEFAULT_LAT, DEFAULT_LON))
            // Mapa escuro, combinando com o tema do app.
            overlayManager.tilesOverlay.setColorFilter(DARK_FILTER)
        }

        binding.selectedDetails.setOnClickListener {
            selected?.let {
                findNavController().navigate(
                    R.id.action_garage_to_detail,
                    bundleOf("vehicleId" to it.id)
                )
            }
        }

        val focusId = requireArguments().getLong("focusVehicleId", 0L)

        viewModel.vehicles.observe(viewLifecycleOwner) { list ->
            showVehicles(list.filter { it.hasLocation }, focusId)
        }
    }

    private fun showVehicles(vehicles: List<Vehicle>, focusId: Long) {
        val map = binding.map
        map.overlays.removeAll { it is Marker }

        binding.emptyText.visibility = if (vehicles.isEmpty()) View.VISIBLE else View.GONE
        binding.hint.visibility = if (vehicles.isEmpty()) View.GONE else View.VISIBLE

        vehicles.forEach { v ->
            val marker = Marker(map).apply {
                position = GeoPoint(v.latitude!!, v.longitude!!)
                title = v.fullName
                snippet = v.foundLocation
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { m, _ ->
                    select(v)
                    map.controller.animateTo(m.position)
                    true
                }
            }
            map.overlays.add(marker)
        }
        map.invalidate()

        val focus = vehicles.firstOrNull { it.id == focusId }
        when {
            focus != null -> {
                map.controller.setZoom(17.0)
                map.controller.setCenter(GeoPoint(focus.latitude!!, focus.longitude!!))
                select(focus)
            }

            vehicles.size == 1 -> {
                map.controller.setZoom(16.0)
                map.controller.setCenter(
                    GeoPoint(vehicles[0].latitude!!, vehicles[0].longitude!!)
                )
            }

            vehicles.size > 1 -> {
                val points = vehicles.map { GeoPoint(it.latitude!!, it.longitude!!) }
                // zoomToBoundingBox só funciona depois que o mapa foi medido.
                map.post {
                    if (_binding != null) {
                        map.zoomToBoundingBox(BoundingBox.fromGeoPointsSafe(points), false, 120)
                    }
                }
            }
        }

        // Carro selecionado foi removido? Esconde o cartão.
        if (selected != null && vehicles.none { it.id == selected?.id }) {
            selected = null
            binding.selectedCard.visibility = View.GONE
        }
    }

    private fun select(v: Vehicle) {
        selected = v
        binding.selectedCard.visibility = View.VISIBLE
        binding.selectedPhoto.loadVehiclePhoto(v.photoPath)
        binding.selectedName.text = "${v.fullName} (${v.year})"
        binding.selectedPlace.text = v.foundLocation.ifBlank {
            String.format(java.util.Locale.US, "%.5f, %.5f", v.latitude, v.longitude)
        }
    }

    override fun onResume() {
        super.onResume()
        _binding?.map?.onResume()
    }

    override fun onPause() {
        _binding?.map?.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        binding.map.onDetach()
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        // Ponta Grossa - PR, usado quando ainda não há carros no mapa.
        const val DEFAULT_LAT = -25.0950
        const val DEFAULT_LON = -50.1620

        /** Inverte as cores dos tiles (modo escuro). */
        val DARK_FILTER = ColorMatrixColorFilter(
            ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }
}
