package com.atmiya.innovation.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.atmiya.innovation.ui.theme.AtmiyaPrimary

@Composable
fun ValidatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leadingIcon: @Composable (() -> Unit)? = null,
    minLines: Int = 1,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SoftTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            placeholder = placeholder,
            isError = errorMessage != null,
            keyboardOptions = keyboardOptions,
            leadingIcon = leadingIcon,
            minLines = minLines,
            readOnly = readOnly,
            modifier = Modifier.fillMaxWidth()
        )
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    options: List<String>,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit,
    errorMessage: String? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedOption ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                placeholder = { Text("Select $label") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                isError = errorMessage != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = AtmiyaPrimary,
                    unfocusedBorderColor = Color.Transparent,
                    errorBorderColor = MaterialTheme.colorScheme.error
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun PhotoUploadField(
    imageUri: Uri?,
    onUploadClick: () -> Unit,
    errorMessage: String? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        SoftCard(
            modifier = Modifier.size(120.dp),
            radius = 60.dp,
            onClick = onUploadClick,
            elevation = 4.dp,
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Profile Photo Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.UploadFile, contentDescription = null, tint = AtmiyaPrimary, modifier = Modifier.size(40.dp))
                }
            }
        }
        if (errorMessage != null) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        } else {
            Text("Tap to upload photo", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
        }
        }
    }


@Composable
fun StepIndicator(step: Int, title: String, isActive: Boolean, isCompleted: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SoftCard(
            modifier = Modifier.size(40.dp),
            radius = 20.dp,
            elevation = if (isActive) 8.dp else 2.dp,
            backgroundColor = if (isActive || isCompleted) AtmiyaPrimary else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (isCompleted) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        text = step.toString(),
                        color = if (isActive || isCompleted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = if (isActive) AtmiyaPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontWeight = if (isActive) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
        )
    }
}
