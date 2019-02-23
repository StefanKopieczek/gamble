package com.kopieczek.gamble.savefiles;

import java.io.*;
import java.util.HashMap;
import java.util.Optional;

public class HashMapDb implements SaveFileDb<String> {
    private static final long serialVersionUID = 1L;
    private HashMap<String, int[]> data = new HashMap<>();
    private File file;

    private HashMapDb(File dbFile) {
        this.file = dbFile;
        if (dbFile.exists()) {
            loadFromDb();
        }
    }

    public static HashMapDb initialize(File dbFile) {
        return new HashMapDb(dbFile);
    }

    @Override
    public void put(String key, int[] ramData) {
        data.put(key, ramData);
        persistDb();
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
            ObjectInputStream oin= new ObjectInputStream(fin);
            data = (HashMap<String, int[]>) oin.readObject();
            oin.close();
            fin.close();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
