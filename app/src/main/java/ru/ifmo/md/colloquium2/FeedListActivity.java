package ru.ifmo.md.lesson7;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class FeedListActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {
    ListView listView;
    EditText editText;
    SimpleCursorAdapter adapter;
    private ServiceResultReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.feed_list_activity);

        listView = (ListView) findViewById(R.id.listView);
        editText = (EditText) findViewById(R.id.editText);

        receiver = new ServiceResultReceiver(new Handler(), this);

        String[] from = new String[]{"title", "url"};
        int[] to = new int[]{R.id.list_item_id};
        adapter = new SimpleCursorAdapter(this, R.layout.list_item, null, from, to, 0);

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (columnIndex == 1) {
                    ((TextView) view).setText(cursor.getString(1));
                    return true;
                }
                return false;
            }
        });

        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
                Cursor c = adapter.getCursor();
                c.moveToPosition(pos);
                String url = c.getString(c.getColumnIndexOrThrow("url"));
                String title = c.getString(c.getColumnIndexOrThrow("title"));
                Intent intent = new Intent(getBaseContext(), PostListActivity.class);
                intent.putExtra("feed_id", id);
                intent.putExtra("title", title);
                intent.putExtra("url", url);
                startActivity(intent);
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int pos, long id) {
                Uri uri = Uri.parse(FeedContentProvider.CONTENT_URI_FEEDS.toString() + "/" + id);
                getContentResolver().delete(uri, null, null);
                return true;
            }
        });

        getLoaderManager().initLoader(0, null, this);
    }

    public void onAddClick(View view) {
        String url = editText.getText().toString();

        // some abbrs to make testing easier:
        if (url.equals("so")) url = "http://stackoverflow.com/feeds/tag/android";
        else if (url.equals("bbc")) url = "http://feeds.bbci.co.uk/news/rss.xml";
        else if (url.equals("echo")) url = "http://echo.msk.ru/interview/rss-fulltext.xml";

        Intent intent = new Intent(this, LoadFeedService.class);
        intent.putExtra("url", url);
        intent.putExtra("feed_id", -1L);
        intent.putExtra("receiver", receiver);
        startService(intent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = {"_id", "title", "url"};
        return new CursorLoader(this, FeedContentProvider.CONTENT_URI_FEEDS, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        adapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        adapter.swapCursor(null);
    }
}
