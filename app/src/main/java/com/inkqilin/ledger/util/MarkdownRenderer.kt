package com.inkqilin.ledger.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

object MarkdownRenderer {

    // 固定的标题字号（避免在 non-Composable 上下文中访问 MaterialTheme）
    private val HEADING1_SIZE = 24.sp
    private val HEADING2_SIZE = 20.sp
    private val HEADING3_SIZE = 16.sp

    @Composable
    fun renderMarkdown(markdown: String, modifier: Modifier = Modifier): AnnotatedString {
        return remember(markdown) { parseMarkdown(markdown) }
    }

    private fun parseMarkdown(text: String): AnnotatedString {
        val builder = AnnotatedString.Builder()
        val lines = text.lines()
        var i = 0
        
        while (i < lines.size) {
            val line = lines[i]
            
            if (line.isBlank()) {
                builder.append("\n")
                i++
                continue
            }

            if (line.trimStart().startsWith("```")) {
                i++
                val codeLines = mutableListOf<String>()
                while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                i++
                builder.append("\n")
                codeLines.forEach { 
                    builder.pushStyle(SpanStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace))
                    builder.append(it)
                    builder.pop()
                    builder.append("\n")
                }
                builder.append("\n")
                continue
            }

            if (line.contains("|") && !line.trim().all { it == '-' || it == '|' || it == ':' || it == ' ' }) {
                val cells = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                cells.forEachIndexed { idx, cell ->
                    builder.append(cell)
                    if (idx < cells.lastIndex) builder.append(" | ")
                }
                builder.append("\n")
                i++
                continue
            }

            if (line.trim().matches(Regex("^[-*_]{3,}$"))) {
                builder.append("\n")
                i++
                continue
            }

            when {
                line.startsWith("# ") -> {
                    builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = HEADING1_SIZE))
                    builder.append(line.substringAfter("# ").trim())
                    builder.pop()
                    builder.append("\n\n")
                }
                line.startsWith("## ") -> {
                    builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = HEADING2_SIZE))
                    builder.append(line.substringAfter("## ").trim())
                    builder.pop()
                    builder.append("\n\n")
                }
                line.startsWith("### ") -> {
                    builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = HEADING3_SIZE))
                    builder.append(line.substringAfter("### ").trim())
                    builder.pop()
                    builder.append("\n\n")
                }
                else -> {
                    builder.append(processInlineFormatting(line.trim()))
                    builder.append("\n")
                }
            }
            i++
        }
        
        return builder.toAnnotatedString()
    }

    private fun processInlineFormatting(text: String): String {
        var result = text
        
        // 粗体 **text** or __text__
        result = Regex("\\*\\*(.+?)\\*\\*").replace(result) { "[B:${it.groupValues[1]}]" }
        result = Regex("__(.+?)__").replace(result) { "[B:${it.groupValues[1]}]" }
        
        // 斜体 *text* or _text_ (排除 **)
        result = Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)").replace(result) { "[I:${it.groupValues[1]}]" }
        result = Regex("(?<!_)_(?!_)(.+?)(?<!_)_(?!_)").replace(result) { "[I:${it.groupValues[1]}]" }
        
        // 删除线 ~~text~~
        result = Regex("~~(.+?)~~").replace(result) { "[S:${it.groupValues[1]}]" }
        
        // 内联代码 `code`
        result = Regex("`(.+?)`").replace(result) { "[C:${it.groupValues[1]}]" }
        
        // 链接 [text](url)
        result = Regex("\\[(.+?)\\]\\([^)]+\\)").replace(result) { "[${it.groupValues[1]}]" }
        
        return result
    }
}