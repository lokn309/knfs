package cn.lokn.knfs;

import com.alibaba.fastjson.JSON;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

/**
 * @description: file download and upload controller.
 * @author: lokn
 * @date: 2024/10/07 11:56
 */
@RestController
public class FileController {

    @Value("${knfs.path}")
    private String uploadPath;

    @Value("${knfs.backupUrl}")
    private String backupUrl;

    @Value("${knfs.autoMd5}")
    private boolean autoMd5;

    @Autowired
    private HttpSyncer httpSyncer;

    @SneakyThrows
    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file,
                         HttpServletRequest request) {
        // 1、处理文件
        boolean neeSync = false;
        // 如果请求头参数为空，则表示是用户上传的数据，否就是同步的数据
        String filename = request.getHeader(HttpSyncer.XFILENAME);
        // 同步文件到 backup
        if (filename == null || filename.isEmpty()) {
            neeSync = true;
//            filename = file.getOriginalFilename();
            filename = FileUtils.getUUIDFile(file.getOriginalFilename());
        }
        String subdir = FileUtils.getSubdir(filename);
        File dest = new File(uploadPath + "/" + subdir + "/" + filename);
        file.transferTo(dest);

        // 2、处理meta
        FileMeta meta = new FileMeta();
        meta.setName(filename);
        meta.setOriginalName(file.getOriginalFilename());
        meta.setSize(file.getSize());
        if (autoMd5) {
            meta.getTags().put("md5", DigestUtils.md5DigestAsHex(new FileInputStream(dest)));
        }

        // 三种存放方式，本地采用2.1的方式
        // 2.1 存放到本地文件
        // 2.2 存放到数据库
        // 2.3 存到配置中或注册中心，比如zk
        String metaName = filename + ".meta";
        File metaFile = new File(uploadPath + "/" + subdir + "/" + metaName);
        FileUtils.writeMeta(metaFile, meta);

        // 3、同步到 backup
        if (neeSync) {
            httpSyncer.sync(dest, backupUrl);
        }

        return filename;
    }

    @RequestMapping("/download")
    public void download(String name, HttpServletResponse response) {
        String subdir = FileUtils.getSubdir(name);
        String path = uploadPath + "/" + subdir + "/" + name;
        File file = new File(path);
        try {
            FileInputStream fis = new FileInputStream(file);
            InputStream is = new BufferedInputStream(fis);
            byte[] bytes = new byte[16*1024];

            // 加一些 response 的头
            response.setCharacterEncoding("utf-8");
            // 二进制流直接下载
            //response.setContentType("application/octet-stream");
            // 默认会下载
//            response.setHeader("Content-Disposition", "attachment;filename=" + name);
            // 在浏览器中预览
            response.setContentType(FileUtils.getMimeType(name));
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

    @RequestMapping("/meta")
    public String meta(String name) {
        String subdir = FileUtils.getSubdir(name);
        String path = uploadPath + "/" + subdir + "/" + name + ".meta";
        File file = new File(path);
        try {
            return FileCopyUtils.copyToString(new FileReader(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
