package com.example.myapplication;

public class ChatEntity {

    private int role;
    private String message;

    //one line of message is a chat entity
    //role=1 is user role=0 is bot
    public ChatEntity(int role,String message) {
        this.role = role;
        this.message  = message;
    }

    public int getRole() {
        return this.role;
    }

    public void setRole(int role) {
        this.role = role;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
