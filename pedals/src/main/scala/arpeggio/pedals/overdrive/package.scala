package arpeggio
package pedals.overdrive

import arpeggio.pedals.volume.adjustLevel
import arpeggio.routing.parallel
import cats.effect.Concurrent

def asymmetricClipping[F[_]](threshold: Float): Pedal[F] =
  _.map(sample => Math.min(sample, threshold))

def symmetricClipping[F[_]](threshold: Float): Pedal[F] =
  _.map(sample => Math.min(Math.max(sample, -threshold), threshold))

def blended[F[_]: Concurrent](
    blend: Float,
    threshold: Float
): Pedal[F] =
  parallel(
    adjustLevel(1 - blend),
    symmetricClipping(threshold) andThen adjustLevel(blend)
  )
