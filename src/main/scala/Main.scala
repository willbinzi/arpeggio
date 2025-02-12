import cats.effect.{IO, ResourceApp, Resource}

object Main extends ResourceApp.Simple:
  def run: Resource[IO, Unit] = for {
    drive <- pedals.overdrive.blended[IO](0.7, 0.1)
    reverb <- pedals.reverbR[IO](0.7, 0.1)
    audioSuite <- io.AudioSuite.default[IO]
    _ <-
      audioSuite.input
        .through(drive)
        .through(reverb)
        .through(audioSuite.output)
        .compile
        .drain
        .toResource
  } yield ()
