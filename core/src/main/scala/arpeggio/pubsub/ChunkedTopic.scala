package arpeggio
package pubsub

import cats.effect.{Concurrent, Resource}
import cats.Functor
import fs2.concurrent.Topic
import fs2.{Chunk, Pipe, Stream}

object ChunkedTopic:
  opaque type ChunkedTopic[F[_], A] = Topic[F, Chunk[A]]

  extension [F[_]: Functor, A](chunkTopic: ChunkedTopic[F, A])
    def publish: Pipe[F, A, Nothing] =
      _.chunks.through(chunkTopic.publish)

    def subscribeAwait(maxQueued: Int): Resource[F, Stream[F, A]] =
      chunkTopic.subscribeAwait(maxQueued).map(_.unchunks)

  object ChunkedTopic:
    def apply[F[_]: Concurrent, A]: F[ChunkedTopic[F, A]] =
      Topic[F, Chunk[A]]
