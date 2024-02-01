# Redirect Importer

This extension provides the ability for Brightspot to import Vanity Redirects utilizing a google spreadsheet. 

* [Prerequisites](#prerequisites)
* [Installation](#installation)
* [Usage](#usage)
* [Documentation](#documentation)
* [Versioning](#versioning)
* [Contributing](#contributing)
* [Local Development](#local-development)
* [License](#license)

## Prerequisites

This extension requires an instance of [Brightspot](https://www.brightspot.com/) and access to the project source code.

The instance of Brightspot should have the following versions:

Brightspot: 4.5.15.8 or higher  

Java: 8 or higher

## Installation

Gradle:
```groovy
api 'com.brightspot:redirect-importer:1.0.0'
```

Maven:
```xml
<dependency>
    <groupId>com.brightspot</groupId>
    <artifactId>redirect-importer</artifactId>
    <version>1.0.0</version>
</dependency>
```

Substitute `1.0.0` for the desired version found on the [releases](https://github.com/brightspot/redirect-importer/tags) page.

## Usage
No specific instructions needed

## Documentation

- [Video Demo](https://www.brightspot.com/documentation/brightspot-cms-user-guide/redirect-importer-demo)
- [User Guide](https://www.brightspot.com/documentation/brightspot-cms-user-guide/redirect-importer)
- [Configuring the Google Drive integration](https://www.brightspot.com/documentation/brightspot-integrations-guide/latest/google-drive)
- [Authenticating with Google Drive](https://www.brightspot.com/documentation/brightspot-integrations-guide/configuring-the-google-drive-integration)
- [Javadocs](https://artifactory.psdops.com/public/com/brightspot/redirect-importer/%5BRELEASE%5D/redirect-importer-%5BRELEASE%5D-javadoc.jar!/index.html)

## Versioning

The version numbers for this extension will strictly follow [Semantic Versioning](https://semver.org/). The latest release can be found [here](https://github.com/brightspot/redirect-importer/tags).

## Contributing

If you have feedback, suggestions or comments on this open-source platform extension, please feel free to make them publicly on the issues tab [here](https://github.com/brightspot/redirect-importer/issues).

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

## Local Development

Assuming you already have a local Brightspot instance up and running, you can 
test this extension by running the following command from this project's root 
directory to install a `SNAPSHOT` to your local Maven repository:

```shell
./gradlew publishToMavenLocal
```

Next, ensure your project's `build.gradle` file contains 

```groovy
repositories {
    mavenLocal()
}
```

Then, add the following to your project's `build.gradle` file:

```groovy
dependencies {
    api 'com.brightspot:redirect-importer:1.0.0-SNAPSHOT'
}
```

Finally, compile your project and run your local Brightspot instance.
