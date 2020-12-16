package progetto.common;

import java.io.Serializable;

public class Request implements Serializable {
    public static final int MAILLIST = 1;                           // Mail list request
    public static final int SEND = 2;                               // Send mail request
    public static final int DELETE = 3;                             // Delete mail request

    private final String address;                                   // Mail address of the requester user
    private final int type;                                         // Type of request from the previous ones
    private final int counter;                                      // Number of mail read, used with mail list request
    private final Mail body;                                        // Mail to be sent

    /**
     * New request with type and address
     * @param type DELETE request
     * @param address requester address
     */
    public Request(int type, String address) {
        this.type = type;
        this.address = address;
        this.body = null;
        this.counter = -1;
    }

    /**
     * New request with type, address and a body
     * @param type SEND request
     * @param address requester address
     * @param body mail to be sent
     */
    public Request(int type, String address, Mail body) {
        this.type = type;
        this.address = address;
        this.body = body;
        this.counter = -1;
    }

    /**
     * New request with type, address and a counter of read mail from the mail list
     * @param type MAILLIST request
     * @param address requester address
     * @param counter number of read mail
     */
    public Request(int type, String address, int counter){
        this.type = type;
        this.address = address;
        this.counter = counter;
        this.body = null;
    }

    // Getters of all fields
    public int getType() { return type; }
    public String getAddress() { return address; }
    public Mail getBody() { return body; }
    public int getCounter() { return counter; }

    @Override
    public String toString() {
        String text;

        switch (getType()) {
            case Request.MAILLIST: {
                text = "MAILLIST ";
                break;
            }

            case Request.SEND: {
                Mail m = getBody();
                text = "SEND MAIL to ";
                StringBuilder recipients = new StringBuilder();
                for (String recipient : m.getRecipients()) {
                    recipients.append(recipient).append(", ");
                }
                recipients.deleteCharAt(recipients.length() - 1);
                text += recipients;
                break;
            }

            case Request.DELETE: {
                Mail m = getBody();
                text = "DELETE MAIL with MailID: " + m.getID();
                break;
            }

            default: {
                text = "BAD REQUEST";
                break;
            }
        }

        return text;
    }
}
