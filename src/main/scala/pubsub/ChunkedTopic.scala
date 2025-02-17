package pubsub

import cats.effect.{Concurrent, Resource}
import fs2.concurrent.Topic
import fs2.{Chunk, Pipe, Stream}

object ChunkedTopic:
  opaque type ChunkedTopic[F[_], A] = Topic[F, Chunk[A]]

  extension [F[_], A](chunkTopic: ChunkedTopic[F, A])
    def observePublishChunks: Pipe[F, A, A] =
      _.chunks.evalTap(chunkTopic.publish1).unchunks

    def subscribeAwaitUnbounded: Resource[F, Stream[F, A]] =
      chunkTopic.subscribeAwaitUnbounded.map(_.unchunks)

  object ChunkedTopic:
    def apply[F[_]: Concurrent, A]: F[ChunkedTopic[F, A]] =
      Topic[F, Chunk[A]]
