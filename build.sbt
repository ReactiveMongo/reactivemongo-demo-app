name := "reactivemongo-demo-app"

val buildVersion = "1.1.0-RC22"

version := buildVersion

resolvers ++= Seq(
  "Central Testing repository" at "https://central.sonatype.com/api/v1/publisher/deployments/download",
  "Sonatype Snapshots" at "https://central.sonatype.com/repository/maven-snapshots/",
  Resolver.typesafeRepo("releases")
)

scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-feature",
  //"-Xfatal-warnings"
)

scalacOptions ++= {
  if (scalaBinaryVersion.value != "3") {
    Seq(
      "-target:jvm-1.8",
      "-Xlint",
      "-g:vars"
    )
  } else Seq.empty
}

scalaVersion := "2.13.18"

val akkaVersion = "2.6.21"

libraryDependencies ++= {
  val os = sys.props.get("os.name") match {
    case Some(osx) if osx.toLowerCase.startsWith("mac") =>
      "osx"

    case _ =>
      "linux"
  }

  val arch = if (sys.props("os.arch").contains("aarch64")) "aarch" else "x86"

  val playVer = buildVersion.split("-").toList match {
    case major :: Nil =>
      s"${major}-play29"

    case vs @ _ => {
      val pv = s"${vs.init.mkString}-play29" :: vs.lastOption.toList

      pv.mkString(".")
    }
  }

  Seq(
    guice,
    "javax.inject" % "javax.inject" % "1",
    "org.reactivemongo" %% "play2-reactivemongo" % playVer,
    "org.reactivemongo" % s"reactivemongo-shaded-native-$os-$arch-64" % buildVersion
  )
}

/* Scalafix
inThisBuild(
  List(
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )
)
 */

import play.sbt.routes.RoutesKeys

RoutesKeys.routesImport += "play.modules.reactivemongo.PathBindables._"

routesGenerator := InjectedRoutesGenerator

run / fork := true

lazy val root = (project in file(".")).enablePlugins(PlayScala)
