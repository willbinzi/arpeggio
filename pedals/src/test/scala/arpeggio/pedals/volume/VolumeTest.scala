package arpeggio
package pedals.volume

import fs2.Stream

class VolumeTest extends ArpeggioSuite:
  test("adjust volume"):
    assertApproxEqual(
      Stream(0.3f, 0.2f, 0.1f, 0f, -0.1f, -0.2f, -0.3f)
        .through(adjustLevel(0.1)),
      Stream(0.03f, 0.02f, 0.01f, 0f, -0.01f, -0.02f, -0.03f)
    )
