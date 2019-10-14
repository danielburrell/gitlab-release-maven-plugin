package uk.co.solong.gitlabrelease;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class GitLabReleaseValidationTest {

    @Test
    public void shouldValidateScmCorrectly() throws MojoExecutionException {
        GitLabRelease r = new GitLabRelease();
        r.setScmDeveloperConnection("scm:git:git@solong.co.uk:owner/project.git");
        r.setServerId("exampleId");
        List<Artifact> artifacts = new ArrayList<>();
        r.setArtifacts(artifacts);
        Settings settings = new Settings();
        Server server = new Server();
        server.setPrivateKey("sampleKey");
        server.setId("exampleId");
        settings.addServer(server);
        r.setSettings(settings);
        r.validate();
    }

    @Test(expected = MojoExecutionException.class)
    public void shouldFailToFindServer() throws MojoExecutionException {
        GitLabRelease r = new GitLabRelease();
        r.setScmDeveloperConnection("scm:git:git@solong.co.uk:owner/project.git");
        r.setServerId("exampleId");
        List<Artifact> artifacts = new ArrayList<>();
        r.setArtifacts(artifacts);
        Settings settings = new Settings();
        Server server = new Server();
        server.setPrivateKey("sampleKey");
        server.setId("noTheExampleId");
        settings.addServer(server);
        r.setSettings(settings);
        r.validate();
    }
}