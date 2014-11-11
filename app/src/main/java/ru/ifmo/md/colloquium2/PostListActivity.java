package ru.ifmo.md.lesson7;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class PostListActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    SimpleCursorAdapter adapter;
    Uri uri;
    String url;
    long feedId;
    private ServiceResultReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        feedId = intent.getLongExtra("feed_id", 0);
        String title = intent.getStringExtra("title");
        url = intent.getStringExtra("url");

        receiver = new ServiceResultReceiver(new Handler(), this);

        uri = Uri.withAppendedPath(FeedContentProvider.CONTENT_URI_POSTS, "" + feedId);
        setTitle(title);

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

        this.setListAdapter(adapter);

        this.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
                Cursor c = adapter.getCursor();
                c.moveToPosition(pos);
                String url = c.getString(c.getColumnIndexOrThrow("url"));
                String title = c.getString(c.getColumnIndexOrThrow("title"));
                Intent intent = new Intent(getBaseContext(), PostActivity.class);
                intent.putExtra("url", url);
                intent.putExtra("title", title);
                startActivity(intent);
            }
        });

        getLoaderManager().initLoader(1, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = {"_id", "title", "url"};
        return new CursorLoader(this, uri, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        adapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        adapter.swapCursor(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.post_list_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                Intent intent = new Intent(this, LoadFeedService.class);
                intent.putExtra("url", url);
                intent.putExtra("feed_id", feedId);
                intent.putExtra("receiver", receiver);
                startService(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
