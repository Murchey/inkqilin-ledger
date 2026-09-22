package com.inkqilin.ledger.util

import android.content.Context
import android.net.Uri
import com.inkqilin.ledger.data.Category
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionType
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat
import java.util.*

object ExcelImporter {
    data class ImportResult(
        val transactions: List<Transaction>,
        val newCategories: List<Category>
    )

    /**
     * 与 ExcelExporter 对齐的列：
     * 0 日期 | 1 类型 | 2 分类 | 3 金额 | 4 币种 | 5 备注 | 6 UUID
     * 另可选读取「自定义分类」Sheet：名称 | 类型 | 图标 | 颜色 | 排序
     */
    fun importTransactionsFromUri(
        context: Context,
        uri: Uri,
        existingCategories: List<Category>,
        onProgress: ((fraction: Float, message: String) -> Unit)? = null
    ): ImportResult {
        val transactions = mutableListOf<Transaction>()
        val newCategories = mutableListOf<Category>()
        val existingCategorySet = existingCategories.map { it.name to it.type }.toSet()
        val pendingCategoryKeys = mutableSetOf<Pair<String, TransactionType>>()

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                onProgress?.invoke(0.05f, "读取文件…")
                val workbook = XSSFWorkbook(inputStream)
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

                // 1) 自定义分类表（可选）
                val catSheet = findSheet(workbook, "自定义分类", "分类")
                if (catSheet != null) {
                    for (rowIndex in 1..catSheet.lastRowNum) {
                        val row = catSheet.getRow(rowIndex) ?: continue
                        try {
                            val name = row.getCell(0)?.toString()?.trim().orEmpty()
                            if (name.isBlank() || name.startsWith("说明") || name.startsWith("名称")) continue
                            val typeStr = row.getCell(1)?.toString()?.trim().orEmpty()
                            val type = when {
                                typeStr.contains("收入") -> TransactionType.INCOME
                                typeStr.contains("支出") -> TransactionType.EXPENSE
                                else -> TransactionType.EXPENSE
                            }
                            val key = name to type
                            if (existingCategorySet.contains(key) || pendingCategoryKeys.contains(key)) continue
                            val icon = row.getCell(2)?.toString()?.trim().orEmpty().ifBlank { "🆕" }
                            val color = row.getCell(3)?.toString()?.trim().orEmpty().ifBlank { "#715CFF" }
                            val sortOrder = try {
                                row.getCell(4)?.numericCellValue?.toInt() ?: 0
                            } catch (_: Exception) {
                                0
                            }
                            newCategories += Category(
                                name = name,
                                icon = icon,
                                type = type,
                                color = color,
                                sortOrder = sortOrder
                            )
                            pendingCategoryKeys += key
                        } catch (_: Exception) {
                        }
                    }
                }

                // 2) 账单表
                val sheet = workbook.getSheetAt(0)
                val lastRow = sheet.lastRowNum
                val dataRows = (lastRow - 1).coerceAtLeast(1)
                onProgress?.invoke(0.1f, "解析账单 0 / $dataRows")
                for (rowIndex in 1..lastRow) {
                    val row = sheet.getRow(rowIndex) ?: continue
                    try {
                        val dateCell = row.getCell(0)
                        val firstCellStr = dateCell?.toString() ?: ""
                        if (firstCellStr.startsWith("说明") || firstCellStr.startsWith("日期")) continue

                        val date = when {
                            dateCell == null -> System.currentTimeMillis()
                            dateCell.cellType == CellType.NUMERIC && DateUtil.isCellDateFormatted(dateCell) ->
                                dateCell.dateCellValue.time
                            else -> try {
                                sdf.parse(firstCellStr)?.time ?: System.currentTimeMillis()
                            } catch (_: Exception) {
                                System.currentTimeMillis()
                            }
                        }

                        val typeStr = try { row.getCell(1).stringCellValue.trim() } catch (_: Exception) { "支出" }
                        if (typeStr != "收入" && typeStr != "支出") continue
                        val type = if (typeStr == "收入") TransactionType.INCOME else TransactionType.EXPENSE

                        val categoryName = try { row.getCell(2).stringCellValue.trim() } catch (_: Exception) { "其他" }
                            .ifBlank { "其他" }
                        val amount = try { row.getCell(3).numericCellValue } catch (_: Exception) { 0.0 }
                        val currency = try {
                            row.getCell(4)?.toString()?.trim().orEmpty().ifBlank { "CNY" }
                        } catch (_: Exception) {
                            "CNY"
                        }
                        // 备注在第 5 列（旧版导入曾误读第 4 列币种）
                        val note = try { row.getCell(5)?.toString()?.trim().orEmpty() } catch (_: Exception) { "" }
                        val uuid = try { row.getCell(6)?.toString()?.trim().orEmpty() } catch (_: Exception) { "" }

                        if (amount <= 0.0 || amount.isNaN()) continue

                        val key = categoryName to type
                        if (!existingCategorySet.contains(key) && !pendingCategoryKeys.contains(key)) {
                            newCategories += Category(
                                name = categoryName,
                                icon = "🆕",
                                type = type
                            )
                            pendingCategoryKeys += key
                        }

                        transactions += Transaction(
                            amount = amount,
                            category = categoryName,
                            note = note,
                            date = date,
                            type = type,
                            currency = currency,
                            uuid = uuid.ifBlank { null }
                        )
                    } catch (_: Exception) {
                    }
                    val scanned = rowIndex
                    if (scanned == lastRow || scanned % 200 == 0) {
                        val fraction = 0.1f + 0.8f * (scanned.toFloat() / dataRows)
                        onProgress?.invoke(fraction.coerceIn(0f, 0.9f), "解析账单 $scanned / $lastRow")
                    }
                }
                workbook.close()
                onProgress?.invoke(0.95f, "解析完成，共 ${transactions.size} 条")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ImportResult(transactions, newCategories)
    }

    private fun findSheet(workbook: XSSFWorkbook, vararg names: String): org.apache.poi.ss.usermodel.Sheet? {
        for (i in 0 until workbook.numberOfSheets) {
            val sheetName = workbook.getSheetName(i)
            if (sheetName.contains("账单")) continue
            if (names.any { sheetName.contains(it, ignoreCase = true) }) {
                return workbook.getSheetAt(i)
            }
        }
        return null
    }
}
