package com.inkqilin.ledger.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.inkqilin.ledger.data.AssetFlow
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.UserAsset
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExcelExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * 导出完整数据到 Excel
     * Sheet1: 账单记录
     * Sheet2: 资产总览 + 流转记录
     */
    fun exportToUri(
        context: Context,
        uri: Uri,
        transactions: List<Transaction>,
        assets: List<UserAsset>,
        flows: List<AssetFlow>
    ): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                writeWorkbook(outputStream, transactions, assets, flows)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun writeWorkbook(
        outputStream: OutputStream,
        transactions: List<Transaction>,
        assets: List<UserAsset>,
        flows: List<AssetFlow>
    ) {
        val workbook = XSSFWorkbook()

        // ===== Sheet1: 账单记录 =====
        val sheet1 = workbook.createSheet("账单记录")
        val headerStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER
            val font = workbook.createFont().apply { bold = true }
            setFont(font)
        }
        val centerStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER
        }

        val headers1 = arrayOf("日期", "类型", "分类", "金额", "币种", "备注")
        val headerRow1 = sheet1.createRow(0)
        headers1.forEachIndexed { i, h ->
            val cell = headerRow1.createCell(i)
            cell.setCellValue(h)
            cell.cellStyle = headerStyle
        }

        transactions.sortedByDescending { it.date }.forEachIndexed { index, tx ->
            val row = sheet1.createRow(index + 1)
            row.createCell(0).apply {
                setCellValue(dateFormat.format(Date(tx.date)))
                cellStyle = centerStyle
            }
            row.createCell(1).apply {
                setCellValue(if (tx.type == com.inkqilin.ledger.data.TransactionType.INCOME) "收入" else "支出")
                cellStyle = centerStyle
            }
            row.createCell(2).apply {
                setCellValue(tx.category)
                cellStyle = centerStyle
            }
            row.createCell(3).apply {
                setCellValue(tx.amount)
                cellStyle = centerStyle
            }
            row.createCell(4).apply {
                setCellValue(tx.currency)
                cellStyle = centerStyle
            }
            row.createCell(5).setCellValue(tx.note)
        }

        // 设置列宽
        sheet1.setColumnWidth(0, 18 * 256)
        sheet1.setColumnWidth(1, 10 * 256)
        sheet1.setColumnWidth(2, 12 * 256)
        sheet1.setColumnWidth(3, 12 * 256)
        sheet1.setColumnWidth(4, 8 * 256)
        sheet1.setColumnWidth(5, 30 * 256)

        // ===== Sheet2: 资产与流转 =====
        val sheet2 = workbook.createSheet("资产与流转")

        // -- 资产总览表头 --
        val assetsHeaderRow = sheet2.createRow(0)
        val assetHeaders = arrayOf("资产名称", "类型", "当前估值", "创建日期", "最后更新", "备注")
        assetHeaders.forEachIndexed { i, h ->
            val cell = assetsHeaderRow.createCell(i)
            cell.setCellValue(h)
            cell.cellStyle = headerStyle
        }

        // -- 资产数据 --
        assets.sortedBy { it.type.ordinal }.forEachIndexed { index, asset ->
            val row = sheet2.createRow(index + 1)
            row.createCell(0).setCellValue(asset.name)
            row.createCell(1).apply {
                setCellValue(asset.type.label)
                cellStyle = centerStyle
            }
            row.createCell(2).apply {
                setCellValue(asset.currentValue)
                cellStyle = centerStyle
            }
            row.createCell(3).apply {
                setCellValue(dateOnlyFormat.format(Date(asset.createdAt)))
                cellStyle = centerStyle
            }
            row.createCell(4).apply {
                setCellValue(dateOnlyFormat.format(Date(asset.lastUpdated)))
                cellStyle = centerStyle
            }
            row.createCell(5).setCellValue(asset.note)
        }

        // -- 分割行 --
        val dividerRowNum = assets.size + 2
        val dividerRow = sheet2.createRow(dividerRowNum)
        val dividerCell = dividerRow.createCell(0)
        dividerCell.setCellValue("───── 资产流转记录 ─────")

        // -- 流转记录表头 --
        val flowHeaderRowNum = dividerRowNum + 1
        val flowHeaderRow = sheet2.createRow(flowHeaderRowNum)
        val flowHeaders = arrayOf("资产名称", "变动类型", "变动金额", "变动后价值", "日期", "备注")
        flowHeaders.forEachIndexed { i, h ->
            val cell = flowHeaderRow.createCell(i)
            cell.setCellValue(h)
            cell.cellStyle = headerStyle
        }

        // -- 流转记录数据 --
        flows.sortedByDescending { it.date }.forEachIndexed { index, flow ->
            val row = sheet2.createRow(flowHeaderRowNum + 1 + index)
            row.createCell(0).setCellValue(flow.assetName)
            row.createCell(1).apply {
                setCellValue(flow.flowType.label)
                cellStyle = centerStyle
            }
            row.createCell(2).apply {
                setCellValue(flow.amount)
                cellStyle = centerStyle
            }
            row.createCell(3).apply {
                setCellValue(flow.newValue)
                cellStyle = centerStyle
            }
            row.createCell(4).apply {
                setCellValue(dateOnlyFormat.format(Date(flow.date)))
                cellStyle = centerStyle
            }
            row.createCell(5).setCellValue(flow.note)
        }

        // 设置 Sheet2 列宽
        sheet2.setColumnWidth(0, 18 * 256)
        sheet2.setColumnWidth(1, 12 * 256)
        sheet2.setColumnWidth(2, 14 * 256)
        sheet2.setColumnWidth(3, 14 * 256)
        sheet2.setColumnWidth(4, 14 * 256)
        sheet2.setColumnWidth(5, 30 * 256)

        workbook.write(outputStream)
        workbook.close()
    }

    /**
     * 导出导入模板（仅账单记录，不含资产）
     */
    fun exportTemplateToUri(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("账单导入模板")
                val headerStyle = workbook.createCellStyle().apply {
                    alignment = HorizontalAlignment.CENTER
                    val font = workbook.createFont().apply { bold = true }
                    setFont(font)
                }
                val centerStyle = workbook.createCellStyle().apply {
                    alignment = HorizontalAlignment.CENTER
                }

                val headers = arrayOf("日期", "类型 (收入/支出)", "分类", "金额", "币种", "备注")
                val headerRow = sheet.createRow(0)
                headers.forEachIndexed { i, h ->
                    val cell = headerRow.createCell(i)
                    cell.setCellValue(h)
                    cell.cellStyle = headerStyle
                }

                val sampleRow = sheet.createRow(1)
                sampleRow.createCell(0).apply {
                    setCellValue(dateOnlyFormat.format(Date()))
                    cellStyle = centerStyle
                }
                sampleRow.createCell(1).apply {
                    setCellValue("支出")
                    cellStyle = centerStyle
                }
                sampleRow.createCell(2).apply {
                    setCellValue("餐饮")
                    cellStyle = centerStyle
                }
                sampleRow.createCell(3).apply {
                    setCellValue(35.5)
                    cellStyle = centerStyle
                }
                sampleRow.createCell(4).apply {
                    setCellValue("CNY")
                    cellStyle = centerStyle
                }
                sampleRow.createCell(5).setCellValue("午餐")

                sheet.setColumnWidth(0, 18 * 256)
                sheet.setColumnWidth(1, 16 * 256)
                sheet.setColumnWidth(2, 12 * 256)
                sheet.setColumnWidth(3, 12 * 256)
                sheet.setColumnWidth(4, 8 * 256)
                sheet.setColumnWidth(5, 30 * 256)

                workbook.write(outputStream)
                workbook.close()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
