package cn.lokn.knfs;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * @description: file download and upload controller.
 * @author: lokn
 * @date: 2024/10/07 11:56
 */
@RestController
public class FileController {

    @Value("${knfs.path}")
    private String uploadPath;
    @SneakyThrows
    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file) {
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            dir.mkdir();
        }
        String filename = file.getOriginalFilename();
        File dest = new File(uploadPath + "/" + filename);

        file.transferTo(dest);
        return filename;
    }

    @RequestMapping("/download")
    public void download(String name, HttpServletResponse response) {
        String path = uploadPath + "/" + name;
        File file = new File(path);
        try {
            FileInputStream fis = new FileInputStream(file);
            InputStream is = new BufferedInputStream(fis);
            byte[] bytes = new byte[16*1024];

            // 加一些 response 的头
            response.setCharacterEncoding("utf-8");
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=" + name);
            response.setHeader("Content-Length", String.valueOf(is.available()));

            // 读取文件信息，并逐段输出
            ServletOutputStream outputStream = response.getOutputStream();
            while (is.read(bytes) != -1) {
                outputStream.write(bytes);
            }
            outputStream.flush();
            is.close();
        } catch (Exception e) {

        }

    }

}
