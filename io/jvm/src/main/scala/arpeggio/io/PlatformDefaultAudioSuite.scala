package arpeggio
package io

import cats.effect.{Resource, Sync}

object PlatformDefaultAudioSuite:
  def resource[F[_]](using F: Sync[F]): Resource[F, AudioSuite[F]] =
    Resource.eval(jSound.JavaSoundAudioSuite.default)
