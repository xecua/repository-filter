package dev.koffein.repoFilter

import jp.ac.titech.c.se.stein.Application

import scala.sys.exit

object Main {
  def main(args: Array[String]): Unit =
    args(0) match {
      case "date" =>
        Application.execute(new DateFilter(), args.slice(1, args.length))
      case "num" =>
        Application.execute(new NumFilter(), args.slice(1, args.length))
      case _ =>
        Console.err.println(s"Invalid filter name: ${args(0)}")
        exit(1)
    }
}
