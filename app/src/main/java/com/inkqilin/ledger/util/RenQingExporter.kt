package com.inkqilin.ledger.util

import android.content.Context
import android.net.Uri
import com.inkqilin.ledger.data.RenQingContact
import com.inkqilin.ledger.data.RenQingEvent
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat
import java.util.*

object RenQingExporter {
    fun exportEventsToUri(context: Context, uri: Uri, events: List<RenQingEvent>): Boolean {
        return try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("人情账单")
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            val headerRow = sheet.createRow(0)
            val headers = listOf("日期", "方向", "事件类型", "标签", "联系人", "金额", "礼物描述", "地点", "备注")
            headers.forEachIndexed { i, h -> headerRow.createCell(i).setCellValue(h) }

            events.forEachIndexed { index, event ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(sdf.format(Date(event.date)))
                row.createCell(1).setCellValue(event.direction.label)
                row.createCell(2).setCellValue(event.eventType.label)
                row.createCell(3).setCellValue(event.tagName)
                row.createCell(4).setCellValue(event.contactName)
                row.createCell(5).setCellValue(event.amount)
                row.createCell(6).setCellValue(event.giftDescription)
                row.createCell(7).setCellValue(event.location)
                row.createCell(8).setCellValue(event.note)
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                workbook.write(outputStream)
            }
            workbook.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun exportContactsToUri(context: Context, uri: Uri, contacts: List<RenQingContact>): Boolean {
        return try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("联系人")
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            val headerRow = sheet.createRow(0)
            val headers = listOf("姓名", "关系", "联系方式", "生日", "备注")
            headers.forEachIndexed { i, h -> headerRow.createCell(i).setCellValue(h) }

            contacts.forEachIndexed { index, contact ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(contact.name)
                row.createCell(1).setCellValue(contact.relationship.label)
                row.createCell(2).setCellValue(contact.phone)
                row.createCell(3).setCellValue(contact.birthday?.let { sdf.format(Date(it)) } ?: "")
                row.createCell(4).setCellValue(contact.note)
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                workbook.write(outputStream)
            }
            workbook.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
