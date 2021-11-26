package com.example.gsonoptimize.gson;

import android.util.Log;

import com.example.gsonoptimize.Person;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * 描述信息：
 *
 * @author xujiafeng
 * @date 2021/10/31
 */
class NoReflectPersonTypeAdapter<T> extends TypeAdapter<T> {

    private Gson gson;

    public NoReflectPersonTypeAdapter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        Person person = (Person) value;

        out.beginObject();
        out.name("name").value(person.name);
        out.name("age").value(person.age);
        out.name("height").value(person.height);
        out.name("isMan").value(person.isMan);
        out.name("mAddress");
        gson.getAdapter(new TypeToken<Person.Address>() {
        }).write(out, person.mAddress);
        out.name("mPhoneList");
        gson.getAdapter(new TypeToken<List<Person.Phone>>() {
        }).write(out, person.mPhoneList);
        out.endObject();
    }


    @Override
    public T read(JsonReader in) throws IOException {
        in.beginObject();
        String name = null;
        int age = 0;
        double height = 0;
        boolean isMan = false;
        Person.Address mAddress = null;
        List<Person.Phone> mPhoneList = null;
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "name":
                    name = in.nextString();
                    break;
                case "age":
                    age = in.nextInt();
                    break;
                case "height":
                    height = in.nextDouble();
                    break;
                case "isMan":
                    isMan = in.nextBoolean();
                    break;
                case "mAddress":
                    mAddress = gson.getAdapter(new TypeToken<Person.Address>() {
                    }).read(in);
                    break;
                case "mPhoneList":
                    mPhoneList = gson.getAdapter(new TypeToken<List<Person.Phone>>() {
                    }).read(in);
                    break;
            }
        }
        in.endObject();
        Log.d(TAG, "invoke NoReflectPersonTypeAdapter");
        return (T) new Person(name, age, height, isMan, mAddress, mPhoneList);
    }

    public static final String TAG = "Test";

}
