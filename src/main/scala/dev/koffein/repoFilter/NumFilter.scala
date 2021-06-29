package dev.koffein.repoFilter

import jp.ac.titech.c.se.stein.core.Context
import jp.ac.titech.c.se.stein.Application
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import picocli.CommandLine
import picocli.CommandLine.Command

@Command(
  name = "NumFilter",
  description = Array(
    "filter commits by number.",
    "Note that every commit is not traced from ref."
  )
)
class NumFilter extends BaseFilter {
  @CommandLine.Option(
    names = Array("--max-num"),
    description = Array("max numbers of commits")
  )
  protected var maxNum: Int = Int.MaxValue

  var commitNum: Int = 0

  override def rewriteCommit(commit: RevCommit, c: Context): ObjectId = {
    commitNum += 1
    if (commitNum > maxNum) ObjectId.zeroId()
    else super.rewriteCommit(commit, c)
  }
}

object NumFilter {
  def main(args: Array[String]): Unit = {
    Application.execute(new NumFilter(), args)
  }
}
