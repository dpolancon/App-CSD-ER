package com.example.ui

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.database.ClubDatabase
import com.example.data.repository.ClubRepository
import com.example.ui.auth.LoginScreen
import com.example.ui.auth.RegisterScreen
import com.example.ui.dues.MemberDashboardScreen
import com.example.ui.admin.AdminDashboardScreen
import com.example.ui.viewmodel.ClubViewModel
import com.example.ui.viewmodel.ClubViewModelFactory

@Composable
fun MainApp() {
    val context = LocalContext.current
    val application = context.applicationContext as Application

    // Instantiating Room SQLite database and repository as per standards
    val database = ClubDatabase.getDatabase(context)
    val repository = ClubRepository(database.clubDao())
    val viewModelFactory = ClubViewModelFactory(application, repository)

    // Obtaining clean ViewModel with our custom constructor-injection factory
    val viewModel: ClubViewModel = viewModel(factory = viewModelFactory)
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("login") {
            LoginScreen(
                viewModel = viewModel,
                onNavigateToRegister = {
                    navController.navigate("register")
                },
                onLoginSuccess = { isAdmin ->
                    if (isAdmin) {
                        navController.navigate("admin_dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        navController.navigate("member_dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("register") {
            RegisterScreen(
                viewModel = viewModel,
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onRegisterSuccess = { isAdmin ->
                    if (isAdmin) {
                        navController.navigate("admin_dashboard") {
                            popUpTo("register") { inclusive = true }
                        }
                    } else {
                        navController.navigate("member_dashboard") {
                            popUpTo("register") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("member_dashboard") {
            MemberDashboardScreen(
                viewModel = viewModel,
                onLogout = {
                    viewModel.logout()
                    navController.navigate("login") {
                        popUpTo("member_dashboard") { inclusive = true }
                    }
                }
            )
        }

        composable("admin_dashboard") {
            AdminDashboardScreen(
                viewModel = viewModel,
                onLogout = {
                    viewModel.logout()
                    navController.navigate("login") {
                        popUpTo("admin_dashboard") { inclusive = true }
                    }
                }
            )
        }
    }
}
