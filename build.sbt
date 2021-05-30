name := "RouteSolver"

version := "1.0"

lazy val `routesolver` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/"

scalaVersion := "2.13.5"

libraryDependencies ++= Seq(ehcache, ws, specs2 % Test, guice)

dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.11.0"

libraryDependencies ++= Seq(
    "org.sangria-graphql" %% "sangria-play-json" % "2.0.1",
    "org.sangria-graphql" %% "sangria" % "2.1.3"
)
libraryDependencies ++= Seq(
    "com.h2database" % "h2" % "1.4.197",
    "mysql" % "mysql-connector-java" % "8.0.16",

    "com.typesafe.play" %% "play-slick" % "5.0.0",
    "com.typesafe.play" %% "play-slick-evolutions" % "5.0.0",
    "com.typesafe.slick" %% "slick" % "3.3.3",
    "com.typesafe.slick" %% "slick-hikaricp" % "3.3.3",

    "io.spray" %% "spray-json" % "1.3.5"

)

libraryDependencies ++= Seq(
    "com.pauldijou" %% "jwt-play" % "5.0.0",
    "com.pauldijou" %% "jwt-core" % "5.0.0",
    "com.auth0" % "jwks-rsa" % "0.6.1",


    "com.auth0" % "auth0" % "1.31.0"
)

libraryDependencies ++= Seq(
    "com.google.maps" % "google-maps-services" % "0.18.1"
)

libraryDependencies ++= Seq(
    "com.graphhopper" % "jsprit-core" % "1.9.0-beta.4"
)

unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")

      