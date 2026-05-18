package com.example.iotambientaltec.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Gráficos ambientales", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { VariableDropdown(state.variable, vm::setVariable) }
        item { ChartCard("Evolución temporal - ${variableLabels[state.variable]}") { LineChart(selected) } }
        item { ChartCard("Promedios diarios") { BarChart(state.data.filter { it.variable == state.variable }) } }
        item { ChartCard("Comparación entre variables") { ComparisonChart(state.data.takeLast(144)) } }
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) = ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(title, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(12.dp)); content() } }

@Composable
private fun LineChart(data: List<EnvironmentalData>) {
    Canvas(Modifier.fillMaxWidth().height(220.dp)) {
        if (data.size < 2) return@Canvas
        val values = data.map { it.value }; val min = values.min(); val max = values.max(); val range = (max - min).takeIf { it > 0 } ?: 1.0
        val path = Path()
        data.forEachIndexed { i, d ->
            val x = i * size.width / (data.lastIndex.coerceAtLeast(1)); val y = size.height - (((d.value - min) / range).toFloat() * size.height)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, Color(0xFF0B6E4F), style = Stroke(5f)); drawLine(Color.Gray, Offset(0f, size.height), Offset(size.width, size.height), 2f)
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
    val avg = data.groupBy { it.variable }.mapValues { it.value.map(EnvironmentalData::value).average() }
    Canvas(Modifier.fillMaxWidth().height(180.dp)) {
        val colors = listOf(Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047)); val max = avg.values.maxOrNull()?.toFloat() ?: 1f
        avg.entries.forEachIndexed { i, e -> val w = (e.value.toFloat() / max) * size.width; drawRect(colors[i % colors.size], topLeft = Offset(0f, i * size.height / 3 + 12f), size = androidx.compose.ui.geometry.Size(w, 28f)) }
    }
}
