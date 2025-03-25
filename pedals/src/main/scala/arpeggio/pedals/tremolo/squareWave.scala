package arpeggio
package pedals.tremolo

import arpeggio.constants.SAMPLE_RATE
import arpeggio.pedals.volume
import cats.effect.Concurrent
import fs2.Stream

import scala.concurrent.duration.Duration

def squareWave[F[_]: Concurrent](
    cycleLength: Duration
): Pedal[F] =
  val halfCycleLengthInFrames =
    (cycleLength.toMicros * SAMPLE_RATE / 2000000).toInt
  volume.sweep(
    (Stream.constant(1f).take(halfCycleLengthInFrames) ++
      Stream.constant(0f).take(halfCycleLengthInFrames)).repeat
  )
