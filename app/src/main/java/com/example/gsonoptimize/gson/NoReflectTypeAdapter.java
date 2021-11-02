package com.example.gsonoptimize.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.HashMap;

/**
 * 描述信息：
 *
 * @author xujiafeng
 * @date 2021/10/31
 */
class NoReflectTypeAdapter extends TypeAdapter {

    @Override
    public void write(JsonWriter out, Object value) throws IOException {

    }

    @Override
    public Object read(JsonReader in) throws IOException {
        return null;
    }
}
