import scala.util.Try

object Main extends App {
  case class User[F[_]](
                         name: F[String],
                         age: F[Int],
                         email: F[String]
                       )

  val UserTry : User[Try] = User(
    ConsoleStringReader.Run(scala.io.StdIn),
    ConsoleIntReader.Run(scala.io.StdIn),
    ConsoleStringReader.Run(scala.io.StdIn),
  )

  println("Получаем : " + UserTry)

  type Id[A] = A
  def ConvertToDefaultUser(user : User[Try]) : Option[User[Id]] = {
    def ConvertToTry(user : User[Try]) : Try[User[Id]] = for {
      name <- user.name
      age <- user.age
      email <- user.email
    } yield User[Id](name,age,email)

    ConvertToTry(user).toOption
  }

  println("Преобразуем в Option и получаем : " + ConvertToDefaultUser(UserTry))

}

trait Reader[A, B]:
  def Run(a: A): B


object ConsoleStringReader extends Reader[scala.io.StdIn.type,Try[String]]:
  def Run(in: scala.io.StdIn.type): Try[String] = Try(in.readLine())

object ConsoleIntReader extends Reader[scala.io.StdIn.type, Try[Int]]:
  def Run(in: scala.io.StdIn.type): Try[Int] = Try(in.readInt())
