package arpeggio

import cats.effect.IO
import fs2.{Pure, Stream}
import munit.CatsEffectSuite

trait ArpeggioSuite extends CatsEffectSuite:
  // We're using streams of floats so approximate equality is all we ca hope for
  def assertApproxEqual(
      obtainedStream: Stream[IO, Float],
      expected: Stream[Pure, Float],
      tolerance: Float = 0.00000001f
  ): IO[Unit] =
    assertIOBoolean(
      obtainedStream
        .zipWith(expected)(_ - _)
        .forall(delta => Math.abs(delta) < tolerance)
        .compile
        .lastOrError
    )
