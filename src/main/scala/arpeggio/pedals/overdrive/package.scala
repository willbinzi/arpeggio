package arpeggio
package pedals.overdrive

import arpeggio.routing.parallel
import cats.effect.Concurrent

def asymmetricClipping[F[_]](threshold: Float): Pedal[F] =
  _.map(sample => math.min(sample, threshold))

def symmetricClipping[F[_]](threshold: Float): Pedal[F] =
  _.map(sample => math.min(math.max(sample, -threshold), threshold))

def blended[F[_]: Concurrent](
    blend: Float,
    threshold: Float
): Pedal[F] =
  parallel(
    _.map(_ * (1 - blend)),
    symmetricClipping(threshold) andThen (_.map(_ * blend))
  )
