package arpeggio

import arpeggio.io.PlatformDefaultAudioSuite
import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple:
  def run: IO[Unit] = PlatformDefaultAudioSuite
    .resource[IO]
    .use(audioSuite =>
      audioSuite.input
        .through(audioSuite.output)
        .compile
        .drain
    )
