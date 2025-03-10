package arpeggio.io.portaudio.unsafe

import arpeggio.io.portaudio.PaSampleFormat
import cbindings.portaudio.aliases.PaStream
import cbindings.portaudio.functions
import cbindings.portaudio.structs.PaStreamParameters

import scala.scalanative.unsafe.{Ptr, Zone}

private def defaultInputStreamParams(using Zone): Ptr[PaStreamParameters] =
  val inputDevice = functions.Pa_GetDefaultInputDevice()
  PaStreamParameters(
    device = inputDevice,
    channelCount = 1,
    sampleFormat = PaSampleFormat.paFloat32,
    suggestedLatency =
      (!functions.Pa_GetDeviceInfo(inputDevice)).defaultLowInputLatency,
    hostApiSpecificStreamInfo = null
  )

private[portaudio] def openDefaultInputPaStream(): Ptr[PaStream] =
  // Zone is for allocating memory for stream parameters
  Zone(openPaStream(pInputStreamParams = Some(defaultInputStreamParams)))
