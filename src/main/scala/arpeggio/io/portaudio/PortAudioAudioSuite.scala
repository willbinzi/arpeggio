package arpeggio
package io.portaudio

import arpeggio.boxing.toBytePointer
import arpeggio.constants.FRAMES_PER_BUFFER
import arpeggio.io.AudioSuite
import cats.effect.{Resource, Sync}
import cats.syntax.functor.toFunctorOps
import cbindings.portaudio.aliases.PaStream
import cbindings.portaudio.functions
import fs2.{Chunk, Pipe, Pull, Stream}

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.UnsignedRichInt

object PortAudioAudioSuite:
  def default[F[_]](using F: Sync[F]): Resource[F, AudioSuite[F]] =
    for {
      _ <- initPortAudio
      pStream <- defaultPaStream
    } yield new AudioSuite[F]:
      def input: Stream[F, Float] =
        Pull
          .eval(F.blocking {
            val inputBuffer = new Array[Float](FRAMES_PER_BUFFER)
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
            val outputBuffer = new Array[Float](FRAMES_PER_BUFFER)
            chunk.copyToArray(outputBuffer, 0)
            functions.Pa_WriteStream(
              stream = pStream,
              buffer = outputBuffer.atUnsafe(0).toBytePointer,
              frames = FRAMES_PER_BUFFER.toULong
            )
            ()
          }
        }

  private def initPortAudio[F[_]](using F: Sync[F]): Resource[F, Unit] =
    Resource.make(F.delay(functions.Pa_Initialize()).void)(_ =>
      F.delay(functions.Pa_Terminate()).void
    )

  private def defaultPaStream[F[_]](using
      F: Sync[F]
  ): Resource[F, Ptr[PaStream]] =
    Resource.make(F.delay(unsafe.openDefaultPaStream()))(pStream =>
      F.delay(functions.Pa_CloseStream(pStream)).void
    )
