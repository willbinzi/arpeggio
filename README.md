## Arpeggio

Arpeggio provides audio processing functionality using FS2. It was created as an idea for a talk on implementing a pedalboard in Scala, which influences some of the nomenclature within the project. Slides for the aforementioned talk can be found [here](https://willbinzi.github.io/arpeggio-slides/).

It currently provides implementations of tremolo, overdrive, delay and reverb effects.

### Project setup
The project is compiled for JVM and native. The native version takes a considerable time to link, and is single threaded (until Cats Effect can be updated to Scala Native 0.5) but the input latency is considerably reduced.

See [here](https://scala-native.org/en/latest/user/setup.html) for instructions on how to set up a working Scala Native environment.

The native version of the `io` module uses the [portaudio](https://www.portaudio.com/) C library for input and output. To manage this dependency, it uses [sn-vcpkg](https://github.com/indoorvivants/sn-vcpkg) which requires the dependencies mentioned [here](https://github.com/indoorvivants/sn-vcpkg?tab=readme-ov-file#docker-base-image).

### Overview
Arpeggio models an audio stream as an `fs2.Stream[F, Float]`. The audio can then be modified by applying instances of `fs2.Pipe[F, Float, Float]` (aliased to `Pedal[F]` within the project).

An audio stream that reads from the default audio device, along with an output sink that plays audio can be obtained using `PlatformDefaultAudioSuite` from the `io` module.

We can then run the audio from the input device, through a pedal (here the Schroeder reverb implementation included in the `pedals` module) and through the output device like so:

```scala
import arpeggio.io.PlatformDefaultAudioSuite
import arpeggio.pedals.reverb
import cats.effect.{IO, IOApp}

import scala.concurrent.duration.*

object Main extends IOApp.Simple:
  def run: IO[Unit] = PlatformDefaultAudioSuite
    .resource[IO]
    .use(audioSuite =>
      audioSuite.input
        .through(
          reverb.schroeder(predelay = 30.millis, decay = 1.second, mix = 0.7)
        )
        .through(audioSuite.output)
        .compile
        .drain
    )
```
The above example is included in the project and can be run with:
```
sbt "exampleReverb/run"
```
Or, to run the JVM version:
```
sbt "exampleReverbJVM/run"
```

### Special thanks
- Manfred R. Schroeder for the [reverb algorithm](https://hajim.rochester.edu/ece/sites/zduan/teaching/ece472/reading/Schroeder_1962.pdf) used in this project
- [Rishikesh Daoo](https://github.com/Rishikeshdaoo) for his [blog post](https://medium.com/the-seekers-project/coding-a-basic-reverb-algorithm-part-2-an-introduction-to-audio-programming-4db79dd4e325), which signposted me to the paper above and showed me how to work with the Java Sound API
