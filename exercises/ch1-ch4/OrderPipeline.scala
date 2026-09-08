//> using scala 2.13
import scala.collection.mutable.ArrayBuffer

object OrderPipeline {

  val rawFeed: List[String] = List(
    "PLACED|o-1001|alice|2500",
    "SHIPPED|o-1001|ups",
    "PLACED|o-1002|bob|13750",
    "CANCELLED|o-1002|out of stock",
    "PLACED|o-1003|alice|899",
    "HEARTBEAT",
    "SHIPPED|o-1003|fedex",
    "PLACED|o-1004|carol|45000",
    "SHIPPED|o-1004|ups",
    "PLACED|o-1005|bob|9900",
    "CANCELLED|o-1005|customer changed mind",
    "PLACED|o-1006|alice|not-a-number",
    "this line is garbage",
    "SHIPPED|o-9999|dhl"
  )

  // TASK 1
  class Cents private (val raw: Int) {
    def plus(other: Cents): Cents = new Cents(raw + other.raw)
    def format: String = {
      val dollars = raw / 100
      val cents = raw % 100
      // f"" (vs s"") lets each $var carry a printf-style spec: %02d = int, zero-padded to width 2.
      // $$ escapes a literal '$' since $ triggers interpolation.
      f"$$$dollars.$cents%02d"
    }
    override def toString: String = format
  }

  object Cents {
    val zero: Cents = new Cents(0)
    def apply(raw: Int): Option[Cents] = {
      if (raw<0) None else Some(new Cents(raw))
    }
  }

  // TASK 2
  // orderId lives on OrderEvent, not Event, so Heartbeat (which has no order) never
  // has to fake an answer. Both traits are sealed, so exhaustiveness checking still
  // works when matching at either level.
  sealed trait Event

  sealed trait OrderEvent extends Event{
    def orderId: String
  }

  case class Placed(orderId: String, customer: String, amount: Cents) extends OrderEvent 
  case class Shipped(orderId: String, carrier: String) extends OrderEvent
  case class Cancelled(orderId: String, reason: String) extends OrderEvent
  case object Heartbeat extends Event

  // TASK 3
  // unapply is what `case IntOf(n) => ...` calls under the hood: Some(v) means the
  // pattern matches (binds n = v), None means it doesn't. IntOf isn't a type String
  // belongs to - it's just an object with an unapply method (an "extractor"), which
  // is why this works with no relation to String in the type hierarchy.
  object IntOf {
    def unapply(s: String): Option[Int] = s.toIntOption
  }

  // TASK 4
  // split('|') takes a Char (literal split). split("|") takes a regex, where | means
  // "alternation" - splitting on that does something else entirely.
  // IntOf(cents) nests one extractor inside another: the Array pattern is checked first,
  // then IntOf.unapply runs on that one element.
  def parse(line: String): Option[Event] = line.split('|') match{
    // for { c <- Cents(cents) } yield ... is a single-generator for-comprehension, i.e.
    // just Cents(cents).map(c => Placed(...)) - it stays None if Cents(cents) is None
    // (negative amount), so a bad PLACED line still gets dropped instead of crashing.
    case Array("PLACED", id, customer, IntOf(cents))=> for {c <- Cents(cents)} yield Placed(id, customer, c)
    case Array("SHIPPED", id, carrier) => Some(Shipped(id, carrier))
    case Array("CANCELLED", id, reason) => Some(Cancelled(id, reason))
    case Array("HEARTBEAT") => Some(Heartbeat)
    case _ => None
  }

  // TASK 5
  // flatMap = map then flatten. Option acts like "a list of 0 or 1 elements" here:
  // Some(event) contributes one element, None contributes zero - so malformed lines
  // just vanish from the result instead of leaving gaps or crashing.
  lazy val events: List[Event] = {
    rawFeed.flatMap(e=>parse(e))
  }

  // TASK 6
  // collect takes a PartialFunction, which has isDefinedAt (can I handle this input?)
  // and apply (compute the result). collect checks isDefinedAt per element and keeps
  // only the elements it's defined for - filter+map in one pass, driven by the case.
  lazy val placed:    List[Placed]    = events.collect {case x: Placed=>x}
  lazy val shipped:   List[Shipped]   = events.collect {case x: Shipped=>x}
  lazy val cancelled: List[Cancelled] = events.collect {case x: Cancelled=>x}

  // TASK 7
  lazy val totalPlaced: Cents = placed.foldLeft(Cents.zero)((acc, next)=>acc.plus(next.amount))
  // Map.map hands you one (key, value) tuple argument, not two separate args - a plain
  // (customer, orders) => ... lambda is a Function2 and doesn't fit. case (customer, orders)
  // is a Function1 whose single argument is destructured; the pattern always matches a
  // 2-tuple, so this is a total function, not selectivity like Task 6's collect use of case.
  lazy val revenueByCustomer: Map[String, Cents] = placed.groupBy(_.customer).map {
    case (customer, orders) => customer -> orders.foldLeft(Cents.zero)((acc, next) => acc.plus(next.amount))
  }
  // _._2 = "take my one argument, read its 2nd tuple element" (shorthand for x => x._2).
  // sortBy is ascending by default; sortBy(-_._2) negates to get highest-first.
  lazy val leaderboard: List[(String, Int)] = revenueByCustomer.toList.map { case (name, cents) => (name, cents.raw) }.sortBy(-_._2)
  lazy val cancellationReasons: List[String] = cancelled.map(order => order.reason)

  // TASK 8
  lazy val fulfilled: List[(String, String, Int, String)] = for {
    p <- placed
    s <- shipped
    if s.orderId == p.orderId
  } yield (p.orderId, p.customer, p.amount.raw, s.carrier)

  lazy val orphanShipments: List[String] = {
    val placedSet = placed.map(order => order.orderId).toSet
    for {
      s <- shipped.filter(order => !placedSet.contains(order.orderId)).map(order => order.orderId)
    } yield s
  }

  // TASK 9
  trait Sink {
    def write(line: String): Unit
  }

  object ConsoleSink extends Sink{
    def write(line: String): Unit = {
      println(line)
    }
  }

  class MemorySink extends Sink{
    val buffer = new ArrayBuffer[String]()
    def write(line: String): Unit = {
      buffer.append(line)
    }
    def lines = buffer
  }

  def report(sink: Sink): Unit = {
    sink.write("hello")
  }

  def main(args: Array[String]): Unit = {
    println(Cents(2500).map(_.format))
  }
}

