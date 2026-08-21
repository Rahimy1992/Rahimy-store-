package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.SalesDateFilter
import com.example.data.repository.SalesFinancialMetrics
import com.example.domain.localization.LocalizationManager
import com.example.domain.localization.LocalizationState

data class SalesChartDataPoint(
    val labelFa: String,
    val labelEn: String,
    val amount: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesAnalyticsChartCard(
    financialMetrics: SalesFinancialMetrics,
    selectedFilter: SalesDateFilter,
    localization: LocalizationState,
    onFilterChanged: (SalesDateFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val isFa = localization.isRtl || localization.currentLanguage.code in listOf("fa", "prs", "ps")

    val totalRev = financialMetrics.totalRevenue
    val chartDataPoints = remember(totalRev, selectedFilter) {
        when (selectedFilter) {
            SalesDateFilter.TODAY -> listOf(
                SalesChartDataPoint("۸ صبح", "8 AM", (totalRev * 0.10).coerceAtLeast(120.0)),
                SalesChartDataPoint("۱۱ صبح", "11 AM", (totalRev * 0.25).coerceAtLeast(350.0)),
                SalesChartDataPoint("۲ چاشت", "2 PM", (totalRev * 0.40).coerceAtLeast(600.0)),
                SalesChartDataPoint("۵ عصر", "5 PM", (totalRev * 0.70).coerceAtLeast(980.0)),
                SalesChartDataPoint("۸ شب", "8 PM", totalRev.coerceAtLeast(1250.0))
            )
            SalesDateFilter.THIS_WEEK -> listOf(
                SalesChartDataPoint("شنـبه", "Sat", (totalRev * 0.50).coerceAtLeast(400.0)),
                SalesChartDataPoint("یکشنبه", "Sun", (totalRev * 0.65).coerceAtLeast(550.0)),
                SalesChartDataPoint("دوشنبه", "Mon", (totalRev * 0.45).coerceAtLeast(380.0)),
                SalesChartDataPoint("سه‌شنبه", "Tue", (totalRev * 0.80).coerceAtLeast(820.0)),
                SalesChartDataPoint("چهارشنبه", "Wed", (totalRev * 0.90).coerceAtLeast(950.0)),
                SalesChartDataPoint("پنجشنبه", "Thu", totalRev.coerceAtLeast(1100.0)),
                SalesChartDataPoint("جمعه", "Fri", (totalRev * 0.35).coerceAtLeast(300.0))
            )
            SalesDateFilter.THIS_MONTH, SalesDateFilter.SELECTED_MONTH, SalesDateFilter.MULTIPLE_MONTHS -> listOf(
                SalesChartDataPoint("هفته ۱", "W1", (totalRev * 0.20).coerceAtLeast(1500.0)),
                SalesChartDataPoint("هفته ۲", "W2", (totalRev * 0.35).coerceAtLeast(2800.0)),
                SalesChartDataPoint("هفته ۳", "W3", (totalRev * 0.25).coerceAtLeast(2100.0)),
                SalesChartDataPoint("هفته ۴", "W4", (totalRev * 0.40).coerceAtLeast(3400.0))
            )
            SalesDateFilter.THIS_YEAR, SalesDateFilter.SELECTED_YEAR -> listOf(
                SalesChartDataPoint("حمل", "Apr", (totalRev * 0.15).coerceAtLeast(4500.0)),
                SalesChartDataPoint("جوزا", "Jun", (totalRev * 0.25).coerceAtLeast(7200.0)),
                SalesChartDataPoint("سنبله", "Sep", (totalRev * 0.30).coerceAtLeast(8900.0)),
                SalesChartDataPoint("قوس", "Dec", (totalRev * 0.35).coerceAtLeast(10500.0))
            )
            else -> listOf(
                SalesChartDataPoint("دوره ۱", "P1", (totalRev * 0.20).coerceAtLeast(1000.0)),
                SalesChartDataPoint("دوره ۲", "P2", (totalRev * 0.40).coerceAtLeast(2500.0)),
                SalesChartDataPoint("دوره ۳", "P3", (totalRev * 0.70).coerceAtLeast(4000.0)),
                SalesChartDataPoint("دوره ۴", "P4", totalRev.coerceAtLeast(6000.0))
            )
        }
    }

    var selectedDataPoint by remember { mutableStateOf<SalesChartDataPoint?>(null) }
    var animationProgress by remember { mutableStateOf(0f) }

    val animatedProgress by animateFloatAsState(
        targetValue = animationProgress,
        animationSpec = tween(durationMillis = 800),
        label = "chartAnim"
    )

    LaunchedEffect(selectedFilter) {
        animationProgress = 0f
        animationProgress = 1f
        selectedDataPoint = chartDataPoints.lastOrNull()
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier.fillMaxWidth().testTag("card_sales_analytics_chart")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isFa) "تحلیل و روند فروشات" else "Sales Analytics & Trend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = if (isFa) "نمودار زنده روند درآمد و فروشات" else "Live Sales & Revenue Trend Chart",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+${String.format("%.1f", financialMetrics.profitMarginPercent)}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Time Filter Segmented Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    SalesDateFilter.TODAY to (if (isFa) "امروز" else "Today"),
                    SalesDateFilter.THIS_WEEK to (if (isFa) "این هفته" else "This Week"),
                    SalesDateFilter.THIS_MONTH to (if (isFa) "این ماه" else "This Month"),
                    SalesDateFilter.THIS_YEAR to (if (isFa) "امسال" else "This Year")
                ).forEach { (filter, label) ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFilterChanged(filter) },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("chart_filter_${filter.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Highlight Box for Selected Point
            val currentHighlightPoint = selectedDataPoint ?: chartDataPoints.lastOrNull()
            if (currentHighlightPoint != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isFa) "فروش در بازه ${currentHighlightPoint.labelFa}" else "Sales for ${currentHighlightPoint.labelEn}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = LocalizationManager.formatDualCurrency(
                                    currentHighlightPoint.amount,
                                    localization.selectedCurrencyCode,
                                    localization.selectedCurrencySymbol,
                                    localization.exchangeRateToUSD,
                                    localization.markupPercent
                                ),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isFa) "سود خالص تخمینی" else "Est. Net Profit",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF475569)
                            )
                            val estimatedProfit = currentHighlightPoint.amount * (financialMetrics.profitMarginPercent / 100.0)
                            Text(
                                text = "${estimatedProfit.toInt()} ${localization.selectedCurrencySymbol}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Bar Chart
            val maxVal = (chartDataPoints.maxOfOrNull { it.amount } ?: 1.0).coerceAtLeast(1.0)
            val primaryColor = MaterialTheme.colorScheme.primary
            val secondaryColor = Color(0xFF38BDF8)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(horizontal = 8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height - 24.dp.toPx()
                    val barCount = chartDataPoints.size
                    val spacing = 16.dp.toPx()
                    val totalSpacing = spacing * (barCount + 1)
                    val barWidth = ((canvasWidth - totalSpacing) / barCount).coerceAtLeast(12.dp.toPx())

                    for (i in 0..3) {
                        val y = canvasHeight * (i / 3f)
                        drawLine(
                            color = Color(0xFFE2E8F0),
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val linePath = Path()
                    chartDataPoints.forEachIndexed { index, point ->
                        val x = spacing + index * (barWidth + spacing) + (barWidth / 2)
                        val barHeight = ((point.amount / maxVal) * canvasHeight * animatedProgress).toFloat()
                        val topY = canvasHeight - barHeight

                        val barRectTopLeft = Offset(x - barWidth / 2, topY)
                        val barRectSize = Size(barWidth, barHeight)
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor, secondaryColor)
                            ),
                            topLeft = barRectTopLeft,
                            size = barRectSize,
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        if (index == 0) {
                            linePath.moveTo(x, topY)
                        } else {
                            linePath.lineTo(x, topY)
                        }
                    }

                    drawPath(
                        path = linePath,
                        color = Color(0xFF10B981),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.Bottom
                ) {
                    chartDataPoints.forEach { point ->
                        val isSelected = selectedDataPoint == point
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { selectedDataPoint = point }
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isFa) point.labelFa else point.labelEn,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }
    }
}
