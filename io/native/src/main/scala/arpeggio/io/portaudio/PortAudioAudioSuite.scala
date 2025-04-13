package arpeggio
package io.portaudio

import arpeggio.boxing.toBytePointer
import arpeggio.constants.FRAMES_PER_BUFFER
import arpeggio.io.AudioSuite
import cats.effect.std.CyclicBarrier
import cats.effect.{Concurrent, Resource, Sync}
import cats.syntax.apply.catsSyntaxApplyOps
import cats.syntax.functor.toFunctorOps
import cbindings.portaudio.functions
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
      // We don't want to allow new data to be read from the input until we have written pending data to the output
      // This is particularly important because we are still using Scala Native 0.4, which is single threaded
      // Since both operations block the thread, we don't want to perform them any more than necessary
      // Hence we use a cyclic barrier to sync the operations
      cyclicBarrier <- Resource.eval(CyclicBarrier(2))
    } yield new AudioSuite[F]:
      def input: Stream[F, Float] =
        Stream
          .bracket(F.blocking(unsafe.openDefaultInputPaStream()))(pStream =>
            F.blocking(functions.Pa_CloseStream(pStream)).void
          )
          .flatMap(pStream =>
            (Pull
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
              .streamNoScope ++ Stream.exec(cyclicBarrier.await)).repeat
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
                functions.Pa_WriteStream(
                  stream = pStream,
                  buffer = array.atUnsafe(offset).toBytePointer,
                  frames = length.toULong
                )
              } *> cyclicBarrier.await
            }
          )
