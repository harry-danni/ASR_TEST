package com.sunyard.asr;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.iflytek.msp.cpdb.lfasr.client.LfasrClientImp;
import com.iflytek.msp.cpdb.lfasr.exception.LfasrException;
import com.iflytek.msp.cpdb.lfasr.model.LfasrType;
import com.iflytek.msp.cpdb.lfasr.model.Message;
import com.sunyard.asr.utils.StrUtils;

public class TaskManager extends Thread {


    private static LfasrClientImp asrClient = null;
    private static HashMap<String, String> asrParams = null;
    /*
     * 转写类型选择：标准版和电话版分别为：
     * LfasrType.LFASR_STANDARD_RECORDED_AUDIO 和 LfasrType.LFASR_TELEPHONY_RECORDED_AUDIO
     * */
    private static final LfasrType ASR_TYPE = LfasrType.LFASR_STANDARD_RECORDED_AUDIO;
    /**
     * 线程间隔时间
     */
    private static final long THREAD_INTERVAL = 5 * 1000;
    /**
     * 任务的最大重传次数
     */
    private static final int TASK_MAX_DUPLICATE_UPLOAD_COUNT = 5;

    public TaskManager(LfasrClientImp asrClient) {
        this.asrClient = asrClient;
    }

    @Override
    public void run() {
        asrParams = new HashMap<>();
        asrParams.put("has_participle", "false");

        int sleepCount = 1;
        while (true) {
            try {
                int listSize = iflyAsrTools.asrData.size();
                int taskUploadCount = 0;
                if (listSize > 0) {
                    sleepCount = 1;
                    String taskStatus = "";
                    int taskErorCount = 0;
                    for (Map<String, String> data : iflyAsrTools.asrData) {
                        taskStatus = StrUtils.toString(data.get("taskStatus"), "-1");
                        taskErorCount = Integer.parseInt(StrUtils.toString(data.get("taskErorCount"), "0"));
                        if ("0".equals(taskStatus) || ( "-1".equals(taskStatus) && taskErorCount < TASK_MAX_DUPLICATE_UPLOAD_COUNT)) {
                            System.out.println(data);
                            // 上传任务
                            System.out.println("创建任务:" + data.get("filePath"));
                            Map<String, String> retMap = uploadTask(data.get("filePath"));
                            if ("0".equals(retMap.get("success"))) { // 上传成功
                                data.put("taskStatus", "1");
                                data.put("taskId", retMap.get("taskId"));
                                System.out.println("创建任务成功:" + data);
                                sleep(200);
                            } else if ("26603".equals(retMap.get("success"))) {
                                sleep(60 * 1000);
                            } else {
                                taskErorCount++;
                                data.put("taskErorCount", String.valueOf(taskErorCount));
                                if (taskErorCount >= TASK_MAX_DUPLICATE_UPLOAD_COUNT) {
                                    data.put("taskStatus", "4");
                                } else {
                                    data.put("taskStatus", "-1");
                                }
                                System.out.println("文件（" + data.get("filePath") + "）上传失败，失败原因：" + retMap.get("errorMsg"));
                                sleep(200);
                            }
                        } else if (!"0".equals(taskStatus) && !"-1".equals(taskStatus)) {
                            taskUploadCount ++;
                        }
                    }
                    if (taskUploadCount == iflyAsrTools.taskCount) {
                        break;
                    }
                } else {
                    sleepCount ++;
                }
                
                System.out.println(taskUploadCount);
                sleep(sleepCount * THREAD_INTERVAL);
                if (sleepCount >= 20) {
                    break;
                }
            } catch (NumberFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private Map<String, String> uploadTask(String filePath)  {
        Map<String, String> retMap = new HashMap<>();
        try {
            // 上传音频文件
            Message uploadMsg = asrClient.lfasrUpload(filePath, ASR_TYPE, asrParams);

            // 判断返回值
            int ok = uploadMsg.getOk();
            if (ok == 0) {
                // 创建任务成功
                retMap.put("success", "0");
                retMap.put("taskId", uploadMsg.getData());
            } else {
                // 创建任务失败-服务端异常
                retMap.put("success", "" + uploadMsg.getErr_no());
                retMap.put("errorMsg", uploadMsg.getErr_no() + "(" + uploadMsg.getFailed() + ")");
            }
        } catch (LfasrException e) {
            // 上传异常，解析异常描述信息
            Message uploadMsg = JSON.parseObject(e.getMessage(), Message.class);
            retMap.put("success", "" + uploadMsg.getErr_no());
            retMap.put("errorMsg", uploadMsg.getErr_no() + "(" + uploadMsg.getFailed() + ")");
        }
        
        return retMap;
    }
}
