package arpeggio
package io

import cats.effect.{Resource, Sync}

object PlatformDefaultAudioInterface:
  def resource[F[_]](using F: Sync[F]): Resource[F, AudioInterface[F]] =
    Resource.eval(jSound.JavaSoundAudioInterface.default)
