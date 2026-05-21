package com.sms.sopnopay;

public class YourResponseModel {
    private String status;
    private String message;

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return "1".equals(status); // Assuming "0" means success in your API
    }
}
