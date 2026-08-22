"""
split_calculator.py — 将 CalculatorScreen.kt 拆分为多个模块文件。
运行方式: python scripts/split_calculator.py
"""
import os, re

SRC = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'java',
                   'com', 'inkqilin', 'ledger', 'ui', 'screens', 'CalculatorScreen.kt')
OUT_DIR = os.path.dirname(SRC)

# 读取原始文件（跳过 BOM）
with open(SRC, 'r', encoding='utf-8-sig') as f:
    all_lines = f.readlines()
print(f"读取 {len(all_lines)} 行")

# 找到每个顶级函数的边界（基于花括号计数）
def find_func(name, lines):
    for i, line in enumerate(lines):
        stripped = line.strip()
        # Match both parameterless and parameterized private fun declarations
        if stripped.startswith(f'private fun {name}(') or stripped.startswith(f'fun {name}('):
            si = i - 1 if i > 0 and '@Composable' in lines[i-1] else i
            depth = 0
            started = False
            for j in range(si, len(lines)):
                for ch in lines[j]:
                    if ch == '{':
                        depth += 1
                        started = True
                    elif ch == '}': depth -= 1
                if started and depth <= 0:
                    return si, j
    return None, None

SCREEN_FUNCS = ['CompoundInterestScreen', 'PersonalIncomeTaxScreen',
                'SavingsGoalScreen', 'InstallmentScreen', 'DcaScreen', 'MathDocsScreen']
HELPER_MAP = {
    'InstallmentScreen': ['solveAPR', 'aprFunc'],
    'DcaScreen': ['dcaCalculate', 'dcaSmartFormat', 'dcaCalcRequiredPMT', 'DcaStackedChart'],
}

func_ranges = {}
for name in SCREEN_FUNCS:
    si, ei = find_func(name, all_lines)
    if si is not None:
        func_ranges[name] = (si, ei)
        print(f"  {name}: 行 {si+1}-{ei+1} ({ei-si+1} 行)")

COMMON_IMPORTS = """package com.inkqilin.ledger.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.math.roundToInt
"""

# ──── 创建 CalculatorScreen.kt（入口）────
entry = COMMON_IMPORTS

# 包含类型定义 + calcItems（行 43-66）
types_block = ''.join(all_lines[42:66])
# Make types non-private so they are visible across files
types_block = types_block.replace('private enum class', 'enum class')
types_block = types_block.replace('private data class', 'data class')
types_block = types_block.replace('private val calcItems', 'internal val calcItems')
entry += types_block + '\n'

# 提取 CalculatorScreen 公共入口函数
cs_start, cs_end = None, None
for i, line in enumerate(all_lines):
    if '@Composable' in line and i+1 < len(all_lines) and 'fun CalculatorScreen(' in all_lines[i+1]:
        cs_start = i
        break
if cs_start is not None:
    depth = 0
    for j in range(cs_start, len(all_lines)):
        for ch in all_lines[j]:
            if ch == '{': depth += 1
            elif ch == '}': depth -= 1
        if depth <= 0 and j > cs_start + 3:
            cs_end = j
            break
    if cs_end:
        entry += ''.join(all_lines[cs_start:cs_end+1]) + '\n'
        print(f"  CalculatorScreen: 行 {cs_start+1}-{cs_end+1}")

# 包含共享组件
for shared_name in ['CalculatorHubScreen', 'OutputRow', 'ResultRow', 'SelectionContainer']:
    si, ei = find_func(shared_name, all_lines)
    if si is not None:
        shared_body = ''.join(all_lines[si:ei+1])
        shared_body = shared_body.replace('private fun ', 'fun ')
        entry += '\n' + shared_body + '\n'
        print(f"  + {shared_name}: 行 {si+1}-{ei+1}")

with open(os.path.join(OUT_DIR, 'CalculatorScreen.kt'), 'w', encoding='utf-8') as f:
    f.write(entry)
print("✅ 创建 CalculatorScreen.kt")

# ──── 创建各个屏幕模块文件 ────
for screen_name, (si, ei) in func_ranges.items():
    content = COMMON_IMPORTS + '\n'
    body = ''.join(all_lines[si:ei+1])
    # Make screen functions non-private so CalculatorScreen.kt can call them
    body = body.replace('private fun ', 'fun ')
    content += body + '\n'

    # Include associated helper functions
    if screen_name in HELPER_MAP:
        for helper_name in HELPER_MAP[screen_name]:
            hsi, hei = find_func(helper_name, all_lines)
            if hsi is not None:
                helper_body = ''.join(all_lines[hsi:hei+1])
                helper_body = helper_body.replace('private fun ', 'fun ')
                content += '\n' + helper_body + '\n'
                print(f"  + {helper_name}: 行 {hsi+1}-{hei+1}")

    with open(os.path.join(OUT_DIR, f'{screen_name}.kt'), 'w', encoding='utf-8') as f:
        f.write(content)
    print(f"✅ 创建 {screen_name}.kt ({ei-si+1} 行)")

print("\n🎉 拆分完成！")
