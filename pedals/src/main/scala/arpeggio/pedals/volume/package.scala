package arpeggio
package pedals.volume

import fs2.Stream

def adjustLevel[F[_]](gain: Float): Pedal[F] =
  _.map(_ * gain)

def sweep[F[_]](volumeControlStream: Stream[F, Float]): Pedal[F] =
  _.zipWith(volumeControlStream)(_ * _)
