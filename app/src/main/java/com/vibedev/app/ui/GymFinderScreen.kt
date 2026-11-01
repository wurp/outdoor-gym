package com.vibedev.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.vibedev.app.LocationFinder
import com.vibedev.app.MapsLauncher
import com.vibedev.app.gyms.Gym
import com.vibedev.app.gyms.OverpassGymSource
import com.vibedev.app.gyms.SearchArea
import kotlinx.coroutines.launch

@Composable
fun GymFinderScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember { GymFinderState(OverpassGymSource(userAgent = context.packageName)) }
    val locationFinder = remember { LocationFinder(context) }
    var showMap by rememberSaveable { mutableStateOf(true) }

    fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun searchNearMe() = scope.launch {
        val here = locationFinder.currentLocation()
        if (here == null) {
            state.showError("Couldn't find your location. Is location turned on?")
            if (state.area == null) state.search(SearchArea.Valencia)
            return@launch
        }
        state.myLocation = here
        state.search(SearchArea.nearMe(here))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted.values.any { it }) searchNearMe()
        else scope.launch { state.search(SearchArea.Valencia) }
    }

    fun onNearMeTapped() {
        if (hasLocationPermission()) searchNearMe()
        else permissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
        )
    }

    LaunchedEffect(Unit) { onNearMeTapped() }

    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Outdoor Gym", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.area?.label == "Near me",
                    onClick = ::onNearMeTapped,
                    label = { Text("Near me") },
                )
                listOf(SearchArea.Valencia, SearchArea.Seattle).forEach { preset ->
                    FilterChip(
                        selected = state.area == preset,
                        onClick = { scope.launch { state.search(preset) } },
                        label = { Text(preset.label) },
                    )
                }
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = showMap,
                    onClick = { showMap = true },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text("Map") }
                SegmentedButton(
                    selected = !showMap,
                    onClick = { showMap = false },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text("List") }
            }
            StatusLine(state)
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (showMap) {
                GymMap(
                    area = state.area,
                    gyms = state.gyms,
                    myLocation = state.myLocation,
                    onGymTapped = { state.selected = it },
                    modifier = Modifier.fillMaxSize(),
                )
                state.selected?.let { gym ->
                    SelectedGymCard(
                        gym = gym,
                        distance = state.origin?.let { formatDistance(gym.location.distanceMetersTo(it)) },
                        onNavigate = { MapsLauncher.navigateTo(context, gym) },
                        onClose = { state.selected = null },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                    )
                }
            } else {
                GymList(state, onNavigate = { MapsLauncher.navigateTo(context, it) })
            }
        }
        Text(
            "Map data © OpenStreetMap contributors",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun StatusLine(state: GymFinderState) {
    when {
        state.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
        state.error != null -> Text(state.error!!, color = MaterialTheme.colorScheme.error)
        state.area != null -> Text(
            "${state.gyms.size} spots found in ${state.area!!.label}",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SelectedGymCard(
    gym: Gym,
    distance: String?,
    onNavigate: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(gym.name, style = MaterialTheme.typography.titleMedium)
            Text(listOfNotNull(gym.kind.label, distance).joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onNavigate) { Text("Navigate in Google Maps") }
                TextButton(onClick = onClose) { Text("Close") }
            }
        }
    }
}

@Composable
private fun GymList(state: GymFinderState, onNavigate: (Gym) -> Unit) {
    val origin = state.origin
    LazyColumn(Modifier.fillMaxSize()) {
        items(state.gymsByDistance, key = { it.id }) { gym ->
            Row(
                Modifier.fillMaxWidth().clickable { onNavigate(gym) }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(gym.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        listOfNotNull(gym.kind.label, origin?.let { formatDistance(gym.location.distanceMetersTo(it)) })
                            .joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(onClick = { onNavigate(gym) }) { Text("Go") }
            }
            HorizontalDivider()
        }
    }
}
