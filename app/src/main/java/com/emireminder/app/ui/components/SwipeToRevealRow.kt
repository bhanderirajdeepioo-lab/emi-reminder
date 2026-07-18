package com.emireminder.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Swipe left to delete. Item is held in dismissed state while the caller removes it;
 * the LazyList key handles the exit animation once the data changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteRow(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    cardCornerRadius: Dp = 16.dp,
    horizontalPadding: Dp = 16.dp,
    verticalPadding: Dp = 4.dp,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { it != SwipeToDismissBoxValue.StartToEnd },
        positionalThreshold = { totalDistance -> totalDistance * 0.45f },
    )

    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val bg by animateColorAsState(
                targetValue = if (state.targetValue == SwipeToDismissBoxValue.EndToStart)
                    MaterialTheme.colorScheme.error else Color.Transparent,
                animationSpec = tween(150),
                label = "swipe_delete_bg",
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                    .clip(RoundedCornerShape(cardCornerRadius))
                    .background(bg),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.padding(end = 20.dp).size(22.dp),
                )
            }
        },
    ) {
        content()
    }
}

/**
 * Swipe left to trigger edit. Item springs back after edit is triggered — it is not dismissed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToEditRow(
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    cardCornerRadius: Dp = 12.dp,
    horizontalPadding: Dp = 16.dp,
    verticalPadding: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onEdit()
            }
            // Never confirm EndToStart — always spring back to Settled.
            value != SwipeToDismissBoxValue.EndToStart
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.4f },
    )

    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val bg by animateColorAsState(
                targetValue = if (state.targetValue == SwipeToDismissBoxValue.EndToStart)
                    MaterialTheme.colorScheme.primary else Color.Transparent,
                animationSpec = tween(150),
                label = "swipe_edit_bg",
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                    .clip(RoundedCornerShape(cardCornerRadius))
                    .background(bg),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = Color.White,
                    modifier = Modifier.padding(end = 20.dp).size(22.dp),
                )
            }
        },
    ) {
        content()
    }
}
