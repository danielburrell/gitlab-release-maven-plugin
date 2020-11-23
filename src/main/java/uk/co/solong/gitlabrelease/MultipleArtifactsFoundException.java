package uk.co.solong.gitlabrelease;

import org.apache.maven.plugin.MojoExecutionException;

public class MultipleArtifactsFoundException extends MojoExecutionException {
    public MultipleArtifactsFoundException(Object source, String shortMessage, String longMessage) {
        super(source, shortMessage, longMessage);
    }

    public MultipleArtifactsFoundException(String message, Exception cause) {
        super(message, cause);
    }

    public MultipleArtifactsFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public MultipleArtifactsFoundException(String message) {
        super(message);
    }
}
