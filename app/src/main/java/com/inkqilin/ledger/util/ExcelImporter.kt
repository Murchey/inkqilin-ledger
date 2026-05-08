package com.inkqilin.ledger.util

import android.content.Context
import android.net.Uri
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat
import java.util.*

object ExcelImporter {
    fun importTransactionsFromUri(context: Context, uri: Uri): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook = XSSFWorkbook(inputStream)
                val sheet = workbook.getSheetAt(0)
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

                for (rowIndex in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(rowIndex) ?: continue
                    
                    try {
                        val dateStr = row.getCell(0).stringCellValue
                        val date = sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
                        
                        val typeStr = row.getCell(1).stringCellValue
                        val type = if (typeStr == "收入") TransactionType.INCOME else TransactionType.EXPENSE
                        
                        val category = row.getCell(2).stringCellValue
                        val amount = row.getCell(3).numericCellValue
                        val note = try { row.getCell(4).stringCellValue } catch (e: Exception) { "" }

                        transactions.add(
                            Transaction(
                                amount = amount,
                                category = category,
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
        return transactions
    }
}
