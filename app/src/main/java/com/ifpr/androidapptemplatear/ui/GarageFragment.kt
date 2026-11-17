package com.ifpr.androidapptemplatear.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ifpr.androidapptemplatear.R
import com.ifpr.androidapptemplatear.data.PhotoStorage
import com.ifpr.androidapptemplatear.game.GameManager
import com.ifpr.androidapptemplatear.data.Vehicle
import com.ifpr.androidapptemplatear.databinding.FragmentGarageBinding
import kotlinx.coroutines.launch
import java.io.File

class GarageFragment : Fragment() {

    private var _binding: FragmentGarageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GarageViewModel by activityViewModels()
    private lateinit var adapter: VehicleAdapter

    private var allVehicles: List<Vehicle> = emptyList()
    private var pendingCameraFile: File? = null

    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchCamera() else toast(R.string.camera_denied)
        }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val file = pendingCameraFile
            pendingCameraFile = null
            if (success && file != null && file.length() > 0) {
                openForm(file.absolutePath)
            } else {
                file?.delete()
            }
        }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) importFromGallery(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getString(KEY_PENDING_FILE)?.let { pendingCameraFile = File(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pendingCameraFile?.let { outState.putString(KEY_PENDING_FILE, it.absolutePath) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGarageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = VehicleAdapter(
            onClick = { v ->
                findNavController().navigate(
                    R.id.action_garage_to_detail,
                    bundleOf("vehicleId" to v.id)
                )
            },
            onFavorite = { v -> viewModel.toggleFavorite(v) }
        )

        // Grade responsiva: número de colunas depende da largura da tela.
        val widthDp = resources.displayMetrics.widthPixels / resources.displayMetrics.density
        val columns = (widthDp / 180).toInt().coerceAtLeast(2)
        binding.vehicleList.layoutManager = GridLayoutManager(requireContext(), columns)
        binding.vehicleList.adapter = adapter

        binding.chipFavorites.setOnCheckedChangeListener { _, _ -> render() }
        binding.addButton.setOnClickListener { showAddDialog() }

        viewModel.vehicles.observe(viewLifecycleOwner) { list ->
            allVehicles = list
            render()
        }
        GameManager.get(requireContext()).score.observe(viewLifecycleOwner) { render() }
    }

    private fun render() {
        val onlyFavorites = binding.chipFavorites.isChecked
        val shown = if (onlyFavorites) allVehicles.filter { it.isFavorite } else allVehicles
        adapter.submitList(shown)
        val game = GameManager.get(requireContext())
        if (game.isGameOver) {
            binding.subtitle.setText(R.string.game_over_restart)
        } else {
            binding.subtitle.text = getString(R.string.garage_subtitle_fmt, allVehicles.size) +
                " • " + getString(R.string.score_fmt, game.currentScore)
        }
        binding.subtitle.setOnClickListener { findNavController().navigate(R.id.navigation_scoreboard) }
        binding.emptyText.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
        binding.emptyText.setText(
            if (onlyFavorites && allVehicles.isNotEmpty()) R.string.garage_empty_fav
            else R.string.garage_empty
        )
    }

    private fun showAddDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_vehicle_title)
            .setItems(arrayOf(getString(R.string.take_photo), getString(R.string.pick_gallery))) { _, which ->
                if (which == 0) startCamera() else pickImage.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
            .show()
    }

    private fun startCamera() {
        // O app declara CAMERA no manifest, então a permissão precisa estar concedida
        // antes de abrir a câmera, senão ACTION_IMAGE_CAPTURE lança SecurityException.
        val granted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) launchCamera() else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun launchCamera() {
        val file = PhotoStorage.newPhotoFile(requireContext())
        pendingCameraFile = file
        try {
            takePicture.launch(PhotoStorage.uriForCamera(requireContext(), file))
        } catch (e: ActivityNotFoundException) {
            pendingCameraFile = null
            toast(R.string.no_camera_app)
        }
    }

    private fun importFromGallery(uri: Uri) {
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val file = PhotoStorage.importFromUri(appContext, uri)
            if (file != null) openForm(file.absolutePath) else toast(R.string.photo_error)
        }
    }

    private fun openForm(photoPath: String) {
        if (_binding == null) return
        findNavController().navigate(
            R.id.action_garage_to_form,
            bundleOf("photoPath" to photoPath, "vehicleId" to 0L)
        )
    }

    private fun toast(resId: Int) =
        Toast.makeText(requireContext(), resId, Toast.LENGTH_LONG).show()

    override fun onDestroyView() {
        super.onDestroyView()
        binding.vehicleList.adapter = null
        _binding = null
    }

    private companion object {
        const val KEY_PENDING_FILE = "pending_camera_file"
    }
}
