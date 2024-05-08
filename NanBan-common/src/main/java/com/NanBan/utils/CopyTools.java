package com.NanBan.utils;

import com.fasterxml.jackson.databind.util.BeanUtil;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

public class CopyTools {
    public static <T, S> List<T> copyList(List<S> sList, Class<T> clazz) {
        List<T> list = new ArrayList<>();
        for (S s : sList){
            T t = null;
            try {
                t = clazz.newInstance();
            }catch (Exception e){
                e.printStackTrace();
            }
            BeanUtils.copyProperties(s, t);
            list.add(t);
        }
        return list;
    }

    public static <T, S>T copy(S s, Class<T>clazz){
        T t = null;
        try{
            t = clazz.newInstance();
        }catch (Exception e){
            e.printStackTrace();
        }
        BeanUtils.copyProperties(s, t);
        return t;
    }
}
