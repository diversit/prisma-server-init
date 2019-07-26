[![Releases](https://img.shields.io/github/release/diversit/prisma-server-init?style=plastic)](https://github.com/diversit/prisma-server-init/releases) [![License](https://img.shields.io/github/license/diversit/prisma-server-init?style=plastic)](https://github.com/diversit/prisma-server-init/blob/master/LICENSE) [![Issues](https://img.shields.io/github/issues/diversit/prisma-server-init?style=plastic)](https://github.com/diversit/prisma-server-init/issues)
[![Docker build](https://img.shields.io/docker/cloud/build/diversit/prisma-server-init?style=plastic)](https://hub.docker.com/r/diversit/prisma-server-init)
[![Docker image tag](https://images.microbadger.com/badges/version/diversit/prisma-server-init.svg?style=plastic)](https://hub.docker.com/r/diversit/prisma-server-init)

# Prisma Server Init

**Goal:** 
A simple utility which can be run as an init-container to setup Prisma Server configuration
based on properties defined in a Secret.

## Reational

[Prisma](https://www.prisma.io) is a Database ORM exposing the database via a GraphQL interface.

For a project I wanted to run [Prisma Server](https://www.prisma.io/docs/prisma-server/) in Kubernetes. When creating a Helm Chart for starting Prisma, I did not wanted to expose secrets in a ConfigMap as suggested by some examples. Also I wanted to be able to change Prisma config settings via Helm's `--set <property>=<value>` so it is possible to change the Chart configuration for different CI environments from a CI script.

## Solution

Ideally I would like to create a Kubernetes Operator to run Prisma. However, since I had to create something quick, I created this tool instead to run as an init-container to setup the configuration for the Prisma Server.

This `prisma-server-init` tool:

- Reads settings from a Secret which is mounted as a volume in the init-container. (See `k8s/secret.yaml`)
- Creates a Prisma configuration file which can be stored in an `EmptyDir` which is shared with the Prisma Server container. (See `k8s/pod.yaml`)

The `prisma-servr-init` tool takes 2 arguments:

1. required - Path to the Secrets volume folder.
2. optional - Path to the Prisma config file to which to store the configuration. If this argument is ommitted, the tool outputs the configuration to the console.

The Prisma Server `PRISMA_CONFIG_PATH` environment variable **must** be set to the location of the config file in the `EmptyDir` volume. (see `k8s/pod.yaml`)

## Secret

These properties are currently supported in a Secret.

| properties | required? |
|:----------:|:---------:|
| managementApiSecret | no |
| apiPort | yes |
| connector | yes |
| host | yes |
| dbPort | yes |
| user | yes |
| password | yes |
| migrations | no |

## Prisma config

The config created by this tool:

```yaml
managementApiSecret: testManagementApiSecret
port: testApiPort
databases:
  default:
    connector: testConnector
    host: testHost
    port: testDbPort
    user: testuser
    password: testpassword
    migrations: testMigrations
```

# Build

## Local

This tool has been writting in Java (version 8) and compiled into native code using [GraalVM](http://graalvm.org/).

To create this tool locally:

- Install GraalVM locally and add the `bin` folder to your PATH.  
    E.g. download a GraalVM version and extract it locally.
    Use SDKMan: `sdk install java graalvm-ce <path-to-graalvm>/Content/Home`.
    Then 'use' this version as you would with any other Java version: `sdk use java graalvm-ce`.
- Install GraalVM native-image:  
    `gu install native-image`
- Build project locally:  
    `mvn test`
- Run _native-image_:  
    `native-image -cp target/classes eu.diversit.k8s.prisma.server.InitConfig prisma-server-init`

## Docker image

The Docker image uses GraalVM to create a native-image and then copies the `prisma-server-init` tool into a small `alpine` image.

# Running locally

Build the docker image.

- `docker build -t prisma-server-init:latest .`

- Use the Kubernetes files in `k8s` folder to:

  1. create a Secret
  2. Create a Pod which uses the init-container and starts a Prisma Server.

- Expose the Prisma Server via `kubectl forward-proxy pod/prisma 4466:4466`.

- Open a browser to http://localhost:4466.

_Note: if the `managementApiKey` property was set, to be able to access the Prisma server, the environment variable `PRISMA_MANAGEMENT_API_SECRET` to the value of the `managementApiKey`. Then `prisma token` can be used to generate a JWT token which can be used in the GraphQL Playground - HTTP Headers._

**GraphQL Playground - HTTP Headers**
```json
{
  "Authorization": "Bearer <your jwt token>"
}
```

- Deploy a database model

  - Install the Prisma tool. See the [Prisma website](https://prisma.io/).
  - Create a `prima.yaml` file

```yaml
endpoint: http://localhost:4466/ipos-api/develop
datamodel: datamodel.prisma
```

  - Define a `database.model`

  - Deploy the database model:  
    `prisma deploy`
