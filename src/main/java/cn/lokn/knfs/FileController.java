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

    @Value("${knfs.syncBackup}")
    private boolean syncBackup;

    @Value("${knfs.downloadUrl}")
    private String downloadUrl;

    @Autowired
    private HttpSyncer httpSyncer;

    @Autowired
    private MQSyncer mqSyncer;

    @SneakyThrows
    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file,
                         HttpServletRequest request) {
        // 1、处理文件
        boolean neeSync = false;
        // 如果请求头参数为空，则表示是用户上传的数据，否就是同步的数据
        String filename = request.getHeader(HttpSyncer.XFILENAME);
        // 同步文件到 backup
        String originalFilename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) { // upload上传文件
            neeSync = true;
            filename = FileUtils.getUUIDFile(originalFilename);
        } else { // 同步文件
            String xor = request.getHeader(HttpSyncer.XORIGFILENAME);
            if (xor != null && !xor.isEmpty()) {
                originalFilename = xor;
            }
        }
        String subdir = FileUtils.getSubdir(filename);
        File dest = new File(uploadPath + "/" + subdir + "/" + filename);
        file.transferTo(dest);

        // 2、处理meta
        FileMeta meta = new FileMeta();
        meta.setName(filename);
        meta.setOriginalName(originalFilename);
        meta.setSize(file.getSize());
        meta.setDownloadUrl(downloadUrl + "/" + filename);
        if (autoMd5) {
            meta.getTags().put("md5", DigestUtils.md5DigestAsHex(new FileInputStream(dest)));
        }

        // 三种存放方式，本地采用2.1的方式
        // 2.1 存放到本地文件
        String metaName = filename + ".meta";
        File metaFile = new File(uploadPath + "/" + subdir + "/" + metaName);
        FileUtils.writeMeta(metaFile, meta);

        // 2.2 存放到数据库
        // 2.3 存到配置中或注册中心，比如zk

        // 3、同步到 backup
        // 同步文件到backup
        // 让我们可以实现同步处理文件复制，也可以异步处理文件复制
        if (neeSync) {
            if (syncBackup) {
                try {
                    httpSyncer.sync(dest, backupUrl, originalFilename);
                } catch (Exception e) {
                    // log ex
                    e.printStackTrace();
                    // 高可用的方式来实现同步
                    // 如果抛出异常，则通过异步的方式来实现文件复制
                    // MQSyncer.sync(backupUrl,meta);
                }
            } else {
                mqSyncer.sync(meta);
            }

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
