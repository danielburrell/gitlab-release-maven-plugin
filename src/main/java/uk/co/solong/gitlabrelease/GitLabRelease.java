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
import uk.co.solong.gitlabrelease.exceptions.NoSuchPackageInGitlabPackageRegistryException;
import uk.co.solong.gitlabrelease.gitlabapi.PackagesApi;
import uk.co.solong.gitlabrelease.gitlabapi.ReleaseApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mojo(name = "gitlab-release", defaultPhase = LifecyclePhase.DEPLOY)
public class GitLabRelease extends AbstractMojo {

    @Parameter
    private List<Artifact> artifacts;

    @Parameter
    private List<Package> packages;

    @Parameter(defaultValue = "${project.version} release")
    private String description;

    @Parameter(defaultValue = "${project.version}")
    private String releaseName;

    @Parameter(defaultValue = "${project.version}")
    private String tag;

    @Parameter(defaultValue = "")
    private String serverId;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    //TODO fix this so it's used.
    @Parameter(defaultValue = "master")
    private String commitish;

    @Parameter(defaultValue = "false")
    private boolean skip;

    @Parameter( defaultValue = "${settings}", readonly = true )
    private Settings settings;

    private String baseUrl;

    private String token;

    private List<Object> requestLog = new ArrayList<>();

    //populated by init()
    private String scmDeveloperConnection;
    private String pathToRepo;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (skip) {
            getLog().info("Skip set to true. Skipping");
            return;
        }
        init();
        validate();
        go();
    }

    private void init() throws MojoExecutionException {
        scmDeveloperConnection = extractDeveloperConnection();
    }

    private String extractDeveloperConnection() throws MojoExecutionException {
        if (this.project.getScm() == null) {
            throw new MojoExecutionException("The project's <scm> tag must be populated");
        }
        if (StringUtils.isEmpty(this.project.getScm().getDeveloperConnection())) {
            throw new MojoExecutionException("The project's <developerConnection> tag within the <scm> tag must be populated");
        } else {
            return this.project.getScm().getDeveloperConnection();
        }
    }

    private void go() throws MultipleArtifactsFoundException, NoSuchFileInPackageInGitlabPackageRegistryException, NoSuchPackageInGitlabPackageRegistryException, MultipleFilesFoundInPackageException {
        ReleaseApi api = new ReleaseApi(baseUrl, token, getLog());
        GetProjectIdResponse projectIdFromOwnerAndRepo = api.getProjectIdFromOwnerAndRepo(pathToRepo);

        CreateReleaseRequest createReleaseRequest = new CreateReleaseRequest();
        Assets assets = new Assets();
        List<Link> links = new ArrayList<>();

        getLog().info("Linking packages");
        for (Package currentPackage : packages) {
            Link l = new Link();
            l.setName(currentPackage.getLabel());
            l.setLinkType("package");
            l.setUrl(deriveUrlFromFacts(baseUrl, projectIdFromOwnerAndRepo.getId(), currentPackage, pathToRepo));
            getLog().info("URL:"+l.getUrl());
            links.add(l);
        }

        getLog().info("Adding artifacts");
        for (Artifact a : artifacts) {
            UploadFileResponse uploadFileResponse = api.uploadFileToProjectId(a, projectIdFromOwnerAndRepo.getId());
            Link l = new Link();
            l.setName(a.getLabel());
            l.setLinkType(a.getLinkType());
            l.setUrl(baseUrl+"/"+pathToRepo+uploadFileResponse.getUrl());
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

    private String deriveUrlFromFacts(String baseUrl, Integer projectId, Package currentPackage, String pathToRepo) throws NoSuchPackageInGitlabPackageRegistryException, MultipleArtifactsFoundException, NoSuchFileInPackageInGitlabPackageRegistryException, MultipleFilesFoundInPackageException {
        //lookup the packages for the given projectId. GET /projects/:id/packages?package_type=maven&package_name=name
        PackagesApi api = new PackagesApi(baseUrl, token, getLog());

        Stream<ListPackagesResponse> listPackagesResponse = api.listPackagesForProject(projectId, currentPackage.getType(), currentPackage.getPackageName());
        List<ListPackagesResponse> packages = listPackagesResponse
                .peek(x -> getLog().info(String.join(":",x.getName(), x.getVersion(), x.getId().toString())))
                .filter(x -> x.getVersion().equals(currentPackage.getVersion()))
                .collect(Collectors.toList());
        //can't link to a package that cannot be found
        if (packages.isEmpty()) {
            throw new NoSuchPackageInGitlabPackageRegistryException("Could not find "+currentPackage.getType()+" package "+currentPackage.getPackageName()+" with version "+currentPackage.getVersion());
        }
        //if there's multiple packages and the user says there shouldn't be.
        if (currentPackage.getMatch().equals("EXACT") && packages.size() > 1) {
            throw new MultipleArtifactsFoundException("Found multiple "+currentPackage.getType()+" package artifacts for criteria "+currentPackage.getPackageName()+" with version "+currentPackage.getVersion()+" but match is set to EXACT");
        }
        //grab the first (and perhaps only) package.
        Integer packageId = packages.get(0).getId();
        Stream<ListPackageFilesResponse> filesInPackage = api.listFilesForPackage(projectId, packageId);
        List<Integer> fileIds = filesInPackage
                .peek(x -> getLog().info(String.join(":", x.getFileName(), x.getCreatedAt(), x.getId().toString())))
                .filter(x -> x.getFileName().equals(currentPackage.getFilename()))
                .map(ListPackageFilesResponse::getId)
                .collect(Collectors.toList());

        if (fileIds.isEmpty()) {
            throw new NoSuchFileInPackageInGitlabPackageRegistryException("Could not find filename "+currentPackage.getFilename()+ " in package");
        }

        if (currentPackage.getMatch().equals("EXACT") && fileIds.size() > 1) {
            throw new MultipleFilesFoundInPackageException("Match is strict but package contains multiple files with filename "+currentPackage.getFilename());
        }

        //domain.com/name/space/project-name/-/package_files/:fileId/download

        String url = String.join("/", new String[]{
                baseUrl,     //   notrailingslash.com
                pathToRepo, //   path/to/repo
                "-",          // ok
                "package_files",      // ok
                fileIds.get(0).toString(),  //  1
                "download"      // ok
        });
        return url;

    }

    public String getScmDeveloperConnection() {
        return scmDeveloperConnection;
    }

    public void setScmDeveloperConnection(String scmDeveloperConnection) {
        this.scmDeveloperConnection = scmDeveloperConnection;
    }

    void validate() throws MojoExecutionException {
        //commitish should not be provided with useExistingTag

        //scm:git:git@solong.co.uk:owner/project.git

        String pattern = "scm:\\w+:\\w+@([\\w+\\.\\_\\-0-9]*):([a-zA-z\\/_\\-0-9]*)\\.git";
        Pattern r = Pattern.compile(pattern);

        // Now create matcher object.
        Matcher m = r.matcher(scmDeveloperConnection);

        if (m.find()) {
            getLog().info("Discovered gitlab domain from SCM: https://"+m.group(1));
            baseUrl="https://"+m.group(1);
            getLog().info("Discovered gitlab path from SCM: "+m.group(2));
            pathToRepo=m.group(2);

        } else {
            getLog().error("Unable to parse SCM String: " + scmDeveloperConnection);
            throw new MojoExecutionException("Could not parse scm information. Ensure scm developerconnection tag is in format: `scm:git:git@gitlabdomain.org:owner/repo.git`");
        }

        if (StringUtils.isEmpty(pathToRepo)) {
            throw new MojoExecutionException("path to repo could not be derived from scm developerConnection tag. Check the developerConnection format is `scm:git:git@gitlabdomain.org:path/torepo.git`");
        }
        if (StringUtils.isEmpty(baseUrl)) {
            throw new MojoExecutionException("baseUrl could not be derived from scm developerConnection tag. Check the developerConnection format is `scm:git:git@baseurl.org:owner/repo.git`");
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
        if (artifacts == null && packages == null) {
            throw new MojoExecutionException("either <packages> or <artifacts> tag must be present");
        }
        if (artifacts == null) {
            artifacts = new ArrayList<>();
        }
        if (packages == null) {
            packages = new ArrayList<>();
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

    public void setProject(MavenProject project) {
        this.project = project;
    }

}