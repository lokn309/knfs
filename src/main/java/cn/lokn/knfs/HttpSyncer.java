package cn.lokn.knfs;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.function.ServerRequest;

import java.io.File;

/**
 * @description:
 * @author: lokn
 * @date: 2024/10/13 22:14
 */
@Component
public class HttpSyncer {

    public static final String XFILENAME = "X-FileName";
    public static final String XORIGFILENAME = "X-Orig-Filename";

    public String sync(File file, String url, String originalFilename) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add(XFILENAME, file.getName());
        headers.add(XORIGFILENAME, originalFilename);

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new FileSystemResource(file));

        HttpEntity<MultiValueMap<String, HttpEntity<?>>> httpEntity
                = new HttpEntity<>(builder.build(), headers);

        ResponseEntity<String> entity = restTemplate.postForEntity(url, httpEntity, String.class);
        String result = entity.getBody();
        System.out.println(" sync result = {}" + result);

        return result;
    }

}
