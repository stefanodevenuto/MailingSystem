package progetto.common;

import java.io.Serializable;

public class Request implements Serializable {
    public static final int GET_FULL_MAILLIST = 0;
    public static final int UPDATE_MAILLIST = 1;
    public static final int SEND = 2;
    public static final int DELETE = 5;

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

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder();

            text.append(getAddress() != null ? getAddress() : "UNKNOWN");
            switch (getType()) {
                case Request.GET_FULL_MAILLIST: {
                    text.append(": Connection and FULL MAILLIST ");
                    break;
                }

                case Request.UPDATE_MAILLIST: {
                    text.append(": Connection and INCREMENTAL MAILLIST ");
                    break;
                }

                case Request.SEND: {
                    Mail m = getBody();
                    text.append(": SEND MAIL request to ");
                    for (String recipient : m.getRecipients()) {
                        text.append(recipient).append(", ");
                    }
                    text.deleteCharAt(text.length() - 1);
                    break;
                }

                case Request.DELETE: {
                    Mail m = getBody();
                    text.append(": DELETE MAIL request of MailID: ").append(m.getID());
                    break;
                }

                default: {
                    text.append(": BAD REQUEST");
                    break;
                }
            }

            return text.toString();

    }
}
