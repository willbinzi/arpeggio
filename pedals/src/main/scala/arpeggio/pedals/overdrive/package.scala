package arpeggio
package pedals.overdrive

import arpeggio.pedals.volume.adjustLevel
import arpeggio.routing.parallel
import cats.effect.Concurrent

def asymmetricHardClipping[F[_]](threshold: Float): Pedal[F] =
  _.map(sample => Math.min(sample, threshold))

def symmetricHardClipping[F[_]](threshold: Float): Pedal[F] =
  _.map(sample => Math.min(Math.max(sample, -threshold), threshold))

def softClipping[F[_]](threshold: Float): Pedal[F] =
  _.map {
    case sample if sample > threshold  => threshold
    case sample if sample < -threshold => -threshold
    case 0f                            => 0f
    case sample if sample > 0 =>
      // Take a "normalised distance" from the threshold and raise it to the power 4 to "softly" decrease it
      threshold * (1 - Math.pow(1 - (sample / threshold), 4)).toFloat
    case sample if sample < 0 =>
      threshold * (-1 + Math.pow(1 + (sample / threshold), 4)).toFloat
  }

def blended[F[_]: Concurrent](
    blend: Float,
    threshold: Float
): Pedal[F] =
  parallel(
    adjustLevel(1 - blend),
    symmetricHardClipping(threshold) andThen adjustLevel(blend)
  )
