package arpeggio

import cats.Semigroup
import fs2.{Pipe, Stream}

type Pedal[F[_]] = Pipe[F, Float, Float]

given pointwiseAdd[F[_]]: Semigroup[Stream[F, Float]] =
  new:
    def combine(
        x: Stream[F, Float],
        y: Stream[F, Float]
    ): Stream[F, Float] =
      x.zipWith(y)(_ + _)
