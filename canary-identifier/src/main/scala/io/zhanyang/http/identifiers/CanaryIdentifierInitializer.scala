package io.zhanyang.http.identifiers

import io.buoyant.linkerd.IdentifierInitializer


class CanaryIdentifierInitializer extends IdentifierInitializer {
  override val configClass = classOf[CanaryIdentifierConfig]
  override val configId = CanaryIdentifierConfig.kind
}

object CanaryIdentifierInitializer extends CanaryIdentifierInitializer