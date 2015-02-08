/**
 * 
 */
package com.spider;

/**
 * @author Administrator
 *
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class Receiver {

	/**
	 * Download resource by given url and name the resource as given file name.
	 * 
	 * @param url
	 * @param resourceFile
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static void downResource(String url, File resourceFile)
			throws ClientProtocolException, IOException {
		resourceFile.getParentFile().mkdirs();
		HttpClient httpclient = new DefaultHttpClient();
		// 创建Get方法实例
		HttpGet httpgets = new HttpGet(url);
		HttpResponse response = httpclient.execute(httpgets);
		HttpEntity entity = response.getEntity();
		if (entity != null) {
			InputStream instreams = entity.getContent();
			// String str = convertStreamToString(instreams);
			convertStreamToFile(instreams, resourceFile);
			// FileUtils.writeStringToFile(resourceFile, str);
			httpgets.abort();
		} 
	}

	public static void convertStreamToFile(InputStream is, File outFile)
			throws IOException {
		byte[] b = new byte[10240000];
		FileOutputStream outs = new FileOutputStream(outFile);
		try {
			int size = 0;
			while ((size = is.read(b)) != -1) {
				outs.write(b, 0, size);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (outs != null)
				outs.close();
		}

	}

	private static String convertStreamToString(InputStream is) {

		BufferedReader reader = new BufferedReader(new InputStreamReader(is));

		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	public static void writeFile(File f, String fileContent) {
		try {
			if (!f.exists()) {
				f.createNewFile();
			} 
			OutputStreamWriter write = new OutputStreamWriter(
					new FileOutputStream(f), "UTF-8");
			BufferedWriter writer = new BufferedWriter(write);
			writer.write(fileContent);
			writer.close();
		} catch (Exception e) {
			System.out.println("写文件内容操作出错");
			e.printStackTrace();
		}
	}

}
