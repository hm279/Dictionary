package com.dict.hm.dictionary.paper;

import android.util.Log;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by hm on 15-3-3.
 */
public class PaperParser{
    public static final int TXT = 0;
    public static final int HTML = 1;
    public static final int URL = 2;

    HashMap<String, Integer> hashMap;
    File in;
    String url;
    String charset;
    int type;
    int error;

    public PaperParser(File in, String charset, int type) {
        this.in = in;
        this.charset = charset;
        this.type = type;
    }

    public PaperParser(String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            this.url = url;
        } else {
            this.url = "http://" + url;
        }
        this.type = URL;
    }

    public ArrayList<JsonEntry> parse() {
        hashMap = new HashMap<>();
        boolean success = false;
        String text;
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

        if (success) {
            return sort(hashMap);
        }
        return null;
    }

    public int getError() {
        return error;
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
            Log.d("parseURL", "return parsed doc");
            FormattingVisitor formattingVisitor = new FormattingVisitor();
            NodeTraversor nodeTraversor = new NodeTraversor(formattingVisitor);
            nodeTraversor.traverse(doc);
            text = formattingVisitor.toString();
        } catch (IOException e) {
            error = PaperErrorCode.ERR_NET;
            e.printStackTrace();
            text = null;
        }
        return text;
    }

    private ArrayList<JsonEntry> sort(HashMap<String, Integer> hashMap) {
        ArrayList<JsonEntry> arrayList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : hashMap.entrySet()) {
            arrayList.add(new JsonEntry(entry.getKey(), entry.getValue()));
        }
        Collections.sort(arrayList, new Comparator<JsonEntry>() {
            @Override
            public int compare(JsonEntry lhs, JsonEntry rhs) {
                if (lhs.getCount() > rhs.getCount()) {
                    return -1;
                } else if (lhs.getCount() < rhs.getCount()) {
                    return 1;
                }
                return 0;
            }
        });
        return arrayList;
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
