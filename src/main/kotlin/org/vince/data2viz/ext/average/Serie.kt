package org.vince.data2viz.ext.average

import io.data2viz.color.Color
import io.data2viz.color.Colors
import io.data2viz.time.Date
import org.nield.kotlinstatistics.SimpleRegression
import org.nield.kotlinstatistics.averageBy
import org.nield.kotlinstatistics.simpleRegression
import org.vince.data2viz.ext.ExtVizDsl
import org.vince.data2viz.ext.sameDate

@ExtVizDsl
class Serie(
  var primaryColor: Color = Colors.Web.blueviolet,
  var secondaryColor: Color = Colors.Web.aliceblue,
  var data: List<Pair<Date, Double>> = listOf(),
  var linearRegression: Boolean = false
) {
  val average: Map<Date, Double>
    get() {
      return data.averageBy(
        keySelector = { (date) -> Triple(date.year(), date.month(), date.dayOfMonth()) },
        doubleSelector = Pair<Date, Double>::second
      )
        .mapKeys { (triple) ->
          val (year, month, day) = triple
          Date(year, month, day, 0, 0, 0, 0)
        }
    }

  val linear: SimpleRegression
    get() {
      return data
        .map { (date, value) ->
          date.getTime() to value
        }
        .simpleRegression()
    }

  fun minMaxOf(localDate: Date): Pair<Double, Double> {
    return data.filter {
      it.first.sameDate(localDate)
    }
      .map { it.second }
      .let { it.min()!! to it.max()!! }
  }
}

fun AverageViz.serie(init: Serie.() -> Unit): Serie = Serie()
  .apply(init)
  .also { series += it }

