package progetto.common;

import java.io.Serializable;
import java.util.List;

public class Response implements Serializable {
    public static final int BAD_REQUEST = 0;
    public static final int ADDRESS_NOT_FOUND = 1;
    public static final int INTERNAL_ERROR = 2;
    public static final int OK = 3;

    private int code;
    private List<Mail> body;

    public Response(int code) {
        this.code = code;
        this.body = null;
    }

    public Response(int code, List<Mail> mailList) {
        this.code = code;
        this.body = mailList;
    }

    public int getCode() {
        return code;
    }

    public List<Mail> getBody() {
        return body;
    }
}
