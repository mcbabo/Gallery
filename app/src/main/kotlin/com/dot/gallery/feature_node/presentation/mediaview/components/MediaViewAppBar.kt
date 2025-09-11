/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.core.Constants.DEFAULT_TOP_BAR_ANIMATION_DURATION
import com.dot.gallery.core.Settings.Misc.rememberAllowBlur
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.util.isVideo
import com.dot.gallery.feature_node.presentation.mediaview.rememberedDerivedState
import com.dot.gallery.feature_node.presentation.util.LocalHazeState
import com.dot.gallery.ui.theme.BlackScrim
import com.dot.gallery.ui.theme.WhiterBlackScrim
import com.dot.gallery.ui.theme.isDarkTheme
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun MediaViewAppBar(
    modifier: Modifier = Modifier,
    showUI: Boolean,
    showInfo: Boolean,
    showDate: Boolean,
    isLocked: Boolean,
    currentMedia: Media?,
    currentDate: AnnotatedString,
    paddingValues: PaddingValues,
    onGoBack: () -> Unit,
    onShowInfo: () -> Unit,
    onLock: () -> Unit
) {
    val allowBlur by rememberAllowBlur()
    val isDarkTheme = isDarkTheme()
    val isVideo by rememberedDerivedState(currentMedia) {
        currentMedia?.isVideo ?: false
    }
    val followTheme = remember(allowBlur, isVideo) { !allowBlur && !isVideo }
    AnimatedVisibility(
        visible = showUI,
        enter = enterAnimation(DEFAULT_TOP_BAR_ANIMATION_DURATION),
        exit = exitAnimation(DEFAULT_TOP_BAR_ANIMATION_DURATION)
    ) {
        val gradientColor by animateColorAsState(
            if (followTheme) {
                if (isDarkTheme) BlackScrim else WhiterBlackScrim
            } else BlackScrim,
        )
        Row(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(gradientColor, Color.Transparent)
                    )
                )
                .padding(top = paddingValues.calculateTopPadding())
                .padding(start = 8.dp, end = if (showInfo) 8.dp else 16.dp)
                .padding(vertical = 8.dp)
                .then(modifier)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val contentColor by animateColorAsState(
                targetValue = if (followTheme) MaterialTheme.colorScheme.onSurface else Color.White,
                label = "AppBarContentColor"
            )
            val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer.copy(0.5f)
            val backgroundModifier = remember(allowBlur) {
                if (!allowBlur) {
                    Modifier.background(
                        color = surfaceContainer,
                        shape = CircleShape
                    )
                } else Modifier
            }
            IconButton(
                modifier = modifier
                    .padding(horizontal = 8.dp)
                    .clip(CircleShape)
                    .then(backgroundModifier)
                    .hazeEffect(
                        state = LocalHazeState.current,
                        style = HazeMaterials.ultraThin(
                            containerColor = surfaceContainer
                        )
                    ),
                onClick = onGoBack
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Go back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.height(48.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                this@Row.AnimatedVisibility(
                    visible = showDate,
                    enter = enterAnimation,
                    exit = exitAnimation
                ) {
                    Text(
                        text = currentDate,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { onLock() }
                            )
                            .padding(horizontal = 4.dp),
                        style = MaterialTheme.typography.titleSmall,
                        color = contentColor,
                        textAlign = TextAlign.Center
                    )
                }
                this@Row.AnimatedVisibility(
                    visible = isLocked,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Locked",
                        modifier = Modifier.height(20.dp)
                    )
                }
            }
            AnimatedVisibility(
                visible = showInfo,
                enter = enterAnimation,
                exit = exitAnimation
            ) {
                IconButton(
                    modifier = modifier
                        .padding(horizontal = 8.dp)
                        .clip(CircleShape)
                        .then(backgroundModifier)
                        .hazeEffect(
                            state = LocalHazeState.current,
                            style = HazeMaterials.ultraThin(
                                containerColor = surfaceContainer
                            )
                        ),
                    onClick = onShowInfo
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "info",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.height(48.dp)
                    )
                }
            }
        }
    }
}