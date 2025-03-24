package arpeggio
package io.jSound

import arpeggio.io.AudioSuite
import cats.effect.Sync
import cats.syntax.flatMap.toFlatMapOps
import cats.syntax.functor.toFunctorOps
import fs2.io.readInputStream
import fs2.{Chunk, Pipe, Stream}

import java.io.InputStream
import java.nio.{ByteBuffer, IntBuffer}
import javax.sound.sampled.{
  AudioFormat,
  AudioInputStream,
  AudioSystem,
  SourceDataLine,
  TargetDataLine
}

object JavaSoundAudioSuite:
  def default[F[_]](using F: Sync[F]): F[AudioSuite[F]] =
    for {
      mixer <- F.delay(AudioSystem.getMixer(null)) // gets default mixer
      inputLine <- F.delay(
        mixer.getTargetLineInfo.headOption
          .fold(throw new RuntimeException("No target line (input) found"))(
            mixer.getLine
          )
          .asInstanceOf[TargetDataLine]
      )
      outputLine <- F.delay(
        mixer.getSourceLineInfo.headOption
          .fold(throw new RuntimeException("No target line (output) found"))(
            mixer.getLine
          )
          .asInstanceOf[SourceDataLine]
      )
    } yield new AudioSuite[F]:
      def input: Stream[F, Float] =
        readInputStream(
          F.delay {
            inputLine.open(AUDIO_FORMAT)
            inputLine.start()
            new AudioInputStream(inputLine)
          }.widen[InputStream],
          BYTES_PER_BUFFER
        )
          .mapChunks(toFloatChunk)

      def output: Pipe[F, Float, Nothing] = stream => {
        // See https://github.com/typelevel/fs2/blob/d7637b419b58e696763b4776f62887a10421f541/io/shared/src/main/scala/fs2/io/io.scala#L112
        // We do basically the same for the SourceDataLine (does not inherit from OutputStream)
        Stream
          .bracket(F.delay {
            outputLine.open(AUDIO_FORMAT)
            outputLine.start()
            outputLine
          })(line => F.blocking(line.close()))
          .flatMap(line =>
            stream
              .chunkN(constants.FRAMES_PER_BUFFER, allowFewer = false)
              .map(toByteArray)
              .foreach { byteArray =>
                F.interruptible {
                  line.write(byteArray, 0, byteArray.length)
                  ()
                }
              } ++
              Stream.exec(F.blocking(line.flush()))
          )
      }

  private def toFloatChunk(chunk: Chunk[Byte]): Chunk[Float] =
    val intBuffer = IntBuffer.allocate(constants.FRAMES_PER_BUFFER)
    intBuffer.put(chunk.toByteBuffer.asIntBuffer)
    val floatArray = intBuffer.array.map(_.toFloat * FULL_SCALE_RECIPROCAL)
    Chunk.ArraySlice(floatArray, 0, floatArray.length)

  private def toByteArray(chunk: Chunk[Float]): Array[Byte] =
    val byteBuffer = ByteBuffer.allocate(BYTES_PER_BUFFER)
    val Chunk.ArraySlice(values, offset, length) = chunk.toArraySlice
    byteBuffer.asIntBuffer.put(
      values.map(float => (float * FULL_SCALE).toInt),
      offset,
      length
    )
    byteBuffer.array

  // Format reads/writes data according to the JVM Int encoding
  private val AUDIO_FORMAT = new AudioFormat(
    AudioFormat.Encoding.PCM_SIGNED,
    constants.SAMPLE_RATE, // sample rate
    32, // 32-bit int samples
    1, // 1 channel
    4, // frame size is 4 bytes per int × number of channels
    constants.SAMPLE_RATE, // frame rate same as sample rate
    true // big endian
  )

  // 4 bytes per int sample
  private val BYTES_PER_BUFFER = constants.FRAMES_PER_BUFFER * 4

  private val FULL_SCALE: Int = 32768
  private val FULL_SCALE_RECIPROCAL: Float = 1f / FULL_SCALE
