package io.nekohasekai.sfa.compose.screen.login

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class WavyCookieShape(private val points: Int = 12, private val waveDepth: Float = 0.15f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val center = Offset(size.width / 2f, size.height / 2f)
        
        val w = size.width
        val h = size.height
        val R = h / 2f
        val cx = if (w > h) w / 2f - R else 0f // distance from center to circle centers
        
        for (i in 0..360) {
            val angle = i * PI / 180f
            val cosA = cos(angle).toFloat()
            val sinA = sin(angle).toFloat()
            
            var t = 0f
            
            // Try top/bottom edges
            if (sinA != 0f) {
                val tEdge = kotlin.math.abs(R / sinA)
                val xIntersect = tEdge * cosA
                if (xIntersect >= -cx && xIntersect <= cx) {
                    t = tEdge
                }
            }
            
            // Try right circle
            if (t == 0f && cosA > 0) {
                val b = -2f * cx * cosA
                val c = cx * cx - R * R
                val discriminant = b * b - 4 * c
                if (discriminant >= 0) {
                    t = (-b + kotlin.math.sqrt(discriminant)) / 2f
                }
            }
            
            // Try left circle
            if (t == 0f && cosA < 0) {
                val b = 2f * cx * cosA
                val c = cx * cx - R * R
                val discriminant = b * b - 4 * c
                if (discriminant >= 0) {
                    t = (-b + kotlin.math.sqrt(discriminant)) / 2f
                }
            }
            
            if (t == 0f) t = R // Fallback
            
            val wave = (0.5f + 0.5f * cos(points * angle).toFloat())
            val rWave = t * (1f - waveDepth * wave)
            
            val x = center.x + rWave * cosA
            val y = center.y + rWave * sinA
            
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        return Outline.Generic(path)
    }
}

@Composable
fun AnimatedBlob(
    shape: Shape,
    color: Color,
    size: Dp,
    durationMillis: Int,
    offset: DpOffset,
    reverse: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition()

    val rotation by infiniteTransition.animateFloat(
        initialValue = if (reverse) 360f else 0f,
        targetValue = if (reverse) 0f else 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rotation"
    )

    Box(
        modifier = Modifier
            .offset(x = offset.x, y = offset.y)
            .size(size)
            .rotate(rotation)
            .clip(shape)
            .background(color)
    )
}

@Composable
fun WavyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    points: Int = 12,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val waveDepth by animateFloatAsState(
        targetValue = if (isPressed) 0.30f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "morph"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Button(
        onClick = onClick,
        modifier = modifier.scale(scale),
        shape = WavyCookieShape(points = points, waveDepth = waveDepth),
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource
    ) {
        content()
    }
}
