package cn.lokn.knfs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * @description: file meta data.
 * @author: lokn
 * @date: 2024/10/16 22:37
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileMeta {

    private String name;
    private String originalName;
    private long size;
    // 高级特性
    private Map<String, String> tags = new HashMap<>();

}
