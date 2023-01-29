name := "reactivemongo-demo-app"

val buildVersion = "1.1.0-RC7"

version := buildVersion

resolvers ++= {
  Resolver.sonatypeOssRepos("snapshots") ++:
  Resolver.sonatypeOssRepos("staging")
}

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

scalaVersion := "3.2.1"

libraryDependencies ++= {
  val os = sys.props.get("os.name") match {
    case Some(osx) if osx.toLowerCase.startsWith("mac") =>
      "osx"

    case _ =>
      "linux"
  }

  val playVer = buildVersion.split("-").toList match {
    case major :: Nil =>
      s"${major}-play28"

    case vs @ _ => {
      val pv = ((vs.init :+ "play28") ++ vs.lastOption.toList)

      pv.mkString("-")
    }
  }

  Seq(
    guice,
    "org.reactivemongo" %% "play2-reactivemongo" % playVer,
    "org.reactivemongo" % s"reactivemongo-shaded-native-${os}-x86-64" % buildVersion
  )
}

libraryDependencies ~= {
  _.map { dep =>
    if (dep.organization == "com.typesafe.play") {
      dep.cross(CrossVersion.for3Use2_13)
    } else {
      dep
    }
  }
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
