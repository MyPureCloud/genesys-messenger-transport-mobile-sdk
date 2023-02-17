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
import com.genesys.cloud.messenger.androidcomposeprototype.util.OKTA_AUTHORIZE_URL
import kotlinx.coroutines.runBlocking

class TestBedFragment : Fragment() {

    private val viewModel: TestBedViewModel by activityViewModels()
    private val onOktaSignIn: () -> Unit = { launchCustomTabs() }

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
            viewModel.init(requireContext(), onOktaSignIn)
        }
    }

    private fun setViewContent(composeView: ComposeView) {
        composeView.setContent {
            TestBedScreen(viewModel)
        }
    }

    private fun launchCustomTabs() {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(requireContext(), Uri.parse(OKTA_AUTHORIZE_URL))
    }
}
