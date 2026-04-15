package eu.kanade.tachiyomi.ui.setting

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.screen.dash.DashSettingsScreenContent
import eu.kanade.presentation.util.DefaultNavigatorScreenTransition
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen

class DashSettingsScreen : Screen() {

    @Composable
    override fun Content() {
        val parentNavigator = LocalNavigator.currentOrThrow
        Navigator(
            screen = DashSettingsScreenContent,
            content = {
                val pop: () -> Unit = {
                    if (it.canPop) {
                        it.pop()
                    } else {
                        parentNavigator.pop()
                    }
                }
                CompositionLocalProvider(LocalBackPress provides pop) {
                    DefaultNavigatorScreenTransition(navigator = it)
                }
            },
        )
    }
}
