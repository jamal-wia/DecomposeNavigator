package io.github.jamal_wia.decomposenavigator

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.extensions.compose.stack.animation.Direction
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.isFront
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimator
import com.arkivanov.decompose.router.stack.ChildStack
import kotlin.math.abs

private const val DEFAULT_DURATION_MS = 500

enum class SlideDirection {
    /** Slide forward: new screen enters from right, old exits to left. */
    FORWARD,

    /** Slide backward: new screen enters from left, old exits to right. */
    BACKWARD,
}

/**
 * Fade (crossfade) animation — suitable for switch/tab transitions.
 *
 * factor → 0 means fully visible, |factor| → 1 means fully hidden.
 */
fun <C : Any, T : Any> fadeStackAnimation(
    animationSpec: FiniteAnimationSpec<Float> = tween(DEFAULT_DURATION_MS),
): StackAnimation<C, T> =
    stackAnimation(stackAnimator(animationSpec) { factor, _, content ->
        content(Modifier.graphicsLayer { alpha = 1f - abs(factor) })
    })

/**
 * A screen that rises from the bottom over the one beneath it and sinks back the same way,
 * while the screen beneath stays where it is, fully drawn — the way a sheet covers a page.
 *
 * Mark a `ScreenConfig` with it and the navigator's standard animation does the rest; nothing
 * is passed at the push. The standard slide fades BOTH screens on the way, so for a moment the
 * container's own background shows through — invisible between two plain screens, a white flash
 * between two drawn on a photograph. A cover never lets the screen beneath fade.
 */
interface CoveringScreen

/**
 * Whether the transition that made [activeConfig] the top screen, in place of [previousConfig],
 * is a cover rather than a slide: it is as soon as either side is a [CoveringScreen], because the
 * screen beneath a cover has to hold still just as much as the cover has to rise.
 */
internal fun isCoverTransition(activeConfig: Any?, previousConfig: Any?): Boolean =
    activeConfig is CoveringScreen || previousConfig is CoveringScreen

/**
 * The navigator's standard animation: a slide that reads [slideDirection] state at each frame —
 * except across a [CoveringScreen], where the cover rises or sinks and the other screen holds.
 *
 * This uses a single [StackAnimation] object, so Decompose's per-child animator
 * caching works correctly — the same animator handles both entering and exiting
 * children, reading the current [slideDirection] dynamically.
 *
 * [SlideDirection.FORWARD]: `translationX = factor * width` (enter from right, exit to left)
 * [SlideDirection.BACKWARD]: `translationX = -(factor * width)` (enter from left, exit to right)
 */
internal fun <C : Any, T : Any> slideStackAnimation(
    slideDirection: State<SlideDirection>,
    animationSpec: FiniteAnimationSpec<Float> = tween(DEFAULT_DURATION_MS),
): StackAnimation<C, T> = CoverAwareSlideStackAnimation(slideDirection, animationSpec)

/**
 * The slide, with one eye on [CoveringScreen]: on every stack change it notes whether the screen
 * that just became active, or the one it replaced, is a cover, and the single animator then reads
 * that note. The front child of a cover transition rises and sinks (never fades); the back child
 * holds still. Everything else slides as before.
 *
 * Built on Decompose's plain single-animator animation and not on its per-child selector, which
 * is marked faulty (it rests on `movableContentOf`, with known bugs). The animator cannot see the
 * configurations, but it does see the [Direction], and the front/back side of a transition is all
 * the cover needs once the class has noted that a cover is involved.
 */
private class CoverAwareSlideStackAnimation<C : Any, T : Any>(
    private val slideDirection: State<SlideDirection>,
    animationSpec: FiniteAnimationSpec<Float>,
) : StackAnimation<C, T> {

    /** Whether the transition under way involves a [CoveringScreen]; read by the animator per frame. */
    private var isCover: Boolean = false

    /** The configuration that was active at the last stack change, to tell what a change replaced. */
    private var previousActiveConfig: C? = null

    /**
     * The key of the child that was active at the last stack change. A change is told by the child,
     * as Decompose tells it, and not by the configuration: a screen taken off and put back in one
     * step — the prayer widget does that with the schedule — has an equal configuration but is a new
     * child, animated as a push. Told by configuration it was no change at all, and the new screen
     * kept whatever the last transition had decided: after a visit to the city picker it rose like one.
     */
    private var previousActiveKey: String? = null

    private val inner: StackAnimation<C, T> = stackAnimation(
        animator = stackAnimator(animationSpec) { factor, direction, content ->
            // Taken once per transition: a screen gets a new direction with every transition it
            // joins, and keeps it to the end. A stack change that arrives mid-transition is held back
            // by Decompose until the running one ends, but it rewrites [isCover] at once — read on
            // every frame, a picker still sinking would turn into a sideways slide under a Back pressed
            // before it was down, and the schedule beneath it would lurch sideways with it.
            val cover: Boolean = remember(direction) { isCover }
            // One call to content, whatever the transition. The kind of transition decides what the
            // layer does, never where the screen sits in the composition: with a call per kind, a
            // screen that went from sliding to lying beneath a cover — or back — was thrown away and
            // built afresh mid-transition, losing its scroll position and everything it remembered.
            content(transitionModifier(factor, direction, cover))
        },
    )

    @Composable
    private fun transitionModifier(factor: Float, direction: Direction, cover: Boolean): Modifier {
        val rtlMirror: Float =
            if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1.0f else 1.0f
        val sign: Float = when (slideDirection.value) {
            SlideDirection.FORWARD -> 1.0f
            SlideDirection.BACKWARD -> -1.0f
        }
        return Modifier.graphicsLayer {
            when {
                !cover -> {
                    translationX = rtlMirror * sign * factor * size.width
                    alpha = 1f - abs(factor)
                }
                // The cover: entering, factor runs 1 → 0 and it rises from a full height below;
                // leaving, 0 → 1 and it sinks back. Never faded — opaque all the way.
                direction.isFront -> translationY = factor * size.height
                // The screen beneath a cover: drawn as is for the whole duration.
                else -> Unit
            }
        }
    }

    @Composable
    override fun invoke(
        stack: ChildStack<C, T>,
        modifier: Modifier,
        content: @Composable (child: Child.Created<C, T>) -> Unit,
    ) {
        val active: Child.Created<C, T> = stack.active
        if (active.key != previousActiveKey) {
            isCover = isCoverTransition(active.configuration, previousActiveConfig)
            previousActiveConfig = active.configuration
            previousActiveKey = active.key
        }
        inner(stack, modifier, content)
    }
}

/**
 * Returns a marker [StackAnimation] that instructs the navigator to use slide animation
 * with the given [direction].
 *
 * The navigator recognizes this marker and updates its internal [SlideDirection] state
 * without replacing the [StackAnimation] object, avoiding Decompose's per-child animator
 * caching issue.
 *
 * Usage:
 * ```
 * navigator.replace(config, slideStackAnimation(SlideDirection.FORWARD))
 * ```
 */
fun <C : Any, T : Any> slideStackAnimation(
    direction: SlideDirection,
): StackAnimation<C, T> = SlideAnimationMarker(direction)

/**
 * Marker class used by navigator to recognize slide animation requests.
 * Never actually rendered — the navigator intercepts it and updates [SlideDirection] state instead.
 */
internal class SlideAnimationMarker<C : Any, T : Any>(
    val direction: SlideDirection,
) : StackAnimation<C, T> {

    @androidx.compose.runtime.Composable
    override fun invoke(
        stack: com.arkivanov.decompose.router.stack.ChildStack<C, T>,
        modifier: Modifier,
        content: @androidx.compose.runtime.Composable (child: com.arkivanov.decompose.Child.Created<C, T>) -> Unit,
    ) {
        error("SlideAnimationMarker should never be rendered. The navigator must intercept it.")
    }
}

/**
 * Push (slide from right) animation — suitable for forward navigation.
 * New screen slides in from the right, old screen shifts slightly to the left.
 *
 * factor semantics:
 * - ENTER_FRONT (new screen entering on push): 1 → 0  (slide from right to center)
 * - EXIT_BACK (old screen leaving on push): 0 → -1  (slide from center to left)
 */
fun <C : Any, T : Any> pushStackAnimation(
    animationSpec: FiniteAnimationSpec<Float> = tween(DEFAULT_DURATION_MS),
): StackAnimation<C, T> =
    stackAnimation(stackAnimator(animationSpec) { factor, _, content ->
        val rtlMirror: Float =
            if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1.0f else 1.0f
        content(
            Modifier.graphicsLayer {
                translationX = rtlMirror * factor * size.width
                alpha = 1f - abs(factor)
            }
        )
    })

/**
 * Pop (slide from left) animation — suitable for backward navigation.
 * Returning screen slides in from the left, current screen slides out to the right.
 *
 * factor semantics:
 * - ENTER_BACK (old screen re-entering on pop): -1 → 0  (slide from left to center)
 * - EXIT_FRONT (current screen leaving on pop): 0 → 1  (slide from center to right)
 */
fun <C : Any, T : Any> popStackAnimation(
    animationSpec: FiniteAnimationSpec<Float> = tween(DEFAULT_DURATION_MS),
): StackAnimation<C, T> =
    stackAnimation(stackAnimator(animationSpec) { factor, _, content ->
        val rtlMirror: Float =
            if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1.0f else 1.0f
        content(
            Modifier.graphicsLayer {
                translationX = rtlMirror * -(factor * size.width)
                alpha = 1f - abs(factor)
            }
        )
    })
