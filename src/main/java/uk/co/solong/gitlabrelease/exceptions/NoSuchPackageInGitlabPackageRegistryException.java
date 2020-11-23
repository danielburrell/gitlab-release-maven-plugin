package uk.co.solong.gitlabrelease.exceptions;

import org.apache.maven.plugin.MojoExecutionException;

public class NoSuchPackageInGitlabPackageRegistryException extends MojoExecutionException {
    public NoSuchPackageInGitlabPackageRegistryException(Object source, String shortMessage, String longMessage) {
        super(source, shortMessage, longMessage);
    }

    public NoSuchPackageInGitlabPackageRegistryException(String message, Exception cause) {
        super(message, cause);
    }

    public NoSuchPackageInGitlabPackageRegistryException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchPackageInGitlabPackageRegistryException(String message) {
        super(message);
    }
}
