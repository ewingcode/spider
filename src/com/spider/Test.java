package com.spider;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {

    public static void main(String[] args) throws IllegalAccessException,
            InvocationTargetException, IOException {
        String regEx = "load\\(.+?\\)";
        String s = "$widgetkit.load('/wp-content/plugins/widgetkit/widgets/lightbox/js/lightbox.js').done(function(){jQuer";
        Pattern pat = Pattern.compile(regEx);
        Matcher mat = pat.matcher(s);
      
        while(mat.find()){
            System.out.println(mat.group());
        }
        
       
    }

}
