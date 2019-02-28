package org.vince.data2viz.ext

import io.data2viz.time.Date
import org.junit.Test
import kotlin.test.assertEquals

class DateExtensionTest {

  @Test
  fun `should remove time from a date-time`(){
    val dateWithouTime = Date(2019,2,28,12,30,40,22).withoutTime
    with(dateWithouTime) {
      assertEquals(2019, year())
      assertEquals(2, month())
      assertEquals(28, dayOfMonth())
      assertEquals(0, hour())
      assertEquals(0, minute())
      assertEquals(0,millisecond())
    }
  }

  @Test
  fun `should test 0am-4am is still evening`(){
    assertEquals(
      "2019-02-27T00:00",
      Date(2019,2,28,3,30,40,22).measureDate.toString()
    )
    assertEquals(
      "2018-12-31T00:00",
      Date(2019,1,1,3,30,40,22).measureDate.toString()
    )
    assertEquals(
      "2019-02-28T00:00",
      Date(2019,2,28,4,30,40,22).measureDate.toString()
    )
  }
}
