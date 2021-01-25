package com.example.myapplication.bean;

import com.google.gson.Gson;

import java.util.List;

public class Data {

    private Gson gson = new Gson();

    /**
     * method : scene.msg
     * body : [{"start_time":1600914856960,"record_id":"915DCBB9C8AD0E1E","scene_result":0,"user_id":"159333305427873213","msg_content":"\"睡眠模式\"场景已执行","logic_id":"l0000002","trigger_type":1,"end_time":1600914857485,"msgTime":1600914857485,"script_id":"BF4A91D97F15B1CC5F48A155492C7138","international_msg_content":"Scene \"睡眠模式\" successfully performed","device_result":[{"start_time":1600914857005,"device_name":"航嘉插座","status":0,"feed_id":"764781593482730317"}]}]
     */

    private String method;
    private String body;
    private List<Body> mBody;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public List<Body> getmBody() {
        return mBody;
    }

    public void setmBody(List<Body> mBody) {
        this.mBody = mBody;
    }
}
