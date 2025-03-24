## arpeggio

arpeggio provides audio processing functionality using fs2. It was created as an idea for a talk on implementing a pedalboard in Scala, which influences some of the nomenclature within the project. Slides for the aforementioned talk can be found [here](https://willbinzi.github.io/arpeggio-slides/).

It currently provides implementations of tremolo, overdrive, delay and reverb effects.

### Project setup
The project is compiled for JVM and native. The JVM version compiles much faster, but the native version has considerably lower latency.

See [here](https://scala-native.org/en/latest/user/setup.html) for instructions on how to set up a working Scala Native environment.

The `portaudio` module uses the [portaudio](https://www.portaudio.com/) C library for input and output. To manage this dependency, it uses [sn-vcpkg](https://github.com/indoorvivants/sn-vcpkg) which requires the dependencies mentioned [here](https://github.com/indoorvivants/sn-vcpkg?tab=readme-ov-file#docker-base-image).

### Special thanks
- Manfred R. Schroeder for the [reverb algorithm](https://hajim.rochester.edu/ece/sites/zduan/teaching/ece472/reading/Schroeder_1962.pdf) used in this project
- [Rishikesh Daoo](https://github.com/Rishikeshdaoo) for his [blog post](https://medium.com/the-seekers-project/coding-a-basic-reverb-algorithm-part-2-an-introduction-to-audio-programming-4db79dd4e325), which signposted me to the paper above and showed me how to work with the Java Sound API
