package enums;

/**
 * 退订方式
 * Created by zlk on 2018/10/22
 */
public enum WayOfUnsubscribe {
    EFFECTIMMEDIATELY("立即生效"),
    EFFECTNEXTMONTH("次月生效");

    String value;

    WayOfUnsubscribe(String value){
        this.value = value;
    }

    public String getValue(){
        return value;
    }
}
