package dev.koffein.repoFilter

import jp.ac.titech.c.se.stein.core.Context
import jp.ac.titech.c.se.stein.Application
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import picocli.CommandLine
import picocli.CommandLine.Command

import java.time.ZonedDateTime

@Command(
  name = "DateFilter",
  description = Array("filter commits by timestamp")
)
class DateFilter extends BaseFilter {

  @CommandLine.Option(
    names = Array("--since"),
    description = Array(
      "time stamp of the time when the commits before will be ignored (exclusive)"
    )
  )
  protected var since: Long = 0

  // until、HEADの書き換えとかいるんじゃね?
  @CommandLine.Option(
    names = Array("--until"),
    description = Array(
      "time stamp of the time when the commits after will be ignored (exclusive)"
    )
  )
  protected var until: Long = ZonedDateTime.now().toEpochSecond

  override def rewriteCommit(commit: RevCommit, c: Context): ObjectId = {
    val commitTimeStamp = commit.getCommitTime
    if (commitTimeStamp < since || until < commitTimeStamp) ObjectId.zeroId()
    else super.rewriteCommit(commit, c)
  }
}

object DateFilter {
  def main(args: Array[String]): Unit = {
    Application.execute(new BaseFilter(), args)
  }
}
