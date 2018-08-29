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
         * ���Լ�
         * filePath : �ļ�·��.
         * taskId : ������
         * taskStatus : ����״̬(-1 : �ϴ�ʧ�ܣ� 0 : δ�ϴ��� 1 : �����ϴ��ɹ�����ת��, 2:ת����� , 3:ת��ʧ��, 4: ���񴴽�ʧ��)
         * taskErorCount : �����ϴ�ʧ�ܴ���
         * asr : ʶ����
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
     * ��ʼ��
     * @return
     */
    private LfasrClientImp init() {
        LfasrClientImp asrClient = null;
        try {
            // ��ʼ��LFASRʵ��
            asrClient = LfasrClientImp.initLfasrClient();
        } catch (LfasrException e) {
            // ��ʼ���쳣�������쳣������Ϣ
            Message initMsg = JSON.parseObject(e.getMessage(), Message.class);
            System.out.println("ecode=" + initMsg.getErr_no());
            System.out.println("failed=" + initMsg.getFailed());
        }
        return asrClient;
    }
    
    /**
     * ����������Ƶ�ļ�
     * @param pfile
     */
    public void listFile(File pfile) {
        // �ļ�
        if (pfile.isFile()) {
            if ("wav".equalsIgnoreCase(getSuffix(pfile.getName()))) {
                addTask(pfile);
            }
        }
        // Ŀ¼
        if (pfile.isDirectory()) {
            for(File child : pfile.listFiles()) {
                listFile(child);
            }
        }
    }

    /**
     * ��ȡ�ļ��ĺ�׺��
     * @param fileName
     * @return
     */
    private String getSuffix(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    /**
     * ��������ӵ��б��д�ִ��ʶ��
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
