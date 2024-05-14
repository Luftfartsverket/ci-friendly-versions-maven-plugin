# ci-friendly-versions-maven-plugin

## Description

This Maven plugin X

 <majorversion [> . <minorversion [> . <incrementalversion ] ] [> - <buildnumber | qualifier ]>


## Configuration

Configure this <configuration> block into the <build> section of  pom.xml file to customize how your plugin behaves during the Maven build process.

```xml
<build>
    <plugins>
        <plugin>
            <artifactId>ci-friendly-versions-maven-plugin</artifactId>
            <packaging>maven-plugin</packaging>
            <version>0.0.1-SNAPSHOT</version>
            <executions>
                <execution>
                    <goals>
                        <goal>set-version-from-tag</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

## Usage

To use the CI Friendly Versions Maven Plugin for command line

`mvn se.lfv.maven.plugins:ci-friendly-versions-maven-plugin:<plugin version>:set-version-from-tag`


## License

This project is licensed under the MIT License.

Plugin contains modified code ([VersionInformation](https://github.com/mojohaus/build-helper-maven-plugin/blob/master/src/main/java/org/codehaus/mojo/buildhelper/versioning/VersionInformation.java)) that is copyrighted by Karl Heinz Marbaise under MIT License.