package com.genesys.cloud.messenger.androidcomposeprototype.ui.testbed

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels

class TestBedFragment : Fragment() {

    private val viewModel: TestBedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewContent(this)
        viewModel.init(requireContext()) { url ->
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
}
