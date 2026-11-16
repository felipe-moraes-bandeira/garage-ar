package com.ifpr.androidapptemplatear.ui

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ifpr.androidapptemplatear.R
import com.ifpr.androidapptemplatear.databinding.FragmentChangeobjectBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Tela "Modelos": lista os GLB incluídos no app e os importados pelo usuário. */
class ChangeObjectFragment : Fragment() {

    private var _binding: FragmentChangeobjectBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedModelViewModel by activityViewModels()
    private lateinit var adapter: ModelAdapter

    private val importLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) importModel(uri)
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangeobjectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ModelAdapter(
            onShow = { item ->
                // Informa a tela de AR qual modelo carregar e abre a câmera.
                sharedViewModel.selectModel(item.file)
                findNavController().navigate(R.id.navigation_camera)
            },
            onDelete = { item -> confirmDelete(item) }
        )
        binding.modelList.layoutManager = LinearLayoutManager(requireContext())
        binding.modelList.adapter = adapter
        binding.importButton.setOnClickListener { importLauncher.launch(arrayOf("*/*")) }

        refresh()
    }

    /** Procura todos os .glb em assets (incluídos) e em filesDir (importados). */
    private fun refresh() {
        val ctx = requireContext()
        val builtin = ctx.assets.list("")
            ?.filter { it.endsWith(".glb", ignoreCase = true) }
            ?.map { ModelItem(it, imported = false) }
            ?: emptyList()
        val imported = importedModels(ctx).map { ModelItem(it.name, imported = true) }
        val all = (builtin + imported).sortedBy { it.file.lowercase() }

        adapter.submitList(all)
        binding.emptyText.visibility = if (all.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun importModel(uri: Uri) {
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) { copyGlb(appContext, uri) }
            if (_binding == null) return@launch
            if (ok) {
                Toast.makeText(requireContext(), R.string.model_import_ok, Toast.LENGTH_SHORT).show()
                refresh()
            } else {
                Toast.makeText(requireContext(), R.string.model_import_fail, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun copyGlb(context: android.content.Context, uri: Uri): Boolean {
        return try {
            val name = context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst() && idx >= 0) c.getString(idx) else null
            } ?: return false
            if (!name.endsWith(".glb", ignoreCase = true)) return false

            // Evita path traversal e sobrescrever arquivos fora do esperado.
            val safeName = File(name).name
            val target = File(context.filesDir, safeName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                val magic = ByteArray(4)
                if (input.read(magic) != 4 || String(magic, Charsets.US_ASCII) != "glTF") {
                    return false
                }
                target.outputStream().use { out ->
                    out.write(magic)
                    input.copyTo(out)
                }
            } ?: return false
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun confirmDelete(item: ModelItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete)
            .setMessage(friendlyModelName(item.file))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                File(requireContext().filesDir, item.file).delete()
                refresh()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.modelList.adapter = null
        _binding = null
    }
}
