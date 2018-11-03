package enums;

/**
 * 订阅状态
 * Created by zlk on 2018/10/22
 */
public enum SubscriptionState {
    ON("正在进行中"),
    END("已失效"),
    COMINGINTOFORCE("即将生效");

    String value;

    SubscriptionState(String value){
        this.value = value;
    }

    public String getValue(){
        return value;
    }
}
