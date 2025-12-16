package com.atmiya.innovation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import compose.icons.TablerIcons
import compose.icons.tablericons.Check
import compose.icons.tablericons.User // Added
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.atmiya.innovation.ui.theme.AtmiyaPrimary

@Composable
fun NetworkCard(
    imageModel: Any?,
    name: String,
    roleOrTitle: String,
    badges: @Composable RowScope.() -> Unit = {},
    infoContent: @Composable ColumnScope.() -> Unit = {},
    primaryButtonText: String,
    onPrimaryClick: () -> Unit,
    secondaryButtonText: String? = null,
    onSecondaryClick: () -> Unit = {},
    isSecondaryButtonEnabled: Boolean = true
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(1.dp), // Optional padding to avoid shadow clipping if tightly packed
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant) // Lighter border
    ) {
             Column(modifier = Modifier.padding(16.dp)) {
                // Header: Image + Info
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Profile Image
                    UserAvatar(
                        model = imageModel,
                        name = name,
                        modifier = Modifier.size(64.dp),
                        size = 64.dp
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = roleOrTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Badges Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            badges()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info Content (Key-Value pairs)
                Column(modifier = Modifier.fillMaxWidth()) {
                    infoContent()
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val primaryModifier = if (secondaryButtonText != null) {
                        Modifier.weight(1f).height(40.dp)
                    } else {
                        Modifier.fillMaxWidth().height(40.dp)
                    }

                    Button(
                        onClick = onPrimaryClick,
                        modifier = primaryModifier,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary, 
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(text = primaryButtonText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (secondaryButtonText != null) {
                        OutlinedButton(
                            onClick = onSecondaryClick,
                            enabled = isSecondaryButtonEnabled,
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = if (isSecondaryButtonEnabled) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isSecondaryButtonEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        ) {
                            Text(text = secondaryButtonText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
    }
}
}

@Composable
fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium, // Slightly bolder than label
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.5f), // Give more space to value
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
fun PillBadge(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = backgroundColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
