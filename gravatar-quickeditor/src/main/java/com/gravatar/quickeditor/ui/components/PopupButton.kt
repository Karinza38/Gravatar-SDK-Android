package com.gravatar.quickeditor.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gravatar.quickeditor.R
import com.gravatar.ui.GravatarTheme

@Composable
internal fun PopupButton(
    text: String,
    @DrawableRes iconRes: Int?,
    contentDescription: String,
    onClick: () -> Unit,
    shape: Shape,
    color: Color? = null,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                color = color ?: MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            iconRes?.let {
                Icon(
                    painter = painterResource(id = it),
                    contentDescription = contentDescription,
                    tint = color ?: MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Preview
@Composable
private fun PopupButtonPreview() {
    GravatarTheme {
        Box(modifier = Modifier.fillMaxWidth()) {
            PopupButton(
                text = "Choose a Photo",
                iconRes = R.drawable.gravatar_photo_library,
                contentDescription = "Content description",
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                onClick = { },
            )
        }
    }
}
