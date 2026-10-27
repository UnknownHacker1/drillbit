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
import kotlin.test.assertTrue

/**
 * Renders the real shared UI off-screen at desktop and phone sizes and checks each frame is a
 * real picture. The PNGs land in `composeApp/build/screenshots`, or in the folder named by
 * `DRILLBIT_SCREENSHOTS` (that is how the README images are made).
 */
class ScreenshotTest {
    private val folder = File(System.getenv("DRILLBIT_SCREENSHOTS") ?: "build/screenshots")

    @Test
    fun designer() =
        shoot("desktop-design", WIDE) {
            selectSet(2)
        }

    @Test
    fun naiveVersusOptimal() {
        for (strategy in AssignmentStrategy.entries) {
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
        folder.mkdirs()
        val state = DrillbitState(demoShow(), initialMode = mode).apply(setUp)
        ImageComposeScene(size.first, size.second, Density(density)) {
            DrillbitTheme { DrillbitApp(state) }
        }.use { scene ->
            scene.render(0)
            val image = scene.render(atMillis * NANOS_PER_MILLI)
            val png = image.encodeToData(EncodedImageFormat.PNG) ?: error("PNG encoding failed")
            // A blank or single-colour frame compresses to a few kilobytes; the real UI is far bigger.
            assertTrue(png.size > MIN_PNG_BYTES, "$name rendered an empty-looking frame (${png.size} bytes)")
            File(folder, "$name.png").writeBytes(png.bytes)
        }
    }

    private companion object {
        val WIDE = 1600 to 960
        val PHONE = (412 * 2.625).toInt() to (915 * 2.625).toInt()
        const val PHONE_DENSITY = 2.625f
        const val PERFORMER = 20
        const val MID_WALK = 1_700L
        const val NANOS_PER_MILLI = 1_000_000L
        const val MIN_PNG_BYTES = 50_000
    }
}
