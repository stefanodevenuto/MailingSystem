package progetto.server;

import progetto.common.Request;
import progetto.common.Response;

public class Log {
    Request request;
    Response response;


    public Log(Request request){
        this.request = request;
        this.response = null;
    }

    public Log(Response response, Request request){
        this.request = request;
        this.response = response;
    }

    public String logText(){
        if(response == null){
            return request.toString();
        } else {
            StringBuilder text = new StringBuilder();
            switch (response.getCode()){
                case Response.OK:{
                    text.append("SUCCESSFULL: ").append(request.toString());
                    break;
                }

                case Response.ADDRESS_NOT_FOUND:{
                    text.append("ADDRESS NOT FOUND: ").append(request.toString());
                    break;
                }

                default:{
                    text.append("BAD REQUEST: ").append(request.toString());
                }
            }

            return text.toString();
        }
    }
}
