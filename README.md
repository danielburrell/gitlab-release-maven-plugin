# Quickstart

gitlab-release-maven-plugin lets you upload artifacts to gitlab and have them shown in the release section as part of your standard deployment workflow.

To use the plugin, go to your `settings.xml` (usually in `~/.m2/settings.xml`)

Edit the file and add the following servers snippet


```xml
<settings>
  ...

  <servers>
    <server>
      <id>gitlab</id> <!-- name this section so we can refer to it later -->
      <privateKey>private_access_token_from_user_settings</privateKey>  <!-- personal access token, (not your password!) -->
    </server>
  </servers>

  ...
</settings>
```

Configure your `maven-release-plugin` to invoke `deploy gitlab-release:gitlab-release` goals:

```xml
 <plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-release-plugin</artifactId>
    <version>2.5.3</version>
    <configuration>
        <!--updateDependencies>false</updateDependencies-->
        <checkModificationExcludes>
            <checkModificationExclude>pom.xml</checkModificationExclude>
        </checkModificationExcludes>
        <autoVersionSubmodules>true</autoVersionSubmodules>
        <useReleaseProfile>false</useReleaseProfile>
        <releaseProfiles>release</releaseProfiles>
        <goals>deploy gitlab-release:gitlab-release</goals>
    </configuration>
</plugin>
```

Then configure the `gitlab-release-maven-plugin` using the following snippet, substituting the configuration values as needed.

```xml
<plugin>
    <groupId>uk.co.solong</groupId>
    <artifactId>gitlab-release-maven-plugin</artifactId>
    <version>1.4</version>
    <configuration>
        <repo>myrepo</repo> <!-- This is the name of the repo https://mygitlab.solong.co.uk/myuser/myrepo -->
        <owner>myuser</owner> <!-- This is the name of the owner https://mygitlab.solong.co.uk/myuser/myrepo -->
        <serverId>gitlab</serverId> <!-- This corresponds to the settings.xml id (see above for a settings.xml snippet)-->
        <tag>${project.artifactId}-${project.version}</tag> <!-- an example tag format -->
        <artifacts>
            <artifact>
                <file>${build.directory}/testattach-${project.version}.jar</file>  <!-- the artifact to attach -->
                <label>something</label> <!-- the display text for the link -->
            </artifact>
            <artifact>
                <file>${build.directory}/different-${project.version}.jar</file>  <!-- another artifact to attach -->
                <label>something else</label> <!-- the display text for the link -->
            </artifact>
        </artifacts>
        <baseUrl>https://mygitlab.solong.co.uk</baseUrl> <!-- the base url of your gitlab instance -->
    </configuration>
</plugin>
```

Then release as normal (typically using something like this)

```
mvn release:clean release:prepare release:perform
```

The plugin will be invoked as part of this sequence during `release:perform` as the plugin has been configured to be invoked by the `maven-release-plugin`.

