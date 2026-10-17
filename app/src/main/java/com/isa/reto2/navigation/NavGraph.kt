package com.isa.reto2.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.isa.reto2.data.SessionManager
import com.isa.reto2.ui.screens.AdminAuditScreen
import com.isa.reto2.ui.screens.HardwareScreen
import com.isa.reto2.ui.screens.LoginScreen
import com.isa.reto2.ui.screens.OperationsScreen
import com.isa.reto2.ui.screens.ProfileScreen
import com.isa.reto2.ui.screens.RegisterScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val OPERATIONS = "operations"
    const val PROFILE = "profile"
    const val ADMIN = "admin"
    const val HARDWARE = "hardware"
}

private fun NavHostController.toLoginClearingStack() {
    navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true }; launchSingleTop = true }
}

@Composable
fun NavGraph() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    // Limpia la pila para no volver al login con "atrás"
                    nav.navigate(Routes.OPERATIONS) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                onGoRegister = { nav.navigate(Routes.REGISTER) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistered = {
                    nav.navigate(Routes.OPERATIONS) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.OPERATIONS) {
            SessionGuard(nav) {
                OperationsScreen(
                    onProfile = { nav.navigate(Routes.PROFILE) },
                    onAudit = { nav.navigate(Routes.ADMIN) },
                    onHardware = { nav.navigate(Routes.HARDWARE) },
                    onLogout = { SessionManager.logout(); nav.toLoginClearingStack() }
                )
            }
        }
        composable(Routes.PROFILE) {
            SessionGuard(nav) { ProfileScreen(onBack = { nav.popBackStack() }) }
        }
        composable(Routes.ADMIN) {
            SessionGuard(nav) { AdminAuditScreen(onBack = { nav.popBackStack() }) }
        }
        composable(Routes.HARDWARE) {
            SessionGuard(nav) { HardwareScreen(onBack = { nav.popBackStack() }) }
        }
    }
}

/** Si la sesión se perdió (proceso reiniciado), vuelve al login. */
@Composable
private fun SessionGuard(nav: NavHostController, content: @Composable () -> Unit) {
    if (SessionManager.currentUser == null) {
        LaunchedEffect(Unit) { nav.toLoginClearingStack() }
    } else {
        content()
    }
}
