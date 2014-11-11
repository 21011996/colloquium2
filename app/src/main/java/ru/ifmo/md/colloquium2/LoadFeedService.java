package ru.ifmo.md.lesson7;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class LoadFeedService extends IntentService {
    public static final int ERROR_FEED_EXISTS = 1;
    public static final int ERROR_EXCEPTION = 2;
    private long feedId;
    private String feedUrl;
    private String feedTitle;
    private Uri feedUri;
    private Uri postsUri;
    private ResultReceiver reciever;

    public LoadFeedService() {
        super("LoadFeedService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        feedUrl = intent.getStringExtra("url");
        feedId = intent.getLongExtra("feed_id", -1);
        reciever = intent.getParcelableExtra("receiver");

        String escapedFeedUrl = DatabaseUtils.sqlEscapeString(feedUrl);

        if (feedId == -1) {
            Cursor c = getContentResolver().query(
                    FeedContentProvider.CONTENT_URI_FEEDS,
                    null,
                    "url=" + escapedFeedUrl,
                    null,
                    null
            );
            if (c.getCount() != 0) {
                reciever.send(ERROR_FEED_EXISTS, Bundle.EMPTY);
                return;
            }

            ContentValues values = new ContentValues();
            values.put("title", feedUrl); // use url as a title until we load one
            values.put("url", feedUrl);
            feedTitle = feedUrl;
            feedUri = getContentResolver().insert(FeedContentProvider.CONTENT_URI_FEEDS, values);
            feedId = Long.parseLong(feedUri.getLastPathSegment());
        } else {
            feedUri = Uri.withAppendedPath(FeedContentProvider.CONTENT_URI_FEEDS, "" + feedId);
            Cursor c = getContentResolver().query(feedUri, new String[]{"title"}, null, null, null);
            c.moveToFirst();
            feedTitle = c.getString(c.getColumnIndexOrThrow("title"));
            c.close();
        }

        postsUri = Uri.withAppendedPath(FeedContentProvider.CONTENT_URI_POSTS, "" + feedId);

        try {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            SAXParser saxParser = saxParserFactory.newSAXParser();
            XMLReader xmlReader = saxParser.getXMLReader();
            RSSHandler rssHandler = new RSSHandler();
            xmlReader.setContentHandler(rssHandler);
            InputSource inputSource = new InputSource(new URL(feedUrl).openStream());
            xmlReader.parse(inputSource);
        } catch (MalformedURLException e) {
            ReportException(e.toString());
        } catch (ParserConfigurationException e) {
            ReportException(e.toString());
        } catch (SAXException e) {
            ReportException(e.toString());
        } catch (IOException e) {
            ReportException(e.toString());
        }
    }

    private void ReportException(String s) {
        Bundle b = new Bundle();
        b.putString("error", s);
        reciever.send(ERROR_EXCEPTION, b);
    }

    private class RSSHandler extends DefaultHandler {

        private boolean saveText;
        private String currentText = "";
        private ContentValues currentValues;

        @Override
        public void startDocument() {
            getContentResolver().delete(postsUri, null, null);
        }


        @Override
        public void startElement(String string, String localName, String qName, Attributes attrs) throws SAXException {
            if (qName.equals("item") || qName.equals("entry")) {
                currentValues = new ContentValues();
            } else if (qName.equals("title") || qName.equals("link")) {
                saveText = true;
                currentText = "";
            }
            if (currentValues != null && qName.equals("link")) {
                currentValues.put("url", attrs.getValue("href"));
            }
        }

        @Override
        public void endElement(String string, String localName, String qName) throws SAXException {
            saveText = false;
            if (currentValues != null) {
                if (localName.equals("title")) {
                    currentValues.put("title", currentText.trim());
                } else if (localName.equals("link") && !currentText.isEmpty()) {
                    currentValues.put("url", currentText);
                } else if (localName.equals("item") || localName.equals("entry")) {
                    getContentResolver().insert(postsUri, currentValues);
                    currentValues = null;
                }
            } else if (localName.equals("title")) {
                String newTitle = currentText.trim();
                if (!newTitle.equals(feedTitle)) {
                    ContentValues values = new ContentValues();
                    values.put("title", newTitle);
                    getContentResolver().update(feedUri, values, null, null);
                }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            String strCharacters = new String(ch, start, length);
            if (saveText) currentText += strCharacters;
        }
    }
}
