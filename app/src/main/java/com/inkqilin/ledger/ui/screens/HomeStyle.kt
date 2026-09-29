package com.inkqilin.ledger.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import com.inkqilin.ledger.data.CurrencyAsset
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.ui.motion.*
import com.inkqilin.ledger.ui.theme.*
import com.inkqilin.ledger.util.AppMode
import androidx.core.graphics.toColorInt
import java.text.SimpleDateFormat
import java.util.*


internal fun Modifier.frostedGlass(
    shape: RoundedCornerShape,
    isDark: Boolean
): Modifier = this
    /*.then(
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            Modifier.blur(20.dp) // iOS-style deep blur on Android 12+
        } else Modifier
    )*/
    .background(
        color = if (isDark) FrostedDark.copy(alpha = 0.8f) else FrostedLight.copy(alpha = 0.75f),
        shape = shape
    )
    .border(
        width = 0.5.dp, // Thinner iOS-style border
        color = if (isDark) FrostedBorderDark.copy(alpha = 0.4f) else FrostedBorderLight.copy(alpha = 0.25f),
        shape = shape
    )

