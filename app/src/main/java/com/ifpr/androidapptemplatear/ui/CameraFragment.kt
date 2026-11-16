package com.ifpr.androidapptemplatear.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.ar.core.Config
import com.ifpr.androidapptemplatear.R
import com.ifpr.androidapptemplatear.databinding.FragmentCameraBinding
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.math.Position

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null

    // Só é válido entre onCreateView e onDestroyView.
    private val binding get() = _binding!!

    private val sharedViewModel: SharedModelViewModel by activityViewModels()

    private var modelNode: ArModelNode? = null
    private var currentModelFile: String = DEFAULT_MODEL
    private var arReady = false
    private var placed = false

    // Activity Result API: o resultado chega no fragment (o requestPermissions
    // antigo, chamado via requireActivity(), entregava o resultado só à Activity).
    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                initAR()
            } else {
                Toast.makeText(requireContext(), R.string.camera_denied, Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)

        binding.sceneView.lightEstimationMode = Config.LightEstimationMode.DISABLED
        binding.place.setOnClickListener {
            if (placed) loadModel(currentModelFile) else placeModel()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentModelFile = sharedViewModel.selectedModel.value ?: DEFAULT_MODEL
        binding.modelName.text = friendlyModelName(currentModelFile)

        sharedViewModel.selectedModel.observe(viewLifecycleOwner) { name ->
            if (name != currentModelFile) {
                currentModelFile = name
                if (arReady) loadModel(name)
            }
        }

        checkCameraPermission()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        modelNode = null
        arReady = false
        _binding = null
    }

    private fun checkCameraPermission() {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) initAR() else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun initAR() {
        if (_binding == null) return
        arReady = true
        loadModel(currentModelFile)
    }

    /** Fixa o modelo no plano detectado; depois dele, o usuário pode mover/girar/escalar. */
    private fun placeModel() {
        val node = modelNode ?: return
        node.anchor()
        node.isEditable = true
        node.isScaleEditable = true

        binding.sceneView.planeRenderer.isVisible = false
        placed = true
        binding.place.setText(R.string.reposition)
    }

    private fun loadModel(fileName: String) {
        val sceneView = _binding?.sceneView ?: return

        modelNode?.let { old ->
            try {
                sceneView.removeChild(old)
            } catch (e: Exception) {
                // nó já removido
            }
        }

        placed = false
        binding.place.setText(R.string.place)
        binding.modelName.text = friendlyModelName(fileName)

        val node = ArModelNode(sceneView.engine, PlacementMode.INSTANT).apply {
            loadModelGlbAsync(
                glbFileLocation = resolveModelLocation(requireContext(), fileName),
                centerOrigin = Position(0f, 0f, 1f)
            ) {
                _binding?.sceneView?.planeRenderer?.isVisible = true
            }
            // Permitir interação com o modelo
            isEditable = true
            isScaleEditable = true
        }
        modelNode = node
        sceneView.addChild(node)
    }
}
