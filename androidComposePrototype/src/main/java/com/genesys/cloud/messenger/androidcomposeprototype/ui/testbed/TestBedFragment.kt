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
import kotlinx.coroutines.runBlocking

private const val AUTHORIZATION_URL =
    "https://dev-2518047.okta.com/oauth2/default/v1/authorize?client_id=0oa7nb5ac1PfdBqZa5d7&response_type=code&scope=openid profile&redirect_uri=com.okta.dev-2518047://oauth2/code&state=state-296bc9a0-a2a2-4a57-be1a-d0e2fd9bb601"

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
        customTabsIntent.launchUrl(requireContext(), Uri.parse(AUTHORIZATION_URL))
    }
}
