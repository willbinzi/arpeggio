package arpeggio

import arpeggio.io.PlatformDefaultAudioInterface
import arpeggio.pedals.reverb
import cats.effect.{IO, IOApp}

import scala.concurrent.duration.*

object Main extends IOApp.Simple:
  def run: IO[Unit] = PlatformDefaultAudioInterface
    .resource[IO]
    .use(interface =>
      interface.input
        .through(
          reverb.schroeder(predelay = 30.millis, decay = 1.second, mix = 0.7)
        )
        .through(interface.output)
        .compile
        .drain
    )
