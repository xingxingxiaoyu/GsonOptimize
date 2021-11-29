package com.example.gsonoptimize.gson;

import android.util.Log;

import com.example.gsonoptimize.Person;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 描述信息：
 *
 * @author xujiafeng
 * @date 2021/10/31
 */
class NoReflectAddressTypeAdapter<T> extends TypeAdapter<T> {

    private Gson gson;

    public NoReflectAddressTypeAdapter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        Person.Address address = (Person.Address) value;
        out.beginObject();
        out.name("detail").value(address.detail);
        out.endObject();
    }

    @Override
    public T read(JsonReader in) throws IOException {
        in.beginObject();
        String detail = null;
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "detail":
                    detail = in.nextString();
                    break;
            }
        }
        in.endObject();
        Log.d(TAG, "invoke NoReflectAddressTypeAdapter");
        return (T) new Person.Address(detail);
    }

    public static final String TAG = "Test";

}
