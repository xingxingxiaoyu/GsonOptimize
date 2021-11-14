package com.example.gsonoptimize;

import android.util.Log;

import com.example.gsonoptimize_annotation.GsonOptimize;

import java.util.List;

/**
 * 描述信息：
 *
 * @author xujiafeng
 * @date 2021/10/31
 */
@GsonOptimize
public class Person {
    public static final String TAG = "Person";
    public String name;
    public int age;
    public double height;
    public boolean isMan;
    public Address mAddress;
    public List<Phone> mPhoneList;

    public static class Address {
        public String detail;

        public Address(String detail) {
            Log.d(TAG, "Address: ");
            this.detail = detail;
        }

        @Override
        public String toString() {
            return "Address{" +
                    "detail='" + detail + '\'' +
                    '}';
        }
    }

    public static class Phone {
        public String number;

        public Phone(String number) {
            Log.d(TAG, "Phone: ");
            this.number = number;
        }

        @Override
        public String toString() {
            return "Phone{" +
                    "number='" + number + '\'' +
                    '}';
        }
    }

    public Person(String name, int age, double height, boolean isMan, Address address, List<Phone> phoneList) {
        this.name = name;
        this.age = age;
        this.height = height;
        this.isMan = isMan;
        mAddress = address;
        mPhoneList = phoneList;
        Log.d(TAG, "Person: ");
    }

    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", height=" + height +
                ", isMan=" + isMan +
                ", mAddress=" + mAddress +
                ", mPhoneList=" + mPhoneList +
                '}';
    }
}
