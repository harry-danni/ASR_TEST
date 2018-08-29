package com.sunyard.asr;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.iflytek.msp.cpdb.lfasr.client.LfasrClientImp;
import com.iflytek.msp.cpdb.lfasr.exception.LfasrException;
import com.iflytek.msp.cpdb.lfasr.model.Message;

public class iflyAsrTools {

    public static List<Map<String, String>>  asrData = new ArrayList<Map<String, String>>();
    static {
        asrData = Collections.synchronizedList(asrData);
        /**
         * 属性集
         * filePath : 文件路径.
         * taskId : 任务编号
         * taskStatus : 任务状态(-1 : 上传失败， 0 : 未上传， 1 : 任务上传成功，待转换, 2:转换完成 , 3:转换失败, 4: 任务创建失败)
         * taskErorCount : 任务上传失败次数
         * asr : 识别结果
         */
    }
    
    public static int taskCount = 0;
    
    public static void main(String[] args) {
        iflyAsrTools asrTools = new iflyAsrTools();
        LfasrClientImp asrClient = asrTools.init();
        TaskManager tm = new TaskManager(asrClient);
        ResutlManager rm = new ResutlManager(asrClient);
        File file = new File("F:\\ASR\\ifly_asr");
        asrTools.listFile(file);
        taskCount = asrData.size();
        tm.start();
        rm.start();
    }


    /**
     * 初始化
     * @return
     */
    private LfasrClientImp init() {
        LfasrClientImp asrClient = null;
        try {
            // 初始化LFASR实例
            asrClient = LfasrClientImp.initLfasrClient();
        } catch (LfasrException e) {
            // 初始化异常，解析异常描述信息
            Message initMsg = JSON.parseObject(e.getMessage(), Message.class);
            System.out.println("ecode=" + initMsg.getErr_no());
            System.out.println("failed=" + initMsg.getFailed());
        }
        return asrClient;
    }
    
    /**
     * 遍历所有音频文件
     * @param pfile
     */
    public void listFile(File pfile) {
        // 文件
        if (pfile.isFile()) {
            if ("wav".equalsIgnoreCase(getSuffix(pfile.getName()))) {
                addTask(pfile);
            }
        }
        // 目录
        if (pfile.isDirectory()) {
            for(File child : pfile.listFiles()) {
                listFile(child);
            }
        }
    }

    /**
     * 获取文件的后缀名
     * @param fileName
     * @return
     */
    private String getSuffix(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    /**
     * 把任务添加到列表中待执行识别。
     * @param file
     */
    public static void addTask(File file) {
        HashMap<String, String> task = new HashMap<>();
        task.put("filePath", file.getAbsolutePath());
        task.put("taskId", "");
        asrData.add(task);
        taskCount++;
    }
}
