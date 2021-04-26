import java.nio.file.Path
import complete.DefaultParsers._

enablePlugins(ScalafmtPlugin)

val classFiles = taskKey[Seq[Path]]("The classfiles generated by compile")
classFiles := {
  val classes = (Compile / classDirectory).value.toGlob / ** / "*.class"
  fileTreeView.value.list(classes).map(_._1)
}
classFiles := classFiles.dependsOn(Compile / compile).value

val compileAndCheckNoClassFileUpdates = taskKey[Unit]("Checks that there are no class file updates")
compileAndCheckNoClassFileUpdates := {
  val current = (classFiles / outputFileStamps).value.toSet
  val previous = (classFiles / outputFileStamps).previous.getOrElse(Nil).toSet
  assert(current == previous, s"$current did not equal $previous")
}

val checkLastModified = inputKey[Unit]("Check the last modified time for a file")
checkLastModified := {
  (Space ~> OptSpace ~> matched(charClass(_ != ' ').+) ~ (Space ~> ('!'.? ~ Digit.+.map(
    _.mkString.toLong
  )))).parsed match {
    case (file, (negate, expectedLastModified)) =>
      val sourceFile = baseDirectory.value / "src" / "main" / "scala" / file
      val lastModified = IO.getModifiedTimeOrZero(sourceFile)
      println(s"$lastModified $expectedLastModified")
      negate match {
        case Some(_) => assert(lastModified != expectedLastModified)
        case None    => assert(lastModified == expectedLastModified)
      }
  }
}

val setLastModified = inputKey[Unit]("Set the last modified time for a file")
setLastModified := {
  val Seq(file, lm) = Def.spaceDelimited().parsed
  val sourceFile = baseDirectory.value / "src" / "main" / "scala" / file
  IO.setModifiedTimeOrFalse(sourceFile, lm.toLong)
}