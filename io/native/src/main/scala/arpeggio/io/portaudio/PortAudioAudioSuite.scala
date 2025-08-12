package arpeggio
package io.portaudio

import arpeggio.boxing.toBytePointer
import arpeggio.constants.FRAMES_PER_BUFFER
import arpeggio.io.AudioSuite
import cats.effect.{Concurrent, Resource, Sync}
import cats.syntax.functor.toFunctorOps
import cbindings.portaudio.{blocking, functions}
import fs2.{Chunk, Pipe, Pull, Stream}

import scala.scalanative.unsafe.UnsafeRichArray
import scala.scalanative.unsigned.UnsignedRichInt

object PortAudioAudioSuite:
  def default[F[_]](using
      F: Sync[F] with Concurrent[F]
  ): Resource[F, AudioSuite[F]] =
    for {
      _ <- Resource.make(F.delay(functions.Pa_Initialize()).void)(_ =>
        F.delay(functions.Pa_Terminate()).void
      )
    } yield new AudioSuite[F]:
      def input: Stream[F, Float] =
        Stream
          .bracket(F.blocking(unsafe.openDefaultInputPaStream()))(pStream =>
            F.blocking(functions.Pa_CloseStream(pStream)).void
          )
          .flatMap(pStream =>
            Pull
              .eval(F.blocking {
                val inputBuffer = new Array[Float](FRAMES_PER_BUFFER)
                blocking.functions.Pa_ReadStream(
                  stream = pStream,
                  buffer = inputBuffer.atUnsafe(0).toBytePointer,
                  frames = FRAMES_PER_BUFFER.toCSize
                )
                Chunk.ArraySlice(inputBuffer, 0, FRAMES_PER_BUFFER)
              })
              .flatMap(Pull.output)
              .streamNoScope
              .repeat
          )

      def output: Pipe[F, Float, Nothing] = stream =>
        Stream
          .bracket(F.blocking(unsafe.openDefaultOutputPaStream()))(pStream =>
            F.blocking(functions.Pa_CloseStream(pStream)).void
          )
          .flatMap(pStream =>
            stream.chunkN(FRAMES_PER_BUFFER).foreach { chunk =>
              val Chunk.ArraySlice(array, offset, length) =
                chunk.toArraySlice
              F.blocking {
                blocking.functions.Pa_WriteStream(
                  stream = pStream,
                  buffer = array.atUnsafe(offset).toBytePointer,
                  frames = length.toCSize
                )
                ()
              }
            }
          )
