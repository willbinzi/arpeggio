package arpeggio
package io.portaudio
package unsafe

import cbindings.portaudio.aliases.{PaError, PaStream}
import cbindings.portaudio.enumerations.PaErrorCode
import cbindings.portaudio.functions
import cbindings.portaudio.structs.PaStreamParameters

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.UnsignedRichInt

private def inputStreamParams(using Zone): Ptr[PaStreamParameters] =
  val inputDevice = functions.Pa_GetDefaultInputDevice()
  PaStreamParameters(
    device = inputDevice,
    channelCount = 1,
    sampleFormat = PaSampleFormat.paFloat32,
    suggestedLatency =
      (!functions.Pa_GetDeviceInfo(inputDevice)).defaultLowInputLatency,
    hostApiSpecificStreamInfo = null
  )

private def outputStreamParams(using Zone): Ptr[PaStreamParameters] =
  val outputDevice = functions.Pa_GetDefaultOutputDevice()
  PaStreamParameters(
    device = functions.Pa_GetDefaultOutputDevice(),
    channelCount = 1,
    sampleFormat = PaSampleFormat.paFloat32,
    suggestedLatency =
      (!functions.Pa_GetDeviceInfo(outputDevice)).defaultLowOutputLatency,
    hostApiSpecificStreamInfo = null
  )

private[portaudio] def openDefaultPaStream(): Ptr[PaStream] =
  Zone { implicit z => // Zone is for allocating memory for stream parameters
    // Pa_OpenStream will store a PaStream pointer in ppStream
    // We immediately load the PaStream pointer from ppStream and return it as the result of this method
    // ppStream itself is not needed beyond the lifetime of this method so we can use stackalloc
    val ppStream: Ptr[Ptr[PaStream]] = stackalloc()
    val errOpenStream: PaError = functions.Pa_OpenStream(
      stream = ppStream,
      inputParameters = inputStreamParams,
      outputParameters = outputStreamParams,
      sampleRate = constants.SAMPLE_RATE,
      framesPerBuffer = constants.FRAMES_PER_BUFFER.toULong,
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
  }
