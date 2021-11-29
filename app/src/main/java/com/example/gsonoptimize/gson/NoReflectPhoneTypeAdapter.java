package com.example.gsonoptimize.gson;

import android.util.Log;

import com.example.gsonoptimize.Person;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * 描述信息：
 *
 * @author xujiafeng
 * @date 2021/10/31
 */
class NoReflectPhoneTypeAdapter<T> extends TypeAdapter<T> {

    private Gson gson;

    public NoReflectPhoneTypeAdapter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        Person.Phone phone = (Person.Phone) value;
        out.beginObject();
        out.name("number").value(phone.number);
        out.endObject();
    }

    @Override
    public T read(JsonReader in) throws IOException {
        in.beginObject();
        String phone = null;
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "number":
                    phone = in.nextString();
                    break;
            }
        }
        in.endObject();
        Log.d(TAG, "invoke NoReflectPhoneTypeAdapter");
        return (T) new Person.Phone(phone);
    }

    public static final String TAG = "Test";
}
