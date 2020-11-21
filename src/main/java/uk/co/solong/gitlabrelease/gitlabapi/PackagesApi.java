package uk.co.solong.gitlabrelease.gitlabapi;

import co.uk.solong.gitlabapi.pojo.ListPackageFilesResponse;
import co.uk.solong.gitlabapi.pojo.ListPackagesResponse;
import org.apache.maven.plugin.logging.Log;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownHttpStatusCodeException;

import java.util.*;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Spliterator.NONNULL;

public class PackagesApi {

    private final String API_BASE;
    private static final String PACKAGES_URL = "/projects/{id}/packages?package_type={package_type}&package_name={package_name}&page={page}";
    private static final String PACKAGE_FILES_URL = "/projects/{id}/packages/{package_id}/package_files";

    private final String token;
    private final Log log;

    public PackagesApi(String baseUrl, String token, Log log) {
        this.API_BASE = baseUrl + "/api/v4/";
        this.token = token;
        this.log = log;
    }

    private List<ListPackagesResponse> getPageOfPackagesForProject(Integer id, String type, String packageName, Integer page) {
        RestTemplate r = new RestTemplate();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("id", id.toString());
        queryParams.put("package_name", packageName);
        queryParams.put("package_type", type);
        queryParams.put("page", Integer.toString(page));

        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.put("PRIVATE-TOKEN", Collections.singletonList(token));

        HttpEntity<String> h = new HttpEntity<>(headers);

        try {
            log.info("Listing packages where package_name is "+packageName+" and type is "+type);
            ResponseEntity<List<ListPackagesResponse>> responseEntity = r.exchange(API_BASE + PACKAGES_URL, HttpMethod.GET, h, new ParameterizedTypeReference<List<ListPackagesResponse>>(){}, queryParams);
            return responseEntity.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException | UnknownHttpStatusCodeException e) {
            log.error("Error occurred "+ e.getResponseBodyAsString(), e);
            throw e;
        }
    }

    public Stream<ListPackagesResponse> listPackagesForProject(Integer id, String type, String packageName) {
        Function<Integer, List<ListPackagesResponse>> apiCall = page -> getPageOfPackagesForProject(id, type, packageName, page);
        return pageApiCall(apiCall);
    }

    public Stream<ListPackageFilesResponse> listFilesForPackage(Integer projectId, Integer packageId) {
        Function<Integer, List<ListPackageFilesResponse>> apiCall = page -> getPageOfFilesForPackage(projectId, packageId, page);
        return pageApiCall(apiCall);
    }

    private <O> Stream<O> pageApiCall(Function<Integer, List<O>> apiCall) {
        AbstractSpliterator<O> packageSupplier = new AbstractSpliterator<O>(Long.MAX_VALUE, NONNULL) {
            private List<O> currentSupplySet = new ArrayList<>();
            private int page = 1;
            private int index = 0;
            private Function<Integer, List<O>> supplySet = apiCall;

            @Override
            public boolean tryAdvance(Consumer<? super O> action) {
                if (index < currentSupplySet.size()) {
                    //end of the current list, get more supplies.
                    currentSupplySet = supplySet.apply(page++);
                    index = 0; //start reading from index 0 again.
                    if (currentSupplySet.size() > 0) {
                        //if there are more elements to pull
                        action.accept(currentSupplySet.get(index++));
                        return true;
                    } else {
                        //otherwise that's it the new supply list was itself empty.
                        return false;
                    }
                } else {
                    action.accept(currentSupplySet.get(index++));
                    return true;
                }
            }
        };
        return StreamSupport.stream(packageSupplier, false);
    }

    private List<ListPackageFilesResponse> getPageOfFilesForPackage(Integer projectId, Integer packageId, Integer page) {
        RestTemplate r = new RestTemplate();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("id", projectId.toString());
        queryParams.put("package_id", packageId.toString());
        queryParams.put("page", Integer.toString(page));

        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.put("PRIVATE-TOKEN", Collections.singletonList(token));

        HttpEntity<String> h = new HttpEntity<>(headers);

        try {
            log.info("Listing files for package where project_id is "+projectId+" and package_id is "+packageId);
            ResponseEntity<List<ListPackageFilesResponse>> responseEntity = r.exchange(API_BASE + PACKAGE_FILES_URL, HttpMethod.GET, h, new ParameterizedTypeReference<List<ListPackageFilesResponse>>(){}, queryParams);
            return responseEntity.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException | UnknownHttpStatusCodeException e) {
            log.error("Error occurred "+ e.getResponseBodyAsString(), e);
            throw e;
        }
    }
}
