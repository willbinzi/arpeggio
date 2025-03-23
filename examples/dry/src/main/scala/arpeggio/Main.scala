package arpeggio

import arpeggio.io.portaudio.PortAudioAudioSuite
import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple:
  def run: IO[Unit] = PortAudioAudioSuite
    .default[IO]
    .use(audioSuite =>
      audioSuite.input
        .through(audioSuite.output)
        .compile
        .drain
    )
