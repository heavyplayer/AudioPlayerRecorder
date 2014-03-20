package com.audiomanager.app;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.audiomanager.fragment.AudioRecorderFragment;
import com.audiomanager.obj.Item;


public class HomeActivity extends ActionBarActivity {
	private static Item[] sItems = {
			new Item(0),
			new Item(1),
			new Item(2),
			new Item(3),
			new Item(4),
			new Item(5),
			new Item(6),
			new Item(7),
			new Item(8),
			new Item(9),
			new Item(11),
			new Item(12),
			new Item(13),
			new Item(14),
	};

	protected ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

	    mListView = (ListView)findViewById(android.R.id.list);
	    mListView.setAdapter(onCreateAdapter(this, sItems));
    }

	protected ListAdapter onCreateAdapter(Context context, Item[] objects) {
		return new ItemAdapter(context, objects);
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

	public void onRecord(View v) {
		new AudioRecorderFragment().show(getSupportFragmentManager(), AudioRecorderFragment.TAG);
	}

	protected class ItemAdapter extends ArrayAdapter<Item> {
		public ItemAdapter(Context context, Item[] objects) {
			super(context, R.layout.home_list_item, R.id.item_title, objects);
		}
	}
}
