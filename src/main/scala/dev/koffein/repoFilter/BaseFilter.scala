package dev.koffein.repoFilter

import jp.ac.titech.c.se.stein.core.{Context, RefEntry, RepositoryRewriter}
import jp.ac.titech.c.se.stein.core.Context.Key
import org.eclipse.jgit.lib.{Constants, ObjectId, Ref}
import org.eclipse.jgit.revwalk.{RevCommit, RevTag}

import scala.annotation.tailrec
import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.util.control.Exception._

class BaseFilter extends RepositoryRewriter {
  override def rewriteParents(parents: Array[ObjectId],
                              c: Context): Array[ObjectId] =
    // parent消したらコミットID変わるからちゃんと追わないといけないね
    parents
      .map(parent => Option(commitMapping.get(parent)))
      .collect { case Some(id) => id }

  override def rewriteTag(tag: RevTag, c: Context): ObjectId =
    if ((allCatch opt {
          target.parseAny(tag.getObject.getId, c)
        }).isDefined) super.rewriteTag(tag, c)
    else ObjectId.zeroId

  override def rewriteRefObject(id: ObjectId,
                                `type`: Int,
                                c: Context): ObjectId =
    `type` match {
      case Constants.OBJ_TAG =>
        Option(tagMapping.get(id))
          .getOrElse(rewriteTag(source.parseTag(id, c), c))
      case Constants.OBJ_COMMIT =>
        findExistingParent(source.walk(c).parseCommit(id), c)
      case _ => id
    }

  // sourceにはあるが、targetにはない → sourceの親を遡りつつ、targetに存在しないか見る
  // parentの数が1でない(rootあるいはmerge commitである)場合、探索を終了
  // だとdefault branchがうまくいかないね
  @tailrec final def findExistingParent(commit: RevCommit,
                                        c: Context): ObjectId = {
    if (commitMapping.containsKey(commit)) commitMapping.get(commit)
    else if (Option(commit.getParents).isEmpty || commit.getParentCount == 0)
      ObjectId.zeroId()
    // first parentを使う パースを挟まないとダメっぽい
    else findExistingParent(source.walk(c).parseCommit(commit.getParent(0)), c)
  }

  // remotesもやっちゃえ
  override def confirmStartRef(ref: Ref, c: Context): Boolean = true

  override protected def updateRefs(c: Context): Unit = {
    val symbolicRefs = new mutable.ArrayBuffer[Ref]()
    source
      .getRefs(c)
      .forEach(ref => {
        if (ref.isSymbolic) {
          symbolicRefs.addOne(ref)
        } else if (confirmUpdateRef(ref, c))
          updateRef(ref, c)
      })
    // symbolicは後回しにする
    symbolicRefs.foreach(updateRef(_, c))
  }

  override protected def rewriteRefEntry(entry: RefEntry,
                                         c: Context): RefEntry =
    if (entry.isSymbolic) {
      val newName = rewriteRefName(entry.name, c)

      val targetRef = c.getRef.getTarget
      val uc = c.`with`(Key.ref, targetRef)
      val newTarget = getRefEntry(new RefEntry(targetRef), uc).name
      // 変換した上で、target repositoryにそのrefがない場合、適当なやつに割り振る(HEAD以外消してもいいかもしれん)
      val targetRepoRefs = target.getRefs(c).asScala
      if (!targetRepoRefs.map(_.getName).contains(newTarget))
        new RefEntry(
          newName,
          targetRepoRefs
            .filter(_.getName.startsWith(Constants.R_REFS))
            .head
            .getName
        )
      else new RefEntry(newName, newTarget)
    } else super.rewriteRefEntry(entry, c)
}
