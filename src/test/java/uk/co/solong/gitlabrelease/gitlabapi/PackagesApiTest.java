package uk.co.solong.gitlabrelease.gitlabapi;

import co.uk.solong.gitlabapi.pojo.ListPackageFilesResponse;
import co.uk.solong.gitlabapi.pojo.ListPackagesResponse;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.Format;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockserver.model.HttpRequest.request;

public class PackagesApiTest {

    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this);

    private MockServerClient mockServerClient;

    @Test
    public void shouldGetFileInPackage() throws IOException {
        Logger logger = new ConsoleLogger(0, "default");
        Log log = new DefaultLog(logger);
        int port = mockServerClient.getPort();
        mockServerClient.withSecure(false);

        mockServerClient.when(request().withMethod("GET")
                .withPath("/api/v4/projects/1/packages/4/package_files")
                .withQueryStringParameter("page", "1")
        ).respond(HttpResponse.response()
                .withBody(
                        new String(
                                Files.readAllBytes(new ClassPathResource("listfilesinpackage.mock.json").getFile().toPath())
                        ), MediaType.APPLICATION_JSON
                ));
        mockServerClient.when(request().withMethod("GET")
                .withPath("/api/v4/projects/1/packages/4/package_files")
                .withQueryStringParameter("page", "2")
        ).respond(HttpResponse.response()
                .withBody(
                        "[]", MediaType.APPLICATION_JSON
                ));


        PackagesApi packagesApi = new PackagesApi("http://127.0.0.1:" + port, "sometoken", log);
        Stream<ListPackageFilesResponse> files = packagesApi.listFilesForPackage(1, 4);

        try {
            Optional<ListPackageFilesResponse> first = files.filter(x -> x.getFileName().equals("maven-metadata.xml")).findFirst();
            assertEquals(new Integer(27), first.get().getId());
        } catch (Throwable e) {
            String requestDefinitions = mockServerClient.retrieveRecordedRequests(request(), Format.JAVA);
            log.error(requestDefinitions);
            fail();
        }
    }

    @Test
    public void shouldReturnPackagesForProject() throws IOException {
        Logger logger = new ConsoleLogger(0, "default");
        Log log = new DefaultLog(logger);
        int port = mockServerClient.getPort();
        mockServerClient.withSecure(false);

        mockServerClient.when(request().withMethod("GET")
                .withPath("/api/v4/projects/1/packages")
                .withQueryStringParameter("package_name", "uk/co/solong/project")
                .withQueryStringParameter("page", "1")
                .withQueryStringParameter("package_type", "maven")
        ).respond(HttpResponse.response()
                .withBody(
                        new String(
                                Files.readAllBytes(new ClassPathResource("server.json").getFile().toPath())
                        ), MediaType.APPLICATION_JSON
                ));
        mockServerClient.when(request().withMethod("GET")
                .withPath("/api/v4/projects/1/packages")
                .withQueryStringParameter("package_name", "uk/co/solong/project")
                .withQueryStringParameter("page", "2")
                .withQueryStringParameter("package_type", "maven")
        ).respond(HttpResponse.response()
                .withBody(
                        "[]", MediaType.APPLICATION_JSON
                ));

        PackagesApi packagesApi = new PackagesApi("http://127.0.0.1:" + port, "sometoken", log);
        Stream<ListPackagesResponse> packages = packagesApi.listPackagesForProject(1, "maven", "uk/co/solong/project");

        try {
            Optional<ListPackagesResponse> first = packages.filter(x -> x.getVersion().equals("1.0-SNAPSHOT")).findFirst();
            assertEquals(new Integer(1), first.get().getId());
        } catch (Throwable e) {
            String requestDefinitions = mockServerClient.retrieveRecordedRequests(request(), Format.JAVA);
            log.error(requestDefinitions);
            fail();
        }
    }
}