package com.dict.hm.dictionary.parse;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.JsonWriter;
import android.util.Log;

import com.dict.hm.dictionary.BaseManagerActivity;

import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TreeMap;

/**
 * Created by hm on 15-3-3.
 */
public class PaperParser extends Thread{
    public static final int TXT = 0;
    public static final int HTML = 1;
    public static final int URL = 2;
    public static final String INFO = "info";
    HashMap<String, Integer> hashMap;
    File in;
    File out;
    String url;
    String charset;
    Handler handler;
    int type;

    public PaperParser(File in, File out, String charset, int type, Handler handler) {
        this.in = in;
        this.out = out;
        this.charset = charset;
        this.type = type;
        this.handler = handler;
    }

    public PaperParser(String url, File out, Handler handler) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            this.url = url;
        } else {
            this.url = "http://" + url;
        }
        this.out = out;
        this.type = URL;
        this.handler = handler;
    }

    @Override
    public void run() {
        hashMap = new HashMap<>();
        boolean success = false;
        String text = null;
        switch (type) {
            case TXT:
                success = parseText(in, charset);
                break;
            case HTML:
                text = parseHtml(in, charset);
                success = parseText(text);
                break;
            case URL:
                if (url == null) {
                    success = false;
                } else {
                    text = parseURL(url);
                    success = parseText(text);
                }
                break;
        }

        if (success && out != null) {
            TreeMap<String, Integer> sortedMap = sort(hashMap);
            storePaper(sortedMap);
            sendMessage(text, BaseManagerActivity.OK);
        } else {
            sendMessage(null, BaseManagerActivity.ERR);
        }
    }

    private void sendMessage(String text, int code) {
        Message message = handler.obtainMessage(code);
        message.obj = out.getName();
        message.arg1 = HTML;
        if (text != null) {
            Bundle bundle = new Bundle();
            bundle.putString(INFO, text);
            message.setData(bundle);
        }
        handler.sendMessage(message);
    }

    private boolean parseText(File txt, String charset) {
        Scanner scanner;
        try {
            scanner = new Scanner(txt, charset);
            scanner.useDelimiter("[^a-zA-Z]+");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return parseText(scanner);
    }

    private boolean parseText(String txt) {
        if (txt == null) {
            return false;
        }
        Scanner scanner;
        scanner = new Scanner(txt);
        scanner.useDelimiter("[^a-zA-Z]+");
        return parseText(scanner);
    }

    private boolean parseText(Scanner scanner) {
        while (scanner.hasNext()) {
            String s = scanner.next();
            s = s.toLowerCase();
            if (hashMap.containsKey(s)) {
                Integer integer = hashMap.get(s);
                hashMap.put(s, integer + 1);
            } else {
                hashMap.put(s, 1);
            }
        }
        scanner.close();
        return true;
    }

    private String parseHtml(File html, String charset) {
        String text = null;
        try {
            Document doc = Jsoup.parse(html, charset);
            FormattingVisitor formattingVisitor = new FormattingVisitor();
            NodeTraversor nodeTraversor = new NodeTraversor(formattingVisitor);
            nodeTraversor.traverse(doc);
            text = formattingVisitor.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text;
    }

    private String parseURL(String url) {
        String text;
        try {
            Document doc = Jsoup.connect(url).timeout(5000).get();
            FormattingVisitor formattingVisitor = new FormattingVisitor();
            NodeTraversor nodeTraversor = new NodeTraversor(formattingVisitor);
            nodeTraversor.traverse(doc);
            text = formattingVisitor.toString();
        } catch (IOException e) {
            e.printStackTrace();
            text = null;
        }
        return text;
    }

    private void storePaper(TreeMap<String, Integer> sortedMap) {
        if (out == null || out.isDirectory()) {
            return;
        }
        JsonWriter jsonWriter = null;
        try {
            out.createNewFile();
            jsonWriter = new JsonWriter(new FileWriter(out));
            jsonWriter.beginObject();
            for (TreeMap.Entry<String, Integer> entry : sortedMap.entrySet()) {
                jsonWriter.name(entry.getKey())
                        .value(entry.getValue());
            }
            jsonWriter.endObject();
            jsonWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (jsonWriter != null) {
                    jsonWriter.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private TreeMap<String, Integer> sort(HashMap<String, Integer> hashMap) {
        TreeMap<String, Integer> treeMap;
        treeMap = new TreeMap<>(new ValueComparator(hashMap));
        treeMap.putAll(hashMap);
        return treeMap;
    }

    private class ValueComparator implements Comparator<String> {
        HashMap<String, Integer> hashMap;

        private ValueComparator(HashMap<String, Integer> hashMap) {
            this.hashMap = hashMap;
        }

        @Override
        public int compare(String lhs, String rhs) {
            if (hashMap.get(lhs) >= hashMap.get(rhs)) {
                return -1;
            }
            return 1;
            //return 0 would merge keys
        }
    }

    private class FormattingVisitor implements NodeVisitor {
        private static final int maxWidth = 80;
        private int width = 0;
        private StringBuilder builder = new StringBuilder();
        @Override
        public void head(Node node, int i) {
            String name = node.nodeName();
            if (node instanceof TextNode) {
                append(((TextNode) node).text());
            } else if (name.equals("li")) {
                append("\n * ");
            }
        }

        @Override
        public void tail(Node node, int i) {
            String name = node.nodeName();
            if (name.equals("br")) {
                append("\n");
            } else if (StringUtil.in(name, "p")) {
                append("\n\n");
//            } else if (name.equals("a")) {
//                append(String.format(" <%s>", node.absUrl("href")));
            }
        }

        private void append(String text) {
            if (text.startsWith("\n")) {
                width = 0;
            }

            if (text.equals(" ") && (builder.length() ==0 ||
                    StringUtil.in(builder.substring(builder.length() -1), " ", "\n"))) {
                return;
            }
            if (text.length() + width > maxWidth) {
                String words[] = text.split("\\s+");
                for (int i = 0; i < words.length; i++) {
                    String word = words[i];
                    boolean last = i == words.length -1;
                    if (!last) {
                        word = word + " ";
                    }
                    if (word.length() + width > maxWidth) {
                        builder.append("\n").append(word);
                        width = word.length();
                    } else {
                        builder.append(word);
                        width = word.length();
                    }
                }
            } else {
                builder.append(text);
                width += text.length();
            }
        }

        @Override
        public String toString() {
            return builder.toString();
        }
    }
}
