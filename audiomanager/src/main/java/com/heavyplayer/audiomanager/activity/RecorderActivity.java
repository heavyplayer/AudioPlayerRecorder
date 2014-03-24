package com.heavyplayer.audiomanager.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.heavyplayer.audiomanager.R;
import com.heavyplayer.audiomanager.fragment.AudioRecorderFragment;
import com.heavyplayer.audiomanager.obj.Item;

public class RecorderActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {
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

	protected TextView mTitleView;
	protected ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

	    mTitleView = (TextView)findViewById(android.R.id.title);

	    mListView = (ListView)findViewById(android.R.id.list);
	    mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
	    mListView.setAdapter(onCreateAdapter(this, sItems));
	    mListView.setOnItemClickListener(this);
	    // Select first item.
	    mListView.performItemClick(mListView.getChildAt(0), 0, mListView.getItemIdAtPosition(0));
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
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		final String fileName = ((Item)parent.getItemAtPosition(position)).getFileName();
		mTitleView.setText(fileName);
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
		final String fileName = getSelectedItem().getFileName();
		if(fileName != null)
			AudioRecorderFragment.createInstance(fileName)
					.show(getSupportFragmentManager(), AudioRecorderFragment.TAG);
	}

	protected Item getSelectedItem() {
		return (Item)mListView.getItemAtPosition(mListView.getCheckedItemPosition());
	}

	protected class ItemAdapter extends ArrayAdapter<Item> {
		public ItemAdapter(Context context, Item[] objects) {
			super(context, R.layout.home_list_item, R.id.item_title, objects);
		}
	}
}
