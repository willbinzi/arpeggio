package arpeggio
package routing

import arpeggio.concurrent.ChunkedTopic.*
import cats.data.NonEmptySeq
import cats.effect.Concurrent
import cats.syntax.traverse.toTraverseOps
import fs2.Stream

def parallel[F[_]: Concurrent](pedals: Pedal[F]*): Pedal[F] =
  assert(pedals.nonEmpty, s"Cannot run 0 pedals in parallel")
  parallelNonEmpty(NonEmptySeq.fromSeqUnsafe(pedals))

def parallelNonEmpty[F[_]: Concurrent](
    pedals: NonEmptySeq[Pedal[F]]
): Pedal[F] =
  stream =>
    for {
      topic <- Stream.eval(ChunkedTopic[F, Float])
      pedalOutputs <- Stream.resource(
        // Here we use maxQueued = 1 in order to backpressure the input stream
        // Without this, we run into problems since Scala Native 0.4 apps are single threaded
        // Unless we backpressure, the app spends almost all of it's time blocking the thread while it waits to write new data from the input stream
        pedals.traverse(topic.subscribeAwait(1).map)
      )
      result <- pedalOutputs.reduce.concurrently(stream.through(topic.publish))
    } yield result
