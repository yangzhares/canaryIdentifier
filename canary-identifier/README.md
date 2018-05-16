# Canary Identifier

Canary Identifier is inherited from Linkerd built-in HeaderTokenIdentifier, but it enables you setup canary deployment per service 
via a HTTP request Header `X-Service-Mesh-Canary` value. Additional, we add a new configuration option `domain`, by default, it's empty,
you need configure it when `Host` header includes domain, because most of situations, remove of the `domain` part, then get your service name, e.g. 

``` bash
curl -s -H "Host: nginx.service.consul" -H "X-Service-Mesh-Canary: nginx=enabled" localhost:14140
```
`domain` is `service.consul`, and service name is `nginx`.

Header `X-Service-Mesh-Canary` can be set as `X-Service-Mesh-Canary: ${servce_name}=${tag}` or `X-Service-Mesh-Canary: ${tag}`, `tag` can be `enabled` or `disabled`.
First configuration option will make the specified service as canary deployment, and second one will make all services as canary deployment. Via `X-Service-Mesh-Canary`
and the value of Header `header` configured by this Identifier, by default, `header` is `Host` , finally Identifier will generate Linkerd `service name` as 
`/${prefix}/${tag}/${header-value}`, e.g. `/svc/enabled/nginx.service.consul`.
 

## Building

This plugin is built with sbt.  Run sbt from the plugins directory.

```
# sbt assembly
```

This will produce the plugin jar at
`canary-identifier/target/scala-2.12/canaryIdentifier-assembly-0.1.jar`.

## Installing

To install this plugin with linkerd, simply move the plugin jar into linkerd's
plugin directory (`$L5D_HOME/plugins`).  Then add an identifier block to the
router in your linkerd config:

```
admin:
  port: 9990
  ip: 0.0.0.0

namers:
- kind: io.l5d.consul
  prefix: /io.l5d.consul
  host: consul
  port: 8500
  includeTag: true
  setHost: false
  useHealthCheck: true

routers:
- protocol: http
  identifier:
    kind: io.l5d.canary
    domain: service.consul
  dtab: |
    /disabled        => /#/io.l5d.consul/dc1/noncanary;
    /enabled         => 1 * /#/io.l5d.consul/dc1/canary & 9 * /#/io.l5d.consul/dc1/noncanary;
    /svc/disabled    => /$/io.buoyant.http.subdomainOfPfx/service.consul/disabled;
    /svc/enabled     => /$/io.buoyant.http.subdomainOfPfx/service.consul/enabled;
  servers:
  - ip: 0.0.0.0
    port: 4140

usage:
  enabled: false
```

## Verify
To verify this Identifier's function, we setup a few services via Docker and Docker-compose, and `docker-compose.yaml` as follow:

```yaml
version: '2'

services:
  consul:
    image: consul
    container_name: consul
    ports:
    - "8500:8500"

  registrator:
    image: gliderlabs/registrator
    container_name: registrator
    network_mode: host
    volumes:
    - /var/run/docker.sock:/tmp/docker.sock
    command: >
      -internal
      consul://localhost:8500
    depends_on:
    - consul

  nginx-noncanary:
    image: nginx
    container_name: nginx-noncanary
    environment:
      SERVICE_NAME: nginx
      SERVICE_TAGS: noncanary
    volumes:
    - ./www/index.html:/usr/share/nginx/html/index.html:ro


  nginx-canary:
      image: nginx
      container_name: nginx-canary
      environment:
        SERVICE_NAME: nginx
        SERVICE_TAGS: canary
      volumes:
      - ./www/canary.html:/usr/share/nginx/html/index.html:ro


  l5d:
    image: buoyantio/linkerd:1.3.6
    container_name: l5d
    ports:
    - "14140:4140"
    - "19990:9990"
    volumes:
    - ./linkerd.yaml:/io.buoyant/linkerd.yaml:ro
    - ../canary-identifier/target/scala-2.12/canaryIdentifier-assembly-0.1.jar:/io.buoyant/linkerd/1.3.6/plugins/canaryIdentifier-assembly-0.1.jar:ro
    command: >
      -log.level=DEBUG
      /io.buoyant/linkerd.yaml
```
This `docker-compose.yaml` will setup Consul, Registrator, Nginx and Linkerd. Then you can change to `docker` directory, execute the below command to
lanuch those services:

```bash
# docker-compose up -d
```

After launching those services, you can verify this Identifier works or not.

```bash
for i in {1..100} ; do curl -s -H "Host: nginx.service.consul" -H "X-Service-Mesh-Canary: nginx=enabled" localhost:14140|grep canary; done
Hey buddy, i'm canary Nginx service!!!
Hey buddy, i'm canary Nginx service!!!
Hey buddy, i'm canary Nginx service!!!
Hey buddy, i'm canary Nginx service!!!
Hey buddy, i'm canary Nginx service!!!
Hey buddy, i'm canary Nginx service!!!
Hey buddy, i'm canary Nginx service!!!
Hey buddy, i'm canary Nginx service!!!
Hey buddy, i'm canary Nginx service!!!
Hey buddy, i'm canary Nginx service!!!
Hey buddy, i'm canary Nginx service!!!
Hey buddy, i'm canary Nginx service!!!
```

the preceding output indicates about 10% requests forwarded to canary nginx service, great, it works, this is we want to get.
