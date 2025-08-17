package arpeggio

import arpeggio.io.PlatformDefaultAudioInterface
import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple:
  def run: IO[Unit] = PlatformDefaultAudioInterface
    .resource[IO]
    .use(interface =>
      interface.input
        .through(interface.output)
        .compile
        .drain
    )
