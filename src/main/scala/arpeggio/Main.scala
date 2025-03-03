package arpeggio

import arpeggio.io.portaudio.PortAudioAudioSuite
import arpeggio.pedals.reverb
import cats.effect.{IO, IOApp}

import scala.concurrent.duration.*

object Main extends IOApp.Simple:
  def run: IO[Unit] = PortAudioAudioSuite
    .default[IO]
    .use(audioSuite =>
      audioSuite.input
        .through(
          reverb.schroeder(predelay = 30.millis, decay = 1.second, mix = 1)
        )
        .through(audioSuite.output)
        .compile
        .drain
    )
