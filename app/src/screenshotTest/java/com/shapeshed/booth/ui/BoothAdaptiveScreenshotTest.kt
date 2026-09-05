package com.shapeshed.booth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.shapeshed.booth.ui.theme.BoothAppTheme

@PreviewTest
@Preview(name = "Compact", device = "spec:width=400dp,height=800dp,dpi=420")
@Preview(name = "Medium", device = "spec:width=610dp,height=800dp,dpi=420")
@Preview(name = "Expanded", device = "spec:width=900dp,height=800dp,dpi=420")
@Composable
fun BoothAdaptiveLayoutScreenshot() {
    BoothAppTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Subscriptions", style = MaterialTheme.typography.headlineSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 960.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    AdaptiveNavigationPreview()
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(3) { index ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "Podcast ${index + 1}",
                                    modifier = Modifier.padding(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdaptiveNavigationPreview() {
    // Keep this preview deterministic while exercising the same compact/expanded navigation
    // components that the home shell uses.
    BoxWithConstraints {
        if (maxWidth < 600.dp) {
            ShortNavigationBar {
                ShortNavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Rounded.GridView, contentDescription = null) },
                    label = { Text("Home") },
                )
                ShortNavigationBarItem(
                    selected = false,
                    onClick = {},
                    icon = { Icon(Icons.Rounded.RssFeed, contentDescription = null) },
                    label = { Text("Subscriptions") },
                )
            }
        } else {
            NavigationRail {
                NavigationRailItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Rounded.GridView, contentDescription = null) },
                    label = { Text("Home") },
                )
                NavigationRailItem(
                    selected = false,
                    onClick = {},
                    icon = { Icon(Icons.Rounded.RssFeed, contentDescription = null) },
                    label = { Text("Subscriptions") },
                )
            }
        }
    }
}
