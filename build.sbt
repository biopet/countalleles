organization := "com.github.biopet"
organizationName := "Biopet"

startYear := Some(2016)

name := "CountAlleles"
biopetUrlName := "countalleles"

biopetIsTool := true

mainClass in assembly := Some(s"nl.biopet.tools.countalleles.CountAlleles")

developers += Developer(id = "ffinfo",
                        name = "Peter van 't Hof",
                        email = "pjrvanthof@gmail.com",
                        url = url("https://github.com/ffinfo"))

scalaVersion := "2.11.12"

libraryDependencies += "com.github.biopet" %% "tool-utils" % "0.6"
libraryDependencies += "com.github.biopet" %% "ngs-utils" % "0.6"
libraryDependencies += "com.github.biopet" %% "tool-test-utils" % "0.3"
