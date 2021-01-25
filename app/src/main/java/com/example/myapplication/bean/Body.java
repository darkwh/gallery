package com.example.myapplication.bean;

import java.util.List;

public class Body {

    /**
     * start_time : 1600914856960
     * record_id : 915DCBB9C8AD0E1E
     * scene_result : 0
     * user_id : 159333305427873213
     * msg_content : "睡眠模式"场景已执行
     * logic_id : l0000002
     * trigger_type : 1
     * end_time : 1600914857485
     * msgTime : 1600914857485
     * script_id : BF4A91D97F15B1CC5F48A155492C7138
     * international_msg_content : Scene "睡眠模式" successfully performed
     * device_result : [{"start_time":1600914857005,"device_name":"航嘉插座","status":0,"feed_id":"764781593482730317"}]
     */

    private long start_time;
    private String record_id;
    private int scene_result;
    private String user_id;
    private String msg_content;
    private String logic_id;
    private int trigger_type;
    private long end_time;
    private long msgTime;
    private String script_id;
    private String international_msg_content;
    private List<DeviceResultBean> device_result;

    public long getStart_time() {
        return start_time;
    }

    public void setStart_time(long start_time) {
        this.start_time = start_time;
    }

    public String getRecord_id() {
        return record_id;
    }

    public void setRecord_id(String record_id) {
        this.record_id = record_id;
    }

    public int getScene_result() {
        return scene_result;
    }

    public void setScene_result(int scene_result) {
        this.scene_result = scene_result;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getMsg_content() {
        return msg_content;
    }

    public void setMsg_content(String msg_content) {
        this.msg_content = msg_content;
    }

    public String getLogic_id() {
        return logic_id;
    }

    public void setLogic_id(String logic_id) {
        this.logic_id = logic_id;
    }

    public int getTrigger_type() {
        return trigger_type;
    }

    public void setTrigger_type(int trigger_type) {
        this.trigger_type = trigger_type;
    }

    public long getEnd_time() {
        return end_time;
    }

    public void setEnd_time(long end_time) {
        this.end_time = end_time;
    }

    public long getMsgTime() {
        return msgTime;
    }

    public void setMsgTime(long msgTime) {
        this.msgTime = msgTime;
    }

    public String getScript_id() {
        return script_id;
    }

    public void setScript_id(String script_id) {
        this.script_id = script_id;
    }

    public String getInternational_msg_content() {
        return international_msg_content;
    }

    public void setInternational_msg_content(String international_msg_content) {
        this.international_msg_content = international_msg_content;
    }

    public List<DeviceResultBean> getDevice_result() {
        return device_result;
    }

    public void setDevice_result(List<DeviceResultBean> device_result) {
        this.device_result = device_result;
    }

    public static class DeviceResultBean {
        /**
         * start_time : 1600914857005
         * device_name : 航嘉插座
         * status : 0
         * feed_id : 764781593482730317
         */

        private long start_time;
        private String device_name;
        private int status;
        private String feed_id;

        public long getStart_time() {
            return start_time;
        }

        public void setStart_time(long start_time) {
            this.start_time = start_time;
        }

        public String getDevice_name() {
            return device_name;
        }

        public void setDevice_name(String device_name) {
            this.device_name = device_name;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getFeed_id() {
            return feed_id;
        }

        public void setFeed_id(String feed_id) {
            this.feed_id = feed_id;
        }
    }
}
