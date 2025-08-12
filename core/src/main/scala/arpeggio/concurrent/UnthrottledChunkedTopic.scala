package arpeggio
package concurrent

import cats.effect.{Concurrent, Resource}
import cats.Functor
import fs2.concurrent.Topic
import fs2.{Chunk, Pipe, Stream}

object UnthrottledChunkedTopic:
  opaque type UnthrottledChunkedTopic[F[_], A] = Topic[F, Chunk[A]]

  extension [F[_]: Functor, A](chunkTopic: UnthrottledChunkedTopic[F, A])
    def publish: Pipe[F, A, Nothing] =
      _.chunks.through(chunkTopic.publish)

    def subscribeAwait: Resource[F, Stream[F, A]] =
      chunkTopic.subscribeAwaitUnbounded.map(_.unchunks)

  object UnthrottledChunkedTopic:
    def apply[F[_]: Concurrent, A]: F[UnthrottledChunkedTopic[F, A]] =
      Topic[F, Chunk[A]]
