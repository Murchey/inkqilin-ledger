package com.inkqilin.ledger.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.inkqilin.ledger.data.AssetFlow
import com.inkqilin.ledger.data.Category
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.UserAsset
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExcelExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /** 进度：0f–1f + 描述 */
    data class Progress(val fraction: Float, val message: String)

    /**
     * 导出完整数据到 Excel
     * Sheet1: 账单记录（含分类、备注、币种、UUID）
     * Sheet2: 资产与流转
     * Sheet3: 自定义分类（名称/类型/图标/颜色/排序）
     *
     * @param onProgress 大数据量时周期性回调进度（约每 200 条一次）
     */
    fun exportToUri(
        context: Context,
        uri: Uri,
        transactions: List<Transaction>,
        assets: List<UserAsset>,
        flows: List<AssetFlow>,
        categories: List<Category> = emptyList(),
        onProgress: ((Progress) -> Unit)? = null
    ): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                writeWorkbook(outputStream, transactions, assets, flows, categories, onProgress)
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
        flows: List<AssetFlow>,
        categories: List<Category>,
        onProgress: ((Progress) -> Unit)? = null
    ) {
        val workbook = XSSFWorkbook()
        val txCount = transactions.size
        val assetCount = assets.size
        val flowCount = flows.size
        val catCount = categories.size
        // 权重：账单写入是主要耗时
        val totalUnits = (txCount + assetCount + flowCount + catCount).coerceAtLeast(1)

        fun report(done: Int, message: String) {
            val fraction = (done.toFloat() / totalUnits).coerceIn(0f, 1f)
            onProgress?.invoke(Progress(fraction, message))
        }

        onProgress?.invoke(Progress(0f, "准备写入…"))

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

        val headers1 = arrayOf("日期", "类型", "分类", "金额", "币种", "备注", "UUID")
        val headerRow1 = sheet1.createRow(0)
        headers1.forEachIndexed { i, h ->
            val cell = headerRow1.createCell(i)
            cell.setCellValue(h)
            cell.cellStyle = headerStyle
        }

        // 说明行（导入时会被自动跳过）
        val noteRow1 = sheet1.createRow(1)
        noteRow1.createCell(0).apply {
            setCellValue("说明：UUID列为空即可，此列用于防止重复导入")
            cellStyle = workbook.createCellStyle().apply {
                val font = workbook.createFont().apply { italic = true; color = IndexedColors.GREY_50_PERCENT.index }
                setFont(font)
            }
        }

        transactions.sortedByDescending { it.date }.forEachIndexed { index, tx ->
            val row = sheet1.createRow(index + 2)
            // 避免 apply/重复 style 赋值带来的额外开销
            row.createCell(0).setCellValue(dateFormat.format(Date(tx.date)))
            row.createCell(1).setCellValue(if (tx.type == com.inkqilin.ledger.data.TransactionType.INCOME) "收入" else "支出")
            row.createCell(2).setCellValue(tx.category)
            row.createCell(3).setCellValue(tx.amount)
            row.createCell(4).setCellValue(tx.currency)
            row.createCell(5).setCellValue(tx.note)
            row.createCell(6).setCellValue(tx.uuid ?: "")

            val done = index + 1
            if (done == txCount || done % 200 == 0) {
                report(done, "写入账单 $done / $txCount")
            }
        }

        // 设置列宽
        sheet1.setColumnWidth(0, 18 * 256)
        sheet1.setColumnWidth(1, 10 * 256)
        sheet1.setColumnWidth(2, 12 * 256)
        sheet1.setColumnWidth(3, 12 * 256)
        sheet1.setColumnWidth(4, 8 * 256)
        sheet1.setColumnWidth(5, 30 * 256)
        sheet1.setColumnWidth(6, 36 * 256)

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
            if (index + 1 == assetCount || (index + 1) % 50 == 0) {
                report(txCount + index + 1, "写入资产 ${index + 1} / $assetCount")
            }
        }

        // -- 分割行 --
        val dividerRowNum = assets.size + 2
        val dividerRow = sheet2.createRow(dividerRowNum)
        val dividerCell = dividerRow.createCell(0)
        dividerCell.setCellValue("───── 资产流转记录 ─────")

        // -- 流转记录表头 --
        val flowHeaderRowNum = dividerRowNum + 1
        val flowHeaderRow = sheet2.createRow(flowHeaderRowNum)
        val flowHeaders = arrayOf("资产名称", "变动类型", "变动金额", "变动后价值", "日期", "备注", "UUID")
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
            row.createCell(6).setCellValue(flow.uuid ?: "")
            val doneTx = txCount
            val doneAsset = assetCount
            val doneFlow = index + 1
            if (doneFlow == flowCount || doneFlow % 100 == 0) {
                report(doneTx + doneAsset + doneFlow, "写入流转 $doneFlow / $flowCount")
            }
        }

        // 设置 Sheet2 列宽
        sheet2.setColumnWidth(0, 18 * 256)
        sheet2.setColumnWidth(1, 12 * 256)
        sheet2.setColumnWidth(2, 14 * 256)
        sheet2.setColumnWidth(3, 14 * 256)
        sheet2.setColumnWidth(4, 14 * 256)
        sheet2.setColumnWidth(5, 30 * 256)

        // ===== Sheet3: 自定义分类 =====
        val sheet3 = workbook.createSheet("自定义分类")
        val catHeaders = arrayOf("名称", "类型", "图标", "颜色", "排序")
        val catHeaderRow = sheet3.createRow(0)
        catHeaders.forEachIndexed { i, h ->
            val cell = catHeaderRow.createCell(i)
            cell.setCellValue(h)
            cell.cellStyle = headerStyle
        }
        val catNoteRow = sheet3.createRow(1)
        catNoteRow.createCell(0).apply {
            setCellValue("说明：导入时会按「名称+类型」去重，已存在的分类不会重复创建")
            cellStyle = workbook.createCellStyle().apply {
                val font = workbook.createFont().apply {
                    italic = true
                    color = IndexedColors.GREY_50_PERCENT.index
                }
                setFont(font)
            }
        }
        categories.sortedWith(compareBy({ it.type.ordinal }, { it.sortOrder }, { it.name }))
            .forEachIndexed { index, cat ->
                val row = sheet3.createRow(index + 2)
                row.createCell(0).setCellValue(cat.name)
                row.createCell(1).apply {
                    setCellValue(if (cat.type == com.inkqilin.ledger.data.TransactionType.INCOME) "收入" else "支出")
                    cellStyle = centerStyle
                }
                row.createCell(2).setCellValue(cat.icon)
                row.createCell(3).setCellValue(cat.color)
                row.createCell(4).apply {
                    setCellValue(cat.sortOrder.toDouble())
                    cellStyle = centerStyle
                }
            }
        sheet3.setColumnWidth(0, 16 * 256)
        sheet3.setColumnWidth(1, 10 * 256)
        sheet3.setColumnWidth(2, 10 * 256)
        sheet3.setColumnWidth(3, 12 * 256)
        sheet3.setColumnWidth(4, 8 * 256)

        onProgress?.invoke(Progress(0.95f, "正在写入文件…"))
        workbook.write(outputStream)
        workbook.close()
        onProgress?.invoke(Progress(1f, "完成"))
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

                val headers = arrayOf("日期", "类型 (收入/支出)", "分类", "金额", "币种", "备注", "UUID")
                val headerRow = sheet.createRow(0)
                headers.forEachIndexed { i, h ->
                    val cell = headerRow.createCell(i)
                    cell.setCellValue(h)
                    cell.cellStyle = headerStyle
                }

                // 说明行（会被导入器自动跳过）
                val noteRow = sheet.createRow(1)
                noteRow.createCell(0).apply {
                    setCellValue("说明：UUID列为空即可，此列用于防止重复导入")
                    cellStyle = sheet.workbook.createCellStyle().apply {
                        val font = workbook.createFont().apply { italic = true; color = IndexedColors.GREY_50_PERCENT.index }
                        setFont(font)
                    }
                }

                val sampleRow = sheet.createRow(2)
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
                sampleRow.createCell(6).setCellValue("")

                sheet.setColumnWidth(0, 18 * 256)
                sheet.setColumnWidth(1, 16 * 256)
                sheet.setColumnWidth(2, 12 * 256)
                sheet.setColumnWidth(3, 12 * 256)
                sheet.setColumnWidth(4, 8 * 256)
                sheet.setColumnWidth(5, 30 * 256)
                sheet.setColumnWidth(6, 36 * 256)

                // ===== Sheet2: 资产与流转模板 =====
                val sheet2 = workbook.createSheet("资产与流转")
                val currentRow = java.util.concurrent.atomic.AtomicInteger(0)

                // -- 资产表头 --
                val assetHeaders = arrayOf("资产名称", "类型", "当前估值", "创建日期", "最后更新", "备注")
                val assetHeaderRow = sheet2.createRow(currentRow.getAndIncrement())
                assetHeaders.forEachIndexed { i, h ->
                    val cell = assetHeaderRow.createCell(i)
                    cell.setCellValue(h)
                    cell.cellStyle = headerStyle
                }

                // 资产说明行
                val assetNoteRow = sheet2.createRow(currentRow.getAndIncrement())
                assetNoteRow.createCell(0).apply {
                    setCellValue("说明：资产名称不可与已有资产重复，重复会跳过导入")
                    cellStyle = workbook.createCellStyle().apply {
                        val font = workbook.createFont().apply { italic = true; color = IndexedColors.GREY_50_PERCENT.index }
                        setFont(font)
                    }
                }

                // 资产示例行
                val assetSampleRow = sheet2.createRow(currentRow.getAndIncrement())
                assetSampleRow.createCell(0).setCellValue("示例房屋")
                assetSampleRow.createCell(1).apply { setCellValue("房屋"); cellStyle = centerStyle }
                assetSampleRow.createCell(2).apply { setCellValue(5000000.0); cellStyle = centerStyle }
                assetSampleRow.createCell(3).apply { setCellValue(dateOnlyFormat.format(Date())); cellStyle = centerStyle }
                assetSampleRow.createCell(4).apply { setCellValue(dateOnlyFormat.format(Date())); cellStyle = centerStyle }
                assetSampleRow.createCell(5).setCellValue("")

                // 空行
                currentRow.incrementAndGet()

                // -- 分隔行 --
                val dividerRow = sheet2.createRow(currentRow.getAndIncrement())
                dividerRow.createCell(0).apply {
                    setCellValue("───────── 资产流转记录 ─────────")
                    cellStyle = workbook.createCellStyle().apply {
                        val font = workbook.createFont().apply { bold = true }
                        setFont(font)
                    }
                }

                // 空行
                currentRow.incrementAndGet()

                // -- 流转表头 --
                val flowHeaders = arrayOf("资产名称", "变动类型", "变动金额", "变动后价值", "日期", "备注", "UUID")
                val flowHeaderRow = sheet2.createRow(currentRow.getAndIncrement())
                flowHeaders.forEachIndexed { i, h ->
                    val cell = flowHeaderRow.createCell(i)
                    cell.setCellValue(h)
                    cell.cellStyle = headerStyle
                }

                // 流转说明行
                val flowNoteRow = sheet2.createRow(currentRow.getAndIncrement())
                flowNoteRow.createCell(0).apply {
                    setCellValue("说明：UUID列为空即可，此列用于防止重复导入。资产名称需与上方资产匹配")
                    cellStyle = workbook.createCellStyle().apply {
                        val font = workbook.createFont().apply { italic = true; color = IndexedColors.GREY_50_PERCENT.index }
                        setFont(font)
                    }
                }

                // 流转示例行1：存入
                val flowSample1 = sheet2.createRow(currentRow.getAndIncrement())
                flowSample1.createCell(0).setCellValue("示例房屋")
                flowSample1.createCell(1).apply { setCellValue("存入/增值"); cellStyle = centerStyle }
                flowSample1.createCell(2).apply { setCellValue(1000000.0); cellStyle = centerStyle }
                flowSample1.createCell(3).apply { setCellValue(6000000.0); cellStyle = centerStyle }
                flowSample1.createCell(4).apply { setCellValue(dateOnlyFormat.format(Date())); cellStyle = centerStyle }
                flowSample1.createCell(5).setCellValue("首付")
                flowSample1.createCell(6).setCellValue("")

                // 流转示例行2：取出
                val flowSample2 = sheet2.createRow(currentRow.getAndIncrement())
                flowSample2.createCell(0).setCellValue("示例房屋")
                flowSample2.createCell(1).apply { setCellValue("取出/减值"); cellStyle = centerStyle }
                flowSample2.createCell(2).apply { setCellValue(50000.0); cellStyle = centerStyle }
                flowSample2.createCell(3).apply { setCellValue(5950000.0); cellStyle = centerStyle }
                flowSample2.createCell(4).apply { setCellValue(dateOnlyFormat.format(Date())); cellStyle = centerStyle }
                flowSample2.createCell(5).setCellValue("维修费")
                flowSample2.createCell(6).setCellValue("")

                // 设置列宽
                sheet2.setColumnWidth(0, 18 * 256)
                sheet2.setColumnWidth(1, 16 * 256)
                sheet2.setColumnWidth(2, 14 * 256)
                sheet2.setColumnWidth(3, 14 * 256)
                sheet2.setColumnWidth(4, 18 * 256)
                sheet2.setColumnWidth(5, 30 * 256)
                sheet2.setColumnWidth(6, 36 * 256)

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
