package io.zhanyang.http.identifiers

import com.twitter.finagle.http.{Fields, Request}
import com.twitter.finagle.{Dtab, Path}
import com.twitter.finagle.buoyant.Dst
import com.twitter.util.{Future, Try}
import io.buoyant.router.RoutingFactory
import io.buoyant.router.RoutingFactory.{IdentifiedRequest, RequestIdentification, UnidentifiedRequest}

case class CanaryIdentifier(
  prefix: Path,
  header: String,
  domain: String,
  baseDtab: () => Dtab = () => Dtab.base
) extends RoutingFactory.Identifier[Request]{

  val ALLOWED_OPTIONS = List("enabled", "disabled")

  private[this] def mkPath(path: Path): Dst.Path = {
    Dst.Path(prefix ++ path, baseDtab(), Dtab.local)
  }

  /**
    * extractCanaryOption extracts the service specified by headerValue if enable canary
    * deployment or not
    * @param headerValue is the service name
    * @param option specifies if enable canary deployment of not
    * @return
    */
  def extractCanaryOption(headerValue: String, option: String): String = {
    /**
      * remove of domain from service name when headerValue includes domain
      * e.g nginx.service.consul, will remove of service.consul
      */
    var svc = ""
    if (headerValue.endsWith(domain)) {
      val index = headerValue.indexOf(domain)
      svc = headerValue.substring(0, index-1)
    } else {
      svc = headerValue
    }

    if (option contains '=') {
      val svcAndTag = option.split("=")
      if (svc.equals(svcAndTag(0))) {
        if (ALLOWED_OPTIONS contains svcAndTag(1)) {
          return svcAndTag(1)
        }
      }
    } else {
      if (ALLOWED_OPTIONS contains option) {
        return option
      }
    }

    "disabled"
  }

  def apply(req: Request): Future[RequestIdentification[Request]] = {
    req.headerMap.get(header) match {
      case None | Some("") =>
        Future.value(new UnidentifiedRequest(s"$header header is absent"))
      case Some(value) =>
        val identified = Try {
          val option = req.headerMap.getOrElse("X-Service-Mesh-Canary", "disabled")
          val tag = extractCanaryOption(value, option)

          /**
            * build Linkerd service name with tag and Header value like
            * /svc/enabled/nginx.service.consul
            */
          val dst = mkPath(Path.Utf8(tag, value))
          new IdentifiedRequest(dst, req)
        }
        Future.const(identified)

    }
  }
}

object CanaryIdentifier {
  def default(prefix: Path, header: String, domain: String, baseDtab: () => Dtab = () => Dtab.base): CanaryIdentifier =
    new CanaryIdentifier(prefix, Fields.Host, "", baseDtab)
}