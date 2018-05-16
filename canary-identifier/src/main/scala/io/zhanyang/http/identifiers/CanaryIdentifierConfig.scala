package io.zhanyang.http.identifiers

import com.fasterxml.jackson.annotation.JsonIgnore
import com.twitter.finagle.http.Fields
import com.twitter.finagle.{Dtab, Path}
import io.buoyant.linkerd.protocol.HttpIdentifierConfig

object CanaryIdentifierConfig {
  val kind = "io.l5d.canary"
  val defaultHeader = Fields.Host
}

case class CanaryIdentifierConfig(
  header: Option[String] = None,
  domain: Option[String] = None
) extends HttpIdentifierConfig {

  @JsonIgnore
  override def newIdentifier(
                              prefix: Path,
                              baseDtab: () => Dtab = () => Dtab.base
                            ) = CanaryIdentifier(
    prefix,
    header.getOrElse(CanaryIdentifierConfig.defaultHeader),
    domain.getOrElse(""),
    baseDtab
  )
}

