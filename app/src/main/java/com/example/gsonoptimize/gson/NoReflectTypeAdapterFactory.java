package com.example.gsonoptimize.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;

/**
 * 描述信息：
 *
 * @author xujiafeng
 * @date 2021/10/31
 */
class NoReflectTypeAdapterFactory implements TypeAdapterFactory {

    private HashMap<TypeToken,NoReflectTypeAdapter> map=new HashMap<>();
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        return map.get(type);
    }
}