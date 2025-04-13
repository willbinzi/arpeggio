package arpeggio
package io

import cats.effect.{Concurrent, Resource, Sync}

object PlatformDefaultAudioSuite:
  def resource[F[_]](using
      F: Sync[F] with Concurrent[F]
  ): Resource[F, AudioSuite[F]] =
    portaudio.PortAudioAudioSuite.default
