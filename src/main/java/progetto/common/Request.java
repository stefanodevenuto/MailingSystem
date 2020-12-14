package progetto.common;

import java.io.Serializable;

public class Request implements Serializable {
    public static final int GET_FULL_MAILLIST = 0;
    public static final int UPDATE_MAILLIST = 1;
    public static final int SEND = 2;
    public static final int DELETE = 5;

    private String address;
    private int type;
    private int counter;
    private Mail body;

    public Request(int type, String address) {
        this.type = type;
        this.address = address;
        this.body = null;
        this.counter = -1;
    }

    public Request(int type, String address, Mail body) {
        this.type = type;
        this.address = address;
        this.body = body;
        this.counter = -1;
    }

    public Request(int type, String address, int counter){
        this.type = type;
        this.address = address;
        this.counter = counter;
        this.body = null;
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

    public int getCounter() {
        return counter;
    }

    @Override
    public String toString() {
        String text = null;

        switch (getType()) {
            case Request.GET_FULL_MAILLIST: {
                text = "FULL MAILLIST ";
                break;
            }

            case Request.UPDATE_MAILLIST: {
                text = "INCREMENTAL MAILLIST ";
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
