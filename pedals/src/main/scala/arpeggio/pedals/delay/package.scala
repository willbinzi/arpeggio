package arpeggio
package pedals.delay

import arpeggio.concurrent.UnthrottledChunkedTopic.*
import arpeggio.constants.SAMPLE_RATE
import arpeggio.pedals.volume.adjustLevel
import arpeggio.routing.parallel
import cats.effect.Concurrent
import fs2.{Pure, Stream}
import cats.syntax.semigroup.*

import scala.concurrent.duration.Duration

def silence(time: Duration): Stream[Pure, Float] =
  val timeInFrames = time.toMicros * SAMPLE_RATE / 1000000
  Stream.constant(0f).take(timeInFrames.toLong)

def delayLine[F[_]](time: Duration): Pedal[F] =
  silence(time) ++ _

def echo[F[_]: Concurrent](
    repeatGain: Float,
    delayTime: Duration
): Pedal[F] =
  parallel(
    identity,
    echoRepeats(repeatGain, delayTime).andThen(adjustLevel(repeatGain))
  )

def echoRepeats[F[_]: Concurrent](
    repeatGain: Float,
    delayTime: Duration
): Pedal[F] = stream =>
  for {
    topic <- Stream.eval(UnthrottledChunkedTopic[F, Float])
    outStream <- Stream.resource(topic.subscribeAwait)
    feedbackStream <- Stream.resource(
      topic.subscribeAwait.map(adjustLevel(repeatGain))
    )
    out <- outStream
      .concurrently(
        (stream |+| feedbackStream)
          .through(delayLine(delayTime))
          .through(topic.publish)
      )
  } yield out
