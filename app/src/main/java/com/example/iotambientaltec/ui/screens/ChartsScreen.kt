package com.example.iotambientaltec.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.iotambientaltec.data.model.EnvironmentalData

@Composable
fun ChartsScreen(factory: AppViewModelFactory) {
    val vm: ChartsViewModel = viewModel(factory = factory)
    val state by vm.state.collectAsState()
    val selected = state.data.filter { it.variable == state.variable }.takeLast(48)
    val comparison = state.data.takeLast(144)
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Gráficos ambientales", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { VariableDropdown(state.variable, vm::setVariable) }
        item { ChartCard("Evolución temporal - ${variableLabels[state.variable]}") { LineChart(selected) } }
        item { ChartCard("Promedios diarios") { BarChart(state.data.filter { it.variable == state.variable }) } }
        item { ChartCard("Comparación entre variables (líneas traslapadas)") { ComparisonChart(comparison) } }
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) = ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(title, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(12.dp)); content() } }

@Composable
private fun LineChart(data: List<EnvironmentalData>) {
    val minMax = remember(data) {
        val values = data.map { it.value }
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: 1.0
        min to max
    }
    val min = minMax.first
    val max = minMax.second
    val range = (max - min).takeIf { it > 0 } ?: 1.0

    Column(Modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(220.dp)) {
            if (data.size < 2) return@Canvas
            val path = Path()
            data.forEachIndexed { i, d ->
                val x = i * size.width / (data.lastIndex.coerceAtLeast(1))
                val y = size.height - (((d.value - min) / range).toFloat() * (size.height - 12f))
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                drawCircle(Color(0xFF0B6E4F), radius = 4f, center = Offset(x, y))
            }
            drawPath(path, Color(0xFF0B6E4F), style = Stroke(4f))
            drawLine(Color.Gray, Offset(0f, size.height), Offset(size.width, size.height), 2f)
        }
        if (data.isNotEmpty()) {
            val first = data.first()
            val middle = data[data.lastIndex / 2]
            val last = data.last()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(first.date, style = MaterialTheme.typography.labelSmall)
                Text(middle.date, style = MaterialTheme.typography.labelSmall)
                Text(last.date, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Dato mínimo: ${"%.2f".format(min)} | Dato máximo: ${"%.2f".format(max)} | Último dato: ${"%.2f".format(last.value)}",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun BarChart(data: List<EnvironmentalData>) {
    val grouped = data.groupBy { it.date }.toSortedMap().mapValues { it.value.map(EnvironmentalData::value).average() }.entries.takeLast(7)
    Canvas(Modifier.fillMaxWidth().height(220.dp)) {
        if (grouped.isEmpty()) return@Canvas
        val max = grouped.maxOf { it.value }.toFloat(); val barWidth = size.width / (grouped.size * 1.5f)
        grouped.forEachIndexed { i, e -> val h = (e.value.toFloat() / max) * size.height; drawRect(Color(0xFF1976D2), topLeft = Offset(i * barWidth * 1.5f + barWidth * .25f, size.height - h), size = androidx.compose.ui.geometry.Size(barWidth, h)) }
    }
}

@Composable
private fun ComparisonChart(data: List<EnvironmentalData>) {
    val colors = mapOf("temperature" to Color(0xFFE53935), "humidity" to Color(0xFF1E88E5), "pm25" to Color(0xFF43A047))
    val groupedByVariable = data.groupBy { it.variable }.mapValues { (_, values) -> values.sortedBy { it.date }.takeLast(24) }
    val allValues = groupedByVariable.values.flatten().map(EnvironmentalData::value)
    val min = allValues.minOrNull() ?: 0.0
    val max = allValues.maxOrNull() ?: 1.0
    val range = (max - min).takeIf { it > 0 } ?: 1.0

    Column(Modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(220.dp).background(Color(0xFFF8F9FA))) {
            groupedByVariable.entries.forEach { (variable, points) ->
                if (points.size < 2) return@forEach
                val color = colors[variable] ?: Color.DarkGray
                val path = Path()
                points.forEachIndexed { i, point ->
                    val x = i * size.width / (points.lastIndex.coerceAtLeast(1))
                    val y = size.height - (((point.value - min) / range).toFloat() * (size.height - 14f))
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    drawCircle(color, radius = 3.5f, center = Offset(x, y))
                }
                drawPath(path, color, style = Stroke(3.5f))
            }
            drawLine(Color.Gray, Offset(0f, size.height), Offset(size.width, size.height), 1.5f)
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            groupedByVariable.keys.forEach { variable ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(colors[variable] ?: Color.DarkGray))
                    Spacer(Modifier.width(4.dp))
                    Text(variableLabels[variable] ?: variable, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Escala compartida para comparar tendencias por fecha y valor.", style = MaterialTheme.typography.labelSmall)
    }
}
