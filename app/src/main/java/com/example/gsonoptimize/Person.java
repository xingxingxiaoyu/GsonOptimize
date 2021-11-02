package com.example.gsonoptimize;

import com.example.gsonoptimize_annotation.GsonOptimize;

/**
 * 描述信息：
 *
 * @author xujiafeng
 * @date 2021/10/31
 */
@GsonOptimize
public class Person {
    public String name;
    public int age;
    public double height;
    public boolean isMan;
    public Address mAddress;

    public static class Address {
        public String detail;
    }
}
