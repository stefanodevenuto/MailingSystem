package progetto.common;

import java.io.Serializable;

public class Request implements Serializable {
    public static final int GET_MAILLIST = 0;
    public static final int REPLY = 1;
    public static final int REPLY_ALL = 2;
    public static final int FORWARD = 3;
    public static final int DELETE = 4;
    public static final int NEW = 5;

    private String address;
    private int type;
    private Mail body;

    public Request(int type, String address) {
        this.type = type;
        this.address = address;
        this.body = null;
    }

    public Request(int type, String address, Mail body) {
        this.type = type;
        this.address = address;
        this.body = body;
    }

    public int getType() {
        return type;
    }
    public String getAddress() {
        return address;
    }
    public Mail getBody() {
        return body;
    }

    // TODO: creare una stringa "stile log" per ogni type (saranno visualizzate nella ListView del server)
    @Override
    public String toString() {
        return super.toString();
    }
}
