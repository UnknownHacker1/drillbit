package dev.teima.drillbit

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import dev.teima.drillbit.core.demo.demoShow
import dev.teima.drillbit.core.plan.AssignmentStrategy
import dev.teima.drillbit.ui.DrillbitApp
import dev.teima.drillbit.ui.state.DrillbitState
import dev.teima.drillbit.ui.state.FormationKind
import dev.teima.drillbit.ui.state.FormationSpec
import dev.teima.drillbit.ui.state.Mode
import dev.teima.drillbit.ui.theme.DrillbitTheme
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/**
 * Renders the real shared UI off-screen and saves PNGs for the README, when
 * `DRILLBIT_SCREENSHOTS` points at a folder. Does nothing otherwise.
 */
class ScreenshotTest {
    private val folder = System.getenv("DRILLBIT_SCREENSHOTS")?.let(::File)

    @Test
    fun designer() =
        shoot("desktop-design", WIDE) {
            selectSet(2)
        }

    @Test
    fun naiveVersusOptimal() {
        for (strategy in listOf(AssignmentStrategy.IN_ORDER, AssignmentStrategy.OPTIMAL)) {
            shoot("desktop-morph-${strategy.name.lowercase()}", WIDE, atMillis = MID_WALK) {
                selectSet(0)
                openDraft(replace = false)
                updateDraft(draft!!.copy(spec = FormationSpec(FormationKind.TEXT, "KOTLIN"), strategy = strategy))
            }
        }
    }

    @Test
    fun performerOnAPhone() =
        shoot("phone-perform", PHONE, density = PHONE_DENSITY, mode = Mode.PERFORM) {
            selectedPerformer = PERFORMER
            selectSet(2)
        }

    @Test
    fun designerOnAPhone() =
        shoot("phone-design", PHONE, density = PHONE_DENSITY) {
            selectSet(1)
        }

    private fun shoot(
        name: String,
        size: Pair<Int, Int>,
        density: Float = 1f,
        mode: Mode = Mode.DESIGN,
        atMillis: Long = 0,
        setUp: DrillbitState.() -> Unit,
    ) {
        val out = folder ?: return
        out.mkdirs()
        val state = DrillbitState(demoShow(), initialMode = mode).apply(setUp)
        ImageComposeScene(size.first, size.second, Density(density)) {
            DrillbitTheme { DrillbitApp(state) }
        }.use { scene ->
            scene.render(0)
            val image = scene.render(atMillis * NANOS_PER_MILLI)
            val png = image.encodeToData(EncodedImageFormat.PNG) ?: error("PNG encoding failed")
            File(out, "$name.png").writeBytes(png.bytes)
        }
    }

    private companion object {
        val WIDE = 1600 to 960
        val PHONE = (412 * 2.625).toInt() to (915 * 2.625).toInt()
        const val PHONE_DENSITY = 2.625f
        const val PERFORMER = 20
        const val MID_WALK = 1_700L
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
