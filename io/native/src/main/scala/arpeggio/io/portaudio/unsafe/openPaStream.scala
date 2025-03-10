package arpeggio
package io.portaudio
package unsafe

import cbindings.portaudio.aliases.{PaError, PaStream}
import cbindings.portaudio.enumerations.PaErrorCode
import cbindings.portaudio.functions
import cbindings.portaudio.structs.PaStreamParameters

import scala.scalanative.unsafe.{stackalloc, Ptr}
import scala.scalanative.unsigned.UnsignedRichInt

private[portaudio] def openPaStream(
    pInputStreamParams: Option[Ptr[PaStreamParameters]] = None,
    pOutputStreamParams: Option[Ptr[PaStreamParameters]] = None
): Ptr[PaStream] =
  // Pa_OpenStream will store a PaStream pointer in ppStream
  // We immediately load the PaStream pointer from ppStream and return it as the result of this method
  // ppStream itself is not needed beyond the lifetime of this method so we can use stackalloc
  val ppStream: Ptr[Ptr[PaStream]] = stackalloc()
  val errOpenStream: PaError = functions.Pa_OpenStream(
    stream = ppStream,
    inputParameters = pInputStreamParams.getOrElse(null),
    outputParameters = pOutputStreamParams.getOrElse(null),
    sampleRate = constants.SAMPLE_RATE,
    framesPerBuffer = constants.FRAMES_PER_BUFFER.toCSize,
    streamFlags = PaStreamFlags.paClipOff,
    streamCallback = null,
    userData = null
  )

  if errOpenStream != PaErrorCode.paNoError then
    throw new RuntimeException(
      s"Stream open terminated with exit code $errOpenStream"
    )
  val errStartStream: PaError = functions.Pa_StartStream(!ppStream)
  if errStartStream != PaErrorCode.paNoError then
    throw new RuntimeException(
      s"Stream start terminated with exit code $errStartStream"
    )

  !ppStream
