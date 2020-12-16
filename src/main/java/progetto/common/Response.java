package progetto.common;

import java.io.Serializable;
import java.util.List;

public class Response implements Serializable {
    public static final int BAD_REQUEST = 0;                            // Bad request response
    public static final int ADDRESS_NOT_FOUND = 1;                      // Address not found response
    public static final int INTERNAL_ERROR = 2;                         // Internal error response
    public static final int OK = 3;                                     // All ok response

    private int code;                                                   // Type of response from the above
    private List<Mail> body;                                            // List of mail in response to mail list request
    private String error;                                               // Descriptive error

    /**
     * New response only with type
     * @param code OK type, BAD REQUEST type
     */
    public Response(int code) {
        this.code = code;
        this.body = null;
        error = null;
    }

    /**
     * New response with type and the mail list
     * @param code OK type
     * @param mailList the current list of mails
     */
    public Response(int code, List<Mail> mailList) {
        this.code = code;
        this.body = mailList;
        error = null;
    }

    /**
     * New response with type and descriptive error
     * @param code ADDRESS NOT FOUND type
     * @param error the failed mail address
     */
    public Response(int code, String error){
        this.code = code;
        this.error = error;
    }

    // Getters
    public int getCode() { return code; }
    public List<Mail> getBody() { return body; }
    public String getError() { return error; }
}
