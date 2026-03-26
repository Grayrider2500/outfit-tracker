package com.dressed.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Static samples for tuning **Color.kt**, **Theme.kt**, and **Type.kt** in Android Studio.
 *
 * Open this file → enable **Split** or **Design** → use **Compose Preview**. Refresh with the gutter ↻ or **Build → Refresh** if colors don’t update.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ThemePlaygroundScreen(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Theme playground", style = typography.headlineSmall, color = scheme.onBackground)
        Text(
            "Tweak Color.kt / Theme.kt / Type.kt, then refresh previews (↻ in the preview panel). " +
                "Use dynamicColor = false in previews to see your static palette, not wallpaper colors.",
            style = typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )

        SectionTitle("Brand constants (Color.kt)")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ColorDotSwatch("Plum", WardrobePlum)
            ColorDotSwatch("Plum soft", WardrobePlumSoft)
            ColorDotSwatch("Cream", WardrobeCream)
            ColorDotSwatch("Ink", WardrobeInk)
            ColorDotSwatch("Tan", WardrobeTan)
        }

        SectionTitle("Material colorScheme roles")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RoleSwatch("primary", scheme.primary, scheme.onPrimary)
            RoleSwatch("onPrimary", scheme.onPrimary, scheme.primary)
            RoleSwatch("primaryCont.", scheme.primaryContainer, scheme.onPrimaryContainer)
            RoleSwatch("secondary", scheme.secondary, scheme.onSecondary)
            RoleSwatch("background", scheme.background, scheme.onBackground)
            RoleSwatch("surface", scheme.surface, scheme.onSurface)
            RoleSwatch("surf. var.", scheme.surfaceVariant, scheme.onSurfaceVariant)
            RoleSwatch("outline", scheme.outline, scheme.surface)
        }

        SectionTitle("Typography (Type.kt + defaults)")
        Text("headlineLarge", style = typography.headlineLarge)
        Text("headlineSmall", style = typography.headlineSmall)
        Text("titleLarge", style = typography.titleLarge)
        Text("titleMedium", style = typography.titleMedium)
        Text("bodyLarge", style = typography.bodyLarge)
        Text("bodyMedium", style = typography.bodyMedium)
        Text("labelSmall", style = typography.labelSmall)

        SectionTitle("Wardrobe-style chips")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("All", "Tops", "Bottoms", "Dresses", "Shoes").forEachIndexed { i, label ->
                FilterChip(
                    selected = i == 1,
                    onClick = {},
                    label = { Text(label) },
                )
            }
        }

        SectionTitle("Buttons")
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = {}) { Text("Primary") }
            OutlinedButton(onClick = {}) { Text("Outlined") }
        }

        SectionTitle("Sample card (grid item)")
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = scheme.surface),
            modifier = Modifier.fillMaxWidth(0.55f),
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .background(scheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("👕", style = typography.displaySmall)
                }
                Column(Modifier.padding(10.dp, 12.dp)) {
                    Text("Sample blouse", style = typography.titleSmall)
                    Text("Tops", style = typography.labelSmall, color = scheme.onSurfaceVariant)
                }
            }
        }

        SectionTitle("Mini scaffold (list + FAB)")
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 2.dp,
            color = scheme.surface,
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Dressed", style = typography.headlineSmall, color = scheme.onPrimary)
                                Text(
                                    "MY PERSONAL WARDROBE",
                                    style = typography.labelSmall,
                                    color = scheme.onPrimary.copy(alpha = 0.75f),
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = scheme.primary,
                            titleContentColor = scheme.onPrimary,
                        ),
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {},
                        containerColor = scheme.secondary,
                        contentColor = scheme.onSecondary,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                    }
                },
            ) { inner ->
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(inner)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Checkroom, contentDescription = null, tint = scheme.primary)
                    Icon(Icons.Filled.Search, contentDescription = null, tint = scheme.primary)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun ColorDotSwatch(label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun RoleSwatch(role: String, background: Color, content: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(content),
        )
        Spacer(Modifier.width(6.dp))
        Text(role, style = MaterialTheme.typography.labelSmall, color = content)
    }
}

@Preview(
    name = "Playground · Light (static palette)",
    showBackground = true,
    backgroundColor = 0xFFF0F0F0,
    heightDp = 900,
)
@Composable
private fun ThemePlaygroundLightPreview() {
    DressedTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ThemePlaygroundScreen()
        }
    }
}

@Preview(
    name = "Playground · Dark (static palette)",
    showBackground = true,
    backgroundColor = 0xFF101010,
    heightDp = 900,
)
@Composable
private fun ThemePlaygroundDarkPreview() {
    DressedTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ThemePlaygroundScreen()
        }
    }
}
