package uk.co.solong.gitlabrelease;

import co.uk.solong.gitlabapi.pojo.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.springframework.util.StringUtils;
import uk.co.solong.gitlabrelease.gitlabapi.ReleaseApi;

import java.util.ArrayList;
import java.util.List;

@Mojo(name = "gitlab-release", defaultPhase = LifecyclePhase.DEPLOY)
public class GitLabRelease extends AbstractMojo {

    @Parameter
    private List<Artifact> artifacts;

    @Parameter(defaultValue = "${project.version} release")
    private String description;

    @Parameter(defaultValue = "${project.version}")
    private String releaseName;

    @Parameter(defaultValue = "${project.version}")
    private String tag;

    @Parameter(defaultValue = "")
    private String serverId;

    @Parameter(defaultValue = "")
    private String owner;

    @Parameter(defaultValue = "")
    private String repo;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "master")
    private String commitish;

    @Parameter(defaultValue = "false")
    private boolean skip;

    @Parameter( defaultValue = "${settings}", readonly = true )
    private Settings settings;

    @Parameter
    private String baseUrl;

    private String token;

    private List<Object> requestLog = new ArrayList<>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (skip) {
            getLog().info("Skip set to true. Skipping");
            return;
        }
        validate();
        go();
    }

    private void go() {
        ReleaseApi api = new ReleaseApi(baseUrl, token, getLog());
        GetProjectIdResponse projectIdFromOwnerAndRepo = api.getProjectIdFromOwnerAndRepo(owner, repo);

        CreateReleaseRequest createReleaseRequest = new CreateReleaseRequest();
        Assets assets = new Assets();
        List<Link> links = new ArrayList<>();
        getLog().info("Adding artifacts");
        for (Artifact a : artifacts) {
            UploadFileResponse uploadFileResponse = api.uploadFileToProjectId(a, projectIdFromOwnerAndRepo.getId());
            Link l = new Link();
            l.setName(a.getLabel());

            l.setUrl(baseUrl+"/"+owner+"/"+repo+uploadFileResponse.getUrl());
            getLog().info("URL:"+l.getUrl());
            links.add(l);
        }
        assets.setLinks(links);
        createReleaseRequest.setAssets(assets);
        createReleaseRequest.setDescription(description);
        createReleaseRequest.setName(releaseName);
        createReleaseRequest.setTagName(tag);

        CreateReleaseResponse release = api.createRelease(projectIdFromOwnerAndRepo.getId(), createReleaseRequest, getLog());
    }

    private void validate() throws MojoExecutionException {
        //commitish should not be provided with useExistingTag
        if (StringUtils.isEmpty(repo)) {
            throw new MojoExecutionException("<repo> tag must be provided. e.g. myproject");
        }
        if (StringUtils.isEmpty(owner)) {
            throw new MojoExecutionException("<owner> tag must be provided e.g. burrelld");
        }
        if (StringUtils.isEmpty(baseUrl)) {
            throw new MojoExecutionException("<baseUrl> tag must be provided e.g. https://mygitlabinstance.myorg.com");
        }
        if (StringUtils.isEmpty(serverId)) {
            throw new MojoExecutionException("<serverId> tag matching the one in settings.xml must be provided e.g. mygitlab");
        } else {
            if (settings == null) {
                throw new MojoExecutionException("settings.xml not found");
            }
            if (settings.getServer(serverId) == null) {
                throw new MojoExecutionException("No serverId found in settings.xml for: "+serverId);
            } else {
                String privateKey = settings.getServer(serverId).getPrivateKey();
                if (StringUtils.isEmpty(privateKey)) {
                    throw new MojoExecutionException("No <privateKey> provided in settings.xml for serverId: "+serverId);
                } else {
                    token = privateKey;
                }
            }
        }
        if (artifacts == null) {
            throw new MojoExecutionException("<artifacts> tag must be present");
        }
        for (Artifact a : artifacts) {
            if (StringUtils.isEmpty(a.getFile())) {
                throw new MojoExecutionException("<artifact> tag must have a <file> tag");
            }
        }
    }

    private boolean isValidToken(String token) {
        //FIXME: call gitlab no-op to check the token
        return true;
    }


    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public void setArtifacts(List<Artifact> artifacts) {
        this.artifacts = artifacts;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setReleaseName(String releaseName) {
        this.releaseName = releaseName;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    void setToken(String token) {
        this.token = token;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}