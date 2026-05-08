package com.inkqilin.ledger.util

import android.content.Context
import android.net.Uri
import com.inkqilin.ledger.data.Transaction
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExcelExporter {
    fun exportTransactionsToUri(context: Context, uri: Uri, transactions: List<Transaction>): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                writeTransactionsToStream(outputStream, transactions)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun exportTemplateToUri(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("墨麒麟账单模板")

                // Header
                val headerRow = sheet.createRow(0)
                headerRow.createCell(0).setCellValue("日期 (格式: 2023-01-01 12:00)")
                headerRow.createCell(1).setCellValue("类型 (收入/支出)")
                headerRow.createCell(2).setCellValue("分类")
                headerRow.createCell(3).setCellValue("金额")
                headerRow.createCell(4).setCellValue("备注")

                // Sample Row
                val sampleRow = sheet.createRow(1)
                sampleRow.createCell(0).setCellValue("2023-01-01 12:00")
                sampleRow.createCell(1).setCellValue("支出")
                sampleRow.createCell(2).setCellValue("餐饮")
                sampleRow.createCell(3).setCellValue(25.5)
                sampleRow.createCell(4).setCellValue("午餐")

                workbook.write(outputStream)
                workbook.close()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun writeTransactionsToStream(outputStream: OutputStream, transactions: List<Transaction>) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("墨麒麟账单")

        // Header
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("日期")
        headerRow.createCell(1).setCellValue("类型")
        headerRow.createCell(2).setCellValue("分类")
        headerRow.createCell(3).setCellValue("金额")
        headerRow.createCell(4).setCellValue("备注")

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        // Data
        transactions.forEachIndexed { index, transaction ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(sdf.format(Date(transaction.date)))
            row.createCell(1).setCellValue(if (transaction.type.name == "INCOME") "收入" else "支出")
            row.createCell(2).setCellValue(transaction.category)
            row.createCell(3).setCellValue(transaction.amount)
            row.createCell(4).setCellValue(transaction.note)
        }

        workbook.write(outputStream)
        workbook.close()
    }
}
