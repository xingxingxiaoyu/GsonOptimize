package com.example.gsonoptimize;

import android.util.Log;

import com.example.gsonoptimize_processor.NoReflectTypeAdapterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;

/**
 * 描述信息：
 *
 * @author xujiafeng
 * @date 2021/11/13
 */
class Test {
    public static final String TAG = "Test";

    public static void test() {
        ArrayList<Person.Phone> phoneList = new ArrayList<>();
        phoneList.add(new Person.Phone("1234"));
        phoneList.add(new Person.Phone("5678"));
        Person person = new Person("xuyu", 20, 175.0, true, new Person.Address("南山"), phoneList);
        String s = new Gson().toJson(person);
        Log.d(TAG, "main: " + s);
        GsonBuilder gsonBuilder = new GsonBuilder().registerTypeAdapterFactory(new NoReflectTypeAdapterFactory());
        Log.d(TAG, "onCreate: " + gsonBuilder.create().fromJson(s, Person.class));
    }

    public static void testTime() {
        ArrayList<Person.Phone> phoneList = new ArrayList<>();
        for (int i = 0; i < 1000000; i++) {
            phoneList.add(new Person.Phone("1234"));
            phoneList.add(new Person.Phone("5678"));
        }
        Person person = new Person("xuyu", 20, 175.0, true, new Person.Address("南山"), phoneList);
        String s = new Gson().toJson(person);
        {
            Gson gsonGood = new GsonBuilder().registerTypeAdapterFactory(new NoReflectTypeAdapterFactory()).create();
            long startTime = System.currentTimeMillis();
            gsonGood.fromJson(s, Person.class);
            Log.d(TAG, "testTime: take time good :" + (System.currentTimeMillis() - startTime));
        }
        {
            Gson gson = new Gson();
            long startTime = System.currentTimeMillis();
            gson.fromJson(s, Person.class);
            Log.d(TAG, "testTime: take time old  :" + (System.currentTimeMillis() - startTime));
        }
    }
}
