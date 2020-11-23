package uk.co.solong.gitlabrelease;

import org.apache.maven.plugin.MojoExecutionException;

public class MultipleFilesFoundInPackageException extends MojoExecutionException {
    public MultipleFilesFoundInPackageException(Object source, String shortMessage, String longMessage) {
        super(source, shortMessage, longMessage);
    }

    public MultipleFilesFoundInPackageException(String message, Exception cause) {
        super(message, cause);
    }

    public MultipleFilesFoundInPackageException(String message, Throwable cause) {
        super(message, cause);
    }

    public MultipleFilesFoundInPackageException(String message) {
        super(message);
    }
}
