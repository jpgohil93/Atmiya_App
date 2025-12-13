package com.atmiya.innovation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atmiya.innovation.ui.theme.*

// --- Core Surfaces ---

@Composable
fun SoftScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        containerColor = MaterialTheme.colorScheme.background,
        content = content
    )
}

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    elevation: Dp = 4.dp,
    radius: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val shadowColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)
    val shape = RoundedCornerShape(radius)

    val cardModifier = modifier
        .shadow(
            elevation = if (isDark) 0.dp else elevation,
            shape = shape,
            spotColor = shadowColor,
            ambientColor = shadowColor
        )
        .then(
            if (isDark) Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), shape) else Modifier
        )
        .clip(shape)
        .background(backgroundColor)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(16.dp)

    Column(
        modifier = cardModifier,
        content = content
    )
}

// --- Inputs ---

@Composable
fun SoftTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
    minLines: Int = 1,
    readOnly: Boolean = false,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            focusedBorderColor = AtmiyaPrimary,
            unfocusedBorderColor = Color.Transparent, // Soft look
            errorBorderColor = MaterialTheme.colorScheme.error
        ),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        isError = isError,
        minLines = minLines,
        readOnly = readOnly,
        singleLine = minLines == 1
    )
}

// --- Buttons ---

@Composable
fun SoftButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    containerColor: Color = AtmiyaPrimary,
    contentColor: Color = Color.White
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 2.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = contentColor, modifier = Modifier.size(24.dp))
        } else {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

// --- Icons ---

@Composable
fun SoftIconBox(
    icon: ImageVector,
    tint: Color = AtmiyaPrimary,
    backgroundColor: Color = PastelBlue,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

// --- Bento Grid ---

enum class BentoCardType {
    HERO, FEATURE, UTILITY
}

data class BentoItem(
    val type: BentoCardType,
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector? = null,
    val metric: String? = null,
    val badge: String? = null,
    val span: Int = 1,
    val onClick: () -> Unit = {}
)

@Composable
fun BentoGrid(
    items: List<BentoItem>,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {},
    footer: @Composable () -> Unit = {}
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 100.dp) // Built-in padding for Floating Pill
    ) {
        item(span = { GridItemSpan(2) }) {
            header()
        }
        items(items, span = { item -> GridItemSpan(item.span) }) { item ->
            BentoCard(item)
        }
        item(span = { GridItemSpan(2) }) {
            footer()
        }
    }
}

@Composable
fun BentoCard(item: BentoItem) {
    val isDark = isSystemInDarkTheme()
    // Dynamic pastel background for Hero/Feature cards if desired, or stick to surface
    val bgColor = if (item.type == BentoCardType.HERO) {
        if (isDark) AtmiyaPrimary.copy(alpha = 0.3f) else PastelBlue
    } else {
        MaterialTheme.colorScheme.surface
    }

    SoftCard(
        onClick = item.onClick,
        backgroundColor = bgColor,
        modifier = Modifier.height(if (item.type == BentoCardType.HERO) 180.dp else 160.dp)
    ) {
        // Badge
        if (item.badge != null) {
            Surface(
                color = AtmiyaSecondary,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = item.badge,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Icon
        if (item.icon != null) {
            SoftIconBox(
                icon = item.icon,
                tint = if (item.type == BentoCardType.HERO) AtmiyaPrimary else AtmiyaAccent,
                backgroundColor = if (item.type == BentoCardType.HERO) Color.White.copy(alpha = 0.5f) else AtmiyaPrimary.copy(alpha = 0.1f),
                size = 40.dp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Text
        Text(
            text = item.title,
            style = if (item.type == BentoCardType.HERO) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (item.subtitle != null) {
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        if (item.metric != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.metric,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = AtmiyaPrimary
            )
        }
    }
}
