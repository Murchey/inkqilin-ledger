package com.inkqilin.ledger.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 隐私政策正文：对齐 README / 开源理念（本地优先、可选联网、无埋点） */
object PrivacyPolicy {
    const val TITLE = "隐私政策"

    val SECTIONS: List<Pair<String, String>> = listOf(
        "一、我们是谁" to
            "墨麒麟记账（InkQilin Ledger）是一款开源的 Android 个人记账应用，采用 GNU GPL v3.0 许可证。" +
            "项目源码公开可审计，不包含广告 SDK、统计埋点或账号体系。",

        "二、数据存储在哪里" to
            "你的账单、人情往来、资产、分类、设置与相册照片均保存在本机（应用私有目录 / 本地数据库 / 偏好存储）。" +
            "在「基础版」模式下，数据完全不离开手机。",

        "三、我们收集什么" to
            "本应用不收集、不上传、不分析你的个人身份信息或账单内容到开发者服务器。" +
            "没有使用任何第三方统计或广告组件。",

        "四、权限与可选联网（均由你主动开启）" to
            "• 相机 / 存储：用于拍照记账、相册选图、导出 Excel；拍照通过系统相机完成。\n" +
            "• 通知监听（自动记账）：仅在你开启后，用于解析支付宝/微信支付/云闪付通知中的金额与分类，结果写入本地账单；可随时在系统设置中撤销。\n" +
            "• 网络：仅在你主动使用时发生——①检查应用更新（Gitee/GitHub）；②智能版 AI 分析 / OCR 识别（使用你自己配置的 API 地址与密钥）；③云备份（使用你自己配置的腾讯云 COS 密钥与存储桶）。\n" +
            "以上任一功能关闭或未配置时，对应网络请求不会发出。",

        "五、AI 与智能版" to
            "智能版的财务评分、消费提醒、OCR 批量识别等，仅在你开启智能模式并自行配置 API 后，" +
            "由你指定的接口处理相关数据。开发者无法访问你的 API 密钥与请求内容。",

        "六、云备份" to
            "若你启用云备份，加密与否由你选择（支持 AES-256-GCM 密码加密）。备份文件上传到你自己的 COS 存储桶，" +
            "不经过开发者服务器。删除备份时会覆写/校验，尽量避免残留。",

        "七、你的控制权" to
            "可随时导出、导入、恢复或删除本地备份；账单支持硬删除（SQLite secure_delete）。" +
            "卸载应用即可清除应用内数据。无需注册，无开发者侧副本。",

        "八、儿童隐私" to
            "本应用不面向儿童收集信息。若你认为儿童向我们提供了个人信息，请通过开源仓库 Issue 联系，我们将协助删除。",

        "九、政策变更与开源承诺" to
            "若隐私政策有实质变更，将在应用内提示。完整历史可在 Gitee / GitHub 仓库查阅。" +
            "欢迎审查源码以验证上述承诺。",

        "十、联系方式" to
            "问题或建议请通过开源仓库反馈：\n" +
            "Gitee：gitee.com/Murchey/inkqinlin-ledger\n" +
            "GitHub：github.com/Murchey/inkqilin-ledger"
    )
}

@Composable
fun PrivacyPolicyContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "最后更新：2026 年 9 月 · 开源项目 GPL-3.0",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        PrivacyPolicy.SECTIONS.forEach { (title, body) ->
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
        }
    }
}

/** 隐私政策对话框；[requireAccept]=true 时为首启确认（仅「同意并继续」可关闭） */
@Composable
fun PrivacyPolicyDialog(
    requireAccept: Boolean,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    val buttons = if (requireAccept) {
        listOf(
            AppleDialogButton("同意并继续", AppleDialogButtonStyle.DEFAULT) { onAccept() }
        )
    } else {
        listOf(
            AppleDialogButton("我已阅读", AppleDialogButtonStyle.DEFAULT) { onDismiss() }
        )
    }
    AppleAlertDialog(
        onDismissRequest = {
            // 首启时点外部不视为同意
            if (!requireAccept) onDismiss()
        },
        title = PrivacyPolicy.TITLE,
        content = {
            PrivacyPolicyContent(modifier = Modifier.height(360.dp))
        },
        buttons = buttons
    )
}
