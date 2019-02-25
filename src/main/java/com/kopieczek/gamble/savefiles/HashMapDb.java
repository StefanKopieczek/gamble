package com.kopieczek.gamble.savefiles;

import java.io.*;
import java.util.HashMap;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

public class HashMapDb implements SaveFileDb<String> {
    private HashMap<String, int[]> data = new HashMap<>();
    private File file;
    private final Timer saveTimer = new Timer(true);

    private HashMapDb(File dbFile) {
        this.file = dbFile;
        if (dbFile.exists()) {
            loadFromDb();
        }

        saveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                persistDb();
            }
        }, 0, 2000);
    }

    public static HashMapDb initialize(File dbFile) {
        return new HashMapDb(dbFile);
    }

    @Override
    public void put(String key, int[] ramData) {
        data.put(key, ramData);
    }

    @Override
    public Optional<int[]> get(String key) {
        return Optional.ofNullable(data.get(key));
    }

    private void persistDb() {
        try {
            FileOutputStream fout = new FileOutputStream(file);
            ObjectOutputStream oout = new ObjectOutputStream(fout);
            oout.writeObject(data);
            oout.close();
            fout.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadFromDb() {
        try {
            FileInputStream fin = new FileInputStream(file);
            ObjectInputStream oin = new ObjectInputStream(fin);
            data = (HashMap<String, int[]>) oin.readObject();
            oin.close();
            fin.close();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
