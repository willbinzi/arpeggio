package arpeggio
package routing

import cats.effect.IO
import fs2.Stream

class ParallelTest extends ArpeggioSuite:
  test("run two pedals in parallel"):
    val pedal1: Pedal[IO] = _.map(_ => 0.2f)
    val pedal2: Pedal[IO] = _.map(_ => 0.3f)
    assertApproxEqual(
      Stream
        .constant[IO, Float](0.1)
        .take(20)
        .rechunkRandomly()
        .through(parallel(pedal1, pedal2)),
      Stream.constant(0.5f).take(20)
    )
