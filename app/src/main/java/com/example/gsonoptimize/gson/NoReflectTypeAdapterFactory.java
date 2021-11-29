package com.example.gsonoptimize.gson;

import com.example.gsonoptimize.Person;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;

/**
 * 描述信息：
 *
 * @author xujiafeng
 * @date 2021/10/31
 */
public class NoReflectTypeAdapterFactory implements TypeAdapterFactory {


    private HashMap<Type, TypeAdapter> map;

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (map == null) {
            initMap(gson);
        }
        return map.get(type.getType());
    }

    private synchronized void initMap(Gson gson) {
        if (map == null) {
            map = new HashMap<>();
            map.put(Person.class, new NoReflectPersonTypeAdapter<>(gson));
            map.put(Person.Address.class, new NoReflectAddressTypeAdapter<>(gson));
            map.put(Person.Phone.class, new NoReflectPhoneTypeAdapter<>(gson));
        }
    }
}