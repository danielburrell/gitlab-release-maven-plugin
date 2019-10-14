package uk.co.solong.gitlabrelease;

public class Artifact {
    private String file;
    private String label;

    public Artifact(String file, String label) {
        this.file = file;
        this.label = label;
    }

    public Artifact() {
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }
}
