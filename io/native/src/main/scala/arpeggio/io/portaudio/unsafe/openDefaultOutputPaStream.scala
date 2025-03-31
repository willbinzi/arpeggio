package arpeggio.io.portaudio.unsafe

import arpeggio.io.portaudio.PaSampleFormat
import cbindings.portaudio.aliases.PaStream
import cbindings.portaudio.functions
import cbindings.portaudio.structs.PaStreamParameters

import scala.scalanative.unsafe.{Ptr, Zone}

private def defaultOutputStreamParams(using Zone): Ptr[PaStreamParameters] =
  val outputDevice = functions.Pa_GetDefaultOutputDevice()
  PaStreamParameters(
    device = functions.Pa_GetDefaultOutputDevice(),
    channelCount = 1,
    sampleFormat = PaSampleFormat.paFloat32,
    suggestedLatency =
      (!functions.Pa_GetDeviceInfo(outputDevice)).defaultLowOutputLatency,
    hostApiSpecificStreamInfo = null
  )

private[portaudio] def openDefaultOutputPaStream(): Ptr[PaStream] =
  Zone { implicit z => // Zone is for allocating memory for stream parameters
    openPaStream(pOutputStreamParams = Some(defaultOutputStreamParams))
  }
