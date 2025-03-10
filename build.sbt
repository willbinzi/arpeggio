val scala3Version = "3.3.1"

enablePlugins(VcpkgNativePlugin, ScalaNativePlugin, BindgenPlugin)

vcpkgDependencies := VcpkgDependencies(
  "portaudio"
)


import scala.scalanative.build._

nativeConfig ~= {
  _.withLTO(LTO.full)
  .withMode(Mode.release)
  .withGC(GC.commix)
}

vcpkgNativeConfig ~= {
  _.withRenamedLibraries(Map("portaudio" -> "portaudio-2.0"))
}

import bindgen.interface.Binding

bindgenBindings := {
  val configurator = vcpkgConfigurator.value
  Seq(
    Binding(
        configurator.includes("portaudio") / "portaudio.h",
        "cbindings.portaudio"
      )
      .withLinkName("portaudio")
  )
}

lazy val root = project
  .in(file("."))
  .settings(
    name := "arpeggio",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies += "org.typelevel" %%% "cats-effect" % "3.5.1",
    libraryDependencies += "co.fs2"        %%% "fs2-core"    % "3.9.2",
    scalafmtOnCompile := true,
    Compile / scalacOptions -= "-Xfatal-warnings"
  )
