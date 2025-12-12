import com.jsuereth.sbtpgp.PgpKeys.publishSigned
import com.typesafe.tools.mima.core.{Problem, ProblemFilters}
import commandmatrix.extra.*

// Used to configure the build so that it would format on compile during development but not on CI.
lazy val isCI = sys.env.get("CI").contains("true")
ThisBuild / scalafmtOnCompile := !isCI

// Used to publish snapshots to Maven Central.
val mavenCentralSnapshots = "Maven Central Snapshots" at "https://central.sonatype.com/repository/maven-snapshots"

// Versions:

val versions = new {
  // Versions we are publishing for.
  val scala212 = "2.12.21"
  val scala213 = "2.13.18"
  val scala3 = "3.3.7"

  // Which versions should be cross-compiled for publishing.
  val scalas = List(scala212, scala213, scala3)
  val platforms = List(VirtualAxis.jvm, VirtualAxis.js, VirtualAxis.native)

  // Dependencies.
  val kindProjector = "0.13.4"
  val munit = "1.2.1"
  val scalaCollectionCompat = "2.14.0"

  // Explicitly handle Scala 2 and Scala 3 separately.
  def fold2[A](scalaVersion: String)(for2: => Seq[A], for3: => Seq[A]): Seq[A] =
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, _)) => for2
      case Some((3, _)) => for3
      case _            => sys.error(s"Unsupported Scala version: $scalaVersion")
    }

  // Explicitly handle Scala 2.12, 2.13 and Scala 3 separately.
  def fold[A](scalaVersion: String)(for2_12: => Seq[A], for2_13: => Seq[A], for3: => Seq[A]): Seq[A] =
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, 12)) => for2_12
      case Some((2, 13)) => for2_13
      case Some((3, _))  => for3
      case _             => sys.error(s"Unsupported Scala version: $scalaVersion")
    }
}

// Development settings:

val dev = new {

  val props = scala.util
    .Using(new java.io.FileInputStream("dev.properties")) { fis =>
      val props = new java.util.Properties()
      props.load(fis)
      props
    }
    .get

  // Which version should be used in IntelliJ
  val ideScala = props.getProperty("ide.scala") match {
    case "2.12" => versions.scala212
    case "2.13" => versions.scala213
    case "3"    => versions.scala3
  }
  val idePlatform = props.getProperty("ide.platform") match {
    case "jvm"    => VirtualAxis.jvm
    case "js"     => VirtualAxis.js
    case "native" => VirtualAxis.native
  }

  def isIdeScala(scalaVersion: String): Boolean =
    CrossVersion.partialVersion(scalaVersion) == CrossVersion.partialVersion(ideScala)
  def isIdePlatform(platform: VirtualAxis): Boolean = platform == idePlatform
}

// Common settings:

Global / excludeLintKeys += git.useGitDescribe
Global / excludeLintKeys += ideSkipProject
val only1VersionInIDE =
  // For the platform we are working with, show only the project for the Scala version we are working with.
  MatrixAction
    .ForPlatform(dev.idePlatform)
    .Configure(
      _.settings(
        ideSkipProject := !dev.isIdeScala(scalaVersion.value),
        bspEnabled := dev.isIdeScala(scalaVersion.value),
        scalafmtOnCompile := !isCI
      )
    ) +:
    // Do not show in IDE and BSP projects for the platform we are not working with.
    versions.platforms.filterNot(dev.isIdePlatform).map { platform =>
      MatrixAction
        .ForPlatform(platform)
        .Configure(_.settings(ideSkipProject := true, bspEnabled := false, scalafmtOnCompile := false))
    }

val addScala213plusDir =
  MatrixAction
    .ForScala(v => (v.value == versions.scala213) || v.isScala3)
    .Configure(
      _.settings(
        Compile / unmanagedSourceDirectories += sourceDirectory.value.toPath.resolve("main/scala-2.13+").toFile,
        Test / unmanagedSourceDirectories += sourceDirectory.value.toPath.resolve("test/scala-2.13+").toFile
      )
    )

val settings = Seq(
  git.useGitDescribe := true,
  git.uncommittedSignifier := None,
  scalacOptions ++= versions.fold(scalaVersion.value)(
    for3 = Seq(
      // format: off
      "-encoding", "UTF-8",
      "-rewrite",
      "-source", "3.3-migration",
      // format: on
      "-unchecked",
      "-deprecation",
      "-explain",
      "-explain-types",
      "-feature",
      "-no-indent",
      "-Wconf:msg=Unreachable case:s", // suppress fake (?) errors in internal.compiletime
      "-Wconf:msg=Missing symbol position:s", // suppress warning https://github.com/scala/scala3/issues/21672
      "-Wnonunit-statement",
      // "-Wunused:imports", // import x.Underlying as X is marked as unused even though it is! probably one of https://github.com/scala/scala3/issues/: #18564, #19252, #19657, #19912
      "-Wunused:privates",
      "-Wunused:locals",
      "-Wunused:explicits",
      "-Wunused:implicits",
      "-Wunused:params",
      "-Wvalue-discard",
      "-Xfatal-warnings",
      "-Xcheck-macros",
      "-Ykind-projector:underscores"
    ),
    for2_13 = Seq(
      // format: off
      "-encoding", "UTF-8",
      "-release", "8",
      // format: on
      "-unchecked",
      "-deprecation",
      "-explaintypes",
      "-feature",
      "-language:higherKinds",
      "-Wconf:origin=scala.collection.compat.*:s", // type aliases without which 2.12 fail compilation but 2.13/3 doesn't need them
      "-Wconf:cat=scala3-migration:s", // silence mainly issues with -Xsource:3 and private case class constructors
      "-Wconf:cat=deprecation&origin=io.scalaland.chimney.*:s", // we want to be able to deprecate APIs and test them while they're deprecated
      "-Wconf:msg=The outer reference in this type test cannot be checked at run time:s", // suppress fake(?) errors in internal.compiletime (adding origin breaks this suppression)
      "-Wconf:src=io/scalaland/chimney/cats/package.scala:s", // silence package object inheritance deprecation
      "-Wconf:msg=discarding unmoored doc comment:s", // silence errors when scaladoc cannot comprehend nested vals
      "-Wconf:msg=Could not find any member to link for:s", // since errors when scaladoc cannot link to stdlib types or nested types
      "-Wconf:msg=Variable .+ undefined in comment for:s", // silence errors when there we're showing a buggy Expr in scaladoc comment
      "-Wconf:msg=a type was inferred to be kind-polymorphic `Nothing` to conform to:s", // silence warn that appeared after updating to Scala 2.13.18
      "-Wunused:patvars",
      "-Xfatal-warnings",
      "-Xlint:adapted-args",
      "-Xlint:delayedinit-select",
      "-Xlint:doc-detached",
      "-Xlint:inaccessible",
      "-Xlint:infer-any",
      "-Xlint:nullary-unit",
      "-Xlint:option-implicit",
      "-Xlint:package-object-classes",
      "-Xlint:poly-implicit-overload",
      "-Xlint:private-shadow",
      "-Xlint:stars-align",
      "-Xlint:type-parameter-shadow",
      "-Xsource:3",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-unused:locals",
      "-Ywarn-unused:imports",
      "-Ywarn-macros:after",
      "-Ytasty-reader"
    ),
    for2_12 = Seq(
      // format: off
      "-encoding", "UTF-8",
      "-target:jvm-1.8",
      // format: on
      "-unchecked",
      "-deprecation",
      "-explaintypes",
      "-feature",
      "-language:higherKinds",
      "-Wconf:cat=deprecation&origin=io.scalaland.chimney.*:s", // we want to be able to deprecate APIs and test them while they're deprecated
      "-Wconf:msg=The outer reference in this type test cannot be checked at run time:s", // suppress fake(?) errors in internal.compiletime (adding origin breaks this suppression)
      "-Wconf:src=io/scalaland/chimney/cats/package.scala:s", // silence package object inheritance deprecation
      "-Wconf:msg=discarding unmoored doc comment:s", // silence errors when scaladoc cannot comprehend nested vals
      "-Wconf:msg=Could not find any member to link for:s", // since errors when scaladoc cannot link to stdlib types or nested types
      "-Wconf:msg=Variable .+ undefined in comment for:s", // silence errors when there we're showing a buggy Expr in scaladoc comment
      "-Xexperimental",
      "-Xfatal-warnings",
      "-Xfuture",
      "-Xlint:adapted-args",
      "-Xlint:by-name-right-associative",
      "-Xlint:delayedinit-select",
      "-Xlint:doc-detached",
      "-Xlint:inaccessible",
      "-Xlint:infer-any",
      "-Xlint:nullary-override",
      "-Xlint:nullary-unit",
      "-Xlint:option-implicit",
      "-Xlint:package-object-classes",
      "-Xlint:poly-implicit-overload",
      "-Xlint:private-shadow",
      "-Xlint:stars-align",
      "-Xlint:type-parameter-shadow",
      "-Xlint:unsound-match",
      "-Xsource:3",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-inaccessible",
      "-Ywarn-infer-any",
      "-Ywarn-numeric-widen",
      "-Ywarn-unused:locals",
      "-Ywarn-unused:imports",
      "-Ywarn-macros:after",
      "-Ywarn-nullary-override",
      "-Ywarn-nullary-unit"
    )
  ),
  Compile / doc / scalacOptions ++= versions.fold2(scalaVersion.value)(
    for3 = Seq("-Ygenerate-inkuire"), // type-based search for Scala 3, this option cannot go into compile
    for2 = Seq.empty
  ),
  Compile / console / scalacOptions --= Seq("-Ywarn-unused:imports", "-Xfatal-warnings"),
  Test / compile / scalacOptions --= versions.fold(scalaVersion.value)(
    for2_12 = Seq("-Ywarn-unused:locals"), // Scala 2.12 ignores @unused warns
    for2_13 = Seq.empty,
    for3 = Seq.empty
  )
)

val dependencies = Seq(
  libraryDependencies ++= Seq(
    "org.scala-lang.modules" %%% "scala-collection-compat" % versions.scalaCollectionCompat,
    "org.scalameta" %%% "munit" % versions.munit % Test
  ),
  libraryDependencies ++= versions.fold2(scalaVersion.value)(
    for2 = Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
      compilerPlugin("org.typelevel" % "kind-projector" % versions.kindProjector cross CrossVersion.full)
    ),
    for3 = Seq.empty
  )
)

val versionSchemeSettings = Seq(versionScheme := Some("early-semver"))

val publishSettings = Seq(
  organization := "io.scalaland",
  homepage := Some(url("https://scalaland.io/chimney")),
  organizationHomepage := Some(url("https://scalaland.io")),
  licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/scalalandio/chimney-macro-commons/"),
      "scm:git:git@github.com:scalalandio/chimney-macro-commons.git"
    )
  ),
  startYear := Some(2017),
  developers := List(
    Developer("krzemin", "Piotr Krzemiński", "", url("https://github.com/krzemin")),
    Developer("MateuszKubuszok", "Mateusz Kubuszok", "", url("https://github.com/MateuszKubuszok"))
  ),
  pomExtra := (
    <issueManagement>
      <system>GitHub issues</system>
      <url>https://github.com/scalalandio/chimney-macro-commons/issues</url>
    </issueManagement>
  ),
  publishTo := {
    if (isSnapshot.value) Some(mavenCentralSnapshots)
    else localStaging.value
  },
  publishMavenStyle := true,
  Test / publishArtifact := false,
  pomIncludeRepository := { _ =>
    false
  },
  // Sonatype ignores isSnapshot setting and only looks at -SNAPSHOT suffix in version:
  //   https://central.sonatype.org/publish/publish-maven/#performing-a-snapshot-deployment
  // meanwhile sbt-git used to set up SNAPSHOT if there were uncommitted changes:
  //   https://github.com/sbt/sbt-git/issues/164
  // (now this suffix is empty by default) so we need to fix it manually.
  git.gitUncommittedChanges := git.gitCurrentTags.value.isEmpty,
  git.uncommittedSignifier := Some("SNAPSHOT")
)

val mimaSettings = Seq(
  mimaPreviousArtifacts := {
    val previousVersions = moduleName.value match {
      case "chimney-macro-commons" => Set() // add after RC-1 publish
      case _                       => Set()
    }
    previousVersions.map(organization.value %% moduleName.value % _)
  },
  mimaFailOnNoPrevious := false // true
)

val noPublishSettings =
  Seq(publish / skip := true, publishArtifact := false)

val ciCommand = (platform: String, scalaSuffix: String) => {
  val isJVM = platform == "JVM"

  val clean = Vector("clean")

  val projects = for {
    name <- Vector("chimneyMacroCommons")
  } yield s"$name${if (isJVM) "" else platform}$scalaSuffix"
  def tasksOf(name: String): Vector[String] = projects.map(project => s"$project/$name")

  val tasks = if (isJVM) {
    clean ++ tasksOf("compile") ++ tasksOf("test") ++ tasksOf("mimaReportBinaryIssues")
  } else {
    clean ++ tasksOf("test")
  }

  tasks.mkString(" ; ")
}

val publishLocalForTests = {
  val jvm = for {
    module <- Vector("chimneyMacroCommons")
    moduleVersion <- Vector(module, module + "3")
  } yield moduleVersion + "/publishLocal"
  val js = for {
    module <- Vector("chimneyMacroCommons").map(_ + "JS")
    moduleVersion <- Vector(module)
  } yield moduleVersion + "/publishLocal"
  jvm ++ js
}.mkString(" ; ")

val releaseCommand = (tag: Seq[String]) => if (tag.nonEmpty) "publishSigned ; sonaRelease" else "publishSigned"

// modules

lazy val root = project
  .in(file("."))
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(settings)
  .settings(publishSettings)
  .settings(noPublishSettings)
  .aggregate(chimneyMacroCommons.projectRefs *)
  .settings(
    moduleName := "chimney-macro-commons-build",
    name := "chimney-macro-commons-build",
    description := "Build setup for Chimney Macro Commons modules",
    logo :=
      s"""Chimney Macro Commons ${(version).value} build for (${versions.scala212}, ${versions.scala213}, ${versions.scala3}) x (Scala JVM, Scala.js $scalaJSVersion, Scala Native $nativeVersion)
         |
         |This build uses sbt-projectmatrix with sbt-commandmatrix helper:
         | - Scala JVM adds no suffix to a project name seen in build.sbt
         | - Scala.js adds the "JS" suffix to a project name seen in build.sbt
         | - Scala Native adds the "Native" suffix to a project name seen in build.sbt
         | - Scala 2.12 adds the suffix "2_12" to a project name seen in build.sbt
         | - Scala 2.13 adds no suffix to a project name seen in build.sbt
         | - Scala 3 adds the suffix "3" to a project name seen in build.sbt
         |
         |When working with IntelliJ or Scala Metals, edit "val ideScala = ..." and "val idePlatform = ..." within "val versions" in build.sbt to control which Scala version you're currently working with.
         |
         |If you need to test library locally in a different project, use publish-local-for-tests or manually publishLocal:
         | - chimney-macro-commons (obligatory)
         |for the right Scala version and platform (see projects task).
         |""".stripMargin,
    usefulTasks := Seq(
      sbtwelcome.UsefulTask("projects", "List all projects generated by the build matrix").noAlias,
      sbtwelcome
        .UsefulTask(
          "test",
          "Compile and test all projects in all Scala versions and platforms (beware! it uses a lot of memory and might OOM!)"
        )
        .noAlias,
      sbtwelcome
        .UsefulTask(releaseCommand(git.gitCurrentTags.value), "Publish everything to release or snapshot repository")
        .alias("ci-release"),
      sbtwelcome.UsefulTask(ciCommand("JVM", "3"), "CI pipeline for Scala 3 on JVM").alias("ci-jvm-3"),
      sbtwelcome.UsefulTask(ciCommand("JVM", ""), "CI pipeline for Scala 2.13 on JVM").alias("ci-jvm-2_13"),
      sbtwelcome.UsefulTask(ciCommand("JVM", "2_12"), "CI pipeline for Scala 2.12 on JVM").alias("ci-jvm-2_12"),
      sbtwelcome.UsefulTask(ciCommand("JS", "3"), "CI pipeline for Scala 3 on Scala JS").alias("ci-js-3"),
      sbtwelcome.UsefulTask(ciCommand("JS", ""), "CI pipeline for Scala 2.13 on Scala JS").alias("ci-js-2_13"),
      sbtwelcome.UsefulTask(ciCommand("JS", "2_12"), "CI pipeline for Scala 2.12 on Scala JS").alias("ci-js-2_12"),
      sbtwelcome.UsefulTask(ciCommand("Native", "3"), "CI pipeline for Scala 3 on Scala Native").alias("ci-native-3"),
      sbtwelcome
        .UsefulTask(ciCommand("Native", ""), "CI pipeline for Scala 2.13 on Scala Native")
        .alias("ci-native-2_13"),
      sbtwelcome
        .UsefulTask(ciCommand("Native", "2_12"), "CI pipeline for Scala 2.12 on Scala Native")
        .alias("ci-native-2_12"),
      sbtwelcome
        .UsefulTask(
          publishLocalForTests,
          "Publishes all Scala 2.13 and Scala 3 JVM artifacts to test snippets in documentation"
        )
        .alias("publish-local-for-tests")
    )
  )

lazy val chimneyMacroCommons = projectMatrix
  .in(file("chimney-macro-commons"))
  .someVariations(versions.scalas, versions.platforms)((addScala213plusDir +: only1VersionInIDE) *)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .disablePlugins(WelcomePlugin)
  .settings(
    moduleName := "chimney-macro-commons",
    name := "chimney-macro-commons",
    description := "Utilities for writing cross-platform macro logic"
  )
  .settings(settings *)
  .settings(versionSchemeSettings *)
  .settings(publishSettings *)
  .settings(dependencies *)
  .settings(mimaSettings *)

//when having memory/GC-related errors during build, uncommenting this may be useful:
Global / concurrentRestrictions := Seq(
  Tags.limit(Tags.Compile, 2) // only 2 compilations at once
)
