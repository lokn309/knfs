package cn.lokn.knfs;

import com.alibaba.fastjson.JSON;
import jakarta.servlet.ServletOutputStream;
import lombok.SneakyThrows;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * @description: Utils for file.
 * @author: lokn
 * @date: 2024/10/14 22:52
 */
public class FileUtils {

    static String DEFAULT_MIME_TYPE = "application/octet-stream";

    public static String getMimeType(String fileName) {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String content = fileNameMap.getContentTypeFor(fileName);
        return content == null ? DEFAULT_MIME_TYPE : content;
    }

    public static void init(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        for (int i = 0; i < 256; i++) {
            // 固定长度为16进制
            String subdir = String.format("%02x", i);
            File file = new File(path + "/" + subdir);
            if (!file.exists()) {
                file.mkdirs();
            }
        }
    }

    public static String getUUIDFile(String file) {
        return UUID.randomUUID().toString() + getExt(file);
    }

    public static String getSubdir(String file) {
        return file.substring(0, 2);
    }

    public static String getExt(String originalFilename) {
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }

    @SneakyThrows
    public static void writeMeta(File metaFile, FileMeta meta) {
        String json = JSON.toJSONString(meta);
        Files.writeString(Paths.get(metaFile.getAbsolutePath()), json,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE);
    }

    @SneakyThrows
    public static void writeString(File file, String content) {
        Files.writeString(Paths.get(file.getAbsolutePath()), content,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE);
    }

    @SneakyThrows
    public static void download(String downloadUrl, File file) {
        System.out.println(" ===>>>>> download file: " + file.getAbsolutePath());
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<?> entity = new HttpEntity<>(new HttpHeaders());
        ResponseEntity<Resource> exchange = restTemplate
                .exchange(downloadUrl, HttpMethod.GET, entity, Resource.class);

        InputStream is = new BufferedInputStream(exchange.getBody().getInputStream());
        byte[] bytes = new byte[16*1024];
        FileOutputStream outputStream = new FileOutputStream(file);
        // 读取文件信息，并逐段输出
        while (is.read(bytes) != -1) {
            outputStream.write(bytes);
        }
        outputStream.flush();
        outputStream.close();
        is.close();
    }
}
