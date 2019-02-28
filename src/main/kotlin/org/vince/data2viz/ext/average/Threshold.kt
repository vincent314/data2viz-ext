package org.vince.data2viz.ext.average

import io.data2viz.color.ColorOrGradient
import io.data2viz.color.Colors
import org.vince.data2viz.ext.ExtVizDsl

@ExtVizDsl
data class Threshold(val value: Double, val color: ColorOrGradient = Colors.Web.red)

fun AverageViz.threshold(value: Double, color: ColorOrGradient = Colors.Web.red) {
  thresholds += Threshold(value, color)
}
