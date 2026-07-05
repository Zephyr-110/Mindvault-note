package org.example.common.file.impl;


import com.aliyun.oss.OSS;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.config.OssConfig;
import org.example.common.exception.BusinessException;
import org.example.common.file.FileService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp",// 图片
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",// 文档
            ".txt", ".md", ".csv", ".json", ".xml", //文本
            ".mp4", ".avi", ".mov", ".mkv", ".webm", ".flv", ".wmv"//视频
    );
    private final OSS ossClient;
    private final OssConfig ossConfig;

    @Override
    public String uploadFile(MultipartFile file) {
        //接收文件
        String originalFilename = file.getOriginalFilename();
        //初始化文件扩展名
        String extension = "";
        //当文件名存在并且并且存在扩展名时将真正的扩展名赋值给extension
        if(originalFilename != null && originalFilename.contains(".")){
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(400, "不支持的文件类型: " + extension);
        }
        //通过下方的方法生成文件名
        String objectName = getFileName(extension);
        //上传文件，异常处理保证程序稳定
        try {
            //把文件上传到云上
            ossClient.putObject(ossConfig.getBucketName(), objectName, file.getInputStream());
            //拼接文件访问路径
            String url = "https://" + ossConfig.getBucketName() + "." + ossConfig.getEndpoint() + "/" + objectName;
            log.info("上传成功: {} -> {}", originalFilename, url);
            //返回文件访问路径
            return url;
        } catch (IOException e) {
            log.error("上传失败: {}", originalFilename, e);
            throw new BusinessException(500, "上传失败");
        }
    }
    //该方法仅服务于上方的uploadFile方法
    private String getFileName(String extension) {
        //format方法将时间格式化为字符串
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        //replace方法将-替换为空
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "files/" + datePath + "/" + uuid + extension;
    }
}