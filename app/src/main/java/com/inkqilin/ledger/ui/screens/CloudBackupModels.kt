package com.inkqilin.ledger.ui.screens

import com.inkqilin.ledger.util.CosObjectMeta
import com.inkqilin.ledger.util.WebDavConfig
import java.io.File

internal sealed class BackupUiState {
    data object Idle : BackupUiState()
    data object Working : BackupUiState()
    data class Error(val message: String) : BackupUiState()
    data class Success(val message: String) : BackupUiState()
}

internal sealed class RestoreConfirm {
    data class Cloud(val item: CosObjectMeta) : RestoreConfirm()
    data class WebDav(val name: String, val config: WebDavConfig) : RestoreConfirm()
    data class Local(val file: File) : RestoreConfirm()
}

internal data class WebDavDeleteTarget(
    val path: String,
    val config: WebDavConfig
)

internal sealed class PendingBackup {
    data object Local : PendingBackup()
    data object Cloud : PendingBackup()
    data object WebDav : PendingBackup()
}
