package org.vince.data2viz.ext

import io.data2viz.axis.Orient
import io.data2viz.axis.axis
import io.data2viz.color.Color
import io.data2viz.color.ColorOrGradient
import io.data2viz.color.Colors
import io.data2viz.scale.LinearScale
import io.data2viz.scale.Scales
import io.data2viz.scale.TimeScale
import io.data2viz.shape.areaBuilder
import io.data2viz.shape.curves
import io.data2viz.time.Date
import io.data2viz.time.date
import io.data2viz.viz.GroupNode
import io.data2viz.viz.Margins
import io.data2viz.viz.Viz
import org.nield.kotlinstatistics.SimpleRegression
import org.nield.kotlinstatistics.averageBy
import org.nield.kotlinstatistics.simpleRegression
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneId.SHORT_IDS

val LocalDate.vizDate: Date
    get() = date(this.year, this.monthValue, this.dayOfMonth)

val LocalDateTime.measureDate: LocalDate
    get() {
        val hourThreshold = 4
        val localDate = toLocalDate()
        return if (toLocalTime().hour > hourThreshold) {
            localDate
        } else {
            localDate.minusDays(1)
        }
    }

val LocalDateTime.millis: Long
    get() {
        val timeZone = ZoneId.of(SHORT_IDS["ECT"])
        return this.atZone(timeZone).toInstant().toEpochMilli()
    }

val LocalDate.millis: Long
    get() = LocalDateTime.of(this, LocalTime.of(0, 0)).millis


@ExtVizDsl
class AverageViz(
        val viz: Viz,
        val canvasWidth: Double,
        val canvasHeight: Double,
        val series: MutableList<Serie> = mutableListOf(),
        val thresholds: MutableList<Threshold> = mutableListOf()) {

    var margin: Margins = Margins(5.0, 5.0, 30.0, 30.0)
    var startAt = 0.0

    val width: Double
        get() = canvasWidth - margin.hMargins

    val height: Double
        get() = canvasHeight - margin.vMargins

    private val dateRange: Pair<LocalDate, LocalDate>
        get() = series.flatMap(Serie::data)
                .map(Pair<LocalDateTime, Double>::first)
                .let {
                    it.min()!!.measureDate to it.max()!!.measureDate
                }

    private val valueRange: Pair<Double, Double>
        get() = series.flatMap(Serie::data)
                .map(Pair<LocalDateTime, Double>::second)
                .let {
                    it.min()!! to it.max()!!
                }

    val dateScale: TimeScale<Double>
        get() = Scales.Continuous.time {
            domain = dateRange.toList()
                    .map(LocalDate::vizDate)
            range = listOf(0.0, width)
        }

    val valueScale: LinearScale<Double>
        get() = Scales.Continuous.linear {
            domain = listOf(startAt, valueRange.second)
            range = listOf(height, 0.0)
        }

    fun buildSerie(group: GroupNode, serie: Serie): GroupNode =
            with(group) {
                val average = serie.average

                group {
                    path {
                        val area = areaBuilder<Pair<LocalDate, Double>> {
                            stroke = serie.primaryColor
                            fill = serie.secondaryColor
                            curve = curves.natural
                            xBaseline = { dateScale(it.first.vizDate) }
                            xTopline = { dateScale(it.first.vizDate) }
                            yBaseline = { valueScale(50.0) }
                            yTopline = { valueScale(it.second) }
                        }
                        area.render(average.toList(), this)
                    }

                    fun buildMinOrMax(localDate: LocalDate, value: Double) = line {
                        strokeWidth = 1.0
                        fill = Colors.Web.gray
                        x1 = dateScale(localDate.vizDate) - 2.0
                        x2 = dateScale(localDate.vizDate) + 2.0
                        y1 = valueScale(value)
                        y2 = valueScale(value)
                    }

                    average.forEach { (localDate, value) ->
                        circle {
                            x = dateScale(localDate.vizDate)
                            y = valueScale(value)
                            radius = 2.0
                            fill = serie.primaryColor
                        }

                        val (min, max) = serie.minMaxOf(localDate)
                        buildMinOrMax(localDate, min)
                        buildMinOrMax(localDate, max)
                        line {
                            val x = dateScale(localDate.vizDate)
                            x1 = x
                            x2 = x
                            y1 = valueScale(min)
                            y2 = valueScale(max)
                        }
                    }

                    if(serie.linearRegression) {
                        val regression = serie.linear
                        line {
                            stroke = serie.primaryColor
                            val (min, max) = dateRange
                            x1 = dateScale(min.vizDate)
                            x2 = dateScale(max.vizDate)
                            y1 = valueScale(regression.predict(min.millis.toDouble()))
                            y2 = valueScale(regression.predict(max.millis.toDouble()))
                        }
                    }
                }
            }

    fun buildAxis(group: GroupNode) {
        with(group) {
            group {
                axis(Orient.BOTTOM, dateScale) {
                    tickFormat = { date -> "${date.dayOfMonth()}/${date.month()}" }
                    transform {
                        translate(0.0, valueScale(50.0))
                    }
                }
            }
            group {
                axis(Orient.LEFT, valueScale) {
                    tickFormat = { value -> value.toInt().toString() }
                }
            }
        }
    }

    fun buildThresholds(group: GroupNode) {
        with(group) {
            for (threshold in thresholds) {
                group {
                    line {
                        stroke = threshold.color
                        x1 = 0.0
                        x2 = width
                        y1 = valueScale(threshold.value)
                        y2 = valueScale(threshold.value)

                    }
                }
            }
        }
    }
}

@ExtVizDsl
class Serie(
        var primaryColor: Color = Colors.Web.blueviolet,
        var secondaryColor: Color = Colors.Web.aliceblue,
        var data: List<Pair<LocalDateTime, Double>> = listOf(),
        var linearRegression:Boolean = false
) {
    val average: Map<LocalDate, Double>
        get() {
            return data.averageBy(
                    keySelector = { it.first.toLocalDate() },
                    doubleSelector = Pair<LocalDateTime, Double>::second
            )
        }

    val linear: SimpleRegression
        get() {
            return data
                    .map { (date, value) ->
                        date.millis to value
                    }
                    .simpleRegression()
        }

    fun minMaxOf(localDate: LocalDate): Pair<Double, Double> {
        return data.filter {
            it.first.toLocalDate() == localDate
        }
                .map { it.second }
                .let { it.min()!! to it.max()!! }
    }
}

@ExtVizDsl
data class Threshold(val value: Double, val color: ColorOrGradient = Colors.Web.red)

fun Viz.averageViz(width: Double, height: Double, init: AverageViz.() -> Unit): AverageViz =
        AverageViz(this, width, height)
                .apply(init)
                .apply {
                    viz.width = canvasWidth
                    viz.height = canvasHeight

                    group {
                        transform {
                            translate(margin.left, margin.top)

                        }
                        series.forEach { buildSerie(this, it) }
                        buildThresholds(this)
                        buildAxis(this)
                    }
                }

fun AverageViz.serie(init: Serie.() -> Unit): Serie = Serie()
        .apply(init)
        .also { series += it }

fun AverageViz.threshold(value: Double, color: ColorOrGradient = Colors.Web.red) {
    thresholds += Threshold(value, color)
}
