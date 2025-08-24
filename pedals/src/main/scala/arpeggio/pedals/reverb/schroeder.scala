package arpeggio
package pedals.reverb

import arpeggio.constants.FRAMES_PER_BUFFER
import arpeggio.pedals.delay.echoRepeats
import arpeggio.pedals.volume.adjustLevel
import arpeggio.routing.parallel
import cats.effect.Concurrent

import scala.concurrent.duration.*

def schroeder[F[_]: Concurrent](
    predelay: Duration,
    decay: Duration,
    mix: Float
): Pedal[F] =
  parallel(
    identity,
    parallel(
      // Create 4 echo stages with slightly differing delays and gain factors
      Seq(1f, 1.17f, 1.34f, 1.5f)
        .map(predelay * _)
        .map(t =>
          echoRepeats(gain(decay, t), t)
            // Rechunking here (and in the allPassStage implementation below) greatly improves CPU consumption
            // Without rechunking, the chunk size resulting from zipping all of these repeat streams together becomes extremely small
            .andThen(_.rechunkN(FRAMES_PER_BUFFER))
        ): _*
    )
      .andThen(allPassStage(0.7, 5.millis))
      .andThen(allPassStage(0.7, 1700.micros))
      .andThen(adjustLevel(mix))
  )

def gain(decay: Duration, predelay: Duration): Float =
  Math.pow(2, (-3f * predelay.toMicros) / decay.toMicros).toFloat

def allPassStage[F[_]: Concurrent](
    repeatGain: Float,
    delayTime: Duration
): Pedal[F] = parallel(
  adjustLevel(-repeatGain),
  echoRepeats(repeatGain, delayTime)
    .andThen(_.rechunkN(FRAMES_PER_BUFFER))
    .andThen(adjustLevel(1 - repeatGain * repeatGain))
)
