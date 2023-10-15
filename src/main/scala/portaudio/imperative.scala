package portaudio

import cats.effect.{ Resource, Sync }
import cats.syntax.functor.*
import fs2.{ Chunk, Pipe, Pull, Stream }
import portaudio.aliases.{ PaError, PaStream }
import portaudio.enumerations.PaErrorCode
import portaudio.structs.{ PaDeviceInfo, PaStreamParameters }

import scala.reflect.ClassTag
import scala.scalanative.runtime.Boxes
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.UnsignedRichInt

val paFloat32 = aliases.PaSampleFormat(0x00000001.toULong)
val paClipOff = aliases.PaStreamFlags(0x00000001.toULong)

val FRAMES_PER_BUFFER = 256

def zone[F[_]: Sync]: Resource[F, Zone]  =
  Resource.make[F, Zone](Sync[F].delay(Zone.open()))(z => Sync[F].delay(z.close()))

def unsafeOpenStream(
  ppStream: Ptr[Ptr[PaStream]],
  inputParams: Ptr[PaStreamParameters],
  outputParams: Ptr[PaStreamParameters]
  ): Ptr[PaStream] =
    val err: PaError = functions.Pa_OpenStream(
      ppStream,
      inputParams,
      outputParams,
      constants.SAMPLE_RATE,
      FRAMES_PER_BUFFER.toULong,
      paClipOff,
      null,
      null
    )

    if err != PaErrorCode.paNoError then
      throw new RuntimeException(s"Stream open terminated with exit code $err")
    val e: PaError = functions.Pa_StartStream(!ppStream)
    if e != PaErrorCode.paNoError then
      throw new RuntimeException(s"Stream start terminated with exit code $e")
    !ppStream

def closeStream[F[_]: Sync](pStream: Ptr[PaStream]): F[Unit] =
  Sync[F].delay {
    functions.Pa_StopStream(pStream)
    functions.Pa_CloseStream(pStream)
  }.void

def inputStreamPointer[F[_]: Sync](using zone: Zone): Resource[F, Ptr[PaStream]] =
  Resource.make[F, Ptr[PaStream]](Sync[F].delay {
    val inputDevice = functions.Pa_GetDefaultInputDevice()
    val inputLatency = (!functions.Pa_GetDeviceInfo(inputDevice)).defaultLowInputLatency
    val inputParams = PaStreamParameters(
      inputDevice,
      1,
      paFloat32,
      inputLatency,
      null
    )
    unsafeOpenStream(stackalloc(), inputParams, null)
  })(closeStream)

def outputStreamPointer[F[_]: Sync](using Zone): Resource[F, Ptr[PaStream]] =
  Resource.make[F, Ptr[PaStream]](Sync[F].delay {
    val outputDevice = functions.Pa_GetDefaultOutputDevice()
    val outputLatency = (!functions.Pa_GetDeviceInfo(outputDevice)).defaultLowOutputLatency
    val outputParams = PaStreamParameters(
      outputDevice,
      1,
      paFloat32,
      outputLatency,
      null
    )

    unsafeOpenStream(stackalloc(), null, outputParams)
  })(closeStream)

def inputR[F[_]](using Zone)(implicit F: Sync[F]): Resource[F, Stream[F, Float]] =
  inputStreamPointer.map(pStream =>
    val floatBuffer: Ptr[Float] = alloc[Float](FRAMES_PER_BUFFER)
    val byteBuffer: Ptr[Byte] = Boxes.boxToPtr(Boxes.unboxToPtr(floatBuffer))
    Pull.eval(F.blocking {
      functions.Pa_ReadStream(pStream, byteBuffer, FRAMES_PER_BUFFER.toULong)
      arrayChunk(floatBuffer, FRAMES_PER_BUFFER)
    }).flatMap(Pull.output).streamNoScope.repeat
  )

def outputR[F[_]](using Zone)(implicit F: Sync[F]): Resource[F, Pipe[F, Float, Nothing]] =
  outputStreamPointer.map(pStream =>
    // TODO: Does this really need to be in the global zone?
    val floatBuffer = alloc[Float](FRAMES_PER_BUFFER)
    _.chunks.foreach { chunk =>
      (0 until chunk.size).foreach(i =>
        floatBuffer(i) = chunk(i)
      )
      F.blocking {
        functions.Pa_WriteStream(pStream, Boxes.boxToPtr(Boxes.unboxToPtr(floatBuffer)), FRAMES_PER_BUFFER.toULong)
        ()
      }
    }
  )

def arrayChunk[O: Tag: ClassTag](pointer: Ptr[O], length: Int): Chunk[O] =
  val array = new Array[O](length)
  (0 until length).foreach(i =>
    array(i) = pointer(i)
  )
  Chunk.array(array)
