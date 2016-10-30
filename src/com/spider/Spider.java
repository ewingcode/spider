package com.spider;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Spider
 */
public class Spider {

	private String website;
	private File localDir;
	private Set<String> RESOURCES = new HashSet<String>();
	private Set<String> LINKS = new HashSet<String>();
	private static Set<String> HISTORIES_LINKS = new HashSet<String>();
	private static Set<String> HISTORIES_RESOURCES = new HashSet<String>();
	private final String WEBINF_PATH = System.getProperty("user.dir")
			+ File.separator + "template" + File.separator + "WEB-INF";
	private int spiderDeep = 0;
	private final String REPLACE_MATCH = ".*\\.(jpg|png|bmp|gif|js|css|ico|cur)";

	public Spider(String website, String local, int spiderDeep) {
		this.website = website;
		this.localDir = new File(local);
		this.spiderDeep = spiderDeep;
		try {
			FileUtils.deleteDirectory(localDir);
			localDir.mkdirs();
			FileUtils.copyDirectoryToDirectory(new File(WEBINF_PATH), localDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void put2Resources(String key, String url) {
		if (StringUtils.isEmpty(url) || url.equals(website))
			return;
		if (HISTORIES_RESOURCES.contains(url))
			return;

		RESOURCES.add(url);
	}

	private void put2Links(String key, String url) {
		if (StringUtils.isEmpty(url))
			return;
		if (!url.startsWith(website))
			return;
		if (HISTORIES_LINKS.contains(url))
			return;
		if (url.matches(REPLACE_MATCH)) {
			put2Resources(key, url);
			return;
		}
		LINKS.add(url);
	}

	public void scan(String url, int deep) throws IOException {
		if (StringUtils.isEmpty(url))
			url = website;
		try {
			System.out.println("Fetching ..." + url);
			File file = getFileByUrl(url);

			if (file.exists()) {
				return;
			}

			Connection conn = Jsoup.connect(url);
			conn.timeout(120 * 1000);
			Document doc = conn.get();
			Elements links = doc.select("a[href]");
			Elements media = doc.select("[src]");
			Elements imports = doc.select("link[href]");

			print("\nMedia: (%d)", media.size());
			for (Element src : media) {
				if (src.tagName().equals("img")) {
					print(" * %s: <%s> %sx%s (%s)", src.tagName(),
							src.attr("abs:src"), src.attr("width"),
							src.attr("height"), trim(src.attr("alt"), 20));
				} else {
					print(" * %s: <%s>", src.tagName(), src.attr("abs:src"));
					print(" * %s: <%s>", src.tagName(),
							src.attr("abs:data-main"));
				}
				put2Resources(src.tagName(), src.attr("abs:src"));
				put2Resources(src.tagName(), src.attr("abs:data-main"));
				src.attr("src", replaceResourceLink(src.attr("abs:src")));
				src.attr("data-main",
						replaceResourceLink(src.attr("abs:data-main")));
			}

			print("\nImports: (%d)", imports.size());
			for (Element link : imports) {

				print(" * %s <%s> (%s)", link.tagName(), link.attr("abs:href"),
						link.attr("rel"));
				put2Resources(link.tagName(), link.attr("abs:href"));
				link.attr("href", replaceResourceLink(link.attr("abs:href")));
			}

			print("\nLinks: (%d)", links.size());
			for (Element link : links) {
				print(" * a: <%s>  (%s)", link.attr("abs:href"),
						trim(link.text(), 35));
				put2Links("a", link.attr("abs:href"));
				link.attr("href", appendHtmlSuffix(replaceResourceLink(link
						.attr("abs:href"))));

			}

			Receiver.writeFile(file, doc.toString());
			receiveResource();
			if (!StringUtils.isEmpty(doc.toString()))
				receiveChildLink(url, deep);
		} catch (Exception e) {
			System.err.println(e.getMessage() + " " + url);
			return;
		}

	}

	private String appendHtmlSuffix(String linkUrl) {
		String url = linkUrl.replace(website, "");
		if (url.equals("/") || url.isEmpty() || url.indexOf(".") > -1)
			return linkUrl;
		linkUrl = website + linkUrl.substring(linkUrl.lastIndexOf("/"));
		return linkUrl + ".html";
	}

	private String replaceLink(String url) { 
		url = appendHtmlSuffix(url);
		if (url.indexOf("?") > -1)
			url = url.substring(0, url.indexOf("?"));
		String urlResult = "";
		if (url.matches(REPLACE_MATCH)) {
			urlResult = url.replace(website, "")/* .replaceAll("\\?", "") */
			.replace(":", "").replace("//", "/").replace("#", "");
		} else {
			urlResult = url.replace(website, "").replaceAll("\\?", "")
			// .replace("/", "")
					.replace(":", "").replace("//", "/").replace("#", "");
		}
		return urlResult;
	}

	private String replaceResourceLink(String oldLink) {
		oldLink = replaceLink(oldLink);
		if (oldLink.startsWith("/"))
			oldLink = oldLink.replaceFirst("/", "");
		return oldLink;
	}

	private File getFileByUrl(String url) {
		String shorUrl = replaceLink(url);
		if (shorUrl.isEmpty())
			return new File(localDir.getAbsolutePath() + File.separator
					+ "index.html");
		File resourceFile = new File(localDir.getAbsolutePath()
				+ File.separator + shorUrl);
		return resourceFile;
	}

	private void receiveChildLink(String rootUrl, int deep) {
		if (deep == -1)
			return;
		if (LINKS.isEmpty())
			return;
		if (deep > spiderDeep)
			return;
		int curDeep = deep + 1;
		System.out.println("curDeep:" + curDeep);
		Set<String> tmpLinks = new HashSet<String>();
		CollectionUtils.addAll(tmpLinks, LINKS.toArray());
		for (String url : tmpLinks) {
			if (StringUtils.isEmpty(url) || url.equals(rootUrl))
				continue;
			if (HISTORIES_LINKS.contains(url))
				continue;
			try {
				HISTORIES_LINKS.add(url);
				scan(url, curDeep);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	private void receiveResource() {
		if (RESOURCES.isEmpty())
			return;
		CountDownLatch latch = new CountDownLatch(RESOURCES.size());
		for (String url : RESOURCES) {
			File resourceFile = getFileByUrl(url);
			try {
				System.out.println("receiving:" + url);
				HISTORIES_RESOURCES.add(url);
				new DownThread(latch, url, resourceFile).start();
			} catch (Exception e) {
				System.err.println("error in revceive " + url);
				e.printStackTrace();
			}
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		RESOURCES.clear();
	}

	class DownThread extends Thread {
		private CountDownLatch latch;
		private String url;
		private File resourceFile;

		public DownThread(CountDownLatch latch, String url, File resourceFile) {
			this.latch = latch;
			this.url = url;
			this.resourceFile = resourceFile;
		}

		public void run() {
			try {

				Receiver.downResource(url, resourceFile);
				// Read css contend and find url link.
				downCssUrl();
				downJsUrl();
			} catch (Exception e) {
				System.err.println("error in revceive " + url);
			} finally {
				latch.countDown();
			}
		}

		private void downJsUrl() {
			String regEx = "load\\(.+?\\)";
			if (url.endsWith(".js")) {
				String jsContent = null;
				try {
					jsContent = FileUtils.readFileToString(resourceFile);
					Pattern pat = Pattern.compile(regEx);
					Matcher mat = pat.matcher(jsContent);
					while (mat.find()) {
						try {
							String cssUrl = mat.group();
							if (!cssUrl.contains("js"))
								continue;
							cssUrl = cssUrl.substring(0, cssUrl.length() - 1)
									.replace("load(", "").replace("'", "");
							// cssUrl = website + cssUrl;
							File cssUrlFile = getFileByUrl(cssUrl);
							System.out.println("receive js url:" + cssUrl
									+ " cssUrlFile:"
									+ cssUrlFile.getAbsolutePath());
							Receiver.downResource(cssUrl, cssUrlFile);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					System.out.println("end receive js url for file " + url);
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("error in receive js url for file "
							+ jsContent);
				}
			}
		}

		private void downCssUrl() {
			String regEx = "url\\(.+?\\)";
			if (url.endsWith(".css")) {
				String cssContent = null;
				try {
					cssContent = FileUtils.readFileToString(resourceFile);
					Pattern pat = Pattern.compile(regEx);
					Matcher mat = pat.matcher(cssContent);
					while (mat.find()) {
						try {
							String cssUrl = mat.group();
							cssUrl = cssUrl.substring(0, cssUrl.length() - 1)
									.replace("url(", "");
							cssUrl = url.replace(resourceFile.getName(), "")
									+ cssUrl;
							cssUrl = cssUrl.replace("\"", "");
							File cssUrlFile = getFileByUrl(cssUrl);
							System.out.println("receive css url:" + cssUrl
									+ " cssUrlFile:"
									+ cssUrlFile.getAbsolutePath());

							Receiver.downResource(cssUrl, cssUrlFile);
							// ��@import
							// url(common.css);��ʱ����Ҫ�����ȡ���ݼ�������
							if (cssUrl.endsWith(".css")) {
								CountDownLatch latch = new CountDownLatch(1);
								File resourceFile = getFileByUrl(cssUrl);
								new DownThread(latch, cssUrl, resourceFile)
										.start();
								try {
									latch.await();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					System.out.println("end receive css url for file " + url);
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("error in receive css url for file "
							+ cssContent);
				}
			}
		}
	}

	private void print(String msg, Object... args) {
		System.out.println("scan result:" + String.format(msg, args));
	}

	private String trim(String s, int width) {
		if (s.length() > width)
			return s.substring(0, width - 1) + ".";
		else
			return s;
	}

	public static void main(String[] args) throws IOException {
		Spider spider = new Spider("https://www.weidian.com/",
				"D:\\ewing\\spiderweb\\weidian", 4);
		spider.scan(null, 0);
	}
}
