package com.example.gsonoptimize.gson;

import com.example.gsonoptimize.Person;
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
public class NoReflectTypeAdapterFactory implements TypeAdapterFactory {


    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (Person.class.equals(type.getType())) {
            return new NoReflectPersonTypeAdapter<>(gson);
        }
        if (Person.Address.class.equals(type.getType())) {
            return new NoReflectAddressTypeAdapter<>(gson);
        }
        if (Person.Phone.class.equals(type.getType())) {
            return new NoReflectPhoneTypeAdapter<>(gson);
        }
        return null;
    }
}