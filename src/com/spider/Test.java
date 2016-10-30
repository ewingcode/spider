package com.spider;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {
	private static final String REPLACE_MATCH = ".*\\.(jpg|png|bmp|gif|js|css|ico|cur)";

	public static void main(String[] args) throws IllegalAccessException,
			InvocationTargetException, IOException {
		String url = "https://www.weidian.com/vdian_index/css/common.css?v=44";
		System.out.println(url.matches(REPLACE_MATCH));

	}

}
