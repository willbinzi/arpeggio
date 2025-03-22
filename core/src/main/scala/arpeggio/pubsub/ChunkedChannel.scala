package arpeggio
package pubsub

import cats.effect.Concurrent
import cats.Functor
import fs2.concurrent.Channel
import fs2.{Chunk, Pipe, Stream}

object ChunkedChannel:
  opaque type ChunkedChannel[F[_], A] = Channel[F, Chunk[A]]

  extension [F[_]: Functor, A](chunkChannel: ChunkedChannel[F, A])
    def sendAll: Pipe[F, A, Nothing] =
      _.chunks.through(chunkChannel.sendAll)

    def stream: Stream[F, A] = chunkChannel.stream.unchunks

  object ChunkedChannel:
    def unbounded[F[_]: Concurrent, A]: F[ChunkedChannel[F, A]] =
      Channel.unbounded
