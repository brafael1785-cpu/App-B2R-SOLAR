package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ProposalTemplateInfo
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.MintContainer
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.SolarGold
import com.example.ui.theme.VibrantGreen

@Composable
fun ProposalTemplateCard(
    template: ProposalTemplateInfo,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 2.2.dp else 1.dp,
            color = if (selected) VibrantGreen else SoftBorder
        ),
        shadowElevation = if (selected) 4.dp else 0.dp,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect)
            .testTag("template_card_${template.number}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF3F8F5))
            ) {
                when (template.number) {
                    1 -> MiniClassicThumbnail()
                    2 -> MiniEditorialThumbnail()
                    3 -> MiniExecutiveThumbnail()
                    4 -> MiniMosaicThumbnail()
                    else -> MiniCompleteCoverThumbnail()
                }
                if (selected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(VibrantGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Modelo selecionado",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = template.title,
                style = MaterialTheme.typography.titleSmall,
                color = ForestGreen,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = template.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun MiniClassicThumbnail() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(ForestGreen)
                .padding(5.dp)
        ) {
            Column {
                Text("Energia Solar", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text("Proposta Comercial", color = Color(0xFFA7F3D0), fontSize = 6.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.weight(1f).height(20.dp).clip(RoundedCornerShape(4.dp)).background(VibrantGreen))
            Box(Modifier.weight(1f).height(20.dp).clip(RoundedCornerShape(4.dp)).background(MintContainer))
            Box(Modifier.weight(1f).height(20.dp).clip(RoundedCornerShape(4.dp)).background(MintContainer))
        }
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            listOf(0.5f, 0.7f, 0.6f, 0.85f, 0.75f, 0.9f, 0.95f, 0.8f).forEachIndexed { idx, h ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(h)
                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                        .background(if (idx == 6) SolarGold else EmeraldPrimary)
                )
            }
        }
    }
}

@Composable
private fun MiniEditorialThumbnail() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FBF9))
            .padding(7.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text("Cliente Destaque", color = ForestGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(ForestGreen)
                .padding(5.dp)
        ) {
            Text("R$ 1.565 / mês", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            listOf(0.6f, 0.65f, 0.7f, 0.55f, 0.8f, 0.9f, 0.75f, 0.85f).forEachIndexed { idx, h ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(h)
                        .background(if (idx == 5) SolarGold else ForestGreen)
                )
            }
        }
    }
}

@Composable
private fun MiniExecutiveThumbnail() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .width(42.dp)
                .fillMaxHeight()
                .background(ForestGreen)
                .padding(5.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("B2R", color = Color(0xFF6EE7B7), fontSize = 7.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.fillMaxWidth().height(8.dp).background(Color.White.copy(alpha = 0.2f)))
            Box(Modifier.fillMaxWidth().height(8.dp).background(Color.White.copy(alpha = 0.2f)))
            Box(Modifier.fillMaxWidth().height(8.dp).background(Color.White.copy(alpha = 0.2f)))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Resumo Executivo", color = ForestGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MintContainer)
            )
        }
    }
}

@Composable
private fun MiniMosaicThumbnail() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6FAF7))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.weight(1.4f).height(20.dp).clip(RoundedCornerShape(4.dp)).background(Color.White))
            Box(Modifier.weight(1f).height(20.dp).clip(RoundedCornerShape(4.dp)).background(ForestGreen))
        }
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            listOf(0.85f, 0.78f, 0.70f, 0.65f, 0.92f, 0.88f).forEachIndexed { idx, w ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(w)
                        .height(7.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (idx == 4) ForestGreen else VibrantGreen)
                )
            }
        }
    }
}

@Composable
private fun MiniCompleteCoverThumbnail() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ForestGreen)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.WbSunny,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.14f),
            modifier = Modifier
                .size(56.dp)
                .align(Alignment.TopEnd)
        )
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            Text("B2R Solar", color = Color(0xFF6EE7B7), fontSize = 7.sp)
            Spacer(Modifier.height(4.dp))
            Text("Energia Solar\nFotovoltaica", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, lineHeight = 12.sp)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(3) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                )
            }
        }
    }
}
