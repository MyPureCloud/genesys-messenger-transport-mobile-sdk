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
        // If authcode is present.
        data.getQueryParameter("code")?.let { authCode ->
            viewModel.authCode = authCode
        }
        // Otherwise there will be an error.
        data.getQueryParameter("error_description")?.let { error ->
            Log.e(TAG, error)
            Toast.makeText(applicationContext, error, Toast.LENGTH_LONG).show()
        }
    }
}
