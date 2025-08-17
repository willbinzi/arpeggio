package arpeggio
package io

import cats.effect.{Concurrent, Resource, Sync}

object PlatformDefaultAudioInterface:
  def resource[F[_]](using
      F: Sync[F] with Concurrent[F]
  ): Resource[F, AudioInterface[F]] =
    portaudio.PortAudioAudioInterface.default
