# Tutor Briefing — Scala Reading Course

Context for an agent tutoring a learner through `CheckoutService.scala` (exercise 2)
and `OrderPipeline.scala` (exercise 1). Read this before answering questions about
either exercise.

---

## 1. Learner profile

**Goal:** read and understand Scala at work. Not writing production Scala, not
mastery. The course was explicitly scoped to a few hours total.

**Codebase:** Scala 2.13. Confirmed by the presence of `foo._` wildcard imports
rather than `foo.*`. Backend services — queues, external service calls,
controllers. Likely `Future`-based; possibly Akka or Play. Cats/ZIO not confirmed.

**Background:**
- Strong Python.
- Strong OOP generally (classes, interfaces, inheritance, Java-style generics).
- Has seen OCaml but is **not** fluent in functional idioms. This matters. Do not
  assume familiarity with folds, currying, monads, point-free style, or the word
  "combinator."

**Environment:** M3 MacBook, macOS. Uses `scala-cli run --watch File.scala`.
Coursier is installed (after a `.bash_profile` permissions fix). Homebrew
`scala-cli` is the primary runner. `sbt` was covered conceptually only.

---

## 2. Teaching contract

The learner gave direct feedback mid-course that module 7 was failing them:

> "you're giving new definitions without contextualizing... not giving enough
> example to nail down points. you're dumping without any backing"

The correction that worked, and that should be maintained:

1. **Start from a problem they can see.** Show code that a reasonable person
   would write.
2. **Show it failing.** Actual compile error or actual limitation, concretely.
3. **Then introduce the notation** as the resolution of that specific failure.
4. **Name the thing last.** Vocabulary after mechanism, never before.

Additional preferences observed:
- Translate to Python or an imperative loop when it helps.
- One concept at a time; do not stack four mechanisms in one pass.
- They will say "move on" when ready. Don't pad.
- They ask sharp follow-ups when something doesn't add up (e.g. spotting `new` on
  what I'd called a singleton, asking how `yield Order` produces an `Either`).
  Treat these as signals that a prior explanation was incomplete, and fix the
  explanation rather than restating it.

**When they're stuck on a task:** ask what they tried, point at the specific
concept, give a smaller worked example of the same shape. Do not paste the
reference solution unless they ask for it. They explicitly said they'd rather have
their own code reviewed than be handed answers.

---

## 3. What the nine modules covered

Vocabulary and examples established. Safe to reference without re-explaining.

**Module 1 — File anatomy and syntax skeleton**
`package`/`import` placement, `val`/`var`/`def`/`lazy val`, types after the colon,
`def foo` vs `def foo()`, everything-is-an-expression (no `return`), `Unit` and
`()`, string interpolation (`s`, `f`, `raw`, triple-quote), the
`Any`/`AnyVal`/`AnyRef`/`Nothing` hierarchy, and how to tell Scala 2 from 3.

**Module 2 — Object model**
Constructor params in the class signature, bare param vs `val` param, class body
as constructor, `private[pkg]`, `case class` and its generated members,
`.copy(...)`, `object` as singleton, companion objects, `apply` as factory,
`sealed trait` + case classes as ADT, `case object`, `trait` as interface /
mixin / ADT root, `extends` vs `with`, self-types and the cake pattern (mentioned
as legacy), `override`, trait vs abstract class.

**Module 3 — Functions and punctuation**
Lambdas, `_` shorthand and its expansion boundary, method vs function value,
eta-expansion and `foo _`, infix method calls, operators as methods, precedence by
first character, right-associative `:`-ending methods (`::`, `+:`, `:+`),
parens vs braces, multiple parameter lists (inference + brace syntax), by-name
parameters `=> A`, default and named args, varargs and `: _*`, `update` and
`foo_=` setters.

**Module 3 addendum** — after the FP-assumption complaint, re-explained:
`foldLeft` as an accumulator loop, currying/partial application (and noted it's
rare in backend code), eta-expansion in plain terms, and `List`/`Nil`/`::`.

**Module 4 — Pattern matching**
`match` as an expression, type patterns (`case s: String`), case class
destructuring, nested patterns, guards, `@` binding, exhaustiveness via `sealed`,
**the lowercase-binds-instead-of-compares gotcha** and backtick fix, list and
tuple patterns, `unapply` and custom extractors, `{ case ... }` as a partial
function, `collect`, and patterns in `val`, `for`, and `catch`.

**Module 5 — Collections and for-comprehensions**
`List`/`Vector`/`Seq`/`Set`/`Map`, immutability by default and the explicit
`mutable.` import, `->` as pair constructor, the method vocabulary table
(`map`, `filter`, `exists`, `forall`, `find`, `head` vs `headOption`, `take`,
`drop`, `sortBy`, `zip`, `zipWithIndex`, `mkString`, `groupBy`, `collect`,
`partition`, `flatMap`), chaining style, **`for` desugaring rules**
(non-final `<-` → `flatMap`, final → `map`, no `yield` → `foreach`, `if` →
`withFilter`, `x = e` → binding), and the key idea that `for` works on anything
with `map`/`flatMap`.

**Module 6 — Option, Either, Try, Future**
The unifying table (what each means, when it short-circuits). `Option`: `Some`/
`None`, `getOrElse`, `fold`, `toRight`, `.get` as a smell. `Either`: `Right`
success / `Left` error, right-biased in 2.13, `type Result[A]` alias pattern,
controller use. `Try`: `Success`/`Failure`, `Try(expr)` as by-name, `recover`,
`toEither`, boundary use. `Future`: `ExecutionContext` as implicit,
**eager evaluation** and the sequential-vs-parallel consequence, `recover`/
`recoverWith`, `Future.sequence`/`traverse`, `Await` as test-only. Closed with
the "can't mix types in one `for`" limitation and `Future[Option[A]]` awkwardness
motivating `EitherT`/ZIO.

**Module 6 follow-ups** (learner-initiated, both worth reusing):
- Why `yield Order(...)` produces `Either[String, Order]` — because `yield` is the
  body of `map`, and `F[A].map(A => B) == F[B]`. The `<-` unwraps temporarily; the
  wrapper is reattached on the way out.
- What happens when futures depend on each other — you *can't* create them side by
  side, and `flatMap` defers the second expression, so dependency forces
  sequencing. Includes the accidental-serialization bug pattern.

**Module 7 — Implicits** (taught twice; the second version is the good one)
Four mechanisms, each with problem → failure → notation:
1. **Implicit parameters** — omitted at call site, matched *by type not name*, only
   the last param list, and **propagation** (worked through with
   `checkout`/`charge` and three options: pass explicitly, import `global`, or
   accept implicitly and let it flow).
2. **Type classes** — motivated by "can't add `extends Loggable` to `Int`."
   Built up: interface fails → parameterized trait with the value as argument →
   pass instance manually → mark both `implicit`. Vocabulary (type class,
   instance) introduced last. Instances in the companion. Context bound `[A: Show]`
   explained as *pure notation* for the implicit param list, with `implicitly[T]`
   as the retrieval. Real examples: `Ordering`, JSON `Encoder`.
3. **Extension methods** — `implicit class`, the wrap-and-call rewrite, the reading
   rule "impossible method call → extension method → find the import." Examples
   already seen: `->`, `5.seconds`, `1 to 10`.
4. **Implicit conversions** — `implicit def A => B`, why it's discouraged, Scala 3
   opt-in.
Plus: where the compiler looks (lexical scope, then companions of the types
involved), ambiguity errors, and the reading strategy.

**Module 8 — Types and generics**
Square-bracket type params. Variance motivated by "is `List[Dog]` a
`List[Animal]`?" — `+A` covariant (safe because immutable), `-A` contravariant
(function parameters), bare `A` invariant (`Array`). Upper bound `<:`, lower bound
`>:` and why covariance forces `def :+[B >: A]`. `type` aliases and abstract type
members / path-dependent types (mentioned, not drilled). `F[_]` as a type
constructor, `[F[_]: Monad]` as a context bound, tagless final named, with the
practical advice "substitute IO for F."

**Module 9 — Packages, imports, sbt**
Package declarations and chained `package` form, package objects as a hiding place
for implicits and aliases, import forms (single, wildcard `_`, selective, rename
`=>`, hide `{X => _, _}`), scoped/local imports, **imports as implicit scope
control**, then `build.sbt` as Scala code, `:=` / `++=`, `%%` vs `%` and the
Scala-version suffix, `% Test`, `project/` layout, `dependsOn`, and sbt commands.

---

## 4. What was NOT covered

Flag these as out of scope unless the learner brings them up:
- Cats, ZIO, Cats Effect beyond one-line mentions
- Akka, Play, http4s specifics
- Macros, reflection, `TypeTag`
- Scala 3 syntax beyond "how to recognize it"
- Concurrency primitives beyond `Future` (no `Promise`, no actors, no STM)
- Testing frameworks (ScalaTest/munit) — only `% Test` in build files
- Performance, JVM tuning, collection complexity beyond `List` vs `Vector`
- Writing idiomatic Scala. The whole course is reading-oriented.

---

## 5. Exercise 1 — `OrderPipeline.scala` (modules 1–5)

Parses a pipe-delimited event feed into an ADT, then aggregates. Nine tasks:
`Cents` with a smart-constructor companion, event ADT, custom `unapply` extractor,
`Array` pattern parser, `flatMap` over `Option`-returning parse, `collect` per
case, `foldLeft`/`groupBy`/`sortBy` aggregations, a for-comprehension join, and a
`Sink` trait with two implementations.

Expected values: 12 parsed events, 5 placed / 4 shipped / 2 cancelled,
total 72049, alice 3399, leaderboard `carol 45000, bob 23650, alice 3399`,
3 fulfilled totalling 48399, one orphan shipment `o-9999`.

Status unknown — the learner did not submit this one for review. Ask if relevant.

---

## 6. Exercise 2 — `CheckoutService.scala` (modules 6–8)

Validates checkout requests, then calls slow fake services. Eleven tasks, ordered
by dependency. Run with `scala-cli run CheckoutService.scala`.

### Fixture facts
- `customers`: alice (email `alice@example.com`, not vip), bob (no email, vip)
- `prices`: widget 2500, gizmo 13750, doohickey 899
- `inventory`: widget 10, gizmo 2, doohickey 0
- 7 requests → 2 succeed, 5 fail, each broken in exactly one way
- Successes: alice/widget/2 = 5000, bob/gizmo/2 = 27500. Total 32500.
- Failures in feed order: `InsufficientStock(gizmo,5,2)`, `UnknownCustomer(carol)`,
  `UnknownSku(sprocket)`, `BadQuantity(abc)`, `InsufficientStock(doohickey,1,0)`

### Task → module map

| Task | Concept | Module |
|---|---|---|
| 1 | error ADT, `type` alias | 2, 6, 8 |
| 2 | `Either`, `Option#toRight` | 6 |
| 3 | for-comprehension over `Either` | 5, 6 |
| 4 | `partitionMap` / `collect` | 4, 5 |
| 5 | for-comprehension over `Option` | 6 |
| 6 | `Try` at a throwing boundary | 6 |
| 7 | implicit params + propagation | 7 (mech 1) |
| 8 | `implicit class` extension method | 7 (mech 3) |
| 9 | type class + context bound | 7 (mech 2) |
| 10 | `Future` sequential / parallel / traverse | 6 |
| 11 | generic method with type param | 8 |

### Reference solutions

Do not volunteer these. Use them to diagnose.

```scala
// TASK 1
sealed trait CheckoutError
case class UnknownCustomer(id: String) extends CheckoutError
case class UnknownSku(sku: String) extends CheckoutError
case class BadQuantity(raw: String) extends CheckoutError
case class InsufficientStock(sku: String, wanted: Int, available: Int) extends CheckoutError

type Result[A] = Either[CheckoutError, A]

// TASK 2
def findCustomer(id: String): Result[Customer] =
  customers.get(id).toRight(UnknownCustomer(id))

def findPrice(sku: String): Result[Int] =
  prices.get(sku).toRight(UnknownSku(sku))

def parseQty(raw: String): Result[Int] =
  raw.toIntOption.toRight(BadQuantity(raw))

def checkStock(sku: String, qty: Int): Result[Int] = {
  val available = inventory.getOrElse(sku, 0)
  if (available >= qty) Right(qty)
  else Left(InsufficientStock(sku, qty, available))
}

// TASK 3
def validate(req: CheckoutRequest): Result[ValidOrder] =
  for {
    c <- findCustomer(req.customerId)
    p <- findPrice(req.sku)
    q <- parseQty(req.qtyRaw)
    _ <- checkStock(req.sku, q)
  } yield ValidOrder(c, req.sku, q, p * q)

// TASK 4
lazy val validated: List[Result[ValidOrder]] = requests.map(validate)
lazy val failures:  List[CheckoutError] = validated.collect { case Left(e)  => e }
lazy val successes: List[ValidOrder]    = validated.collect { case Right(v) => v }

// TASK 5
def emailDomain(customerId: String): Option[String] =
  for {
    c <- customers.get(customerId)
    e <- c.email
    d <- e.split('@').lift(1)
  } yield d

// TASK 6
def safeParse(s: String): Try[Int] = Try(legacyParse(s))

def describeFailure(t: Try[Int]): String = t match {
  case Success(n) => s"parsed $n"
  case Failure(e) => s"failed: ${e.getMessage}"
}
// fold variant: t.fold(e => s"failed: ${e.getMessage}", n => s"parsed $n")

// TASK 7
object Clock {
  implicit val system: Clock = new Clock {
    def nowMillis: Long = System.currentTimeMillis()
  }
}

def stamp(msg: String)(implicit c: Clock): String = s"[${c.nowMillis}] $msg"

def receiptFor(o: ValidOrder)(implicit c: Clock): Receipt =
  Receipt(o.sku, o.qty, o.totalCents, stamp(s"${o.sku} x${o.qty}"))

// TASK 8
implicit class IntOps(n: Int) {
  def cents: String = "$%d.%02d".format(n / 100, n % 100)
}

// TASK 9
object Describe {
  implicit val describeError: Describe[CheckoutError] = new Describe[CheckoutError] {
    def describe(a: CheckoutError): String = a match {
      case UnknownCustomer(id)              => s"unknown customer: $id"
      case UnknownSku(sku)                  => s"unknown sku: $sku"
      case BadQuantity(raw)                 => s"bad quantity: $raw"
      case InsufficientStock(sku, w, avail) => s"$sku: wanted $w, have $avail"
    }
  }

  implicit val describeOrder: Describe[ValidOrder] = new Describe[ValidOrder] {
    def describe(o: ValidOrder): String =
      s"${o.customer.name}: ${o.qty} x ${o.sku} = ${o.totalCents.cents}"
  }

  implicit val describeReceipt: Describe[Receipt] = new Describe[Receipt] {
    def describe(r: Receipt): String =
      s"receipt: ${r.qty} x ${r.sku} = ${r.totalCents.cents}"
  }
}

def render[A: Describe](a: A): String = implicitly[Describe[A]].describe(a)

// TASK 10
def checkoutSequential(o: ValidOrder): Future[String] =
  for {
    res <- reserveStock(o.sku, o.qty)
    chg <- chargeCard(o.customer.id, o.totalCents)
  } yield s"$res|$chg"

def checkoutParallel(o: ValidOrder): Future[String] = {
  val fRes  = reserveStock(o.sku, o.qty)   // both started before the for
  val fRate = loadTaxRate(o.sku)
  for {
    r <- fRes
    t <- fRate
  } yield s"$r|$t"
}

def reserveAll(orders: List[ValidOrder]): Future[List[String]] =
  Future.traverse(orders)(o => reserveStock(o.sku, o.qty))

// TASK 11
def firstSuccess[A](rs: List[Result[A]]): Option[A] =
  rs.collectFirst { case Right(a) => a }
// Answer to the comment question: [A] preserves the element type, so a caller
// gets Option[ValidOrder] rather than Option[Any] and doesn't have to cast.
```

### Known friction points

**Task 4 and `partitionMap`.** The task text suggests `validated.partitionMap(identity)`,
but the skeleton declares `failures` and `successes` as two separate `lazy val`s.
A tuple-destructuring `lazy val (f, s) = ...` is awkward and evaluates oddly. If
they try it and hit trouble, tell them to either use `collect` (which fits the
skeleton) or restructure into a single non-lazy `val (failures, successes)`.
Both are correct; the checks only inspect the results.

**Task 7 and the "ambiguity" stretch goal.** Stretch goal B claims adding a second
implicit `Clock` produces an ambiguity error. That is only true for two implicits
at the *same* priority level. In `main`, the local `implicit val testClock`
correctly wins over `Clock.system` in the companion, because lexical scope
outranks companion scope — no ambiguity, and this is the intended, working
behavior. If the learner tries the stretch goal and sees no error, that's why. To
actually produce ambiguity they need two implicits of the same type in the same
scope. Worth turning into a teaching moment about implicit priority.

**Task 11 and inference.** `firstSuccess(failures.map(Left(_)))` should infer
`A = Nothing` and compile, but the inference is a little tight. If it fails, the
fix is an ascription: `failures.map(e => Left(e): Result[ValidOrder])`.

**Task 9 SAM shorthand.** `implicit val d: Describe[CheckoutError] = { case ... }`
works in 2.12+ via single-abstract-method conversion. It's tidier but less
obvious. Show the explicit `new Describe[...] { ... }` form first; mention SAM only
if they ask why library code looks terser.

**Task 10 timing checks.** `checkoutParallel` asserts under 500ms and the services
sleep 300ms each. If they write it as a plain for-comprehension with the calls
inline, the value will be right and the timing check will fail. That failure *is*
the lesson — the eager-evaluation point from module 6. Point at where the
expressions are evaluated rather than at the `for` syntax.

**`.cents` before Task 8.** The harness runs the `.cents` checks first, so the file
won't get past the first three checks until Task 8 is done. This is intentional.

---

## 7. Suggested next steps after exercise 2

The learner's real goal is reading their own codebase. Once exercise 2 is done,
the highest-value move is opening one real file from work and reading it end to
end, looking up whatever stops them. Offer to walk through a pasted file. That is
better use of time than a third synthetic exercise.

If they want more structured practice instead, the gaps most worth filling for a
`Future`-based backend are: error handling patterns across a request lifecycle,
and recognizing `EitherT` / `OptionT` if their codebase uses Cats.
