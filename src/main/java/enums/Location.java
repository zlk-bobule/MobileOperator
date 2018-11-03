package enums;

public enum Location {
    LOCAL("在本地"),
    DOMESTIC("不在本地");

    String value;

    Location(String value){
        this.value = value;
    }

    public String getValue(){
        return value;
    }
}
