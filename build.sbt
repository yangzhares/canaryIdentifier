
def twitterUtil(mod: String) =
  "com.twitter" %% s"util-$mod" %  "6.45.0"

def finagle(mod: String) =
  "com.twitter" %% s"finagle-$mod" % "6.45.0"

def linkerd(mod: String) =
  "io.buoyant" %% s"linkerd-$mod" % "1.3.6"

val canaryIdentifier =
  project.in(file("canary-identifier")).
    settings(
      inThisBuild(List(
        organization := "io.zhanyang",
        scalaVersion := "2.12.1",
        version      := "0.1"
      )),
      name := "canaryIdentifier",
      resolvers ++= Seq(
        "twitter" at "https://maven.twttr.com",
        "local-m2" at ("file:" + Path.userHome.absolutePath + "/.m2/repository")
      ),
      libraryDependencies ++=
        finagle("http") % "provided" ::
        twitterUtil("core") % "provided" ::
        linkerd("core") % "provided" ::
        linkerd("protocol-http") % "provided" ::
        Nil,
      assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)
    )
