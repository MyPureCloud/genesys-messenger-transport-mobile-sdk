package com.genesys.cloud.messenger.androidcomposeprototype.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.genesys.cloud.messenger.androidcomposeprototype.ui.testbed.TestBedViewModel
import com.genesys.cloud.messenger.androidcomposeprototype.ui.theme.WebMessagingTheme

@Composable
fun PrototypeLauncherView(
    viewModel: TestBedViewModel,
    testBedBtnOnClick: () -> Unit,
) {
    WebMessagingTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DeploymentIdTextField(
                deploymentId = viewModel.deploymentId,
                onValueChanged = viewModel::onDeploymentIdChanged
            )

            RegionDropDownMenu(
                region = viewModel.region,
                regionsList = viewModel.regions,
                onRegionChanged = viewModel::onRegionChanged
            )

            ButtonsColumn(testBedBtnOnClick)
        }
    }
}

@Composable
private fun ButtonsColumn(
    testBedBtnOnClick: () -> Unit
) {
    Column(
        modifier =
            Modifier
                .height(86.dp)
                .width(260.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LaunchScreenButton(buttonText = "TestBed View", btnOnClick = testBedBtnOnClick)
    }
}

@Composable
private fun LaunchScreenButton(buttonText: String, btnOnClick: () -> Unit) {
    TextButton(
        modifier =
            Modifier
                .height(40.dp)
                .border(1.dp, Color.LightGray)
                .padding(4.dp),
        onClick = { btnOnClick() }
    ) {
        Text(buttonText)
    }
}

@Composable
private fun RegionDropDownMenu(
    region: String,
    regionsList: List<String>,
    onRegionChanged: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        TextButton(
            modifier =
                Modifier
                    .width(200.dp)
                    .border(1.dp, Color.LightGray)
                    .background(Color.LightGray),
            onClick = { expanded = true }
        ) {
            Text(text = region)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            regionsList.forEach { region ->
                DropdownMenuItem(
                    onClick = {
                        onRegionChanged(region)
                        expanded = false
                    }
                ) {
                    Text(text = region)
                }
            }
        }
    }
}

@Composable
private fun DeploymentIdTextField(deploymentId: String, onValueChanged: (String) -> Unit) {
    OutlinedTextField(
        modifier =
            Modifier
                .fillMaxWidth(0.8f),
        value = deploymentId,
        onValueChange = { onValueChanged(it) },
        label = { Text(text = "Deployment ID") }
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    PrototypeLauncherView(viewModel = TestBedViewModel()) { }
}
