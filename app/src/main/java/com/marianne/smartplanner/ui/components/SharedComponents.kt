package com.marianne.smartplanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marianne.smartplanner.ui.theme.LocalAppColors

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = LocalAppColors.current
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = c.cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun SectionTitle(text: String) {
    val c = LocalAppColors.current
    Text(
        text  = text,
        style = TextStyle(
            fontSize   = 15.sp,
            fontWeight = FontWeight.Bold,
            fontStyle  = FontStyle.Italic,
            color      = c.pink
        ),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun CheckRow(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onToggle: () -> Unit,
    label: String,
    completedAt: String? = null,
    onTimeClick: (() -> Unit)? = null,
    indent: Dp = 0.dp,
    labelSize: androidx.compose.ui.unit.TextUnit = 14.sp
) {
    val c = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = indent, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector        = if (checked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
            contentDescription = null,
            tint               = c.pink,
            modifier           = Modifier.size(22.dp).clickable { onToggle() }
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            fontSize       = labelSize,
            color          = if (checked) c.textSub else c.textMain,
            textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
            modifier       = Modifier.weight(1f)
        )
        if (checked && completedAt != null && onTimeClick != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                completedAt,
                fontSize   = 11.sp,
                color      = c.pink,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier
                    .background(c.pinkContainer, RoundedCornerShape(4.dp))
                    .clickable { onTimeClick() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
