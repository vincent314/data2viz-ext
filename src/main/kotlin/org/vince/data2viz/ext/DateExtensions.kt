package org.vince.data2viz.ext

import io.data2viz.time.Date

val Date.withoutTime: Date
  get() = Date(year(), month(), dayOfMonth(), 0, 0, 0, 0)

fun Date.sameDate(date: Date): Boolean {
  return year() == date.year() && month() == date.month() && dayOfMonth() == date.dayOfMonth()
}

val Date.measureDate: Date
  get() {
    val hourThreshold = 4
    val date = withoutTime
    return if (hour() >= hourThreshold) {
      date
    } else {
      date.minusMilliseconds(1000 * 60 * 60 * 24)
    }
  }
