package com.inkqilin.ledger.ui.screens

import androidx.navigation.NavController

fun androidx.navigation.NavController.navigateSingle(route: String) {
    navigate(route) { launchSingleTop = true }
}
