package com.genesys.cloud.messenger.androidcomposeprototype

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.genesys.cloud.messenger.androidcomposeprototype.ui.launcher.PrototypeLauncherView
import com.genesys.cloud.messenger.androidcomposeprototype.ui.testbed.TestBedFragment
import com.genesys.cloud.messenger.androidcomposeprototype.ui.testbed.TestBedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class MainActivity :
    AppCompatActivity(),
    CoroutineScope {
    private val TAG = MainActivity::class.simpleName

    override val coroutineContext = Dispatchers.Main + Job()

    private val viewModel: TestBedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setPrototypeLauncherView()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        intent?.data?.doIfRedirectedFromOkta { uri ->
            handleOktaRedirect(uri)
        }
    }

    private fun setPrototypeLauncherView() {
        setContent {
            PrototypeLauncherView(
                viewModel = viewModel,
                testBedBtnOnClick = { goToTestBedView() }
            )
        }
    }

    private fun goToTestBedView() {
        setContentView(R.layout.activity_main)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_container, TestBedFragment())
            .commit()
    }

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed")
        if (supportFragmentManager.fragments.isNotEmpty()) {
            setPrototypeLauncherView()
            supportFragmentManager.popBackStackImmediate()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data.doIfRedirectedFromOkta { uri ->
            handleOktaRedirect(uri)
        }
    }

    private inline fun Uri?.doIfRedirectedFromOkta(block: (uri: Uri) -> Unit) {
        // Check with scheme from AndroidManifest.MainActivity
        if (this?.scheme == "com.oktapreview.genesys-cloud") {
            block(this)
        }
    }

    private fun handleOktaRedirect(data: Uri) {
        Log.d(TAG, "handleOktaRedirect uri: $data")

        data.getQueryParameter("code")?.let { authCode ->
            viewModel.authCode = authCode
            viewModel.idToken = ""
        } ?: run {
            data.fragment?.let { fragment ->
                parseFragmentParameters(fragment)
            }
        }
        data.getQueryParameter("error")?.let { error ->
            val errorDescription = data.getQueryParameter("error_description") ?: error
            Log.e(TAG, "Okta error: $error - $errorDescription")
            Toast.makeText(applicationContext, errorDescription, Toast.LENGTH_LONG).show()
        }
    }

    private fun parseFragmentParameters(fragment: String) {
        val params = mutableMapOf<String, String>()
        fragment.split("&").forEach { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                val key = Uri.decode(parts[0])
                val value = Uri.decode(parts[1])
                params[key] = value
            }
        }
        params["id_token"]?.let { idToken ->
            Log.d(TAG, "handleOktaRedirect id token: $idToken")
            viewModel.idToken = idToken
            viewModel.authCode = ""
        }
        params["error"]?.let { error ->
            val errorDescription = params["error_description"] ?: error
            Log.e(TAG, "Okta error: $error - $errorDescription")
            Toast.makeText(applicationContext, errorDescription, Toast.LENGTH_LONG).show()
        }
    }
}
