package com.ifpr.androidapptemplatear.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.location.LocationListener
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.ifpr.androidapptemplatear.game.GameManager
import androidx.navigation.fragment.findNavController
import com.ifpr.androidapptemplatear.R
import com.ifpr.androidapptemplatear.data.Drivetrain
import com.ifpr.androidapptemplatear.data.PhotoStorage
import com.ifpr.androidapptemplatear.data.Rarity
import com.ifpr.androidapptemplatear.data.Vehicle
import com.ifpr.androidapptemplatear.databinding.FragmentVehicleFormBinding
import java.util.Calendar

/** Cadastro (vehicleId = 0, photoPath preenchido) e edição (vehicleId > 0). */
class VehicleFormFragment : Fragment() {

    private var _binding: FragmentVehicleFormBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GarageViewModel by activityViewModels()

    private var vehicleId = 0L
    private var photoPath: String? = null
    private var existing: Vehicle? = null
    private var saved = false

    /** Posição onde o cadastro está sendo feito. */
    private var location: Location? = null
    private var locationListener: LocationListener? = null

    private val locationPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.any { it }) fetchLocation() else {
                _binding?.locationStatus?.setText(R.string.location_denied)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVehicleFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vehicleId = requireArguments().getLong("vehicleId", 0L)
        photoPath = requireArguments().getString("photoPath")

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        binding.inputDrivetrain.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, Drivetrain.options)
        )
        binding.inputRarity.setAdapter(
            ArrayAdapter(
                requireContext(), android.R.layout.simple_list_item_1,
                Rarity.values().map { it.label })
        )

        val isEditing = vehicleId != 0L
        if (isEditing) {
            binding.toolbar.setTitle(R.string.edit)
            viewModel.vehicle(vehicleId).observe(viewLifecycleOwner) { v ->
                if (v != null && existing == null) {
                    existing = v
                    photoPath = v.photoPath
                    showPhoto()
                    // Em rotação de tela os campos já restauram o texto digitado.
                    if (savedInstanceState == null) fill(v)
                }
            }
        } else {
            binding.inputRarity.setText(Rarity.COMUM.label, false)
            showPhoto()
        }

        binding.saveButton.setOnClickListener { save() }

        binding.locationButton.setOnClickListener { requestLocation() }
        if (isEditing) {
            binding.locationStatus.setText(R.string.location_missing)
        } else {
            requestLocation()
        }
    }

    private fun requestLocation() {
        val ctx = requireContext()
        val granted = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).any { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }

        if (granted) {
            fetchLocation()
        } else {
            locationPermission.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocation() {
        val ctx = requireContext()
        val manager = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        binding.locationStatus.setText(R.string.location_getting)

        try {
            // Última posição conhecida: resposta imediata enquanto a posição atual não chega.
            listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            ).mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
                .maxByOrNull { it.time }
                ?.let { onLocation(it) }

            val provider = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .firstOrNull { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }

            if (provider == null) {
                if (location == null) binding.locationStatus.setText(R.string.location_off)
                return
            }

            // Posição atual: uma única atualização, com prazo de 20 s.
            stopLocationUpdates()
            val listener = object : LocationListener {
                override fun onLocationChanged(fresh: Location) {
                    stopLocationUpdates()
                    onLocation(fresh)
                }

                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit

                @Deprecated("Callback utilizado em versões antigas do Android")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            }
            locationListener = listener
            manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())

            binding.root.postDelayed({
                if (locationListener === listener) {
                    stopLocationUpdates()
                    if (location == null) _binding?.locationStatus?.setText(R.string.location_missing)
                }
            }, 20_000L)
        } catch (e: SecurityException) {
            binding.locationStatus.setText(R.string.location_denied)
        }
    }

    private fun stopLocationUpdates() {
        val listener = locationListener ?: return
        locationListener = null
        val manager = context?.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        runCatching { manager?.removeUpdates(listener) }
    }

    private fun onLocation(loc: Location) {
        if (_binding == null) return
        location = loc
        binding.locationStatus.text = getString(
            R.string.location_ok_fmt, if (loc.hasAccuracy()) loc.accuracy.toInt() else 0
        )

        // Se "Local onde foi encontrado" está vazio, sugere o endereço da posição.
        if (binding.inputLocation.text.isNullOrBlank()) {
            val appContext = requireContext().applicationContext
            viewLifecycleOwner.lifecycleScope.launch {
                val address = withContext(Dispatchers.IO) {
                    try {
                        if (!Geocoder.isPresent()) null else {
                            @Suppress("DEPRECATION")
                            Geocoder(appContext, Locale("pt", "BR"))
                                .getFromLocation(loc.latitude, loc.longitude, 1)
                                ?.firstOrNull()?.getAddressLine(0)
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                if (_binding != null && !address.isNullOrBlank() &&
                    binding.inputLocation.text.isNullOrBlank()
                ) {
                    binding.inputLocation.setText(address)
                }
            }
        }
    }

    private fun showPhoto() {
        photoPath?.let { binding.photo.loadVehiclePhoto(it) }
    }

    private fun fill(v: Vehicle) = with(binding) {
        inputBrand.setText(v.brand)
        inputModel.setText(v.model)
        inputYear.setText(v.year.toString())
        inputEngine.setText(v.engine)
        inputHp.setText(if (v.horsepower > 0) v.horsepower.toString() else "")
        inputTorque.setText(v.torque)
        inputDrivetrain.setText(v.drivetrain, false)
        inputRarity.setText(v.rarity, false)
        inputLocation.setText(v.foundLocation)
        inputNotes.setText(v.notes)
        switchFavorite.isChecked = v.isFavorite
    }

    private fun save() {
        with(binding) {
        val path = photoPath ?: return@with
        if (vehicleId != 0L && existing == null) return@with // ainda carregando

        val brand = inputBrand.text.toString().trim()
        val model = inputModel.text.toString().trim()
        val yearText = inputYear.text.toString().trim()
        val hpText = inputHp.text.toString().trim()

        layoutBrand.error = if (brand.isEmpty()) getString(R.string.required) else null
        layoutModel.error = if (model.isEmpty()) getString(R.string.required) else null

        val maxYear = Calendar.getInstance().get(Calendar.YEAR) + 1
        val year = yearText.toIntOrNull()
        layoutYear.error = when {
            yearText.isEmpty() -> getString(R.string.required)
            year == null || year !in 1886..maxYear -> getString(R.string.invalid_year)
            else -> null
        }

        val hp = if (hpText.isEmpty()) 0 else hpText.toIntOrNull()
        layoutHp.error = if (hp == null) getString(R.string.invalid_hp) else null

        if (listOf(layoutBrand, layoutModel, layoutYear, layoutHp).any { it.error != null }) {
            return@with
        }

        val old = existing
        val vehicle = Vehicle(
            id = old?.id ?: 0L,
            photoPath = path,
            brand = brand,
            model = model,
            year = year!!,
            engine = inputEngine.text.toString().trim(),
            horsepower = hp!!,
            torque = inputTorque.text.toString().trim(),
            drivetrain = inputDrivetrain.text.toString().trim(),
            rarity = inputRarity.text.toString().ifBlank { Rarity.COMUM.label },
            foundLocation = inputLocation.text.toString().trim(),
            notes = inputNotes.text.toString().trim(),
            isFavorite = switchFavorite.isChecked,
            latitude = location?.latitude ?: old?.latitude,
            longitude = location?.longitude ?: old?.longitude,
            createdAt = old?.createdAt ?: System.currentTimeMillis()
        )
        confirmAndSave(vehicle)
        }
    }

    /**
     * Confirmação do cadastro: se já existe o mesmo carro (marca + modelo + ano),
     * novo cadastro perde pontos e abre a tela de derrota; edição só mostra o erro.
     */
    private fun confirmAndSave(vehicle: Vehicle) {
        binding.saveButton.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val duplicate = viewModel.isDuplicate(
                vehicle.brand, vehicle.model, vehicle.year, excludeId = vehicle.id
            )
            if (_binding == null) return@launch

            when {
                !duplicate -> {
                    saved = true
                    viewModel.save(vehicle)
                    findNavController().popBackStack()
                }

                vehicle.id != 0L || GameManager.get(requireContext()).isFinished -> {
                    binding.saveButton.isEnabled = true
                    binding.layoutModel.error = getString(R.string.duplicate_vehicle)
                }

                else -> {
                    // A foto não foi salva (saved = false): onDestroyView a apaga.
                    val score = GameManager.get(requireContext()).penalizeDuplicate(vehicle.fullName)
                    findNavController().navigate(
                        R.id.action_form_to_game_over,
                        bundleOf("score" to score, "vehicleName" to vehicle.fullName)
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopLocationUpdates()
        _binding = null
        // Cadastro cancelado: apaga a foto que ficou órfã (exceto em rotação de tela).
        if (!saved && vehicleId == 0L && !requireActivity().isChangingConfigurations) {
            PhotoStorage.delete(photoPath)
        }
    }
}
