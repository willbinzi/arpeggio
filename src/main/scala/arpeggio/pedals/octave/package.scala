package arpeggio
package pedals.octave

import cats.effect.Concurrent
import fs2.{Chunk, Stream}

def octaveDown[F[_]: Concurrent]: Pedal[F] =
  // def doCopy(targetArray1: Array[Float], targetArray2: Array[Float], arraySize: Int, index: Int, elem: Float): Unit =
  //   if index < arraySize then targetArray1(index) = elem
  //   else targetArray2(index - arraySize) = elem
  _.chunks
    .flatMap(chunk =>
      // Crudely halve the frequency of the signal by doubling every frame in place
      // We output two chunks for each chunk in the input
      val array1 = new Array[Float](chunk.size)
      val array2 = new Array[Float](chunk.size)
      inline def doCopy(index: Int, elem: Float): Unit =
        if index < chunk.size then array1(index) = elem
        else array2(index - chunk.size) = elem
      chunk.foreachWithIndex((elem, i) =>
        doCopy(2 * i, elem)
        doCopy(2 * i + 1, elem)
      )
      Stream.chunk(Chunk.array(array1)) ++ Stream.chunk(Chunk.array(array2))
    )
    .prefetch

def octaveUp[F[_]: Concurrent]: Pedal[F] =
  _.chunks
    .flatMap(chunk =>
      // Crudely double the frequency of the signal by only keeping every other frame in the chunk
      // Note that in the case of odd size chunks, 1 more than half of the frames in the chunk will be dropped
      // This technically can result in the frequency being more than doubled
      // The output chunk size is halved form that of the input
      val array = new Array[Float](chunk.size / 2)
      chunk.foreachWithIndex((elem, i) =>
        if i % 2 == 0 then array(i / 2) = elem
      )
      Stream.chunk(Chunk.array(array))
    )
    .prefetch
