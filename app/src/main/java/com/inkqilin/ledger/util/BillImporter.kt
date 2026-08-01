package com.inkqilin.ledger.util

import android.content.Context
import android.net.Uri
import com.inkqilin.ledger.data.TransactionType
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

object BillImporter {

    data class ParsedBill(
        val date: Date,
        val amount: Double,
        val category: String,
        val note: String,
        val type: TransactionType
    )

    /** 支付宝 CSV 交易分类 → 本 App 分类映射 */
    private val alipayCategoryMap = mapOf(
        "餐饮美食" to "餐饮", "日用百货" to "购物", "生活服务" to "其他",
        "文化休闲" to "娱乐", "交通出行" to "交通", "教育培训" to "教育",
        "服饰美容" to "购物", "数码家电" to "购物", "运动户外" to "娱乐",
        "医疗健康" to "医疗", "家居家装" to "居住", "通讯物流" to "其他",
        "住房物业" to "居住", "汽车服务" to "交通", "政务服务" to "其他",
        "理财保险" to "理财", "商业服务" to "其他", "公益" to "其他",
        "退款" to "其他", "其他" to "其他"
    )

    /** 微信交易类型 → 本 App 分类映射 */
    private val wechatCategoryMap = mapOf(
        "商户消费" to "其他", "转账" to "人情",
        "红包" to "人情", "退款" to "其他",
        "信用卡还款" to "其他", "充值" to "其他", "提现" to "其他"
    )

    private val alipayDateFmt = SimpleDateFormat("yyyy/M/d HH:mm", Locale.getDefault())
    private val alipayDateFmt2 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val wechatDateFmts = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy-M-d HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy/M/d HH:mm:ss", Locale.getDefault())
    )
    private val appDateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun parseAlipayCsv(context: Context, uri: Uri): List<ParsedBill> {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return emptyList()

        // 支付宝账单通常为 GBK 编码，但也可能是 UTF-8（示例文件等）
        val results = doParseAlipayContent(bytes, Charset.forName("GBK"))
        if (results.isNotEmpty()) return results

        // GBK 无结果时回退 UTF-8
        return doParseAlipayContent(bytes, Charsets.UTF_8)
    }

    private fun doParseAlipayContent(bytes: ByteArray, charset: Charset): List<ParsedBill> {
        val results = mutableListOf<ParsedBill>()
        val content = String(bytes, charset)

        var headerFound = false
        for (line in content.lines()) {
            // 去掉 UTF-8 BOM 头
            val cleanLine = line.trimStart('\uFEFF')
            if (!headerFound) {
                if (cleanLine.startsWith("交易时间,") || cleanLine.contains("交易时间,")) {
                    headerFound = true
                }
                continue
            }
            if (cleanLine.isBlank() ||
                cleanLine.startsWith("-") ||
                cleanLine.startsWith("特别提示") ||
                cleanLine.startsWith("导出信息") ||
                cleanLine.startsWith("共") ||
                cleanLine.startsWith("支付宝")
            ) continue

            val fields = splitCsvLine(cleanLine)
            if (fields.size < 7) continue

            val dateStr = fields.getOrElse(0) { "" }.trim()
            val alipayCat = fields.getOrElse(1) { "" }.trim()
            val productDesc = fields.getOrElse(4) { "" }.trim()
            val typeStr = fields.getOrElse(5) { "" }.trim()
            val amountStr = fields.getOrElse(6) { "" }.trim()
                .replace("¥", "").replace("￥", "").replace(",", "").trim()

            val type = when (typeStr) {
                "支出" -> TransactionType.EXPENSE
                "收入" -> TransactionType.INCOME
                else -> continue
            }

            val amount = amountStr.toDoubleOrNull() ?: continue
            if (amount <= 0) continue

            val date = parseDate(dateStr, listOf(alipayDateFmt, alipayDateFmt2))
            val category = alipayCategoryMap[alipayCat] ?: "其他"
            val note = productDesc.ifBlank { alipayCat }

            results.add(ParsedBill(date, amount, category, note, type))
        }
        return results
    }

    fun parseWechatXlsx(context: Context, uri: Uri): List<ParsedBill> {
        val results = mutableListOf<ParsedBill>()
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val workbook = try {
                WorkbookFactory.create(stream)
            } catch (_: Exception) {
                return results
            }
            val sheet = workbook.getSheetAt(0) ?: return results

            for (rowIndex in 0 until sheet.physicalNumberOfRows) {
                val row = sheet.getRow(rowIndex) ?: continue
                val firstCell = row.getCell(0)?.toString()?.trim() ?: ""

                // Skip header and metadata rows
                if (firstCell.isBlank() || firstCell == "交易时间" ||
                    firstCell.startsWith("微信支付") || firstCell.startsWith("账单") ||
                    firstCell.startsWith("导出") || firstCell.startsWith("共") ||
                    firstCell.startsWith("---")
                ) continue

                // 微信账单格式: 交易时间 | 交易类型 | 交易对方 | 商品 | 收/支 | 金额(元) | 支付方式 | 当前状态 | 交易单号 | 商户单号 | 备注
                val date = parseWechatDateCell(row.getCell(0))
                if (date == null) continue
                val productDesc = row.getCell(3)?.toString()?.trim() ?: ""
                val typeStr = row.getCell(4)?.toString()?.trim() ?: ""
                val amountStr = row.getCell(5)?.toString()?.trim()?.replace("¥", "")?.replace("￥", "")?.replace(",", "") ?: ""

                val type = when (typeStr) {
                    "支出" -> TransactionType.EXPENSE
                    "收入" -> TransactionType.INCOME
                    else -> continue
                }

                val amount = amountStr.toDoubleOrNull() ?: continue
                if (amount <= 0) continue

                val wechatType = row.getCell(1)?.toString()?.trim() ?: ""
                val category = wechatCategoryMap[wechatType] ?: "其他"
                val note = productDesc.ifBlank { "$wechatType - ${row.getCell(2)?.toString()?.trim() ?: ""}" }

                results.add(ParsedBill(date, amount, category, note, type))
            }
            workbook.close()
        }
        return results
    }

    fun parseAppXlsx(context: Context, uri: Uri): List<ParsedBill> {
        val results = mutableListOf<ParsedBill>()
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val workbook = try {
                WorkbookFactory.create(stream)
            } catch (_: Exception) {
                return results
            }
            val sheet = workbook.getSheetAt(0) ?: return results

            for (rowIndex in 1 until sheet.physicalNumberOfRows) { // skip header
                val row = sheet.getRow(rowIndex) ?: continue
                val firstCell = row.getCell(0)?.toString()?.trim() ?: ""

                // Skip metadata rows
                if (firstCell.isBlank() || firstCell == "说明" || firstCell == "日期" ||
                    firstCell.startsWith("导出") || firstCell.startsWith("共")
                ) continue

                // 导出格式: 日期 | 类型 | 分类 | 金额 | 币种 | 备注
                val dateStr = firstCell
                val typeStr = row.getCell(1)?.toString()?.trim() ?: ""
                val category = row.getCell(2)?.toString()?.trim() ?: "其他"
                val amountStr = row.getCell(3)?.toString()?.trim()
                    ?.replace("¥", "")?.replace("￥", "")?.replace(",", "") ?: ""
                // col 4 = 币种 (暂不使用)
                val note = row.getCell(5)?.toString()?.trim() ?: ""

                val type = when (typeStr) {
                    "收入" -> TransactionType.INCOME
                    "支出" -> TransactionType.EXPENSE
                    else -> continue
                }

                val amount = amountStr.toDoubleOrNull() ?: continue
                if (amount <= 0) continue

                val date = parseDate(dateStr, listOf(appDateFmt))

                results.add(ParsedBill(date, amount, category.ifBlank { "其他" }, note, type))
            }
            workbook.close()
        }
        return results
    }

    private fun parseDate(dateStr: String, formats: List<SimpleDateFormat>): Date {
        for (fmt in formats) {
            try { return fmt.parse(dateStr.trim()) ?: Date() } catch (_: Exception) {}
        }
        return Date()
    }

    /** 解析微信账单的日期单元格：优先用 Excel 日期值，回退到字符串解析 */
    private fun parseWechatDateCell(cell: Cell?): Date? {
        if (cell == null) return null
        return try {
            // Excel 日期序列号
            if (DateUtil.isCellDateFormatted(cell)) {
                cell.dateCellValue
            } else {
                // 回退：字符串格式
                val str = cell.toString().trim()
                if (str.isBlank()) null
                else parseDate(str, wechatDateFmts)
            }
        } catch (_: Exception) {
            null
        }
    }

    /** 简单的 CSV 行解析，处理引号包裹的字段 */
    private fun splitCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    fields.add(current.toString().trim().removeSurrounding("\""))
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        fields.add(current.toString().trim().removeSurrounding("\""))
        return fields
    }
}
