scalaVersion := "2.13.8"
name := "prob_gen"
organization := "com.sri"
version := "1.0"
resolvers ++= Seq(
  "Sonatype OSS Snapshots" at "https://s01.oss.sonatype.org/content/repositories/public",
)
libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-native" % "4.0.3",
  "com.github.tototoshi" %% "scala-csv" % "1.3.6",
  "org.scalanlp" %% "breeze" % "2.0.1-RC1",
  "org.scalatest" %% "scalatest" % "3.0.8" % "test",
  "de.halcony" %% "scala-argparse" % "1.1.11"
)
