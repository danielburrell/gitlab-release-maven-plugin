package uk.co.solong.gitlabrelease.gitlabapi;

import co.uk.solong.gitlabapi.pojo.CreateReleaseRequest;
import co.uk.solong.gitlabapi.pojo.CreateReleaseResponse;
import co.uk.solong.gitlabapi.pojo.GetProjectIdResponse;
import co.uk.solong.gitlabapi.pojo.UploadFileResponse;
import org.apache.maven.plugin.logging.Log;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownHttpStatusCodeException;
import uk.co.solong.gitlabrelease.Artifact;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ReleaseApi {

    private final String API_BASE;
    private static final String CREATE_URL = "/projects/{id}/releases";
    private static final String GET_RELEASE_BY_TAG_URL = "/projects/{encoded_repo}";
    private static final String UPLOAD_URL = "/projects/{id}/uploads";
    private final String token;
    private final Log log;

    public ReleaseApi(String baseUrl, String token, Log log) {
        this.API_BASE = baseUrl + "/api/v4/";
        this.token = token;
        this.log = log;
    }

    //GET https://BASE_URL/api/v4/projects/danbur1%2Fgitlabapi
    public GetProjectIdResponse getProjectIdFromOwnerAndRepo(String pathToRepo) {

        RestTemplate r = new RestTemplate();
        Map<String, String> urlParams = new HashMap<>();
        urlParams.put("encoded_repo", pathToRepo);

        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.put("PRIVATE-TOKEN", Collections.singletonList(token));

        HttpEntity h = new HttpEntity(headers);

        try {
            ResponseEntity<GetProjectIdResponse> responseEntity = r.exchange(API_BASE + GET_RELEASE_BY_TAG_URL, HttpMethod.GET, h, GetProjectIdResponse.class, urlParams);
            return responseEntity.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException | UnknownHttpStatusCodeException e) {
            log.error("Error occurred "+ e.getResponseBodyAsString(), e);
            throw e;
        }

    }


    public CreateReleaseResponse createRelease(Integer id, CreateReleaseRequest createReleaseRequest, Log log) {

        RestTemplate r = new RestTemplate();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("id", id.toString());

        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.put("PRIVATE-TOKEN", Collections.singletonList(token));

        HttpEntity<CreateReleaseRequest> h = new HttpEntity<>(createReleaseRequest, headers);

        try {
            log.info("Creating release with "+createReleaseRequest.getName()+"/"+createReleaseRequest.getDescription()+"/"+ createReleaseRequest.getTagName());
            ResponseEntity<CreateReleaseResponse> responseEntity = r.exchange(API_BASE + CREATE_URL, HttpMethod.POST, h, CreateReleaseResponse.class, queryParams);
            return responseEntity.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException | UnknownHttpStatusCodeException e) {
            log.error("Error occurred "+ e.getResponseBodyAsString(), e);
            throw e;
        }
    }

    // POST /projects/:id/uploads
    public UploadFileResponse uploadFileToProjectId(Artifact a, Integer id) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("PRIVATE-TOKEN", token);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", getFile(a));
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        Map<String, String> urlParams = new HashMap<>();
        urlParams.put("id", id.toString());

        String serverUrl = API_BASE + UPLOAD_URL;

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<UploadFileResponse> response = restTemplate.postForEntity(serverUrl, requestEntity, UploadFileResponse.class, urlParams);
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException | UnknownHttpStatusCodeException e) {
            log.error("Error occurred "+ e.getResponseBodyAsString(), e);
            throw e;
        }
    }

    private FileSystemResource getFile(Artifact a) {
        FileSystemResource f = new FileSystemResource(a.getFile());
        return f;
    }
}
