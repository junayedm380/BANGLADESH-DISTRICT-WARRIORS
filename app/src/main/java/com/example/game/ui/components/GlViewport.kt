package com.example.game.ui.components

import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.game.engine.GameSimulation
import com.example.game.engine.GlRenderMode
import com.example.game.engine.WorldGlRenderer
import com.example.game.save.GraphicsQuality

@Composable
fun GlViewport(
    simulation: GameSimulation,
    renderMode: GlRenderMode,
    graphicsQuality: GraphicsQuality,
    modifier: Modifier = Modifier,
    onSimulationTick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val renderer = remember(renderMode) {
        WorldGlRenderer(
            simulation = simulation,
            renderMode = renderMode,
            onSimulationTick = onSimulationTick?.let { cb -> { _ -> cb() } }
        )
    }
    renderer.graphicsQuality = graphicsQuality

    val glView = remember(renderMode) {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(2)
            setRenderer(renderer)
            this.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    DisposableEffect(lifecycleOwner, glView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glView.onResume()
                Lifecycle.Event.ON_PAUSE -> glView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            glView.onPause()
        }
    }

    AndroidView(
        factory = { glView },
        modifier = modifier
    )
}
