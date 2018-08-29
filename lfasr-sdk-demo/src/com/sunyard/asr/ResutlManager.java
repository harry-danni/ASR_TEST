package com.sunyard.asr;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iflytek.msp.cpdb.lfasr.client.LfasrClientImp;
import com.iflytek.msp.cpdb.lfasr.exception.LfasrException;
import com.iflytek.msp.cpdb.lfasr.model.Message;
import com.sunyard.asr.utils.StrUtils;

public class ResutlManager extends Thread {

    private static LfasrClientImp asrClient = null;
    
    public  ResutlManager(LfasrClientImp asrClient) {
        this.asrClient = asrClient;
    }

    private static String outFile = "F:\\ASR\\ifly_asr\\out.csv";
    /**
     * �̼߳��ʱ��
     */
    private static final long THREAD_INTERVAL = 25 * 1000;
    
    @Override
    public void run() {
        FileWriter fw = getWriter(outFile);
        while(true) {
            try {
                int taskSuccessCount = 0;
                int listSize = iflyAsrTools.asrData.size();
                if (listSize > 0) {
                    for (Map<String, String> data : iflyAsrTools.asrData) {
                        String taskid = StrUtils.toString(data.get("taskId"));
                        String taskStatus = StrUtils.toString(data.get("taskStatus"), "-1");
                        if("1".equals(taskStatus) && !taskid.isEmpty()) {
                            Map<String, String> retMap = getASRresult(taskid);
                            if ("0".equals(retMap.get("success"))) {
                                data.put("asr", retMap.get("result"));
                                data.put("taskStatus", "2");
                                fw.write(getOutputString(data));
                                fw.write("\n");
                                fw.flush();
                            } else if("26603".equals(retMap.get("success"))){
                                // ���ʽӿڹ���Ƶ�������1������ٷ��ʡ�
                                sleep(60 * 1000);
                            } else if ("26605".equals(retMap.get("success"))){
                                // ���������ڴ����Ժ�����
                                sleep(200); 
                            } else {
                                System.out.println("����(" + taskid + ")��ȡ���ʧ�ܡ�" + retMap.get("errorMsg"));
                                // �������Ĭ��תдʧ�ܡ�
                                data.put("taskStatus", "3");
                                fw.write(getOutputString(data));
                                fw.write("\n");
                                fw.flush();
                            }
                        } else if ("2".equals(taskStatus) || "3".equals(taskStatus) || "4".equals(taskStatus)) {
                            taskSuccessCount ++;
                        }
                    }
                    if (taskSuccessCount == iflyAsrTools.taskCount) {
                        fw.flush();
                        fw.close();
                        break;
                    }
                }
                sleep(THREAD_INTERVAL);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private Map<String, String> getASRresult(String taskid) {
        Map<String, String> retMap = new HashMap<String, String>();
        try {
            Message resultMsg = asrClient.lfasrGetResult(taskid);
            // �������״̬����0����������ɹ�
            if (resultMsg.getOk() == 0) {
                retMap.put("success", "0");
                retMap.put("result", resultMsg.getData());
            } else {
                // תдʧ�ܣ�����ʧ����Ϣ���д���
                retMap.put("success", "" + resultMsg.getErr_no() );
                retMap.put("errorMsg", resultMsg.getErr_no() + "(" + resultMsg.getFailed() + ")");
            }
        } catch (LfasrException e) {
            // ��ȡ����쳣���������쳣������Ϣ
            Message resultMsg = JSON.parseObject(e.getMessage(), Message.class);
            retMap.put("success", "" + resultMsg.getErr_no() );
            retMap.put("errorMsg", resultMsg.getErr_no() + "(" + resultMsg.getFailed() + ")");
        }
        System.out.println("����" + taskid + "���ķ��ؽ���ǣ�" + retMap);
        return retMap;
    }

    private FileWriter getWriter(String filePath) {
        FileWriter fw;
        try {
            File file = new File(filePath);
            if (file.exists()) {
                file.mkdirs();
            }
            fw = new FileWriter(file);
        } catch (IOException e) {
            e.printStackTrace();
            fw = null;
        }
        return fw;
    }
    
    private String getOutputString(Map<String, String> data){
        StringBuffer asrRes = new StringBuffer();
        String asrJson = StrUtils.toString(data.get("asr"), "");
        if("2".equals(data.get("taskStatus"))) {
            JSONArray jarray = JSON.parseArray(asrJson);
            JSONObject jobj  = null;
            for (int index = 0; index < jarray.size() ; index++) {
                jobj = jarray.getJSONObject(index);
                asrRes.append(jobj.getString("speaker") + ":" + jobj.getString("onebest") + "\t");
            }
            
        }
        return data.get("filePath") + "," + data.get("taskId") + "," + data.get("taskStatus") + "," + asrRes.toString() + "," + asrJson.replaceAll(",", "��");
    }
}
