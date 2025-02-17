package io.portaudio

import boxing.toBytePointer
import cats.effect.{Resource, Sync}
import cbindings.portaudio.functions
import constants.FRAMES_PER_BUFFER
import fs2.{Chunk, Pull, Pipe, Stream}
import io.AudioSuite

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.UnsignedRichInt

object PortAudioAudioSuite:
  def resource[F[_]](using F: Sync[F]): Resource[F, AudioSuite[F]] =
    // Note: This is not thread safe!
    // Scala native 0.4 is single threaded so we can re-use the same input and output buffer each time
    // Once this project moves to use multithreading, this will no longer be possible
    val inputBuffer = new Array[Float](FRAMES_PER_BUFFER)
    val outputBuffer = new Array[Float](FRAMES_PER_BUFFER)

    for {
      _ <- initPortAudio[F]
      pStream <- defaultPaStream[F]
    } yield new AudioSuite[F]:
      def input: Stream[F, Float] =
        Pull
          .eval(F.blocking {
            functions.Pa_ReadStream(
              stream = pStream,
              buffer = inputBuffer.atUnsafe(0).toBytePointer,
              frames = FRAMES_PER_BUFFER.toULong
            )
            Chunk.ArraySlice(inputBuffer, 0, FRAMES_PER_BUFFER)
          })
          .flatMap(Pull.output)
          .streamNoScope
          .repeat

      def output: Pipe[F, Float, Nothing] =
        _.chunkN(FRAMES_PER_BUFFER, allowFewer = false).foreach { chunk =>
          F.blocking {
            chunk.copyToArray(outputBuffer, 0)
            functions.Pa_WriteStream(
              stream = pStream,
              buffer = outputBuffer.atUnsafe(0).toBytePointer,
              frames = FRAMES_PER_BUFFER.toULong
            )
            ()
          }
        }
