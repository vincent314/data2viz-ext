package org.vince.data2viz.ext.average

import io.data2viz.axis.Orient
import io.data2viz.axis.axis
import io.data2viz.color.Colors
import io.data2viz.scale.LinearScale
import io.data2viz.scale.Scales
import io.data2viz.scale.TimeScale
import io.data2viz.shape.areaBuilder
import io.data2viz.shape.curves
import io.data2viz.time.Date
import io.data2viz.viz.GroupNode
import io.data2viz.viz.Margins
import io.data2viz.viz.Viz
import org.vince.data2viz.ext.ExtVizDsl
import org.vince.data2viz.ext.measureDate

@ExtVizDsl
class AverageViz(
        val viz: Viz,
        val canvasWidth: Double,
        val canvasHeight: Double,
        val series: MutableList<Serie> = mutableListOf(),
        val thresholds: MutableList<Threshold> = mutableListOf()) {

    var margin: Margins = Margins(5.0, 5.0, 30.0, 30.0)
    var startAt = 0.0

    private val width: Double
        get() = canvasWidth - margin.hMargins

    private val height: Double
        get() = canvasHeight - margin.vMargins

    private val dateRange: Pair<Date, Date>
        get() = series.flatMap(Serie::data)
                .map(Pair<Date, Double>::first)
                .let {
                    it.minBy(Date::getTime)!!.measureDate to it.maxBy(Date::getTime)!!.measureDate
                }

    private val valueRange: Pair<Double, Double>
        get() = series.flatMap(Serie::data)
                .map(Pair<Date, Double>::second)
                .let {
                    it.min()!! to it.max()!!
                }

    val dateScale: TimeScale<Double>
        get() = Scales.Continuous.time {
            domain = dateRange.toList()
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
                        val area = areaBuilder<Pair<Date, Double>> {
                            stroke = serie.primaryColor
                            fill = serie.secondaryColor
                            curve = curves.natural
                            xBaseline = { (date) -> dateScale(date) }
                            xTopline = { (date) -> dateScale(date) }
                            yBaseline = { valueScale(50.0) }
                            yTopline = { (_, value) -> valueScale(value) }
                        }
                        area.render(average.toList(), this)
                    }

                    fun buildMinOrMax(localDate: Date, value: Double) = line {
                        strokeWidth = 1.0
                        fill = Colors.Web.gray
                        x1 = dateScale(localDate) - 2.0
                        x2 = dateScale(localDate) + 2.0
                        y1 = valueScale(value)
                        y2 = valueScale(value)
                    }

                    average.forEach { (localDate, value) ->
                        circle {
                            x = dateScale(localDate)
                            y = valueScale(value)
                            radius = 2.0
                            fill = serie.primaryColor
                        }

                        val (min, max) = serie.minMaxOf(localDate)
                        buildMinOrMax(localDate, min)
                        buildMinOrMax(localDate, max)
                        line {
                            val x = dateScale(localDate)
                            x1 = x
                            x2 = x
                            y1 = valueScale(min)
                            y2 = valueScale(max)
                        }
                    }

                    if (serie.linearRegression) {
                        val regression = serie.linear
                        line {
                            stroke = serie.primaryColor
                            val (min, max) = dateRange
                            x1 = dateScale(min)
                            x2 = dateScale(max)
                            y1 = valueScale(regression.predict(min.getTime()))
                            y2 = valueScale(regression.predict(max.getTime()))
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


