package part1_recap

object ScalaRecap extends App {

  // OO features of Scala
  class Animal
  trait Carnivore {
    def eat(a: Animal): Unit
  }

  object Carnivore

  // method notation
  1 + 2 // infix notation
  1.+(2)

  // FP
  val anIncrementer: Int => Int = (x: Int) => x + 1

  anIncrementer(1)

  List(1, 2, 3).map(anIncrementer)

  // Monads: Option, Try

  // type aliases
  type AkkaReceive = PartialFunction[Any, Unit]

  def receive: AkkaReceive = {
    case 1 => print("hello")
    case _ => print("confused")
  }

  // Implicits
  implicit val timeout: Int = 3000
  def setTimeout(f: () => Unit)(implicit timeout: Int) = f()

  setTimeout(() => print("timeout"))

  // conversions
  // 1. Implicit methods
  case class Person(name: String) {
    def greet: String = s"Hi, $name"
  }

  implicit def stringToPerson(name: String) = Person(name)
  "Peter".greet
  // stringToPerson("Peter").greet

  // implicit classes

  // implicit organizations
  // local scope
  // imported scope
  // companion objects

}
