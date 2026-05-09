package com.inkqilin.ledger.util

import android.content.Context
import android.net.Uri
import com.inkqilin.ledger.data.Category
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat
import java.util.*

object ExcelImporter {
    data class ImportResult(
        val transactions: List<Transaction>,
        val newCategories: List<Category>
    )

    fun importTransactionsFromUri(context: Context, uri: Uri, existingCategories: List<Category>): ImportResult {
        val transactions = mutableListOf<Transaction>()
        val newCategoriesMap = mutableMapOf<Pair<String, TransactionType>, Category>()
        val existingCategorySet = existingCategories.map { it.name to it.type }.toSet()

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook = XSSFWorkbook(inputStream)
                val sheet = workbook.getSheetAt(0)
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

                for (rowIndex in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(rowIndex) ?: continue
                    
                    try {
                        val dateCell = row.getCell(0)
                        val firstCellStr = dateCell?.toString() ?: ""

                        if (firstCellStr.startsWith("说明") || firstCellStr.startsWith("日期")) {
                            continue
                        }

                        val date = when {
                            dateCell == null -> System.currentTimeMillis()
                            dateCell.cellType == org.apache.poi.ss.usermodel.CellType.NUMERIC && org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(dateCell) -> 
                                dateCell.dateCellValue.time
                            else -> try {
                                sdf.parse(firstCellStr)?.time ?: System.currentTimeMillis()
                            } catch (e: Exception) {
                                System.currentTimeMillis()
                            }
                        }
                        
                        val typeStr = try { row.getCell(1).stringCellValue } catch (e: Exception) { "支出" }
                        if (typeStr != "收入" && typeStr != "支出") continue
                        val type = if (typeStr == "收入") TransactionType.INCOME else TransactionType.EXPENSE
                        
                        val categoryName = try { row.getCell(2).stringCellValue } catch (e: Exception) { "其他" }
                        val amount = try { row.getCell(3).numericCellValue } catch (e: Exception) { 0.0 }
                        val note = try { row.getCell(4).stringCellValue } catch (e: Exception) { "" }

                        if (amount <= 0.0 || amount.isNaN()) continue

                        // 检查分类是否存在
                        if (!existingCategorySet.contains(categoryName to type) && 
                            !newCategoriesMap.containsKey(categoryName to type)) {
                            newCategoriesMap[categoryName to type] = Category(
                                name = categoryName,
                                icon = "🆕", // 自动创建的分类使用默认图标
                                type = type
                            )
                        }

                        transactions.add(
                            Transaction(
                                amount = amount,
                                category = categoryName,
                                note = note,
                                date = date,
                                type = type
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                workbook.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ImportResult(transactions, newCategoriesMap.values.toList())
    }
}
