package arpeggio
package boxing

import scala.scalanative.runtime.Boxes
import scala.scalanative.unsafe.Ptr

extension (pFloat: Ptr[Float])
  def toBytePointer: Ptr[Byte] =
    Boxes.boxToPtr(Boxes.unboxToPtr(pFloat))
