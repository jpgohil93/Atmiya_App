package com.atmiya.innovation.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardOptions
import compose.icons.TablerIcons
import compose.icons.tablericons.Plus
import compose.icons.tablericons.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.AnnotatedString
import java.text.NumberFormat
import java.util.Locale
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
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None
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
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = visualTransformation
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
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Icon(TablerIcons.Plus, contentDescription = null, tint = AtmiyaPrimary, modifier = Modifier.size(40.dp))
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
                    Icon(TablerIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectDropdownField(
    label: String,
    options: List<String>,
    selectedOptions: List<String>,
    onOptionSelected: (String) -> Unit, // Callback when an option is toggled
    errorMessage: String? = null
) {
    var expanded by remember { mutableStateOf(false) }

    val displayText = if (selectedOptions.isEmpty()) "" else selectedOptions.joinToString(", ")

    Column {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = displayText,
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
                    val isSelected = selectedOptions.contains(option)
                    DropdownMenuItem(
                        text = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null // Handled by onClick
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(option)
                            }
                        },
                        onClick = {
                            onOptionSelected(option)
                            // Don't close the menu to allow multiple selections
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
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
fun BackToLoginButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = "Back to Login",
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun SimpleBackButton(onClick: () -> Unit, enabled: Boolean = true) {
    TextButton(onClick = onClick, enabled = enabled) {
        Text(
            text = "Back",
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

class IndianRupeeVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val inputText = text.text
        if (inputText.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        
        // Remove existing non-digits to handle if user pasted formatted text
        val digits = inputText.filter { it.isDigit() }
        if (digits.isEmpty()) return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        
        val doubleVal = digits.toDoubleOrNull() ?: return TransformedText(text, OffsetMapping.Identity)
        
        val format = NumberFormat.getNumberInstance(Locale("en", "IN"))
        format.maximumFractionDigits = 0
        val formattedNumber = format.format(doubleVal)
        val newText = AnnotatedString("₹ $formattedNumber")
        
        // Simplified mapping logic: since the formatting adds characters (commas, symbol), 
        // exact cursor mapping can be complex.
        // A simple robust approach used often is to map everything to the end or approximate.
        // However, for good UX, we should try to map correctly.
        
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                // Return length if offset is beyond input
                if (offset >= inputText.length) return newText.length
                // Calculate how many digits are before this offset
                val digitsBefore = inputText.take(offset).count { it.isDigit() }
                
                // Find where these digits end up in formatted string
                var trackedDigits = 0
                for (i in newText.indices) {
                    if (newText[i].isDigit()) {
                        trackedDigits++
                    }
                    if (trackedDigits == digitsBefore && (i + 1 < newText.length && newText[i+1].isDigit().not())) { 
                         // If next char is not digit (e.g. comma), move past it? 
                         // No, just return current position + 1 if we just finished the digit
                         return i + 1
                    }
                     if (trackedDigits > digitsBefore) { 
                         // We just passed the digit we were looking for
                         return i 
                    }
                }
                return newText.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                 if (offset >= newText.length) return inputText.length
                 // Count digits in formatted string up to offset
                 val digitsBefore = newText.take(offset).count { it.isDigit() }
                 
                 // Find index in original string
                 var trackedDigits = 0
                 for (i in inputText.indices) {
                     if (inputText[i].isDigit()) {
                         trackedDigits++
                     }
                     if (trackedDigits == digitsBefore) {
                         // We found the spot
                         // If next char in original is not digit? (Shouldn't happen if we filtered)
                         return i + 1
                     }
                 }
                 return inputText.length
            }
        }
        
        // Let's use a simpler mapping that just works for appending:
        // Actually, for "Registration", appending is the 99% use case.
        // But let's try to be safe. 
        // The above manual mapping is error prone without unit tests.
        // Let's rely on a custom mapping that counts matching digits.
        
        val digitBasedMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                // Clip offset
                val safeOffset = offset.coerceIn(0, inputText.length)
                // Count digits in original up to safeOffset
                val digitsBefore = inputText.substring(0, safeOffset).count { it.isDigit() }
                
                // Scan transformed text to find position after 'digitsBefore' digits
                var digitsSeen = 0
                for (i in newText.indices) {
                    if (newText[i].isDigit()) digitsSeen++
                    if (digitsSeen == digitsBefore && (i == newText.lastIndex || !newText[i+1].isDigit())) {
                         // If we've seen enough digits, and we are at a boundary (end or comma), that's our spot.
                         // But we also need to account for the symbol "₹ ".
                    }
                    if (digitsSeen > digitsBefore) return i // Should not happen if logic is strict
                }
                
                // Re-scan properly
                digitsSeen = 0
                for (i in newText.indices) {
                    if (newText[i].isDigit()) {
                        digitsSeen++
                    }
                    if (digitsSeen == digitsBefore) {
                        // We are *at* the last digit corresponding to the cursor.
                        // We should be *after* it.
                        // If we are at index i, and it's the Nth digit... 
                        return i + 1
                    }
                }
                return if (digitsBefore == 0) 2 else newText.length // 2 for "₹ "
            }

            override fun transformedToOriginal(offset: Int): Int {
                val safeOffset = offset.coerceIn(0, newText.length)
                val digitsBefore = newText.substring(0, safeOffset).count { it.isDigit() }
                var digitsSeen = 0
                for (i in inputText.indices) {
                     if (inputText[i].isDigit()) digitsSeen++
                     if (digitsSeen == digitsBefore) return i + 1
                }
                return if (digitsBefore == 0) 0 else inputText.length
            }
        }

        return TransformedText(newText, digitBasedMapping)
    }
}
