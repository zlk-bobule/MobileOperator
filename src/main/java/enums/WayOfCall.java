package enums;

public enum WayOfCall {
    DIAL("拨打"),
    ANSWER("接听");

    String value;

    WayOfCall(String value){
        this.value = value;
    }

    public String getValue(){
        return value;
    }
}
