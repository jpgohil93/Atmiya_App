package com.atmiya.innovation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.atmiya.innovation.utils.StringUtils

@Composable
fun UserAvatar(
    model: Any?,
    name: String?,
    modifier: Modifier = Modifier,
    size: Dp? = 40.dp,
    shape: Shape = CircleShape,
    fontSize: TextUnit = 16.sp,
    contentScale: ContentScale = ContentScale.Crop
) {
    val isModelEmpty = when (model) {
        null -> true
        is String -> model.isBlank()
        else -> false
    }

    Box(
        modifier = modifier
            .then(if (size != null) Modifier.size(size) else Modifier)
            .clip(shape)
            .background(if (isModelEmpty) StringUtils.getAvatarColor(name) else Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        if (!isModelEmpty) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(model)
                    .crossfade(true)
                    .build(),
                contentDescription = name ?: "Avatar",
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = StringUtils.getInitials(name),
                color = Color.White,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
