package com.example.myapplication.bean;

import com.google.gson.Gson;

public class MessageBean {

    private Gson gson = new Gson();

    /**
     * messageName : 39
     * formType : 0
     * data : {"method":"scene.msg","body":"[{\"start_time\":1600914856960,\"record_id\":\"915DCBB9C8AD0E1E\",\"scene_result\":0,\"user_id\":\"159333305427873213\",\"msg_content\":\"\\\"睡眠模式\\\"场景已执行\",\"logic_id\":\"l0000002\",\"trigger_type\":1,\"end_time\":1600914857485,\"msgTime\":1600914857485,\"script_id\":\"BF4A91D97F15B1CC5F48A155492C7138\",\"international_msg_content\":\"Scene \\\"睡眠模式\\\" successfully performed\",\"device_result\":[{\"start_time\":1600914857005,\"device_name\":\"航嘉插座\",\"status\":0,\"feed_id\":\"764781593482730317\"}]}]"}
     * createTime : 1600914857763
     * isBroadcast : 0
     * source : 6
     * pacageName : com.autoai.weos
     * sendTime : 1600914857788
     */

    private String messageName;
    private int formType;
    private String data;
    private long createTime;
    private int isBroadcast;
    private int source;
    private String pacageName;
    private long sendTime;
    private Data mData;

    public String getMessageName() {
        return messageName;
    }

    public void setMessageName(String messageName) {
        this.messageName = messageName;
    }

    public int getFormType() {
        return formType;
    }

    public void setFormType(int formType) {
        this.formType = formType;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
        try {
            mData = gson.fromJson(data, Data.class);
        } catch (Exception e) {
            mData = null;
        }
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public int getIsBroadcast() {
        return isBroadcast;
    }

    public void setIsBroadcast(int isBroadcast) {
        this.isBroadcast = isBroadcast;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public String getPacageName() {
        return pacageName;
    }

    public void setPacageName(String pacageName) {
        this.pacageName = pacageName;
    }

    public long getSendTime() {
        return sendTime;
    }

    public void setSendTime(long sendTime) {
        this.sendTime = sendTime;
    }

    public Data getmData() {
        return mData;
    }

    public void setmData(Data mData) {
        this.mData = mData;
    }
}
