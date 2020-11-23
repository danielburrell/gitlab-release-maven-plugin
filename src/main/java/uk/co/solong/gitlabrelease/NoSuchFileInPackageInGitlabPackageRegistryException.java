package uk.co.solong.gitlabrelease;

import org.apache.maven.plugin.MojoExecutionException;

public class NoSuchFileInPackageInGitlabPackageRegistryException extends MojoExecutionException {
    public NoSuchFileInPackageInGitlabPackageRegistryException(Object source, String shortMessage, String longMessage) {
        super(source, shortMessage, longMessage);
    }

    public NoSuchFileInPackageInGitlabPackageRegistryException(String message, Exception cause) {
        super(message, cause);
    }

    public NoSuchFileInPackageInGitlabPackageRegistryException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchFileInPackageInGitlabPackageRegistryException(String message) {
        super(message);
    }
}
