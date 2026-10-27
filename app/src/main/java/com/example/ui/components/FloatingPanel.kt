package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import com.example.ThemeManager
import com.example.ui.theme.MyApplicationTheme

/**
 * Floating popup card: appears in the middle of the screen with margins on every side
 * (never docked to the bottom bar). Drop-in replacement for ModalBottomSheet, so existing
 * call sites keep their parameters; sheetState, shape and dragHandle are ignored.
 */
@Composable
fun FloatingPanel(
    onDismissRequest: () -> Unit,
    sheetState: Any? = null,
    containerColor: Color? = null,
    shape: Shape? = null,
    dragHandle: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val activeSurface = containerColor ?: ThemeManager.active.composeSurface
    val activeBorder = ThemeManager.active.composeBorder

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        MyApplicationTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = activeSurface,
                    border = BorderStroke(1.dp, activeBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(top = 8.dp, bottom = 8.dp),
                        content = content
                    )
                }
            }
        }
    }
}
