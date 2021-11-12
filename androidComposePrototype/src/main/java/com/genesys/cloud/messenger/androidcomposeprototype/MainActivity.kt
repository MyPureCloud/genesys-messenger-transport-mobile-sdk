package com.genesys.cloud.messenger.androidcomposeprototype

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.genesys.cloud.messenger.androidcomposeprototype.ui.launcher.PrototypeLauncherView
import com.genesys.cloud.messenger.androidcomposeprototype.ui.testbed.TestBedFragment
import com.genesys.cloud.messenger.androidcomposeprototype.ui.testbed.TestBedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class MainActivity : AppCompatActivity(), CoroutineScope {

    private val TAG = MainActivity::class.simpleName

    override val coroutineContext = Dispatchers.Main + Job()

    private val viewModel: TestBedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setPrototypeLauncherView()
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
        supportFragmentManager.beginTransaction()
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
}
