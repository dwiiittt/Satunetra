package com.example.satunetra_v3.helper;

import android.content.Context;

import com.example.satunetra_v3.local.AppDatabase;
import com.example.satunetra_v3.local.table.ConsulEntity;
import com.example.satunetra_v3.local.table.UserEntity;


public class RoomHelper {
    private final AppDatabase roomDb;
    private boolean status;

    public RoomHelper(Context context){
        roomDb = AppDatabase.getInstance(context);
    }

    //read user
    public UserEntity readUser(){
        return roomDb.userDao().getUser();
    }

    public void firstTake(int id){
        roomDb.userDao().firstTake(true, id).subscribe(()->{
            status = true;
        }, throwable -> {
            status = false;
        });
    }

    public String readHistory(){
        StringBuilder temp = new StringBuilder("Berikut Riwayat Konsultasi Anda : \n");
        for(ConsulEntity riwayat : roomDb.userDao().getHistory()){
            temp.append(riwayat.getDate()).append(": ");
            temp.append("Anda Merasa ").append(riwayat.getFeel());
            temp.append(" Dan Kami memutarkan Anda ").append(riwayat.getInstruction()).append(" .\n");
        }
        temp.append("Untuk melanjutkan sesi obrolan usap layar dari atas ke bawah, dan untuk keluar dari aplikasi usap layar dari kiri ke kanan");
        return temp.toString();
    }

    public boolean isUserExist(){
        return roomDb.userDao().isUserExist();
    }

    public void createUser(int id, String name) {
        UserEntity user = new UserEntity(id, name);
        roomDb.userDao().createUser(user).subscribe(() -> {
            status = true;
        }, throwable -> {
            status = false;

        });
    }

    public void insertConsul(String instruction, String feel, String date){
        ConsulEntity consul = new ConsulEntity(instruction,feel, date);
        roomDb.userDao().addConsul(consul).subscribe(() -> {
            status = true;
        }, throwable -> {
            status = false;
        });
    }
}
