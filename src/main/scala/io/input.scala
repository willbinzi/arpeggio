package io

import cats.effect.Sync
import constants.FRAMES_PER_BUFFER
import fs2.{Chunk, Pull, Stream}
import portaudio.aliases.PaStream
import portaudio.functions

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.UnsignedRichInt

def inputStreamFromPointer[F[_]](pStream: Ptr[PaStream])(implicit
    F: Sync[F]
): Stream[F, Float] =
  Pull
    .eval(F.blocking {
      val buffer = new Array[Float](FRAMES_PER_BUFFER)
      functions.Pa_ReadStream(
        pStream,
        buffer.atUnsafe(0).toBytePointer,
        FRAMES_PER_BUFFER.toCSize
      )
      Chunk.ArraySlice(buffer, 0, FRAMES_PER_BUFFER)
    })
    .flatMap(Pull.output)
    .streamNoScope
    .repeat
