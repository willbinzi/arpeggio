package arpeggio
package io

import fs2.{Pipe, Stream}

trait AudioInterface[F[_]] {
  def input: Stream[F, Float]
  def output: Pipe[F, Float, Nothing]
}
