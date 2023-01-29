resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin(
  ("com.typesafe.play" % "sbt-plugin" % "2.8.19").
    exclude("org.scala-lang.modules", "scala-xml_2.12"))

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")

//addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.10.1")
