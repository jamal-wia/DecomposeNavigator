package io.github.jamal_wia.decomposenavigator

/**
 * A [RenderComponent] that holds navigation of its own and can put it back to the screen it started
 * on, forwarding the same request to whatever it has on top.
 *
 * Navigation here is a tree, not a stack: the app's own stack holds a tab bar, the tab bar holds a
 * switch, and a tab may hold a stack again. "Take the reader back to the first screen" therefore
 * cannot be answered by any one level — clearing the outermost stack leaves a screen pushed inside a
 * tab exactly where it was, and the reader is still not looking at what they asked for. The guest's
 * answer to the home-screen prayer widget is the clearest case: their Home is the first screen of the
 * Home tab's own stack, so a notifications list opened over it survives anything the outer stack does.
 *
 * Implemented by the levels that navigate and ignored by the screens that do not, so a request walks
 * down as far as there is navigation to undo and stops there.
 */
interface ReturnsToFirstScreen {

    /** Returns this component's own navigation to its first screen, and asks its child to do the same. */
    fun returnToFirstScreen()
}
