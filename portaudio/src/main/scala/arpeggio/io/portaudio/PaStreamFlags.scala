package arpeggio
package io.portaudio

import cbindings.portaudio.aliases

import scala.scalanative.unsigned.UnsignedRichInt

object PaStreamFlags:
  val paClipOff = aliases.PaStreamFlags(0x00000001.toULong)
