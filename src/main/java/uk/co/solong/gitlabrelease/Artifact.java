package uk.co.solong.gitlabrelease;

public class Artifact {
    private String file;
    private String label;
    private String linkType;

    public Artifact(String file, String label, String linkType) {
        this.file = file;
        this.label = label;
        this.linkType = linkType;
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

    public String getLinkType() {
        return linkType;
    }

    public void setLinkType(String linkType) {
        this.linkType = linkType;
    }
}
