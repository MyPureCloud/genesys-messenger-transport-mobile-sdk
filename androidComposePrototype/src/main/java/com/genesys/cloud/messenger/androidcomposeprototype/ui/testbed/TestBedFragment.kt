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
import com.genesys.cloud.messenger.androidcomposeprototype.util.getSharedPreferences
import kotlinx.coroutines.runBlocking

class TestBedFragment : Fragment() {

    private val viewModel: TestBedViewModel by activityViewModels()
    private val onOktaSignIn: (url: String) -> Unit = { url -> launchCustomTabs(url) }
    private val onOktaLogout: () -> Unit = {
        context?.getSharedPreferences()?.edit()?.run {
            remove("authCode")
            apply()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewContent(this)
    }

    override fun onResume() {
        super.onResume()
        runBlocking {
            viewModel.init(requireContext(), onOktaSignIn, onOktaLogout)
        }
    }

    private fun setViewContent(composeView: ComposeView) {
        composeView.setContent {
            TestBedScreen(viewModel)
        }
    }

    private fun launchCustomTabs(url: String) {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        println("Launching chrome custom tab with url: $url")
        customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
    }
}
