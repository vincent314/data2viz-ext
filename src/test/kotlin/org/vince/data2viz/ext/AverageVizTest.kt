package org.vince.data2viz.ext

import io.data2viz.viz.viz
import org.junit.Test
import org.vince.data2viz.ext.average.averageViz
import org.vince.data2viz.ext.average.serie

class AverageVizTest {

    @Test
    fun `should build average vizualisation`() {
        viz {
            averageViz(300.0, 300.0) {
                serie() {
                    data = listOf()
                }
            }
        }
    }
}