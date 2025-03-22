package arpeggio
package io.portaudio

import cbindings.portaudio.aliases

import scala.scalanative.unsigned.UnsignedRichInt

object PaSampleFormat:
  val paFloat32 = aliases.PaSampleFormat(0x00000001.toULong)
