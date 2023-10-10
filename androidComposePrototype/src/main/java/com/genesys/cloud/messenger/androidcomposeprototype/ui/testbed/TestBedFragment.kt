package com.genesys.cloud.messenger.androidcomposeprototype.ui.testbed

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.genesys.cloud.messenger.transport.core.FileAttachmentProfile
import com.genesys.cloud.messenger.transport.util.DefaultVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TestBedFragment : Fragment() {

    private val viewModel: TestBedViewModel by activityViewModels()
    private val selectFileResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ::handleFileSelectionResult
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewContent(this)
        viewModel.init(
            requireContext(),
            { fileAttachmentProfile -> selectFile(fileAttachmentProfile) })
        { url ->
            launchCustomTabs(url)
        }
    }

    private fun setViewContent(composeView: ComposeView) {
        composeView.setContent {
            TestBedScreen(viewModel)
        }
    }

    private fun launchCustomTabs(url: String) {
        println("Launching chrome custom tab with url: $url")
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
    }

    private fun selectFile(fileAttachmentProfile: FileAttachmentProfile) {
        if (!fileAttachmentProfile.hasWildCard && fileAttachmentProfile.allowedFileTypes.isEmpty()) {
            viewModel.onCancelFileSelection()
            return
        }
        val filesWithType =
            if (fileAttachmentProfile.hasWildCard) arrayOf("*/*") else fileAttachmentProfile.allowedFileTypes.toTypedArray()
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = filesWithType[0]
            putExtra(Intent.EXTRA_MIME_TYPES, filesWithType)
        }
        selectFileResult.launch(intent)
    }

    private fun handleFileSelectionResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    processFile(uri)
                }
            } ?: viewModel.onCancelFileSelection()
        } else {
            viewModel.onCancelFileSelection()
        }
    }

    private fun processFile(uri: Uri) {
        try {
            context?.contentResolver?.openInputStream(uri)?.use {
                viewModel.onFileSelected(it.readBytes(), uri.getFileName())
            }
        } catch (e: Exception) {
            viewModel.onErrorFilePick(e)
        }
    }

    private fun Uri.getFileName(): String {
        val cursor = DefaultVault.context?.contentResolver?.query(this, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            it.getString(nameIndex)
        } ?: "unknown"
    }
}
