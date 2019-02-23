package com.kopieczek.gamble.savefiles;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class TestHashMapDb {
    private static int[] ARRAY_1 = new int[] {-10, 3, 255};
    private static int[] ARRAY_2 = new int[] {100, 1, 2, 3, -255, 255, 19};

    @Test
    public void test_open_empty_file_succeeds() throws Exception {
        File file = getTempFile();
        HashMapDb.initialize(file);
    }

    @Test
    public void test_put_and_retrieve_one_array() throws Exception {
        HashMapDb db = getTempDb();
        int[] expected = new int[] {-10, 3, 255};
        db.put("foo", expected);
        int[] actual = db.get("foo").get();
        assertArrayEquals(expected, actual);
    }

    @Test
    public void test_retrieve_missing_key_results_in_empty_optional() throws Exception {
        HashMapDb db = getTempDb();
        assertEquals(Optional.empty(), db.get("key that doesn't exist"));
    }

    @Test
    public void test_put_and_retrieve_two_values() throws Exception {
        HashMapDb db = getTempDb();
        db.put("foo", ARRAY_1);
        db.put("bar", ARRAY_2);
        assertArrayEquals(ARRAY_1, db.get("foo").get());
        assertArrayEquals(ARRAY_2, db.get("bar").get());
    }

    @Test
    public void test_put_and_retrieve_two_values_in_reverse_order() throws Exception {
        HashMapDb db = getTempDb();
        db.put("foo", ARRAY_1);
        db.put("bar", ARRAY_2);
        assertArrayEquals(ARRAY_2, db.get("bar").get());
        assertArrayEquals(ARRAY_1, db.get("foo").get());
    }

    @Test
    public void test_overwrite_value() throws Exception {
        HashMapDb db = getTempDb();
        db.put("foo", ARRAY_1);
        db.put("foo", ARRAY_2);
        assertArrayEquals(ARRAY_2, db.get("foo").get());
    }

    @Test
    public void test_get_missing_key_after_adding_a_value() throws Exception {
        HashMapDb db = getTempDb();
        db.put("legit key", ARRAY_1);
        assertEquals(Optional.empty(), db.get("missing key"));
    }

    @Test
    public void test_and_reload_db_with_one_key() throws Exception {
        File dbFile = getTempFile();
        HashMapDb first = HashMapDb.initialize(dbFile);
        first.put("key", ARRAY_1);
        HashMapDb second = HashMapDb.initialize(dbFile);
        assertArrayEquals(ARRAY_1, second.get("key").get());
    }

    @Test
    public void test_and_reload_db_with_two_keys() throws Exception {
        File dbFile = getTempFile();
        HashMapDb first = HashMapDb.initialize(dbFile);
        first.put("foo", ARRAY_1);
        first.put("bar", ARRAY_2);
        HashMapDb second = HashMapDb.initialize(dbFile);
        assertArrayEquals(ARRAY_1, second.get("foo").get());
        assertArrayEquals(ARRAY_2, second.get("bar").get());
    }

    @Test
    public void test_missing_key_still_missing_after_reload() throws Exception {
        File dbFile = getTempFile();
        HashMapDb first = HashMapDb.initialize(dbFile);
        first.put("foo", ARRAY_1);
        HashMapDb second = HashMapDb.initialize(dbFile);
        assertEquals(Optional.empty(), second.get("bar"));
    }

    @Test
    public void test_reloading_different_path_doesnt_load_original_db() throws Exception {
        HashMapDb first = getTempDb();
        first.put("foo", ARRAY_1);
        HashMapDb second = getTempDb();
        assertEquals(Optional.empty(), second.get("foo"));
    }

    private static File getTempFile() throws IOException {
        File f = File.createTempFile("test-db", ".db");
        f.delete(); // DB creation expects a non-existent file
        f.deleteOnExit();
        return f;
    }

    private static HashMapDb getTempDb() throws IOException {
        return HashMapDb.initialize(getTempFile());
    }
}
